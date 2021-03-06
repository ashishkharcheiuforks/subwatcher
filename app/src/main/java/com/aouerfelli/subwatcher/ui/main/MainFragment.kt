package com.aouerfelli.subwatcher.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import com.aouerfelli.subwatcher.BuildConfig
import com.aouerfelli.subwatcher.R
import com.aouerfelli.subwatcher.Subreddit
import com.aouerfelli.subwatcher.databinding.MainFragmentBinding
import com.aouerfelli.subwatcher.repository.Result
import com.aouerfelli.subwatcher.repository.SubredditName
import com.aouerfelli.subwatcher.repository.asUrl
import com.aouerfelli.subwatcher.ui.BaseFragment
import com.aouerfelli.subwatcher.util.EventSnackbar
import com.aouerfelli.subwatcher.util.SnackbarLength
import com.aouerfelli.subwatcher.util.extensions.launch
import com.aouerfelli.subwatcher.util.extensions.onSwipe
import com.aouerfelli.subwatcher.util.extensions.setThemeColorScheme
import com.aouerfelli.subwatcher.util.makeSnackbar
import com.aouerfelli.subwatcher.util.observe
import com.aouerfelli.subwatcher.util.toAndroidString
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import timber.log.warn
import javax.inject.Inject

class MainFragment : BaseFragment<MainFragmentBinding, MainViewModel>() {

  companion object {
    private const val ADD_SUBREDDIT_REQUEST_CODE = 2
  }

  @Inject
  lateinit var viewModelFactory: MainViewModel.Factory
  override val viewModelClass = MainViewModel::class

  @Inject
  lateinit var imageLoader: ImageLoader
  private lateinit var subredditListAdapter: SubredditListAdapter

  private val eventSnackbar = EventSnackbar()

  override fun inflateView(
    inflater: LayoutInflater,
    root: ViewGroup?,
    attachToRoot: Boolean
  ): MainFragmentBinding {
    return MainFragmentBinding.inflate(inflater, root, attachToRoot)
  }

  override fun createViewModel(handle: SavedStateHandle) = viewModelFactory.create(handle)

  override fun onBindingCreated(binding: MainFragmentBinding, savedInstanceState: Bundle?) {
    subredditListAdapter = SubredditListAdapter(imageLoader) { subreddit, viewContext ->
      subreddit.name.asUrl().launch(viewContext)
      viewModel.updateLastPosted(subreddit)
    }
    binding.subredditList.adapter = subredditListAdapter
    binding.subredditList.onSwipe { viewHolder, _ ->
      val position = viewHolder.adapterPosition
      val item = subredditListAdapter.currentList[position]
      viewModel.delete(item)
    }
    binding.subredditList.doOnApplyWindowInsets { view, insets, initialState ->
      view.updatePadding(
        left = insets.systemWindowInsetLeft + initialState.paddings.left,
        top = insets.systemWindowInsetTop + initialState.paddings.top,
        right = insets.systemWindowInsetRight + initialState.paddings.right,
        bottom = insets.systemWindowInsetBottom + initialState.paddings.bottom
      )
    }

    binding.subredditsRefresh.setThemeColorScheme()
    binding.subredditsRefresh.setOnRefreshListener {
      viewModel.refresh()
    }

    binding.addSubredditButton.setOnClickListener {
      val dialogFragment = AddSubredditDialogFragment()
      dialogFragment.setTargetFragment(this, ADD_SUBREDDIT_REQUEST_CODE)
      dialogFragment.show(requireActivity().supportFragmentManager, dialogFragment.tag)
    }
    binding.addSubredditButton.setOnLongClickListener {
      if (BuildConfig.DEBUG) {
        viewModel.add(SubredditName("random"))
        true
      } else {
        false
      }
    }
    binding.addSubredditButton.doOnApplyWindowInsets { view, insets, initialState ->
      view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        updateMargins(
          left = insets.systemWindowInsetLeft + initialState.margins.left,
          top = insets.systemWindowInsetTop + initialState.margins.top,
          right = insets.systemWindowInsetRight + initialState.margins.right,
          bottom = insets.systemWindowInsetBottom + initialState.margins.bottom
        )
      }
    }

