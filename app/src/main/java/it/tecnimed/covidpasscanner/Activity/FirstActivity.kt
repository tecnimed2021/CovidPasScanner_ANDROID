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

package it.tecnimed.covidpasscanner.Activity;

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import android.view.View.*
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import it.tecnimed.covidpasscanner.Activity.MainActivity
import it.tecnimed.covidpasscanner.BuildConfig
import it.tecnimed.covidpasscanner.Fragment.CodeReaderFragment
import it.tecnimed.covidpasscanner.Fragment.CodeVerificationFragment
import it.tecnimed.covidpasscanner.Fragment.CodeVerificationFragment_GeneratedInjector
import it.tecnimed.covidpasscanner.Fragment.UserDataReaderFragment
import it.tecnimed.covidpasscanner.R
import it.tecnimed.covidpasscanner.VerificaApplication
import it.tecnimed.covidpasscanner.databinding.ActivityFirstBinding
import it.tecnimed.covidpasscanner.data.local.PrefKeys
import it.tecnimed.covidpasscanner.model.CertificateSimple
import it.tecnimed.covidpasscanner.model.FirstViewModel
import it.tecnimed.covidpasscanner.uart.UARTDriver
import it.tecnimed.covidpasscanner.util.ConversionUtility
import it.tecnimed.covidpasscanner.util.FORMATTED_DATE_LAST_SYNC
import it.tecnimed.covidpasscanner.util.TimeUtility.parseTo
import it.tecnimed.covidpasscanner.util.Utility
import java.util.*

