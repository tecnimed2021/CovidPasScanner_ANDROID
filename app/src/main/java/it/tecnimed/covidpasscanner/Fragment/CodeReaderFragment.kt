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
 *
 */

package it.tecnimed.covidpasscanner.Fragment

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.camera.CameraSettings
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.R
import it.tecnimed.covidpasscanner.VL.VLTimer
import it.tecnimed.covidpasscanner.model.VerificationViewModel
import it.tecnimed.covidpasscanner.databinding.FragmentCodeReaderBinding
import java.lang.ClassCastException

@AndroidEntryPoint
class CodeReaderFragment : Fragment(),
    View.OnClickListener, DecoratedBarcodeView.TorchListener , VLTimer.OnTimeElapsedListener{

    private var mListener: OnFragmentInteractionListener? = null
    /**
     * Here we define the methods that we can fire off
     * in our parent Activity once something has changed
     * within the fragment.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteractionCodeReader(qrcodeText: String)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(requireActivity())
        mListener = try {
            activity as OnFragmentInteractionListener
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

    private var _binding: FragmentCodeReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var beepManager: BeepManager
    private var lastText: String? = null

    private val viewModel by viewModels<VerificationViewModel>()
    private var torchOn = false

    private lateinit var mTimeVar: VLTimer

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.barcodeFormat != BarcodeFormat.QR_CODE && result.barcodeFormat != BarcodeFormat.AZTEC) {
                return
            }
            if (result.text == null || result.text == lastText) {
                return
            }
            binding.barcodeScanner.pause()

            lastText = result.text

            try {
                beepManager.playBeepSoundAndVibrate()
            } catch (e: Exception) {
            }

            navigateToVerificationPage(result.text)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback { requireActivity().finish() }
        mTimeVar = VLTimer.create(this)
        mTimeVar.startSingle(10000)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hintsMap: MutableMap<DecodeHintType, Any> = HashMap()
        val formats: Collection<BarcodeFormat> = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.AZTEC)
        hintsMap[DecodeHintType.TRY_HARDER] = false
        binding.barcodeScanner.barcodeView.decoderFactory = DefaultDecoderFactory(formats, hintsMap, null, 0)
        binding.barcodeScanner.cameraSettings.isAutoFocusEnabled = true

        // Force Front Camera
        viewModel.setFrontCameraStatus(true)

        if (viewModel.getFrontCameraStatus()) {
            binding.barcodeScanner.barcodeView.cameraSettings.focusMode =
                CameraSettings.FocusMode.INFINITY
        }
        binding.barcodeScanner.initializeFromIntent(requireActivity().intent)

        if (viewModel.getFrontCameraStatus()) {
            binding.barcodeScanner.cameraSettings.requestedCameraId = 1
        } else {
            binding.barcodeScanner.cameraSettings.requestedCameraId = -1
        }

        if (viewModel.getFrontCameraStatus()) {
            binding.torchButton.visibility = View.INVISIBLE
        } else {
            binding.torchButton.visibility = View.VISIBLE
        }

        if (!hasFlash()) {
            binding.torchButton.visibility = View.GONE
        } else {
            binding.torchButton.setOnClickListener(this)
            binding.barcodeScanner.setTorchListener(this)
        }
        binding.barcodeScanner.pause()
        binding.barcodeScanner.resume()

        binding.barcodeScanner.decodeContinuous(callback)
        binding.barcodeScanner.statusView.text = ""
        beepManager = BeepManager(requireActivity())

//        binding.backImage.setOnClickListener(this)
//        binding.backText.setOnClickListener(this)
//        binding.flipCamera.setOnClickListener(this)

        binding.backImage.visibility = View.INVISIBLE;
        binding.backText.visibility = View.INVISIBLE;
        binding.flipCamera.visibility = View.INVISIBLE;
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        lastText = ""
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    private fun navigateToVerificationPage(text: String) {
        if (mListener != null) {
            mListener!!.onFragmentInteractionCodeReader(text)
        }
    }

    override fun VLTimerTimeElapsed(timer: VLTimer) {
        if (timer === mTimeVar) {
            if (mListener != null) {
                mListener!!.onFragmentInteractionCodeReader("")
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_image -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionCodeReader("")
                }
            }
            R.id.back_text -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionCodeReader("")
                }
            }
            R.id.flip_camera -> {
                binding.barcodeScanner.pause()
                binding.barcodeScanner.cameraSettings.requestedCameraId *= -1
                binding.barcodeScanner.resume()

                if (binding.barcodeScanner.cameraSettings.requestedCameraId == 1) {
                    viewModel.setFrontCameraStatus(true)
                } else if (binding.barcodeScanner.cameraSettings.requestedCameraId == -1) {
                    viewModel.setFrontCameraStatus(false)
                }

                if (viewModel.getFrontCameraStatus()) {
                    binding.torchButton.visibility = View.INVISIBLE
                } else {
                    binding.torchButton.visibility = View.VISIBLE
                }
                if (!hasFlash()) {
                    binding.torchButton.visibility = View.GONE
                } else {
                    binding.torchButton.setOnClickListener(this)
                    binding.barcodeScanner.setTorchListener(this)
                }
            }
            R.id.torch_button -> {
                if (torchOn) {
                    binding.barcodeScanner.setTorchOff()
                } else {
                    binding.barcodeScanner.setTorchOn()
                }
            }
        }
    }


    private fun hasFlash(): Boolean {
        return requireActivity().packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    override fun onTorchOn() {
        torchOn = true
    }

    override fun onTorchOff() {
        torchOn = false
    }
}