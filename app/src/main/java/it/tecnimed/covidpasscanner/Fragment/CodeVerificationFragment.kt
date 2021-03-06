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
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.*
import it.tecnimed.covidpasscanner.VL.VLTimer
import it.tecnimed.covidpasscanner.VL.VLTimer.OnTimeElapsedListener
import it.tecnimed.covidpasscanner.databinding.FragmentCodeVerificationBinding
import it.tecnimed.covidpasscanner.model.CertificateModel
import it.tecnimed.covidpasscanner.model.CertificateStatus
import it.tecnimed.covidpasscanner.model.CertificateViewBean
import it.tecnimed.covidpasscanner.model.VerificationViewModel
import it.tecnimed.covidpasscanner.util.*
import it.tecnimed.covidpasscanner.util.TimeUtility.formatDateOfBirth
import java.lang.ClassCastException
import java.util.*

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class CodeVerificationFragment : Fragment(), View.OnClickListener, OnTimeElapsedListener {

//    private val args by navArgs<VerificationFragmentArgs>()
    private val viewModel by viewModels<VerificationViewModel>()

    private var _binding: FragmentCodeVerificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var certificateModel: CertificateViewBean

    private var qrcodestr : String? = ""

    private lateinit var mTimeVar: VLTimer

    private val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 500)

    private var mListener: OnFragmentInteractionListener? = null
    /**
     * Here we define the methods that we can fire off
     * in our parent Activity once something has changed
     * within the fragment.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteractionCodeVerification(certSimple: CertificateViewBean?)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(requireActivity())
        mListener = try {
            activity as CodeVerificationFragment.OnFragmentInteractionListener
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
            qrcodestr = it.getString("QRCODESTR")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeVerificationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.TVCognome.visibility = View.INVISIBLE
        binding.TVDataNascita.visibility = View.INVISIBLE
        viewModel.certificate.observe(viewLifecycleOwner) { certificate ->
            certificate?.let {
                certificateModel = it
                Log.d("Person", it.person?.familyName.plus(" ").plus(it.person?.givenName));
                Log.d("Date of Birth", it.dateOfBirth?.formatDateOfBirth() ?: "");
                binding.TVCognome.text = it.person?.familyName
                binding.TVNome.text = it.person?.givenName
                binding.TVDataNascita.text = it.dateOfBirth?.formatDateOfBirth() ?: ""
                if(certificate.certificateStatus == CertificateStatus.VALID) {
                    binding.TVGreenPassValidity.text = getString(R.string.label_gp_valid)
                    binding.TVGreenPassValidity.setTextColor(Color.parseColor("#00ff00"))
                }
                else if(certificate.certificateStatus == CertificateStatus.NOT_VALID) {
                    binding.TVGreenPassValidity.text = getString(R.string.label_gp_notvalid)
                    binding.TVGreenPassValidity.setTextColor(Color.parseColor("#ff0000"))
                }
                else if(certificate.certificateStatus == CertificateStatus.NOT_VALID_YET) {
                    binding.TVGreenPassValidity.text = getString(R.string.label_gp_notyetvalid)
                    binding.TVGreenPassValidity.setTextColor(Color.parseColor("#ffA500"))
                }
                else if(certificate.certificateStatus == CertificateStatus.NOT_EU_DCC) {
                    binding.TVGreenPassValidity.text = getString(R.string.label_gp_noteudcc)
                    binding.TVGreenPassValidity.setTextColor(Color.parseColor("#ff0000"))
                }
                else if(certificate.certificateStatus == CertificateStatus.TEST_NEEDED) {
                    binding.TVGreenPassValidity.text = getString(R.string.label_gp_testneeded)
                    binding.TVGreenPassValidity.setTextColor(Color.parseColor("#ffA500"))
                }
//                if (certificate.certificateStatus == CertificateStatus.VALID) {
                    mTimeVar = VLTimer.create(this)
                    mTimeVar.startSingle(1000)
//                    Log.d("Validita", getString(R.string.label_gp_valid));
//                }
            }
        }

        try {
            binding.TVGreenPassValidity.text = ""
            binding.TVCognome.text = ""
            binding.TVNome.text = ""
            binding.TVDataNascita.text = ""
            qrcodestr?.let { viewModel.init(it, true) }
        } catch (e: VerificaMinSDKVersionException) {
            Log.d("VerificationFragment", "Min SDK Version Exception")
        } catch (e: VerificaMinVersionException) {
            Log.d("VerificationFragment", "Min App Version Exception")
        } catch (e: VerificaDownloadInProgressException) {
            Log.d("VerificationFragment", "Download In Progress Exception")
        }
    }

    override fun onClick(v: View?) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun VLTimerTimeElapsed(timer: VLTimer) {
        if (timer === mTimeVar) {
            if (mListener != null) {
                if(certificateModel.certificateStatus == CertificateStatus.VALID)
                    mListener!!.onFragmentInteractionCodeVerification(certificateModel)
                else {
                    toneG.startTone(ToneGenerator.TONE_SUP_PIP, 2000)
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        toneG.release()
                    }, (2000 + 50).toLong())
                    mListener!!.onFragmentInteractionCodeVerification(null)
                }
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
        fun newInstance(param1: String) =
            CodeVerificationFragment().apply {
                arguments = Bundle().apply {
                    putString("QRCODESTR", param1)
                }
            }
    }
}
