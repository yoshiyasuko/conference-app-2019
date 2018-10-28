package io.github.droidkaigi.confsched2019.session.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.viewpager.widget.ViewPager
import dagger.Module
import dagger.Provides
import io.github.droidkaigi.confsched2019.ext.android.changed
import io.github.droidkaigi.confsched2019.model.LoadingState
import io.github.droidkaigi.confsched2019.model.SessionTab
import io.github.droidkaigi.confsched2019.session.R
import io.github.droidkaigi.confsched2019.session.databinding.FragmentAllSessionsBinding
import io.github.droidkaigi.confsched2019.session.ui.actioncreator.AllSessionActionCreator
import io.github.droidkaigi.confsched2019.session.ui.actioncreator.SessionActionCreator
import io.github.droidkaigi.confsched2019.session.ui.store.SessionStore
import io.github.droidkaigi.confsched2019.ui.DaggerFragment
import io.github.droidkaigi.confsched2019.user.store.UserStore
import io.github.droidkaigi.confsched2019.util.ProgressTimeLatch
import javax.inject.Inject
import javax.inject.Named

class AllSessionsFragment : DaggerFragment() {

    lateinit var binding: FragmentAllSessionsBinding

    @Inject lateinit var sessionActionCreator: SessionActionCreator
    @Inject lateinit var allSessionActionCreator: AllSessionActionCreator
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var userStore: UserStore

    private lateinit var progressTimeLatch: ProgressTimeLatch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_all_sessions,
            container,
            false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        userStore.logined.changed(viewLifecycleOwner) { logined ->
            if (logined) allSessionActionCreator.load()
        }

        binding.sessionsTabLayout.setupWithViewPager(binding.sessionsViewpager)
        binding.sessionsViewpager.adapter = object : FragmentStatePagerAdapter(
            childFragmentManager
        ) {
            override fun getItem(position: Int): Fragment {
                return SessionsFragment.newInstance(SessionsFragmentArgs
                        .Builder(position)
                        .build())
            }

            override fun getPageTitle(position: Int) = SessionTab.tabs[position].title
            override fun getCount(): Int = SessionTab.tabs.size
        }
        binding.sessionsViewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                allSessionActionCreator.selectTab(SessionTab.tabs[position])
            }
        })
        progressTimeLatch = ProgressTimeLatch { showProgress ->
            binding.progressBar.isVisible = showProgress
        }.apply {
            loading = true
        }
        sessionStore.loadingState.changed(this) {
            progressTimeLatch.loading = it == LoadingState.LOADING
        }
    }
}

@Module
abstract class AllSessionsFragmentModule {
    @Module
    companion object {
        @Named("AllSessionsFragment") @JvmStatic @Provides fun providesLifecycle(
            allSessionsFragment: AllSessionsFragment
        ): Lifecycle {
            return allSessionsFragment.viewLifecycleOwner.lifecycle
        }
    }
}
