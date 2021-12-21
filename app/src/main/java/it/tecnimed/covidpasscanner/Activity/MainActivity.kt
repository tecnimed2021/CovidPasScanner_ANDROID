package it.tecnimed.covidpasscanner.Activity;

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.navArgs
import dagger.hilt.android.AndroidEntryPoint;
import it.tecnimed.covidpasscanner.VerificaDownloadInProgressException
import it.tecnimed.covidpasscanner.VerificaMinSDKVersionException
import it.tecnimed.covidpasscanner.VerificaMinVersionException
import it.tecnimed.covidpasscanner.data.local.PrefKeys
import it.tecnimed.covidpasscanner.model.CertificateSimple
import it.tecnimed.covidpasscanner.model.CertificateStatus
import it.tecnimed.covidpasscanner.model.VerificationViewModel
import it.tecnimed.covidpasscanner.util.FORMATTED_DATE_LAST_SYNC
import it.tecnimed.covidpasscanner.util.TimeUtility.parseTo
import it.tecnimed.covidpasscanner.BuildConfig;
import it.tecnimed.covidpasscanner.R;
import it.tecnimed.covidpasscanner.VL.VLTimer
import it.tecnimed.covidpasscanner.databinding.ActivityMainBinding
import it.tecnimed.covidpasscanner.util.FORMATTED_VALIDATION_DATE
import it.tecnimed.covidpasscanner.util.TimeUtility.formatDateOfBirth
import kotlin.ExperimentalUnsignedTypes;

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