@AndroidEntryPoint
class FirstActivity : AppCompatActivity(), View.OnClickListener,
    CodeReaderFragment.OnFragmentInteractionListener,
    CodeVerificationFragment.OnFragmentInteractionListener,
    UserDataReaderFragment.OnFragmentInteractionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityFirstBinding
    private lateinit var shared: SharedPreferences

    private val viewModel by viewModels<FirstViewModel>()

    private val verificaApplication = VerificaApplication()

    private val mContext: Context? = this
    private val mSerilaDrv: UARTDriver? = UARTDriver.create(mContext)

    private lateinit var mCodeReaderFrag: Fragment;
    private lateinit var mCodeVerificationFrag: Fragment;
    private lateinit var mUserDataReaderFrag: Fragment;

    private val requestPermissionLauncherQr =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openQrCodeReader()
            }
        }
    private val requestPermissionLauncherOcr =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openOcrReader()
            }
        }

    override fun onFragmentInteraction(qrcodeText: String) {
        val fm = supportFragmentManager
        val tr = fm.beginTransaction()
        if(qrcodeText != ""){
            var crf : Fragment = CodeVerificationFragment.newInstance(qrcodeText)
            tr.replace(R.id.frag_anch_point, crf)
            mCodeVerificationFrag = crf
            tr.commitAllowingStateLoss()
        }
        else{
            tr.remove(mCodeReaderFrag)
            tr.commitAllowingStateLoss()
        }
    }

    override fun onFragmentInteraction(certSimple: CertificateSimple) {
        var cert : CertificateSimple
        val fm = supportFragmentManager
        val tr = fm.beginTransaction()
        tr.remove(mCodeVerificationFrag)
        tr.commitAllowingStateLoss()
    }

    override fun onFragmentInteraction() {
        val fm = supportFragmentManager
        val tr = fm.beginTransaction()
        tr.remove(mUserDataReaderFrag)
        tr.commitAllowingStateLoss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstBinding.inflate(layoutInflater)
        shared = this.getSharedPreferences(PrefKeys.USER_PREF, Context.MODE_PRIVATE)
        setContentView(binding.root)
        setSecureWindowFlags()
        setOnClickListeners()
        setupUI()
        observeLiveData()
    }

    private fun observeLiveData() {
        observeSyncStatus()
        observeRetryCount()
        observeSizeOverThreshold()
        observeInitDownload()
        observeScanMode()
    }

    private fun observeInitDownload() {
        viewModel.initDownloadLiveData.observe(this) {
            if (it) {
                enableInitDownload()
            }
        }
    }

    private fun observeSizeOverThreshold() {
        viewModel.sizeOverLiveData.observe(this) {
            if (it) {
                createDownloadAlert()
            }
        }
    }

    private fun observeRetryCount() {
        viewModel.maxRetryReached.observe(this) {
            if (it) {
                enableInitDownload()
            }
        }
    }

    private fun observeScanMode() {
        viewModel.scanMode.observe(this, {
            setScanModeTexts(it)
        })
    }

    private fun observeSyncStatus() {
        viewModel.fetchStatus.observe(this) {
            if (it) {
                binding.qrButton.background.alpha = 128
                binding.ocrButton.background.alpha = 128
            } else {
                if (!viewModel.getIsPendingDownload() && viewModel.maxRetryReached.value == false) {
                    viewModel.getDateLastSync().let { date ->
                        binding.dateLastSyncText.text = getString(
                            R.string.lastSyncDate,
                            if (date == -1L) getString(R.string.notAvailable) else date.parseTo(
                                FORMATTED_DATE_LAST_SYNC
                            )
                        )
                    }
                    binding.qrButton.background.alpha = 255
                    binding.ocrButton.background.alpha = 255
                    hideDownloadProgressViews()
                }
            }
        }
    }

    private fun setupUI() {
        val string = getString(R.string.version, BuildConfig.VERSION_NAME)
        val spannableString = SpannableString(string).also {
            it.setSpan(UnderlineSpan(), 0, it.length, 0)
            it.setSpan(StyleSpan(Typeface.BOLD), 0, it.length, 0)
        }
        binding.versionText.text = spannableString
        binding.dateLastSyncText.text = getString(R.string.loading)

        binding.updateProgressBar.max = viewModel.getTotalChunk().toInt()
        updateDownloadedPackagesCount()

        viewModel.getResumeAvailable().let {
            if (it != -1L) {
                if (it == 0.toLong() || viewModel.getIsPendingDownload()) {
                    binding.qrButton.background.alpha = 128
                    binding.ocrButton.background.alpha = 128
                    binding.resumeDownload.visibility = VISIBLE
                    binding.dateLastSyncText.text = getString(R.string.incompleteDownload)
                    binding.chunkCount.visibility = VISIBLE
                    binding.chunkSize.visibility = VISIBLE
                    binding.updateProgressBar.visibility = VISIBLE
                } else {
                    binding.resumeDownload.visibility = INVISIBLE
                }
            }
        }
    }

    private fun setSecureWindowFlags() {
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    private fun setOnClickListeners() {
        binding.qrButton.setOnClickListener(this)
        binding.ocrButton.setOnClickListener(this)
        binding.scanModeButton.setOnClickListener(this)
        binding.initDownload.setOnClickListener {
            if (Utility.isOnline(this)) {
                startDownload()
            } else {
                createCheckConnectionAlertDialog()
            }
        }

        binding.resumeDownload.setOnClickListener {
            if (Utility.isOnline(this)) {
                viewModel.setResumeAsAvailable()
                binding.resumeDownload.visibility = INVISIBLE
                binding.dateLastSyncText.text = getString(R.string.updatingRevokedPass)
                startSyncData()
            } else {
                createCheckConnectionAlertDialog()
            }
        }

        binding.uartButton.setOnClickListener{
            binding.uartTest.text = "Open OK";
            if(mSerilaDrv?.init() == false)
            {
                binding.uartTest.text = "Init Fail";
            }
            else {
                if (mSerilaDrv?.openPort(
                        UARTDriver.UARTDRIVER_PORT_MODE_NOEVENT,
                        0,
                        38400,
                        UARTDriver.UARTDRIVER_STOPBIT_2,
                        UARTDriver.UARTDRIVER_PARITY_NONE
                    ) == false
                ) {
                    binding.uartTest.text = "Open Fail";
                }
            }
        }
    }

    private fun setScanModeTexts(currentScanMode: String) {
        if (!viewModel.getScanModeFlag()) {
            val s = SpannableStringBuilder()
                .bold { append(getString(R.string.label_choose_scan_mode)) }
            binding.scanModeButton.text = s
        } else {
            var chosenScanMode =
                if (currentScanMode == "3G") getString(R.string.scan_mode_3G_header) else getString(
                    R.string.scan_mode_2G_header
                )
            chosenScanMode += "\n"
            val chosenScanModeText =
                if (currentScanMode == "3G") getString(R.string.scan_mode_3G) else getString(R.string.scan_mode_2G)
            val s = SpannableStringBuilder()
                .bold { append(chosenScanMode) }
                .append(chosenScanModeText)
            binding.scanModeButton.text = s
        }
    }

    private fun startDownload() {
        prepareForDownload()
        showDownloadProgressViews()
        binding.initDownload.visibility = INVISIBLE
        binding.dateLastSyncText.text = getString(R.string.updatingRevokedPass)
        startSyncData()
    }

    private fun createCheckConnectionAlertDialog() {
        val builder = AlertDialog.Builder(this)
        var dialog: AlertDialog? = null
        builder.setTitle(
            getString(R.string.no_internet_title)
        )
        builder.setMessage(getString(R.string.no_internet_message))
        builder.setPositiveButton(getString(R.string.ok)) { _, _ -> }
        dialog = builder.create()
        dialog.show()
    }


    private fun checkCameraPermissionQr() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            createPermissionAlertQr()
        } else {
            openQrCodeReader()
        }
    }

    private fun checkCameraPermissionOcr() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            createPermissionAlertOcr()
        } else {
            openOcrReader()
        }
    }

    private fun createPermissionAlertQr() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.privacyTitle))
            builder.setMessage(getString(R.string.privacy))
            builder.setPositiveButton(getString(R.string.next)) { _, _ ->
                requestPermissionLauncherQr.launch(Manifest.permission.CAMERA)
            }
            builder.setNegativeButton(getString(R.string.back)) { _, _ ->
            }
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            requestPermissionLauncherQr.launch(Manifest.permission.CAMERA)
        }
    }

    private fun createPermissionAlertOcr() {
        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.privacyTitle))
            builder.setMessage(getString(R.string.privacy))
            builder.setPositiveButton(getString(R.string.next)) { _, _ ->
                requestPermissionLauncherOcr.launch(Manifest.permission.CAMERA)
            }
            builder.setNegativeButton(getString(R.string.back)) { _, _ ->
            }
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            requestPermissionLauncherOcr.launch(Manifest.permission.CAMERA)
        }
    }

    private fun createDownloadAlert() {
        try {
            val builder = AlertDialog.Builder(this)
            var dialog: AlertDialog? = null
            builder.setTitle(
                getString(
                    R.string.titleDownloadAlert,
                    ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
                )
            )
            builder.setMessage(
                getString(
                    R.string.messageDownloadAlert,
                    ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
                )
            )
            builder.setPositiveButton(getString(R.string.label_download)) { _, _ ->
                dialog?.dismiss()
                if (Utility.isOnline(this)) {
                    startDownload()
                } else {
                    createCheckConnectionAlertDialog()
                    enableInitDownload()
                }
            }
            builder.setNegativeButton(getString(R.string.after_download)) { _, _ ->
                enableInitDownload()
                dialog?.dismiss()
            }
            dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()
        } catch (e: Exception) {
        }
    }

    private fun prepareForDownload() {
        viewModel.resetCurrentRetry()
        viewModel.setShouldInitDownload(true)
        viewModel.setDownloadAsAvailable()
    }

    private fun startSyncData() {
        verificaApplication.setWorkManager()
    }

    private fun enableInitDownload() {
        binding.resumeDownload.visibility = INVISIBLE
        binding.initDownload.visibility = VISIBLE
        binding.qrButton.background.alpha = 128
        binding.ocrButton.background.alpha = 128
        hideDownloadProgressViews()
        binding.dateLastSyncText.text = when (viewModel.getTotalSizeInByte()) {
            0L -> {
                hideDownloadProgressViews()
                getString(
                    R.string.label_download_alert_simple
                )
            }
            else ->
                getString(
                    R.string.label_download_alert_complete,
                    ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
                )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!shared.getBoolean("scan_mode_flag", false)) {
            val s = SpannableStringBuilder()
                .bold { append(getString(R.string.label_choose_scan_mode)) }
            binding.scanModeButton.text = s
        } else {
            var chosenScanMode =
                if (shared.getString(
                        "scan_mode",
                        "3G"
                    ) == "3G"
                ) getString(R.string.scan_mode_3G_header) else getString(R.string.scan_mode_2G_header)
            chosenScanMode += "\n"
            val chosenScanModeText =
                if (shared.getString(
                        "scan_mode",
                        "3G"
                    ) == "3G"
                ) getString(R.string.scan_mode_3G) else getString(R.string.scan_mode_2G)
            val s = SpannableStringBuilder()
                .bold { append(chosenScanMode) }
                .append(chosenScanModeText)
            binding.scanModeButton.text = s
        }
        viewModel.getAppMinVersion().let {
            if (Utility.versionCompare(
                    it,
                    BuildConfig.VERSION_NAME
                ) > 0 || viewModel.isSDKVersionObsoleted()
            ) {
                createForceUpdateDialog()
            }
        }
    }

    private fun openQrCodeReader() {
        var crf : Fragment = CodeReaderFragment()
        val fm = supportFragmentManager
        val tr = fm.beginTransaction()
        tr.add(R.id.frag_anch_point, crf)
        tr.commitAllowingStateLoss()
        mCodeReaderFrag = crf
    }

    private fun openOcrReader() {
        val fm = supportFragmentManager
        val tr = fm.beginTransaction()
        var crf : Fragment = UserDataReaderFragment.newInstance("", "")
        tr.add(R.id.frag_anch_point, crf)
        tr.commitAllowingStateLoss()
        mUserDataReaderFrag = crf
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.qrButton) {
            viewModel.getDrlDateLastSync().let {
                if (binding.resumeDownload.isVisible) {
                    createNoSyncAlertDialog(getString(R.string.label_drl_download_in_progress))
                    return
                }
                if ((viewModel.getIsDrlSyncActive() && System.currentTimeMillis() >= it + 24 * 60 * 60 * 1000) ||
                    (viewModel.getIsDrlSyncActive() && it == -1L)
                ) {
                    createNoSyncAlertDialog(getString(R.string.noKeyAlertMessageForDrl))
                    return
                }
            }
        }
        if (v?.id == R.id.ocrButton) {
            viewModel.getDrlDateLastSync().let {
                if (binding.resumeDownload.isVisible) {
                    createNoSyncAlertDialog(getString(R.string.label_drl_download_in_progress))
                    return
                }
                if ((viewModel.getIsDrlSyncActive() && System.currentTimeMillis() >= it + 24 * 60 * 60 * 1000) ||
                    (viewModel.getIsDrlSyncActive() && it == -1L)
                ) {
                    createNoSyncAlertDialog(getString(R.string.noKeyAlertMessageForDrl))
                    return
                }
            }
        }

        when (v?.id) {
            R.id.qrButton -> checkCameraPermissionQr()
            R.id.ocrButton -> checkCameraPermissionOcr()
            R.id.scan_mode_button -> showScanModeChoiceAlertDialog()
        }
    }

    private fun showScanModeChoiceAlertDialog() {
        val mBuilder = AlertDialog.Builder(this)
        val chosenScanMode = if (viewModel.getScanMode() == "3G") 1 else 0
        val scanModeChoices = arrayOf(
            getString(
                R.string.label_alert_dialog_option,
                getString(R.string.scan_mode_2G_header).substringAfter(
                    ' '
                ).toUpperCase(Locale.ROOT),
                getString(R.string.scan_mode_2G)

            ),
            getString(
                R.string.label_alert_dialog_option,
                getString(R.string.scan_mode_3G_header).substringAfter(' ').toUpperCase(
                    Locale.ROOT
                ),
                getString(R.string.scan_mode_3G)

            )
        )

        mBuilder.setTitle(getString(R.string.label_scan_mode))
        mBuilder.setSingleChoiceItems(scanModeChoices, chosenScanMode) { dialog, which ->
            if (!viewModel.getScanModeFlag()) viewModel.setScanModeFlag(true)
            if (which == 0) {
                viewModel.setScanMode("2G")
            } else if (which == 1) {
                viewModel.setScanMode("3G")
            }
            dialog.dismiss()
        }
        val mDialog = mBuilder.create()
        mDialog.setCancelable(false)
        mDialog.show()
    }

    private fun createNoScanModeChosenAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.noKeyAlertTitle))
        builder.setMessage(SpannableString(getString(R.string.label_no_scan_mode_chosen)).also {
            Linkify.addLinks(it, Linkify.ALL)
        })
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
        val alertMessage = dialog.findViewById<TextView>(android.R.id.message) as TextView
        alertMessage.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun createNoSyncAlertDialog(alertMessage: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.noKeyAlertTitle))
        builder.setMessage(alertMessage)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun createForceUpdateDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.updateTitle))
        builder.setMessage(getString(R.string.updateMessage))

        builder.setPositiveButton(getString(R.string.updateLabel)) { _, _ ->
            openGooglePlay()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun openGooglePlay() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        shared.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != null) {
            when (key) {
                PrefKeys.CURRENT_CHUNK -> {
                    updateDownloadedPackagesCount()
                    Log.i(key.toString(), viewModel.getCurrentChunk().toString())
                }
                PrefKeys.KEY_TOTAL_CHUNK -> {
                    val totalChunk = viewModel.getTotalChunk().toInt()
                    binding.updateProgressBar.max = totalChunk
                    binding.updateProgressBar.visibility = VISIBLE
                    binding.chunkCount.visibility = VISIBLE
                    binding.chunkSize.visibility = VISIBLE
                    updateDownloadedPackagesCount()
                    Log.i(PrefKeys.KEY_TOTAL_CHUNK, totalChunk.toString())
                }
                PrefKeys.AUTH_TO_RESUME -> {
                    val authToResume = viewModel.getResumeAvailable().toInt()
                    Log.i(PrefKeys.AUTH_TO_RESUME, authToResume.toString())
                    if (viewModel.getResumeAvailable() == 0L) {
                        binding.resumeDownload.visibility = VISIBLE
                        binding.qrButton.background.alpha = 128
                        binding.ocrButton.background.alpha = 128
                    }
                }
                PrefKeys.KEY_DRL_DATE_LAST_FETCH -> {
                    viewModel.getDateLastSync().let { date ->
                        binding.dateLastSyncText.text = getString(
                            R.string.lastSyncDate,
                            if (date == -1L) getString(R.string.notAvailable) else date.parseTo(
                                FORMATTED_DATE_LAST_SYNC
                            )
                        )
                    }
                    hideDownloadProgressViews()
                }
                else -> {

                }
            }
        }
    }

    private fun hideDownloadProgressViews() {
        binding.updateProgressBar.visibility = INVISIBLE
        binding.chunkCount.visibility = INVISIBLE
        binding.chunkSize.visibility = INVISIBLE
    }

    private fun showDownloadProgressViews() {
        binding.updateProgressBar.visibility = VISIBLE
        binding.chunkCount.visibility = VISIBLE
        binding.chunkSize.visibility = VISIBLE
    }

    override fun onBackPressed() {
        moveTaskToBack(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        shared.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun updateDownloadedPackagesCount() {
        val lastDownloadedChunk = viewModel.getCurrentChunk().toInt()
        val lastChunk = viewModel.getTotalChunk().toInt()
        val singleChunkSize = viewModel.getSizeSingleChunkInByte()

        binding.updateProgressBar.progress = lastDownloadedChunk
        binding.chunkCount.text = getString(R.string.chunk_count, lastDownloadedChunk, lastChunk)
        binding.chunkSize.text = getString(
            R.string.chunk_size,
            ConversionUtility.byteToMegaByte((lastDownloadedChunk * singleChunkSize.toFloat())),
            ConversionUtility.byteToMegaByte(viewModel.getTotalSizeInByte().toFloat())
        )
    }
}
