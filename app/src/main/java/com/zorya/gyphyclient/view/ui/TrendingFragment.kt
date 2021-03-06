package com.zorya.gyphyclient.view.ui

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zorya.gyphyclient.GiphyApplication
import com.zorya.gyphyclient.R
import com.zorya.gyphyclient.di.DaggerAppComponent
import com.zorya.gyphyclient.view.adapter.TrendingAdapter
import com.zorya.gyphyclient.viewmodel.TrendingViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_top.*
import javax.inject.Inject

class TrendingFragment : Fragment() {
    @Inject
    lateinit var trendingAdapter: TrendingAdapter
    private val viewModel: TrendingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerAppComponent.create().inject(this)
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_CLASS, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_top, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setUpRecyclerView()
        observeInProgress()
        observeIsError()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            observeGiphyList()
        }
    }

    override fun onPause() {
        super.onPause()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor
            .putInt("FIRSTPOSITION", recycler_view.firstVisibleItemPosition)
            .apply()
    }

    override fun onResume() {
        super.onResume()

        var firstPosition = 0
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(GiphyApplication.getAppContext())
        preferences.apply {
            firstPosition = getInt("FIRSTPOSITION", 0)
        }
        recycler_view.scrollToPosition(firstPosition - 1)
    }

    private fun setUpRecyclerView() {
        recycler_view.apply {
            layoutManager = StaggeredGridLayoutManager(SPAN_COUNT, ORIENTATION)
            adapter = trendingAdapter
            setActionInTheEnd {
                viewModel.getData(adapter?.itemCount ?: 0)
            }
        }
    }

    private fun observeInProgress() {
        viewModel._isInProgress.observe(viewLifecycleOwner, { isLoading ->
            isLoading.let {
                if (it) {
                    empty_text.visibility = View.GONE
                    recycler_view.visibility = View.GONE
                    fetch_progress.visibility = View.VISIBLE
                } else {
                    fetch_progress.visibility = View.GONE
                }
            }
        })
    }

    private fun observeIsError() {
        viewModel._isError.observe(viewLifecycleOwner, { isError ->
            isError.let {
                if (it) {
                    disableViewsOnError()
                } else {
                    empty_text.visibility = View.GONE
                    fetch_progress.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun disableViewsOnError() {
        fetch_progress.visibility = View.VISIBLE
        empty_text.visibility = View.VISIBLE
        recycler_view.visibility = View.GONE
        trendingAdapter.setUpData(emptyList(), {}, {})
        fetch_progress.visibility = View.GONE
    }

    private fun observeGiphyList() {
        viewModel.data.observe(viewLifecycleOwner, { giphies ->
            giphies?.let {
                if (it.isNotEmpty()) {
                    fetch_progress.visibility = View.VISIBLE
                    recycler_view.visibility = View.VISIBLE
                    recycler_view.isLoading = false
                    trendingAdapter.setUpData(it,
                        { gif ->
                            viewModel.repository.gifShare(gif, requireContext())
                        },
                        { gif ->
                            viewModel.addToFavorite(gif)
                        }
                    )
                    empty_text.visibility = View.GONE
                    fetch_progress.visibility = View.GONE
                } else {
                    disableViewsOnError()
                }
            }
        })
    }

    companion object {
        private const val SPAN_COUNT = 2
        private const val ORIENTATION = 1
        const val TAG = "TrendingFragment"
    }
}