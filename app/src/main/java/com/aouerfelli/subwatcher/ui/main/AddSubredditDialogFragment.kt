package com.aouerfelli.subwatcher.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.aouerfelli.subwatcher.R
import com.aouerfelli.subwatcher.databinding.AddSubredditDialogFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.chrisbanes.insetter.doOnApplyWindowInsets

class AddSubredditDialogFragment : BottomSheetDialogFragment() {

  companion object {
    const val SUBREDDIT_NAME_KEY = "subreddit_name"
  }

  private var binding: AddSubredditDialogFragmentBinding? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return AddSubredditDialogFragmentBinding.inflate(inflater, container, false)
      .also { binding = it }
      .root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    // This is only called after onCreateView(), which is where binding gets assigned.
    val binding = binding!!

    binding.root.doOnApplyWindowInsets { v, insets, initialState ->
      v.updatePadding(
        bottom = insets.systemWindowInsetBottom + initialState.paddings.bottom
      )
      v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        leftMargin = insets.systemWindowInsetLeft + initialState.margins.left
        rightMargin = insets.systemWindowInsetRight + initialState.margins.right
      }
    }

    binding.addButton.setOnClickListener {
      val subredditName = binding.subredditField.editText?.text?.toString()?.ifBlank { null }
      if (subredditName == null) {
        binding.subredditField.error = getString(R.string.add_subreddit_dialog_error)
        return@setOnClickListener
      }
      val intent = Intent().putExtra(SUBREDDIT_NAME_KEY, subredditName)
      targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
      dismiss()
    }

    binding.subredditField.editText?.setOnEditorActionListener { _, actionId, event ->
      val isDone = actionId == EditorInfo.IME_ACTION_DONE
      val isEnter = event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER
      if (isDone || isEnter) {
        binding.addButton.callOnClick()
        true
      } else {
        false
      }
    }

    val inputFilters = binding.subredditField.editText?.filters.orEmpty()
    val inputWhitespaceFilter = InputFilter { source, _, _, _, _, _ ->
      source.filterNot(Char::isWhitespace)
    }
    // Whitespace filter should come before the maxLength filter so that the character limit doesn't
    // account for possible whitespace.
    binding.subredditField.editText?.filters = arrayOf(inputWhitespaceFilter, *inputFilters)

    binding.subredditField.editText?.doAfterTextChanged {
      val isNotBlank = !it.isNullOrBlank()
      binding.addButton.isEnabled = isNotBlank
      if (isNotBlank) {
        binding.subredditField.isErrorEnabled = false
      }
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    // Disable fitsSystemWindow on the dialog container to allow the dialog to be drawn under the
    // system bars.
    // The inner container also has fitsSystemWindow disabled to prevent the text selection toolbar
    // from adding padding to the view.
    (view?.parent?.parent as? View)?.apply {
      fitsSystemWindows = false
      (parent as? View)?.fitsSystemWindows = false
    }
  }

  override fun onResume() {
    super.onResume()
    // Bring up soft input method automatically.
    binding?.subredditField?.editText?.requestFocus()
    dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }
}
