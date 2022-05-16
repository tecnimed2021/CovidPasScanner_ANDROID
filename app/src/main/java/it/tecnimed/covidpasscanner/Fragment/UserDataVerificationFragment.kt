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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.zxing.client.android.BeepManager
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.*
import it.tecnimed.covidpasscanner.VL.VLTimer
import it.tecnimed.covidpasscanner.VL.VLTimer.OnTimeElapsedListener
import it.tecnimed.covidpasscanner.databinding.FragmentUserdataVerificationBinding
import it.tecnimed.covidpasscanner.model.VerificationViewModel
import it.tecnimed.covidpasscanner.util.*
import java.lang.ClassCastException
import java.util.*
import android.media.ToneGenerator

import android.media.AudioManager
import android.os.Handler
import android.os.Looper


@ExperimentalUnsignedTypes
@AndroidEntryPoint
class UserDataVerificationFragment : Fragment(), View.OnClickListener, OnTimeElapsedListener {

//    private val args by navArgs<VerificationFragmentArgs>()
    private val viewModel by viewModels<VerificationViewModel>()

    private var _binding: FragmentUserdataVerificationBinding? = null
    private val binding get() = _binding!!

    private var mFirstName : String? = ""
    private var mLastName : String? = ""

    private lateinit var mTimeVar: VLTimer

    private lateinit var beepManager: BeepManager
    private val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 500)

    private var mListener: OnFragmentInteractionListener? = null
    /**
     * Here we define the methods that we can fire off
     * in our parent Activity once something has changed
     * within the fragment.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteractionUserDataVerification()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(requireActivity())
        mListener = try {
            activity as UserDataVerificationFragment.OnFragmentInteractionListener
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserdataVerificationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.TVCognome.visibility = View.INVISIBLE
        if(mFirstName == "" && mLastName == "") {
            binding.TVUserDataValidity.text = getString(R.string.label_ud_notvalid)
            binding.TVUserDataValidity.setTextColor(Color.parseColor("#ff0000"))
            binding.TVCognome.text = ""
            binding.TVNome.text = ""
            binding.TVUserDataGo.text = ""
            toneG.startTone(ToneGenerator.TONE_SUP_PIP, 2000)
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                toneG.release()
            }, (2000 + 50).toLong())
        }
        else {
            binding.TVUserDataValidity.text = getString(R.string.label_ud_valid)
            binding.TVUserDataValidity.setTextColor(Color.parseColor("#00ff00"))
            binding.TVCognome.text = mFirstName
            binding.TVNome.text = mLastName
            binding.TVUserDataGo.text = getString(R.string.label_ud_go)
            binding.TVUserDataGo.setTextColor(Color.parseColor("#00ff00"))
            toneG.startTone(ToneGenerator.TONE_DTMF_5, 2000)
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                toneG.release()
            }, (2000 + 50).toLong())
        }
        mTimeVar = VLTimer.create(this)
        mTimeVar.startSingle(4000)
        beepManager = BeepManager(requireActivity())
    }

    override fun onClick(v: View?) {
        when (v?.id) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun VLTimerTimeElapsed(timer: VLTimer) {
        if (timer === mTimeVar) {
            if (mListener != null) {
                mListener!!.onFragmentInteractionUserDataVerification()
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
            UserDataVerificationFragment().apply {
                arguments = Bundle().apply {
                    putString("FIRSTNAMESTR", param1)
                    putString("LASTNAMESTR", param2)
                }
            }
    }
}
