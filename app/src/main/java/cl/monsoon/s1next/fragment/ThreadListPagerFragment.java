package cl.monsoon.s1next.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import cl.monsoon.s1next.Api;
import cl.monsoon.s1next.R;
import cl.monsoon.s1next.activity.PostListActivity;
import cl.monsoon.s1next.adapter.ThreadListRecyclerAdapter;
import cl.monsoon.s1next.model.Forum;
import cl.monsoon.s1next.model.list.Threads;
import cl.monsoon.s1next.model.mapper.ThreadsWrapper;
import cl.monsoon.s1next.util.IntentUtil;
import cl.monsoon.s1next.util.ToastUtil;
import cl.monsoon.s1next.view.BaseRecyclerView;
import cl.monsoon.s1next.widget.AsyncResult;
import cl.monsoon.s1next.widget.HttpGetLoader;
import cl.monsoon.s1next.widget.RecyclerViewHelper;

/**
 * A Fragment representing one of the pages of threads.
 * <p>
 * All activities containing this Fragment must implement {@link PagerCallback}.
 */
public final class ThreadListPagerFragment extends BaseFragment<ThreadsWrapper> {

    private static final String ARG_FORUM_ID = "forum_id";
    private static final String ARG_PAGE_NUM = "page_num";

    private String mForumId;
    private int mPageNum;

    private BaseRecyclerView mRecyclerView;
    private ThreadListRecyclerAdapter mRecyclerAdapter;

    private PagerCallback mPagerCallback;
    private SubForumsCallback mSubForumsCallback;

    public static ThreadListPagerFragment newInstance(String forumId, int pageNum) {
        ThreadListPagerFragment fragment = new ThreadListPagerFragment();

        Bundle bundle = new Bundle();
        bundle.putString(ARG_FORUM_ID, forumId);
        bundle.putInt(ARG_PAGE_NUM, pageNum);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_base, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mForumId = getArguments().getString(ARG_FORUM_ID);
        mPageNum = getArguments().getInt(ARG_PAGE_NUM);

        mRecyclerView = (BaseRecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerAdapter = new ThreadListRecyclerAdapter();
        mRecyclerView.setAdapter(mRecyclerAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerViewHelper(
                getActivity(),
                mRecyclerView,
                new RecyclerViewHelper.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {
                        startActivity(view, position, false);
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {
                        // cause NullPointerException sometimes when orientation changes
                        try {
                            startActivity(view, position, true);
                        } catch (NullPointerException ignore) {

                        }
                    }

                    private void startActivity(View view, int position, boolean shouldGoToLastPage) {
                        // if user has no permission to access this thread
                        if (!view.isEnabled()) {
                            return;
                        }

                        Intent intent = new Intent(getActivity(), PostListActivity.class);

                        cl.monsoon.s1next.model.Thread thread = mRecyclerAdapter.getItem(position);
                        intent.putExtra(PostListActivity.ARG_THREAD, thread);
                        if (shouldGoToLastPage) {
                            intent.putExtra(PostListActivity.ARG_SHOULD_GO_TO_LAST_PAGE, true);
                        }

                        ThreadListPagerFragment.this.startActivity(intent);
                    }
                }
        ));

        onInsetsChanged();
        enableToolbarAndFabAutoHideEffect(mRecyclerView, null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mPagerCallback = (PagerCallback) getFragmentManager().findFragmentByTag(ThreadListFragment.TAG);
        mSubForumsCallback = (SubForumsCallback) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mPagerCallback = null;
        mSubForumsCallback = null;
    }

    @Override
    public void onInsetsChanged(@NonNull Rect insets) {
        setRecyclerViewPadding(
                mRecyclerView,
                insets,
                getResources().getDimensionPixelSize(R.dimen.list_view_padding));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_thread_pager, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_browser:
                IntentUtil.startViewIntentExcludeOurApp(getActivity(),
                        Uri.parse(Api.getThreadListUrlForBrowser(mForumId, mPageNum)));

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<AsyncResult<ThreadsWrapper>> onCreateLoader(int id, Bundle args) {
        super.onCreateLoader(id, args);

        return new HttpGetLoader<>(
                getActivity(),
                Api.getThreadListUrl(mForumId, mPageNum),
                ThreadsWrapper.class);
    }

    @Override
    public void onLoadFinished(Loader<AsyncResult<ThreadsWrapper>> loader, AsyncResult<ThreadsWrapper> asyncResult) {
        super.onLoadFinished(loader, asyncResult);

        if (asyncResult.exception != null) {
            if (getUserVisibleHint()) {
                ToastUtil.showByResId(asyncResult.getExceptionStringRes(), Toast.LENGTH_SHORT);
            }
        } else {
            Threads threads = asyncResult.data.getThreads();

            // if user has logged out or has no permission to access this forum
            if (threads.getThreadList().isEmpty()) {
                String message = asyncResult.data.getResult().getMessage();
                if (!TextUtils.isEmpty(message)) {
                    ToastUtil.showByText(message, Toast.LENGTH_SHORT);
                }
            } else {
                mRecyclerAdapter.setDataSet(threads.getThreadList());
                mRecyclerAdapter.notifyDataSetChanged();

                new Handler().post(() ->
                        mPagerCallback.setTotalPageByThreads(threads.getThreadListInfo().getThreads()));
            }

            if (!threads.getSubForumList().isEmpty()) {
                mSubForumsCallback.setupSubForums(threads.getSubForumList());
            }
        }
    }

    /**
     * A callback interface that all activities containing this Fragment must implement.
     */
    public interface PagerCallback {

        /**
         * A callback to set actual total pages which used for {@link android.support.v4.view.PagerAdapter}。
         */
        void setTotalPageByThreads(int threads);
    }

    public interface SubForumsCallback {

        void setupSubForums(List<Forum> forumList);
    }
}
