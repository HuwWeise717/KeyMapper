package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConfigKeymapBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.adapter.ConfigKeymapPagerAdapter
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.util.result.getFullMessage
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.coroutines.showAndAwait
import splitties.alertdialog.appcompat.message
import splitties.experimental.ExperimentalSplittiesApi
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.snackbar.snack

/**
 * Created by sds100 on 19/02/2020.
 */
@ExperimentalSplittiesApi
class ConfigKeymapFragment : Fragment() {
    private val mArgs by navArgs<ConfigKeymapFragmentArgs>()

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mArgs.keymapId)
    }

    private val mFragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String) =
            when (className) {
                TriggerFragment::class.java.name -> TriggerFragment(mArgs.keymapId)
                ActionsFragment::class.java.name -> ActionsFragment(mArgs.keymapId)
                KeymapOptionsFragment::class.java.name -> KeymapOptionsFragment(mArgs.keymapId)
                KeymapConstraintsFragment::class.java.name -> KeymapConstraintsFragment(mArgs.keymapId)
                ConstraintsAndMoreFragment::class.java.name -> ConstraintsAndMoreFragment()

                else -> super.instantiate(classLoader, className)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = mFragmentFactory

        super.onCreate(savedInstanceState)
    }

    @ExperimentalSplittiesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentConfigKeymapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            viewPager.adapter = ConfigKeymapPagerAdapter(this@ConfigKeymapFragment, mArgs.keymapId)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = strArray(R.array.config_keymap_tab_titles)[position]
            }.attach()

            requireActivity().onBackPressedDispatcher.addCallback {
                findNavController().navigateUp()
            }

            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_save -> {
                        mViewModel.saveKeymap(lifecycleScope)
                        findNavController().navigateUp()

                        true
                    }

                    R.id.action_help -> {
                        val direction = ConfigKeymapFragmentDirections.actionGlobalHelpFragment()
                        findNavController().navigate(direction)

                        true
                    }

                    else -> false
                }
            }

            mViewModel.showOnboardingPrompt.observe(viewLifecycleOwner, EventObserver {

                lifecycleScope.launchWhenCreated {
                    val approvedWarning = requireActivity().alertDialog {
                        message = str(it.message)

                    }.showAndAwait(okValue = true, cancelValue = null, dismissValue = false)

                    if (approvedWarning) {
                        it.onApproved.invoke()
                    }
                }
            })

            mViewModel.showFixActionPrompt.observe(viewLifecycleOwner, EventObserver {
                coordinatorLayout.longSnack(it.getFullMessage(requireContext())) {

                    //only add an action to fix the error if the error can be recovered from
                    if (it is RecoverableFailure) {
                        action(R.string.snackbar_fix) {
                            lifecycleScope.launch {
                                it.recover(requireActivity()) {
                                    mViewModel.rebuildActionModels()
                                }
                            }
                        }
                    }

                    show()
                }
            })

            mViewModel.startRecordingTriggerInService.observe(viewLifecycleOwner, EventObserver {
                val serviceEnabled = AccessibilityUtils.isServiceEnabled(requireContext())

                if (serviceEnabled) {
                    MyAccessibilityService.provideBus().value =
                        Event(MyAccessibilityService.EVENT_RECORD_TRIGGER to null)
                } else {
                    mViewModel.promptToEnableAccessibilityService.value = Event(Unit)
                }
            })

            mViewModel.promptToEnableAccessibilityService.observe(viewLifecycleOwner, EventObserver {
                coordinatorLayout.snack(R.string.error_accessibility_service_disabled_record_trigger) {
                    setAction(str(R.string.snackbar_fix)) {
                        AccessibilityUtils.enableService(requireContext())
                    }
                }
            })

            return this.root
        }
    }
}