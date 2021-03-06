/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 */

package it.tecnimed.covidpasscanner.Fragment

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.client.android.BeepManager
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.*
import it.tecnimed.covidpasscanner.VL.VLTimer
import it.tecnimed.covidpasscanner.databinding.FragmentUserdataReaderBinding
import it.tecnimed.covidpasscanner.model.VerificationViewModel
import java.lang.ClassCastException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class UserDataReaderFragment : Fragment(), View.OnClickListener, VLTimer.OnTimeElapsedListener {

    private val viewModel by viewModels<VerificationViewModel>()

    private var _binding: FragmentUserdataReaderBinding? = null
    private val binding get() = _binding!!

    private var mFirstName : String? = ""
    private var mLastName : String? = ""
    private var mFirstNameFounded: Boolean? = false
    private var mLastNameFounded: Boolean? = false

    private lateinit var beepManager: BeepManager

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private var mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var mTimeVar: VLTimer

    private var mListener: OnFragmentInteractionListener? = null
    /**
     * Here we define the methods that we can fire off
     * in our parent Activity once something has changed
     * within the fragment.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteractionUserDataReader(UserDataFound: Boolean)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(requireActivity())
        mListener = try {
            activity as UserDataReaderFragment.OnFragmentInteractionListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                activity.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
    }
    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mFirstName = it.getString("FIRSTNAMESTR")
            mLastName = it.getString("LASTNAMESTR")
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        mTimeVar = VLTimer.create(this)
        mTimeVar.startSingle(10000)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserdataReaderBinding.inflate(inflater, container, false)

//        binding.backImage2.setOnClickListener(this)
//        binding.backText2.setOnClickListener(this)
        binding.backImage2.visibility = View.INVISIBLE;
        binding.backText2.visibility = View.INVISIBLE;

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        beepManager = BeepManager(requireActivity())
        startCamera()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_image2 -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionUserDataReader(false)
                }
            }
            R.id.back_text2 -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionUserDataReader(false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        cameraProvider.unbindAll()
        _binding = null
    }

    override fun VLTimerTimeElapsed(timer: VLTimer) {
        if (timer === mTimeVar) {
            if (mListener != null) {
                mListener!!.onFragmentInteractionUserDataReader(false)
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment BlankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UserDataReaderFragment().apply {
                arguments = Bundle().apply {
                    putString("FIRSTNAMESTR", param1)
                    putString("LASTNAMESTR", param2)
                }
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    binding.previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // Image Analysis
            val imageAnalysis = ImageAnalysis.Builder()
                // enable the following line if RGBA output is needed.
                // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(Size(256, 192))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                        val mediaImage = imageProxy.image;
                        val mediaImageRotationDegrees = imageProxy.imageInfo.rotationDegrees
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, mediaImageRotationDegrees)
                            // Pass image to an ML Kit Vision API
                            val result = textRecognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    // Task completed successfully
                                    val resultText = visionText.text
                                    for (block in visionText.textBlocks) {
                                        val blockText = block.text
                                        val blockCornerPoints = block.cornerPoints
                                        val blockFrame = block.boundingBox
                                        for (line in block.lines) {
                                            val lineText = line.text
                                            val lineCornerPoints = line.cornerPoints
                                            val lineFrame = line.boundingBox
                                            for (element in line.elements) {
                                                val elementText = element.text.uppercase()
                                                val elementCornerPoints = element.cornerPoints
                                                val elementFrame = element.boundingBox
                                                if(elementText.contains(mFirstName.toString().uppercase()))
                                                    mFirstNameFounded = true;
                                                if(elementText.contains(mLastName.toString().uppercase()))
                                                    mLastNameFounded = true;
                                                if(mFirstNameFounded == true && mLastNameFounded == true)
                                                    break
                                            }
                                            if(mFirstNameFounded == true && mLastNameFounded == true)
                                                break
                                        }
                                        if(mFirstNameFounded == true && mLastNameFounded == true)
                                            break
                                    }
                                    // after done, release the ImageProxy object
                                    imageProxy.close()
                                    // ...
                                    if(mFirstNameFounded == true && mLastNameFounded == true)
                                    {
                                        if (mListener != null) {
                                            mListener!!.onFragmentInteractionUserDataReader(true)
                                        }
                                        mFirstNameFounded = false;
                                        mLastNameFounded = false;
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Task failed with an exception
                                    Log.d("TextRecognizer", "Failed")
                                    // ...
                                    // after done, release the ImageProxy object
                                    imageProxy.close()
                                }
                            // ...
                        }
                    })
                }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, mCameraSelector, preview, imageAnalysis)

            } catch(exc: Exception) {
                Log.d("Use case binding failed", exc.toString())
            }

        }, ContextCompat.getMainExecutor(requireActivity()))
    }
}
