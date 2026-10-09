package com.spore.gemleap.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Offers a newer Play Store version when the game opens, using Play's in-app updates.
 *
 * Normal releases use the flexible flow: the player taps "Update", the download runs in
 * the background, and a second prompt offers to restart into the new version. A release
 * published with in-app update priority 4+ switches to the immediate (blocking) flow.
 * Only builds installed from Play can see updates, so sideloaded/debug builds simply
 * never show the prompt. Construct it in `onCreate` (it registers an activity-result
 * launcher).
 */
class UpdateManager(private val activity: ComponentActivity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private var info: AppUpdateInfo? = null
    private var dismissed = false
    private var readyDismissed = false

    /** A newer version is available and the player hasn't answered the prompt yet. */
    var updateAvailable by mutableStateOf(false)
        private set

    /** A flexible update finished downloading and just needs a restart to install. */
    var updateReady by mutableStateOf(false)
        private set

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) Log.i(TAG, "Update flow ended: ${result.resultCode}")
    }

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) updateReady = !readyDismissed
    }

    /** Asks Play whether a newer version exists. Safe to call on every resume. */
    fun check() {
        manager.appUpdateInfo
            .addOnSuccessListener { i ->
                when {
                    i.installStatus() == InstallStatus.DOWNLOADED -> updateReady = !readyDismissed
                    i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && !dismissed -> {
                        info = i
                        updateAvailable = true
                    }
                    i.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                        // An immediate update was interrupted; Play resumes it.
                        start(i, AppUpdateType.IMMEDIATE)
                }
            }
            .addOnFailureListener { Log.d(TAG, "Update check skipped: ${it.message}") }
    }

    /** The player tapped "Update now". */
    fun startUpdate() {
        val i = info ?: return
        updateAvailable = false
        val type = if (i.updatePriority() >= IMMEDIATE_PRIORITY && i.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            AppUpdateType.IMMEDIATE
        } else {
            AppUpdateType.FLEXIBLE
        }
        if (!i.isUpdateTypeAllowed(type)) {
            openStore()
            return
        }
        start(i, type)
    }

    private fun start(i: AppUpdateInfo, type: Int) {
        manager.registerListener(installListener)
        runCatching {
            manager.startUpdateFlowForResult(i, launcher, AppUpdateOptions.newBuilder(type).build())
        }.onFailure {
            Log.w(TAG, "Could not start update flow: ${it.message}")
            openStore()
        }
    }

    /** The player tapped "Later"; stay quiet until the next launch. */
    fun later() {
        dismissed = true
        updateAvailable = false
    }

    /** Restarts the app into the downloaded version. */
    fun installNow() {
        updateReady = false
        manager.completeUpdate()
    }

    fun installLater() {
        readyDismissed = true
        updateReady = false
    }

    fun release() {
        manager.unregisterListener(installListener)
    }

    private fun openStore() {
        val pkg = activity.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        runCatching { activity.startActivity(market) }.onFailure {
            runCatching {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")),
                )
            }
        }
    }

    private companion object {
        const val TAG = "UpdateManager"
        const val IMMEDIATE_PRIORITY = 4
    }
}
