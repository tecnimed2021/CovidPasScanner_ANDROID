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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.google.zxing.client.android.BeepManager
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.databinding.FragmentTempReaderBinding
import kotlin.system.measureTimeMillis
import it.tecnimed.covidpasscanner.R


@AndroidEntryPoint
class TempReaderFragment : Fragment(), View.OnClickListener {

    private var mListener: OnFragmentInteractionListener? = null

    /**
     * Here we define the methods that we can fire off
     * in our parent Activity once something has changed
     * within the fragment.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteractionTempReader(temp: String)
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

    private var _binding: FragmentTempReaderBinding? = null
    private val binding get() = _binding!!


    private val sensSizeX = 16
    private val sensSizeY = 12
    private val sensScale = 1
    private var sensorBmp = Array(sensSizeY) { Array(sensSizeX) { 0.0f } }
    private var sensorThermalImage = Array(sensSizeY * sensScale) { Array(sensSizeX * sensScale) { 0.0f } }
    private var sensorThermalImageRGB = Array(sensSizeY * sensScale) { Array(sensSizeX * sensScale) { 0 } }


    private var lastText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback { requireActivity().finish() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTempReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backImage3.setOnClickListener(this)
        binding.backText3.setOnClickListener(this)
        binding.backImage3.visibility = View.VISIBLE;
        binding.backText3.visibility = View.VISIBLE;


        for (i in 0 until sensSizeY) {
            for (j in 0 until sensSizeX) {
                sensorBmp[i][j] = 25.0f + j.toFloat()
            }
        }
        val elapsed = measureTimeMillis {
            generateThrmalBmp()
        }
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
    }

    private fun navigateToNextPage(text: String) {
        if (mListener != null) {
            mListener!!.onFragmentInteractionTempReader(text)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_image3 -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionTempReader("")
                }
            }
            R.id.back_text3 -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionTempReader("")
                }
            }
        }
    }

    private fun generateThrmalBmp() {
        var MaxT: Float = -1000000.0f;
        var MinT: Float = 10000000.0f;

        var idx: Int = 0;
        var idy: Int = 0;
        var mx: Float = 0.0f;
        var qx: Float = 0.0f;
        var my: Float = 0.0f;
        var qy: Float = 0.0f;

        // Interpolazione
        for (i in 0 until sensSizeY) {
            for (j in 0 until sensSizeX) {
                if(sensorBmp[i][j] < MinT)
                    MinT = sensorBmp[i][j]
                if(sensorBmp[i][j] > MaxT)
                    MaxT = sensorBmp[i][j]
                for (ky in 0 until sensScale) {
                    for (kx in 0 until sensScale) {
                        sensorThermalImage[i * sensScale + ky][j * sensScale + kx] = sensorBmp[i][j]
                    }
                }
            }
        }
        for (i in 0 until ((sensSizeY - 1) * sensScale)) {
            for (j in 0 until ((sensSizeX - 1) * sensScale) step sensScale) {
                idx = (j / sensScale) * sensScale
                mx =
                    (sensorThermalImage[i][idx + sensScale] - sensorThermalImage[i][idx]) / sensScale
                qx = sensorThermalImage[i][idx]
                for (kx in 0 until sensScale) {
                    sensorThermalImage[i][idx + kx] = mx * kx + qx
                }
            }
        }
        for (j in 0 until ((sensSizeX - 1) * sensScale)) {
            for (i in 0 until ((sensSizeY - 1) * sensScale) step sensScale) {
                idy = (i / sensScale) * sensScale
                my =
                    (sensorThermalImage[idy + sensScale][j] - sensorThermalImage[idy][j]) / sensScale
                qy = sensorThermalImage[idy][j]
                for (ky in 0 until sensScale) {
                    sensorThermalImage[idy + ky][j] = my * ky + qy
                }
            }
        }

        // Conversione RGB
        var m: Float = - (250.0f / (MaxT - MinT))
        var q: Float = 0 - (m * MaxT)
        for (i in 0 until (sensSizeY * sensScale)) {
            for (j in 0 until (sensSizeX * sensScale)) {
                // H = Angolo Gradi, S = 0..1, B = 0..1
                var H : Float = (m * sensorBmp[i][j]) + q
                var S: Float = 1.0f
                var B: Float = 1.0f
                var color: Int = convertHSB2RGB(H, S, B)
                sensorThermalImageRGB[i][j] = color
            }
        }

        var bmp : Bitmap = createImage()
        binding.IVTemp.setImageBitmap(bmp)
        binding.IVTempOutline.setImageResource(R.drawable.outline)

    }

    private fun createImage(): Bitmap {
        var bitmap: Bitmap = Bitmap.createBitmap(sensSizeX * sensScale, sensSizeY * sensScale, Bitmap.Config.ARGB_8888);
        var canvas: Canvas = Canvas(bitmap)
        var paint: Paint = Paint()
        for (i in 0 until (sensSizeY * sensScale)) {
            for (j in 0 until (sensSizeX * sensScale)) {
                paint.setColor(sensorThermalImageRGB[i][j])
                canvas.drawPoint(j.toFloat(), i.toFloat(), paint)
            }
        }
        return bitmap;
    }

    private fun convertHSB2RGB(H : Float, S : Float, B : Float): Int {
        var cA: Float = 1.0f
        var cR: Float = 0.0f
        var cG: Float = 0.0f
        var cB: Float = 0.0f

        var hh: Float
        val p: Float
        val q: Float
        val t: Float
        val ff: Float
        val i: Int

        hh = H
        if (hh >= 360.0f) hh = 0.0f
        hh /= 60.0f
        i = hh.toInt()
        ff = hh - i
        p = B * (1.0f - S)
        q = B * (1.0f - S * ff)
        t = B * (1.0f - S * (1.0f - ff))

        when (i) {
            0 -> {
                cR = B
                cG= t
                cB = p
            }
            1 -> {
                cR = q
                cG = B
                cB = p
            }
            2 -> {
                cR = p
                cG = B
                cB = t
            }
            3 -> {
                cR = p
                cG = q
                cB = B
            }
            4 -> {
                cR = t
                cG = p
                cB = B
            }
            5 -> {
                cR = B
                cG = p
                cB = q
            }
            else -> {
                cR = B
                cG = p
                cB = q
            }
        }

        cA = cA * 255.0f
        cR = cR * 255.0f
        cG = cG * 255.0f
        cB = cB * 255.0f

        val color: Int =
            cA.toInt() and 0xff shl 24 or (cR.toInt() and 0xff shl 16) or (cG.toInt() and 0xff shl 8) or (cB.toInt() and 0xff)

        return color
    }
}