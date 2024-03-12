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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.R
import it.tecnimed.covidpasscanner.Tecnimed.AppSetup
import it.tecnimed.covidpasscanner.Tecnimed.VLTimer
import it.tecnimed.covidpasscanner.Tecnimed.VLTimer.OnTimeElapsedListener
import it.tecnimed.covidpasscanner.databinding.FragmentSetupBinding
import it.tecnimed.covidpasscanner.model.VerificationViewModel
import java.lang.ClassCastException


@ExperimentalUnsignedTypes
@AndroidEntryPoint
class SetupFragment : Fragment(), View.OnClickListener, OnTimeElapsedListener {

//    private val args by navArgs<VerificationFragmentArgs>()
    private val viewModel by viewModels<VerificationViewModel>()

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private lateinit var mSetup: AppSetup;
    private var mSequenceGp: Boolean = false
    private var mSequenceDoc: Boolean = false


    private lateinit var mTimeVar: VLTimer

    private var mListener: OnFragmentInteractionListener? = null
    /**
     * Here we define the methods that we can fire off
     * in our parent Activity once something has changed
     * within the fragment.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteractionSetup(setupParams: AppSetup)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(requireActivity())
        mListener = try {
            activity as SetupFragment.OnFragmentInteractionListener
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
            mSetup = it.getSerializable("SETUP_PARAMS") as AppSetup
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backImageSetup.setOnClickListener(this)
        binding.backTextSetup.setOnClickListener(this)
        binding.backImageSetup.visibility = View.VISIBLE;
        binding.backTextSetup.visibility = View.VISIBLE;

        binding.SWTemperatura.setChecked(mSetup.getSequenceTemperature());
        binding.SWTemperatura.setOnClickListener{
            mSetup.setSequenceTemperature(binding.SWTemperatura.isChecked)
        }
        binding.SWGreenPass.setChecked(mSetup.getSequenceGreenPass());
        binding.SWGreenPass.setOnClickListener{
            mSetup.setSequenceGreenPass(binding.SWGreenPass.isChecked)
        }
        binding.SWDocumento.setChecked(mSetup.getSequenceDocument());
        binding.SWDocumento.setOnClickListener{
            mSetup.setSequenceDocument(binding.SWDocumento.isChecked)
        }
        binding.TVRangeGreen.text = getString(R.string.strf41, mSetup.getRangeGreen())
        binding.BRangeGreenP.setOnClickListener(){
            mSetup.rangeGreen = mSetup.rangeGreen + 0.1f
            if(mSetup.rangeGreen > 39.0f)
                mSetup.rangeGreen = 39.0f
            binding.TVRangeGreen.text = getString(R.string.strf41, mSetup.getRangeGreen())
        }
        binding.BRangeGreenM.setOnClickListener(){
            mSetup.rangeGreen = mSetup.rangeGreen - 0.1f
            if(mSetup.rangeGreen < 30.0f)
                mSetup.rangeGreen = 30.0f
            binding.TVRangeGreen.text = getString(R.string.strf41, mSetup.getRangeGreen())
        }
        binding.TVRangeOrange.text = getString(R.string.strf41, mSetup.getRangeOrange())
        binding.BRangeOrangeP.setOnClickListener(){
            mSetup.rangeOrange = mSetup.rangeOrange + 0.1f
            if(mSetup.rangeOrange > 2.0f)
                mSetup.rangeOrange = 2.0f
            binding.TVRangeOrange.text = getString(R.string.strf41, mSetup.getRangeOrange())
        }
        binding.BRangeOrangeM.setOnClickListener(){
            mSetup.rangeOrange = mSetup.rangeOrange - 0.1f
            if(mSetup.rangeOrange < 0.0f)
                mSetup.rangeOrange = 0.0f
            binding.TVRangeOrange.text = getString(R.string.strf41, mSetup.getRangeOrange())
        }
        if(mSetup.correction == AppSetup.ORAL)
            binding.RBOral.isChecked = true;
        binding.RBOral.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked)
                mSetup.correction = AppSetup.ORAL
        }
        if(mSetup.correction == AppSetup.RECTAL)
            binding.RBRectal.isChecked = true;
        binding.RBRectal.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked)
                mSetup.correction = AppSetup.RECTAL
        }
        if(mSetup.correction == AppSetup.AXILLA)
            binding.RBAxilla.isChecked = true;
        binding.RBAxilla.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked)
                mSetup.correction = AppSetup.AXILLA
        }
        if(mSetup.correction == AppSetup.CORE)
            binding.RBCore.isChecked = true;
        binding.RBCore.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked)
                mSetup.correction = AppSetup.CORE
        }
        binding.SWAir.setChecked(mSetup.correctionAir);
        binding.SWAir.setOnClickListener{
            mSetup.correctionAir = binding.SWAir.isChecked
        }
        mTimeVar = VLTimer.create(this)
        mTimeVar.startSingle(30000)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_image_setup -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionSetup(mSetup)
                }
            }
            R.id.back_text_setup -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionSetup(mSetup)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun VLTimerTimeElapsed(timer: VLTimer) {
        if (timer === mTimeVar) {
            if (mListener != null) {
                mListener!!.onFragmentInteractionSetup(mSetup)
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
        fun newInstance(param1: AppSetup) =
            SetupFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("SETUP_PARAMS", param1)
                }
            }
    }
}
