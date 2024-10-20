package com.kylecorry.intervalometer.ui

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.intervalometer.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : BoundFragment<FragmentMainBinding>() {

    @Inject
    lateinit var formatter: FormatService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.grantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val prefs = SharedPreferences(requireContext())

        binding.interval.doOnTextChanged { text, _, _, _ ->
            prefs.putLong("interval", text.toString().toLongOrNull() ?: 0)
        }

        binding.interval.setText(prefs.getLong("interval")?.toString())

        binding.shutterButtonIds.doOnTextChanged { text, _, _, _ ->
            prefs.putString("shutter_buttons", text.toString())
        }

        binding.shutterButtonIds.setText(prefs.getString("shutter_buttons"))
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(layoutInflater, container, false)
    }

    fun emitKeyPress(keyCode: Int) {
        // Emit the key press at the system level (used to trigger the camera shutter in another app)
        val instrumentation = Instrumentation()
        instrumentation.sendKeyDownUpSync(keyCode)
    }
}