    viewModel.subredditList
      .onEach { list ->
        binding.emptyStateContainer.isGone = list.isNotEmpty()
        subredditListAdapter.submitList(list)
        binding.subredditsRefresh.isEnabled = list.isNotEmpty()
      }
      .launchIn(viewLifecycleOwner.lifecycleScope)
    viewModel.isLoading.observe(viewLifecycleOwner, binding.subredditsRefresh::setRefreshing)
    viewModel.refreshedSubreddits.observe(viewLifecycleOwner, ::onSubredditsRefreshed)
    viewModel.addedSubreddit.observe(viewLifecycleOwner, ::onSubredditAdded)
    viewModel.deletedSubreddit.observe(viewLifecycleOwner, ::onSubredditDeleted)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (resultCode != Activity.RESULT_OK) return
    when (requestCode) {
      ADD_SUBREDDIT_REQUEST_CODE -> {
        val key = AddSubredditDialogFragment.SUBREDDIT_NAME_KEY
        val subredditName = data?.getStringExtra(key)?.let(::SubredditName)
        if (subredditName != null) {
          viewModel.add(subredditName)
        }
      }
    }
  }

  private inline fun onError(result: Result.Error, crossinline onHandled: () -> Unit) {
    val stringRes = when (result) {
      Result.Error.ConnectionError -> R.string.no_connection
      Result.Error.NetworkError -> R.string.server_unreachable
    }
    val snackbar = binding?.root?.makeSnackbar(stringRes.toAndroidString())
      ?.setAnchorView(binding?.addSubredditButton)
    eventSnackbar.set(snackbar) { onHandled() }
  }

  private fun onSubredditsRefreshed(result: Result<Nothing>) {
    when (result) {
      is Result.Success.Empty -> Unit
      is Result.Error -> onError(result, viewModel.refreshedSubreddits::clear)
      else -> Timber.warn { "Refreshed subreddits result $result is not handled." }
    }
  }

  private fun onSubredditAdded(nameAndResult: Pair<SubredditName, Result<Subreddit>>) {
    val (name, result) = nameAndResult

    fun onSuccess(subreddit: Subreddit) {
      val snackbar = binding?.root?.makeSnackbar(
        getString(R.string.added_subreddit, subreddit.name.name).toAndroidString(),
        R.string.action_view.toAndroidString(),
        length = SnackbarLength.LONG
      ) {
        context?.let(subreddit.name.asUrl()::launch)
      }?.setAnchorView(binding?.addSubredditButton)
      eventSnackbar.set(snackbar, viewModel.addedSubreddit::clear)
    }

    fun onFailure(failure: Result.Failure) {
      val stringRes = when (failure) {
        Result.Failure.NetworkFailure -> R.string.added_subreddit_does_not_exist
        Result.Failure.DatabaseFailure -> R.string.added_subreddit_exists
      }
      val string = getString(stringRes, name.name).toAndroidString()
      val snackbar = binding?.root?.makeSnackbar(string)?.setAnchorView(binding?.addSubredditButton)
      eventSnackbar.set(snackbar, viewModel.addedSubreddit::clear)
    }

    when (result) {
      is Result.Success -> onSuccess(result.data)
      is Result.Failure -> onFailure(result)
      is Result.Error -> onError(result, viewModel.addedSubreddit::clear)
      else -> Timber.warn { "Add subreddit result $result is not handled." }
    }
  }

  private fun onSubredditDeleted(result: Result<Subreddit>) {
    fun onSuccess(subreddit: Subreddit) {
      val snackbar = binding?.root?.makeSnackbar(
        getString(R.string.deleted_subreddit, subreddit.name.name).toAndroidString(),
        R.string.action_undo.toAndroidString(),
        length = SnackbarLength.LONG
      ) {
        viewModel.add(subreddit)
      }?.setAnchorView(binding?.addSubredditButton)
      eventSnackbar.set(snackbar, viewModel.deletedSubreddit::clear)
    }

    when (result) {
      is Result.Success -> onSuccess(result.data)
      else -> Timber.warn { "Delete subreddit result $result is not handled." }
    }
  }
}
