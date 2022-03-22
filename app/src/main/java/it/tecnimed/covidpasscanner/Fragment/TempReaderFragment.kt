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
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.google.zxing.client.android.BeepManager
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.R
import it.tecnimed.covidpasscanner.databinding.FragmentTempReaderBinding
import it.tecnimed.covidpasscanner.uart.UARTDriver
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.system.measureTimeMillis


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


    private val sensSizeX = 32
    private val sensSizeY = 24
    private val sensTargetPositionCoordN = 5
    private val sensTargetPositionCoordNPix = 10

    private var sensorEnv = 0.0f
    private var sensorObj = Array(sensSizeY) { Array(sensSizeX) { 0.0f } }
    private var sensorObjMax = 0.0f
    private var sensorObjMin = 0.0f
    private var sensorObjImageRGB = Array(sensSizeY) { Array(sensSizeX) { 0 } }
    private var sensorTHInt = 0
    private var sensorTHExt = 0
    private var sensorTObjMax = 0.0f
    private var sensorTargetPosition = 0
    private var sensorTargetCoordX = Array(sensTargetPositionCoordN) { Array(sensTargetPositionCoordNPix) { 0 } }
    private var sensorTargetCoordY = Array(sensTargetPositionCoordN) { Array(sensTargetPositionCoordNPix) { 0 } }
    private var sensorTargetCoordPnt = 0
    private var sensorTargetTObjMax = 0.0f
    private var sensorTargetTObjAve = 0.0f
    private var TargetState = false
    private var TargetTimeout = 0

    private var Tenv = 0.0f
    private var Tobj = 0.0f

    private var AveSensorTargetTObjMax: Float = 0.0f

    private lateinit var beepManager: BeepManager

    private lateinit var mSerialDrv: UARTDriver
    private val ThermalImageHwInterfaceHandler = object:  Handler(Looper.getMainLooper()) {
    }
    private val ThermalImageHwInterface: Runnable = object : Runnable {
        override fun run() {
            try {
                getThermalImage()
                generateThrmalBmp()
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                ThermalImageHwInterfaceHandler.postDelayed(this, 100)
            }
        }
    }
    private val TimeoutHandler = object:  Handler(Looper.getMainLooper()) {
    }
    private val TimeoutHnd: Runnable = object : Runnable {
        override fun run() {
            try {
                if (TargetTimeout > 0) {
                    TargetTimeout--;
                }
                if(TargetTimeout == 0) {
                    binding.TVPosition.setText("--")
                    binding.TVTempTargetMaxFreeze.setText("--")
                    binding.TVTempTargetFreeze.setText("--")
                    binding.TVTempTargetMax.setText("--")
                    binding.TVTempTarget.setText("--")
                    TargetState = false;
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                TimeoutHandler.postDelayed(this, 50)
            }
        }
    }


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
        binding.TVTempEnvThInt.setText("---")
        binding.TVTempEnvSensor.setText("---")
        binding.TVTempWndMax.setText("---")
        binding.TVTempTargetMax.setText("---")
        binding.TVTempTarget.setText("---")
        binding.TVTempTargetMaxFreeze.setText("---")
        binding.TVTempTargetFreeze.setText("---")

        mSerialDrv = UARTDriver.create(context)
        ThermalImageHwInterface.run();
        TimeoutHnd.run();
        beepManager = BeepManager(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
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

    private fun getThermalImage() {
        var serialOk : Boolean = true;

        if(mSerialDrv?.init() == false)
            return;

        if (mSerialDrv?.openPort(
                UARTDriver.UARTDRIVER_PORT_MODE_NOEVENT,
                0,
                115200,
                UARTDriver.UARTDRIVER_STOPBIT_1,
                UARTDriver.UARTDRIVER_PARITY_NONE
            ) == false
        ) {
            serialOk = false;
        }
        if(serialOk == false)
            return;

        val cmdObj = ByteArray(2)
        cmdObj[0] = 'T'.code.toByte()
        cmdObj[1] = '\r'.code.toByte()
        mSerialDrv.write(cmdObj, 2)
        var n : Int = 0
        while(n == 0) {
            val datasize = 1+(4)+(4)+(4)+(sensSizeY*sensSizeX*4)+1+((sensTargetPositionCoordN*sensTargetPositionCoordNPix*4*2)+1)+(4)+(4)+(4)+2;
            var ans = ByteArray(datasize)
            n = mSerialDrv.read(ans, 1000)
            if(n >= datasize) {
                if(ans[0] == 'T'.code.toByte()) {
                    var bf = ByteArray(4)
                    var k: Int = 1;
                    bf[0] = ans[k+3]
                    bf[1] = ans[k+2]
                    bf[2] = ans[k+1]
                    bf[3] = ans[k+0]
                    sensorTHInt = ByteBuffer.wrap(bf).getInt()
                    k += 4
                    bf[0] = ans[k+3]
                    bf[1] = ans[k+2]
                    bf[2] = ans[k+1]
                    bf[3] = ans[k+0]
                    sensorTHExt = ByteBuffer.wrap(bf).getInt()
                    k += 4
                    bf[0] = ans[k+3]
                    bf[1] = ans[k+2]
                    bf[2] = ans[k+1]
                    bf[3] = ans[k+0]
                    sensorEnv = ByteBuffer.wrap(bf).getFloat()
                    k += 4
                    for (i in 0 until sensSizeY) {
                        for (j in 0 until sensSizeX) {
                            bf[0] = ans[k+3]
                            bf[1] = ans[k+2]
                            bf[2] = ans[k+1]
                            bf[3] = ans[k+0]
                            sensorObj[i][j] = ByteBuffer.wrap(bf).getFloat()
                            k += 4
                        }
                    }
                    sensorTargetPosition = ans[k].toInt()
                    k++
                    for (i in 0 until sensTargetPositionCoordN) {
                        for (j in 0 until sensTargetPositionCoordNPix) {
                            bf[0] = ans[k+3]
                            bf[1] = ans[k+2]
                            bf[2] = ans[k+1]
                            bf[3] = ans[k+0]
                            sensorTargetCoordX[i][j] = ByteBuffer.wrap(bf).getInt()
                            k += 4
                        }
                    }
                    for (i in 0 until sensTargetPositionCoordN) {
                        for (j in 0 until sensTargetPositionCoordNPix) {
                            bf[0] = ans[k+3]
                            bf[1] = ans[k+2]
                            bf[2] = ans[k+1]
                            bf[3] = ans[k+0]
                            sensorTargetCoordY[i][j] = ByteBuffer.wrap(bf).getInt()
                            k += 4
                        }
                    }
                    sensorTargetCoordPnt = ans[k].toInt()
                    k++
                    bf[0] = ans[k+3]
                    bf[1] = ans[k+2]
                    bf[2] = ans[k+1]
                    bf[3] = ans[k+0]
                    sensorTObjMax = ByteBuffer.wrap(bf).getFloat()
                    k += 4
                    bf[0] = ans[k+3]
                    bf[1] = ans[k+2]
                    bf[2] = ans[k+1]
                    bf[3] = ans[k+0]
                    sensorTargetTObjMax = ByteBuffer.wrap(bf).getFloat()
                    k += 4
                    bf[0] = ans[k+3]
                    bf[1] = ans[k+2]
                    bf[2] = ans[k+1]
                    bf[3] = ans[k+0]
                    sensorTargetTObjAve = ByteBuffer.wrap(bf).getFloat()
                }
            }
        }
        AveSensorTargetTObjMax = sensorTargetTObjMax
        processTemperature()
    }

    private fun processTemperature()
    {
        // Calcolo
        val x: Float = sensorTHInt.toFloat()
        Tenv = (x * x * x * 0.00000000217112f)
        Tenv += (x * x * (-0.0000120088f))
        Tenv += (x * 0.041993773f)
        Tenv += (-29.28437706f)
        Tobj = AveSensorTargetTObjMax + (Tenv * Tenv * Tenv * -0.00002633053221f +
                Tenv * Tenv * 0.004149859944f +
                Tenv * -0.2638655462f +
                6.25f)

        // Visualizzazione
        binding.TVTempEnvThInt.setText("Int\n" + getString(R.string.strf41, Tenv))
        binding.TVTempEnvSensor.setText("Sns\n" + getString(R.string.strf41, sensorEnv))
        binding.TVTempWndMax.setText("MaxW\n" + getString(R.string.strf41, sensorTObjMax))
        binding.TVTempTargetMax.setText(getString(R.string.strf41, AveSensorTargetTObjMax))
        binding.TVTempTarget.setText(getString(R.string.strf41, Tobj))
        if(sensorTargetPosition != 0){
            if(TargetState == false) {
                if (sensorTargetPosition == 1)
                    binding.TVPosition.setText("<-Sx")
                else if (sensorTargetPosition == 2)
                    binding.TVPosition.setText("Dx->")
                else {
                    binding.TVPosition.setText("--")
                    binding.TVTempTargetMax.setText("--")
                    binding.TVTempTarget.setText("--")
                }
            }
        }
        else if(sensorTargetPosition == 0) {
            if(TargetState == false) {
                binding.TVPosition.setText("OK")
                binding.TVTempTargetMaxFreeze.setText(getString(R.string.strf41, AveSensorTargetTObjMax))
                binding.TVTempTargetFreeze.setText(getString(R.string.strf41, Tobj))
                try {
                    beepManager.playBeepSoundAndVibrate()
                } catch (e: Exception) {
                }
                TargetTimeout = 40
                TargetState = true
            }
        }
    }
    private fun generateThrmalBmp() 
    {
        var idx: Int = 0;
        var idy: Int = 0;
        var mx: Float = 0.0f;
        var qx: Float = 0.0f;
        var my: Float = 0.0f;
        var qy: Float = 0.0f;

        // Interpolazione
        sensorObjMax = -1000000.0f;
        sensorObjMin = 10000000.0f;
        for (i in 0 until sensSizeY) {
            for (j in 0 until sensSizeX) {
                if(sensorObj[i][j] < sensorObjMin)
                    sensorObjMin = sensorObj[i][j]
                if(sensorObj[i][j] > sensorObjMax)
                    sensorObjMax = sensorObj[i][j]
            }
        }

        // Conversione RGB
        var m: Float = - (250.0f / (sensorObjMax - sensorObjMin))
        var q: Float = 0 - (m * sensorObjMax)
        for (i in 0 until sensSizeY) {
            for (j in 0 until sensSizeX) {
                // H = Angolo Gradi, S = 0..1, B = 0..1
                var H: Float = (m * sensorObj[i][j]) + q
                var S: Float = 1.0f
                var B: Float = 1.0f
                var color: Int = convertHSB2RGB(H, S, B)
                sensorObjImageRGB[i][j] = color
            }
        }

        // Posizione target
        for (k in 0 until sensorTargetCoordPnt) {
            for (l in 0 until sensTargetPositionCoordNPix) {
                sensorObjImageRGB[sensorTargetCoordX[k][l]][sensorTargetCoordY[k][l]] = Color.BLACK;
            }
        }

        var bmp : Bitmap = createImage()
        binding.IVTemp.setImageBitmap(bmp)
        binding.IVTempOutline.setImageResource(R.drawable.reticolo)
    }

    private fun createImage(): Bitmap {
        var bitmap: Bitmap = Bitmap.createBitmap(sensSizeY, sensSizeX, Bitmap.Config.ARGB_8888);
        var canvas: Canvas = Canvas(bitmap)
        var paint: Paint = Paint()
        for (i in 0 until (sensSizeY)) {
            for (j in 0 until (sensSizeX)) {
                paint.setColor(sensorObjImageRGB[i][j])
                canvas.drawPoint(i.toFloat(), j.toFloat(), paint)
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