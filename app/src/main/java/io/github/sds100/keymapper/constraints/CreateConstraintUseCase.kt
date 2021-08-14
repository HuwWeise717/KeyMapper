package io.github.sds100.keymapper.constraints

import android.os.Build
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.util.Error

/**
 * Created by sds100 on 06/07/2021.
 */

class CreateConstraintUseCaseImpl(
    private val networkAdapter: NetworkAdapter
) : CreateConstraintUseCase {

    override fun isSupported(constraint: ChooseConstraintType): Error? {
        when (constraint) {
            ChooseConstraintType.FLASHLIGHT_ON, ChooseConstraintType.FLASHLIGHT_OFF ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.M)
                }
        }

        return null
    }

    override fun getKnownWiFiSSIDs(): List<String>? {
        return networkAdapter.getKnownWifiSSIDs()
    }
}

interface CreateConstraintUseCase {
    fun isSupported(constraint: ChooseConstraintType): Error?
    fun getKnownWiFiSSIDs(): List<String>?
}