//        private var GP_SCARABINO = "HC1:6BFOXN%TS3DHC S+/C6Y5J:74H95NL-AH.TAIOOA%I+645DOE+IKGOTKGRJPC%OQHIZC4.OI:OIC*I80PWW2G%89-85QNG.8G%8*743\$2L4LH*A:%05\$0958L956D6XHQ\$ZO3KLET4615IHP8-R6QQG9RN95U/32+8\$C9HKMIQC89M1WM8UKVPCODM/EM/H8/AI1JAA/C/NJ9IKSCHUKN HGWUJ6HG0KLETIVUKHJL:SHVII1UI4KLD.K%*GFTHXUJ6DBZ.C*HJG.CZ.CTC91FA5H8YJDF.9/H8BJC/DD%JD0D9E2LBHHGKLO-K%FG5IAMEADII-GGUJKXGGEC8.-B97U3-SY\$NU\$PH5DS34S/4348: KJQ76/4-Z7AT4.1S.28QNQDCQK+4YVQR95-:1.:A:TUUVPQRHIY1+ HNQ1PRAAUIWVHON1L%EIHPTU7*FC*LBYAV5L6PTF.L3E%3CGIA3FUMT7190MOLDDJ7UQ C3FOC3AG\$C0OLR.UUBH28NTIBRVPT*4O4RKH671R\$LCDJIEPKS JC 3SPU6Y0SZSQ7G"
//        private var GP_FEDE = "HC1:6BFOXN%TS3DHPVO13J /G-/2YRVA.Q/R8VRU2FC1J9M\$DI9C6H9.2R\$7C:QU7JM:UC*GPXS40 LHZATG90OA/.DV2MGDIK3MXGG HG4HGBIKLIA5D8MJKQJK:JMO-KPGG6IASD9M82IAB4EOHCRM47/9738D1JAA/C/NJ9IK6CH9VL3LNGKLYHI0UJ SHKIJ0IHMIHCVM2RIUBINJS6NDW+S*WD:XIBEIVG395EV3EVCK09D5WC.XIMXB8UJ06JSVBDKBY8M0OIOTI4BL%ZJ8OA0OIFVA.QO5VA81K0ECM8CCR1LKREA7IB6\$C94JB9C9FDJK2A:ULA7IX6KMIA3 3T/5-Z76T4/0Q+-OLHPQKRR95-:1Y*9UM97H98\$QJEQF69AKPNF0+CS*88350 GM4H46L*5O6DEDKU.SI%RS2FQ7GG+044OM:BR:O5UJMH/KK4W:+V.XTQLVV1SH8K+6NWF4YA1PDDFP1*9T%3B7WR2%VA8R6+7 ETVFHHB04.GLDF"
        private var GP_FRANCO = "HC1:6BFOXN%TS3DHC S+/C6Y5J:74H95NL-AHXP2IOOA%I+645DOX-IK5R%QHRJPC%OQHIZC4.OI1RM8ZA.A57\$PMKN4NN3F85QNCY0O%0D0H0 2OC9JU0D0HT0HB2PL/IB*09B9LW4G%89-85QNG.8:Y0YE9/MVEK0WLIFO5ON1NX7T%P0\$J3X56*J.ZJ:U32T9%T5%T58YJXQ1FS9*T5R\$98\$B..P9.I SI5K1*TB3:U-1VVS1UU1CIG3ZCP9JH0D3ZCU7JB+2CN54%K*\$S1E5:15DZI\$XK8X2.36D-I/2DBAJDAJMJ0*%6Q3QR\$P2OIC0J%PIC.SJ SSPTJVTTPTHOJF1JB\$PF/94O57UE/NE%QEQV1A6VQK9TN84PIQJAZGA+1V2:UJMI:TU+MMWW5SZ9.T1+ZEYV5GT3S.FF38E130FVIQN1XH313NDDB%2U7JC/PGQU4IS4/S+/FYEWWDE5QAA.IWQPBFBSMR3W43U520NHBNOZVV\$UU.ML0HGKT KPCET4W7F004:EE2"

        private var GP = GP_FRANCO;

        private lateinit var binding: ActivityMainBinding
        private lateinit var shared: SharedPreferences

        private val viewModel by viewModels<VerificationViewModel>()
        private lateinit var certificateModel: CertificateSimple

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                binding = ActivityMainBinding.inflate(layoutInflater)
                shared = this.getSharedPreferences(PrefKeys.USER_PREF, Context.MODE_PRIVATE)
                setContentView(binding.root)

                viewModel.certificate.observe(this) { certificate ->
                        certificate?.let {
                                certificateModel = it
                                Log.d("Person", it.person?.familyName.plus(" ").plus(it.person?.givenName));
                                Log.d("Date of Birth", it.dateOfBirth?.formatDateOfBirth() ?: "");
                                if(certificate.certificateStatus == CertificateStatus.VALID)
                                        Log.d("Cert Status", "VALID");
                                else if(certificate.certificateStatus == CertificateStatus.NOT_VALID)
                                        Log.d("Cert Status", "NOT VALID");
                                else if(certificate.certificateStatus == CertificateStatus.NOT_VALID_YET)
                                        Log.d("Cert Status", "NOT VALID YET");
                                else if(certificate.certificateStatus == CertificateStatus.NOT_EU_DCC)
                                        Log.d("Cert Status", "NOT EU DCC");
                                Log.d("Timestamp", it.timeStamp?.parseTo(FORMATTED_VALIDATION_DATE));
                                if (
                                        viewModel.getTotemMode() &&
                                        (certificate.certificateStatus == CertificateStatus.VALID)
                                ) {
                                        Log.d("Validita", "Valido");
                                }
                        }
                }
                viewModel.inProgress.observe(this) {
//                        binding.progressBar.isVisible = it
                }

                try {
                        viewModel.init(GP, true)
                } catch (e: VerificaMinSDKVersionException) {
                        Log.d("VerificationFragment", "Min SDK Version Exception")
//                        createForceUpdateDialog(getString(R.string.updateMessage))
                } catch (e: VerificaMinVersionException) {
                        Log.d("VerificationFragment", "Min App Version Exception")
//                        createForceUpdateDialog(getString(R.string.updateMessage))
                } catch (e: VerificaDownloadInProgressException) {
                        Log.d("VerificationFragment", "Download In Progress Exception")
//                        createForceUpdateDialog(getString(R.string.messageDownloadStarted))
                }
        }

        override fun onStart() {
                super.onStart()
                shared.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
                if (key != null) {
/*
                        when (key) {
                                PrefKeys.CURRENT_CHUNK -> {
                                        updateDownloadedPackagesCount()
                                        Log.i(key.toString(), viewModel.getCurrentChunk().toString())
                                }
                                PrefKeys.KEY_TOTAL_CHUNK -> {
                                        val totalChunk = viewModel.getTotalChunk().toInt()
                                        binding.updateProgressBar.max = totalChunk
                                        binding.updateProgressBar.show()
                                        binding.chunkCount.show()
                                        binding.chunkSize.show()
                                        updateDownloadedPackagesCount()
                                        Log.i(PrefKeys.KEY_TOTAL_CHUNK, totalChunk.toString())
                                }
                                PrefKeys.AUTH_TO_RESUME -> {
                                        val authToResume = viewModel.getResumeAvailable().toInt()
                                        Log.i(PrefKeys.AUTH_TO_RESUME, authToResume.toString())
                                        if (viewModel.getResumeAvailable() == 0L) {
                                                binding.resumeDownload.show()
                                                binding.qrButton.background.alpha = 128
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
                        }*/
                }
        }

        override fun onClick(v: View?) {
/*
                if (v?.id == R.id.qrButton) {
                        viewModel.getDateLastSync().let {
                                if (!viewModel.getScanModeFlag() && v.id != R.id.scan_mode_button) {
                                        createNoScanModeChosenAlert()
                                        return
                                } else if (it == -1L) {
                                        createNoSyncAlertDialog(getString(R.string.noKeyAlertMessage))
                                        return
                                }
                        }
                }

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
                when (v?.id) {
                        R.id.qrButton -> checkCameraPermission()
                        R.id.settings -> openSettings()
                        R.id.scan_mode_button -> showScanModeChoiceAlertDialog()
                }

 */
        }
}
