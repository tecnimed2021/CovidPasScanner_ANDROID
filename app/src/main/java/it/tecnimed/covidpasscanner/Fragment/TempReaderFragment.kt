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
import android.app.KeyguardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.Image
import android.media.ToneGenerator
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.google.zxing.client.android.BeepManager
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.R
import it.tecnimed.covidpasscanner.Tecnimed.AppSetup
import it.tecnimed.covidpasscanner.databinding.FragmentTempReaderBinding
import it.tecnimed.covidpasscanner.uart.UARTDriver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@AndroidEntryPoint
class TempReaderFragment : Fragment(), View.OnClickListener {

    private lateinit var mActivity: Activity;
    private var mListener: OnFragmentInteractionListener? = null
    private var fos: OutputStream? = null

    private val DebugActive: Boolean = false

    /**
     * Here we define the methods that we can fire off
     * in our parent Activity once something has changed
     * within the fragment.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteractionTempReader(temp: String, tempcolor: Int)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(requireActivity())
        mActivity = activity;
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

    private var sensCom = false

    private val sensSizeX = 16 * 2
    private val sensSizeY = 12 * 2
    private val sensTargetPositionCoordN = 5
    private val sensTargetPositionCoordNPix = 8

    private var sensorMeasureEnabled = true
    private var sensorEnv = 0.0f
    private var sensorObj = Array(sensSizeY) { Array(sensSizeX) { 0.0f } }
    private var sensorObjPrev = Array(sensSizeY) { Array(sensSizeX) { 0.0f } }
    private var sensorObjMax = 0.0f
    private var sensorObjMin = 0.0f
    private var sensorObjImageRGB = Array(sensSizeY) { Array(sensSizeX) { 0 } }
    private var sensorTHInt = 0.0f
    private var sensorTHExt = 0.0f
    private var sensorTObjMax = 0.0f
    private var sensorTargetPosition = 0
    private var sensorTargetCoordX =
        Array(sensTargetPositionCoordN) { Array(sensTargetPositionCoordNPix) { 0 } }
    private var sensorTargetCoordY =
        Array(sensTargetPositionCoordN) { Array(sensTargetPositionCoordNPix) { 0 } }
    private var sensorTargetCoordPnt = 0
    private var sensorTargetTObjMax = 0.0f
    private var sensorTargetTObjMaxAdjusted = 0.0f
    private var sensorTargetTObjAve = 0.0f
    private var sensorTargetTObjAveAdjusted = 0.0f
    private var TargetState = false
    private var TargetTimeout = 0
    private var sensorTargetTObjMaxAdjustedIsValid = false
    private var sensorDistancePosition = 0
    private var sensorDistanceAmbientLight = 0.0f
    private var sensorDistanceTargetPositionOK = 0

    private var sensorDistanceNoTarget = false
    private var sensorDistanceNoTargetTimeout = 600

    private lateinit var beepManager: BeepManager
    private val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 500)

    private lateinit var mSerialDrv: UARTDriver
    private val ThermalImageHwInterfaceHandler = object : Handler(Looper.getMainLooper()) {
    }
    private val ThermalImageHwInterface: Runnable = object : Runnable {
        override fun run() {
            try {
                if(sensorMeasureEnabled == true) {
                    sensCom = getThermalImage()
                    if (sensCom == true)
                        generateThrmalBmp()
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                if(sensCom == true) {
                    ThermalImageHwInterfaceHandler.postDelayed(this, 25)
                }
                else {
                    ThermalImageHwInterfaceHandler.postDelayed(this, 1000)
                    navigateToNextPage("NOCOM", R.color.covidpasscanner_green)
                }
            }
        }
    }
    private val TimeoutHandler = object : Handler(Looper.getMainLooper()) {
    }
    private val TimeoutHnd: Runnable = object : Runnable {
        override fun run() {
            try {
                if (TargetTimeout > 0) {
                    TargetTimeout--
                }
                if (TargetTimeout == 0) {
                    if (TargetState == true) {
                        binding.TVPosition.setText("--")
                        binding.TVTempTargetMaxFreeze.setText("--")
                        binding.TVTempTargetFreeze.setText("--")
                        binding.TVTempTargetMax.setText("--")
//                        binding.TVTempTarget.setText("--")
                        TargetState = false;
                        if(sensorTargetTObjMaxAdjustedIsValid == true) {
                            NavigateToNextPageHandler.postDelayed(NavigateToNextPageHnd, 2000)
                        }
                        else
                        {
                            ResumeMeasureHandler.postDelayed(ResumeMeasureHnd, 2000)
                        }
                    }
                }
                if (sensorDistanceNoTargetTimeout > 0) {
                    sensorDistanceNoTargetTimeout--
                }
                if (sensorDistanceNoTargetTimeout == 0) {
                    sensorDistanceNoTarget = true
                }
            } finally {
                // Circa 80ms
                TimeoutHandler.postDelayed(this, 40)
            }
        }
    }
    private val ScreenshotHandler = object : Handler(Looper.getMainLooper()) {
    }
    private val ScreenshotHnd: Runnable = object : Runnable {
        override fun run() {
            try {
                val bmp: Bitmap? = getScreenShotFromView(mActivity.getWindow().decorView)
                if(bmp != null)
                    saveMediaToStorage(bmp);
            } finally {
            }
        }
    }
    private val NavigateToNextPageHandler = object : Handler(Looper.getMainLooper()) {
    }
    private val NavigateToNextPageHnd: Runnable = object : Runnable {
        override fun run() {
            try {
                if(sensorTargetTObjMaxAdjusted <= AppSetup.RangeTempGreen)
                    navigateToNextPage(getString(R.string.strf41, sensorTargetTObjMaxAdjusted), R.color.covidpasscanner_green)
                else if(sensorTargetTObjMaxAdjusted <= (AppSetup.RangeTempGreen + AppSetup.RangeTempOrange))
                    navigateToNextPage(getString(R.string.strf41, sensorTargetTObjMaxAdjusted), R.color.covidpasscanner_orange)
                else
                    navigateToNextPage(getString(R.string.strf41, sensorTargetTObjMaxAdjusted), R.color.covidpasscanner_red)
            } finally {
            }
        }
    }
    private val ResumeMeasureHandler = object : Handler(Looper.getMainLooper()) {
    }
    private val ResumeMeasureHnd: Runnable = object : Runnable {
        override fun run() {
            try {
                sensorMeasureEnabled = true
                sensorTargetTObjMaxAdjustedIsValid = false
            } finally {
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
        binding.TVPosition.setText("---")
        if(DebugActive == false) {
            binding.TVTempEnvThInt.visibility = View.INVISIBLE
            binding.TVTempEnvSensor.visibility = View.INVISIBLE
            binding.TVTempWndMax.visibility = View.INVISIBLE
            binding.TVTempTargetMax.visibility = View.INVISIBLE
            binding.TVTempTarget.visibility = View.VISIBLE
            binding.TVTempTargetMaxFreeze.visibility = View.INVISIBLE
            binding.TVTempTargetFreeze.visibility = View.INVISIBLE
            binding.TVPosition.visibility = View.INVISIBLE
            binding.TVMotionSensor.visibility = View.VISIBLE
        }
        else
        {
            binding.TVTempEnvThInt.visibility = View.VISIBLE
            binding.TVTempEnvSensor.visibility = View.VISIBLE
            binding.TVTempWndMax.visibility = View.VISIBLE
            binding.TVTempTargetMax.visibility = View.VISIBLE
            binding.TVTempTarget.visibility = View.VISIBLE
            binding.TVTempTargetMaxFreeze.visibility = View.VISIBLE
            binding.TVTempTargetFreeze.visibility = View.VISIBLE
            binding.TVPosition.visibility = View.VISIBLE
            binding.TVMotionSensor.visibility = View.VISIBLE
        }
        sensorMeasureEnabled = true
        sensorTargetTObjMaxAdjustedIsValid = false
        mSerialDrv = UARTDriver.create(context)
        ThermalImageHwInterface.run()
        TimeoutHnd.run();
        beepManager = BeepManager(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ThermalImageHwInterfaceHandler.removeCallbacks(ThermalImageHwInterface)
        TimeoutHandler.removeCallbacks(TimeoutHnd)
        ScreenshotHandler.removeCallbacks(ScreenshotHnd)
        NavigateToNextPageHandler.removeCallbacks(NavigateToNextPageHnd)
        _binding = null
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun navigateToNextPage(temp: String, tempcolor: Int) {
        if (mListener != null) {
            mListener!!.onFragmentInteractionTempReader(temp, tempcolor)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_image3 -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionTempReader("", R.color.covidpasscanner_green)
                }
            }
            R.id.back_text3 -> {
                if (mListener != null) {
                    mListener!!.onFragmentInteractionTempReader("", R.color.covidpasscanner_green)
                }
            }
        }
    }

    private fun getThermalImage(): Boolean {
        var serialOk: Boolean = true;

        if (mSerialDrv?.init() == false)
            return false;

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
        if (serialOk == false)
            return false;

        sensorTargetPosition = 255;
        sensorTargetCoordPnt = 0;

        // Lettura dati da seriale
        val cmdObj = ByteArray(2)
        cmdObj[0] = 'T'.code.toByte()
        cmdObj[1] = '\r'.code.toByte()
        mSerialDrv.write(cmdObj, 2)
        var n: Int = 0
        val datasize = 1 + (4) + (4) + (4) + (sensSizeY * sensSizeX * 4) + 1 + ((sensTargetPositionCoordN * sensTargetPositionCoordNPix * 4 * 2) + 1) +
                       (4) + (4) + (4) + (4) + (4) + (1 + 4) + 2;
        var ans = ByteArray(datasize)
        n = mSerialDrv.read(ans, 1000)
        mSerialDrv?.closePort()

        // Decodifica
        if (n < datasize)
            return true;
        if (ans[0] != 'T'.code.toByte())
            return true;

        var bf = ByteArray(4)
        var k: Int = 1;
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorTHInt = ByteBuffer.wrap(bf).getFloat()
        k += 4
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorTHExt = ByteBuffer.wrap(bf).getFloat()
        k += 4
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorEnv = ByteBuffer.wrap(bf).getFloat()
        k += 4
        for (i in 0 until sensSizeY) {
            for (j in 0 until sensSizeX) {
                bf[0] = ans[k + 3]
                bf[1] = ans[k + 2]
                bf[2] = ans[k + 1]
                bf[3] = ans[k + 0]
                sensorObj[i][j] = ByteBuffer.wrap(bf).getFloat()
                k += 4
            }
        }
        sensorTargetPosition = ans[k].toInt()
        k++
        for (i in 0 until sensTargetPositionCoordN) {
            for (j in 0 until sensTargetPositionCoordNPix) {
                bf[0] = ans[k + 3]
                bf[1] = ans[k + 2]
                bf[2] = ans[k + 1]
                bf[3] = ans[k + 0]
                sensorTargetCoordX[i][j] = ByteBuffer.wrap(bf).getInt()
                k += 4
            }
        }
        for (i in 0 until sensTargetPositionCoordN) {
            for (j in 0 until sensTargetPositionCoordNPix) {
                bf[0] = ans[k + 3]
                bf[1] = ans[k + 2]
                bf[2] = ans[k + 1]
                bf[3] = ans[k + 0]
                sensorTargetCoordY[i][j] = ByteBuffer.wrap(bf).getInt()
                k += 4
            }
        }
        sensorTargetCoordPnt = ans[k].toInt()
        k++
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorTObjMax = ByteBuffer.wrap(bf).getFloat()
        k += 4
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorTargetTObjMax = ByteBuffer.wrap(bf).getFloat()
        k += 4
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorTargetTObjMaxAdjusted = ByteBuffer.wrap(bf).getFloat()
        k += 4
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorTargetTObjAve = ByteBuffer.wrap(bf).getFloat()
        k += 4
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorTargetTObjAveAdjusted = ByteBuffer.wrap(bf).getFloat()
        k += 4
        sensorDistancePosition = ans[k].toInt()
        k += 1
        bf[0] = ans[k + 3]
        bf[1] = ans[k + 2]
        bf[2] = ans[k + 1]
        bf[3] = ans[k + 0]
        sensorDistanceAmbientLight = ByteBuffer.wrap(bf).getFloat()

        motionDetection()

        processTemperature()


        return true;
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun motionDetection() {

        sensorDistanceTargetPositionOK = sensorDistancePosition

        if (sensorDistanceTargetPositionOK > 1) {
            sensorDistanceNoTarget = false
            sensorDistanceNoTargetTimeout = 600
        }

        if(sensorDistanceNoTarget == false) {
            mActivity.runOnUiThread(java.lang.Runnable {
                mActivity.setTurnScreenOn(true)
                val keyguardManager = mActivity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
                keyguardManager!!.requestDismissKeyguard(mActivity, null)
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            })
        }
        else{
            mActivity.runOnUiThread(java.lang.Runnable {
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            })
        }

        if (sensorDistanceTargetPositionOK != 0) {
            binding.TVMotionSensor.visibility = View.VISIBLE
            if(sensorDistanceTargetPositionOK == 2)
                // OK
                binding.TVMotionSensor.text = ""
            else if(sensorDistanceTargetPositionOK == 3)
                // Too Far
                binding.TVMotionSensor.text = getString(R.string.label_temp_position_toofar)
            else if(sensorDistanceTargetPositionOK == 4)
                // Too Close
                binding.TVMotionSensor.text = getString(R.string.label_temp_position_tooclose)
            else
                // No target
                binding.TVMotionSensor.text = ""
        }
        else {
            binding.TVMotionSensor.visibility = View.INVISIBLE
            binding.TVMotionSensor.text = ""
        }
    }

    private fun processTemperature() {
        // Visualizzazione
        binding.TVTempEnvThInt.setText("Int\n" + getString(R.string.strf41, sensorTHInt))
        binding.TVTempEnvSensor.setText("Sns\n" + getString(R.string.strf41, sensorEnv))
        binding.TVTempWndMax.setText("MaxW\n" + getString(R.string.strf41, sensorTObjMax))
        binding.TVTempTargetMax.setText(getString(R.string.strf41, sensorTargetTObjMax))
        if (sensorTargetPosition == 0 || sensorTargetPosition == 3) {
            if (TargetState == false && sensorDistanceTargetPositionOK == 2) {
                binding.TVPosition.setText("OK")
                binding.TVMotionSensor.text = ""
                binding.TVTempTargetMaxFreeze.setText(
                    getString(
                        R.string.strf41,
                        sensorTargetTObjMax
                    )
                )
                binding.TVTempTargetFreeze.setText(
                    getString(
                        R.string.strf41,
                        sensorTargetTObjMaxAdjusted
                    )
                )
                binding.TVTempTarget.setText(getString(R.string.strf41, sensorTargetTObjMaxAdjusted))
                if(sensorTargetTObjMaxAdjusted <= AppSetup.RangeTempGreen)
                    binding.TVTempTarget.setTextColor(getColor(mActivity, R.color.covidpasscanner_green))
                else if(sensorTargetTObjMaxAdjusted <= (AppSetup.RangeTempGreen + AppSetup.RangeTempOrange))
                    binding.TVTempTarget.setTextColor(getColor(mActivity, R.color.covidpasscanner_orange))
                else
                    binding.TVTempTarget.setTextColor(getColor(mActivity, R.color.covidpasscanner_red))
                if(sensorTargetTObjMaxAdjusted > (AppSetup.RangeTempGreen + AppSetup.RangeTempOrange))
                {
                    binding.TVUserTempReaderTitle.setText(getString(R.string.label_position_temp))
                    sensorMeasureEnabled = false
                    sensorTargetTObjMaxAdjustedIsValid = false
                }
                else
                {
                    binding.TVUserTempReaderTitle.setText(getString(R.string.label_position_temp))
                    sensorMeasureEnabled = false
                    sensorTargetTObjMaxAdjustedIsValid = true
                }
                if(DebugActive) {
                    // Valori temperature sequenze target
                    var strdate: String = ""
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
                        strdate = current.format(formatter)
                    } else {
                        var date = Date()
                        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                        strdate = formatter.format(date)
                    }
                    var s: String = strdate + "\n"
                    for (k in 0 until sensorTargetCoordPnt) {
                        s = s + getString(R.string.strint, k + 1) + " - "
                        for (l in 0 until sensTargetPositionCoordNPix) {
                            s = s + getString(
                                R.string.strf41,
                                sensorObj[sensorTargetCoordX[k][l]][sensorTargetCoordY[k][l]]
                            ) + ", "
                        }
                        s = s + "\n"
                    }
                    binding.TVUserTempReaderTitle.setText(s)
                    // Screenshot
                    ScreenshotHandler.postDelayed(ScreenshotHnd, 50)
                }
                // Sound
                if(sensorTargetTObjMaxAdjustedIsValid == true) {
                    try {
                        beepManager.playBeepSoundAndVibrate()
                    } catch (e: Exception) {
                    }
                }
                else
                {
                    toneG.startTone(ToneGenerator.TONE_SUP_PIP, 2000)
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        toneG.stopTone()
                    }, (2000 + 50).toLong())
                }
                TargetTimeout = 20
                if(sensorTargetTObjMaxAdjustedIsValid == false)
                    TargetTimeout = 40
                TargetState = true
            }
        }
        else {
            if (TargetState == false) {
                if(DebugActive) {
                    binding.TVUserTempReaderTitle.setText("")
                }
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
    }

    private fun generateThrmalBmp() {
        var idx: Int = 0;
        var idy: Int = 0;
        var mx: Float = 0.0f;
        var qx: Float = 0.0f;
        var my: Float = 0.0f;
        var qy: Float = 0.0f;

        // Calcolo Max e Min
        sensorObjMax = -1000000.0f;
        sensorObjMin = 10000000.0f;
        for (i in 0 until sensSizeY) {
            for (j in 0 until sensSizeX) {
                if (sensorObj[i][j] < sensorObjMin)
                    sensorObjMin = sensorObj[i][j]
                if (sensorObj[i][j] > sensorObjMax)
                    sensorObjMax = sensorObj[i][j]
            }
        }

        // Conversione RGB
        var m: Float = -(250.0f / (sensorObjMax - sensorObjMin))
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
                if (sensorTargetPosition == 0)
                    sensorObjImageRGB[sensorTargetCoordX[k][l]][sensorTargetCoordY[k][l]] =
                        Color.BLACK;
                else
                    sensorObjImageRGB[sensorTargetCoordX[k][l]][sensorTargetCoordY[k][l]] =
                        Color.WHITE;
            }
        }

        var bmp: Bitmap = createImage()
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

    private fun convertHSB2RGB(H: Float, S: Float, B: Float): Int {
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
                cG = t
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

    private fun getScreenShotFromView(v: View): Bitmap? {
        // create a bitmap object
        var screenshot: Bitmap? = null
        try {
            // inflate screenshot object
            // with Bitmap.createBitmap it
            // requires three parameters
            // width and height of the view and
            // the background color
            screenshot = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
            // Now draw this bitmap on a canvas
            val canvas = Canvas(screenshot)
            v.draw(canvas)
        } catch (e: Exception) {
        }
        // return the bitmap
        return screenshot
    }

    private fun saveMediaToStorage(bitmap: Bitmap)
    {
        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q (29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            context?.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
                fos?.use {
                    //Finally writing the bitmap to the output stream that we opened
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    Toast.makeText(context, "Saved to Photos", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            //These for devices running on android < Q (29)
            //So I don't think an explanation is needed here
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imagefile = File(imagesDir, filename)
            if (imagefile.exists()) imagefile.delete()
            try {
                val out = FileOutputStream(imagefile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                // sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                //     Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
                out.flush()
                out.close()
                val values = ContentValues()
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                values.put(MediaStore.Images.Media.DATA, imagefile.absolutePath)
                // .DATA is deprecated in API 29
                context?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ConvertImageToBitmapRGBA888(image : Image) : Bitmap {
        val buffer = image.getPlanes().get(0).buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    private fun ConvertImageToBitmapYUV(image : Image) : Bitmap {
        val yBuffer = image.getPlanes().get(0).buffer // Y
        val vuBuffer = image.getPlanes().get(2).buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}