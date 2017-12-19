package com.slgunz.root.sialia.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.slgunz.root.sialia.R;
import com.slgunz.root.sialia.data.model.Tweet;
import com.slgunz.root.sialia.data.model.User;
import com.slgunz.root.sialia.di.ActivityScoped;
import com.slgunz.root.sialia.ui.common.EndlessRecyclerViewScrollListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;


@ActivityScoped
public class HomeFragment extends DaggerFragment implements HomeContract.View {

    @Inject
    HomeContract.Presenter mPresenter;

    private HomeAdapter mHomeAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private RecyclerView mHomeRecyclerView;

    private EndlessRecyclerViewScrollListener mListener;

    private ProgressBar mProgressBar;

    private Button mNewTweetSendButton;

    private EditText mNewTweetEditView;

    private ImageView mAccountProfileImage;

    private TextView mAccountName;

    private TextView mAccountScreenName;

    @Inject
    public HomeFragment() {
        // requires empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHomeAdapter = new HomeAdapter(new ArrayList<>(0));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.home_frag, container, false);

        mHomeRecyclerView = root.findViewById(R.id.home_timeline_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mHomeRecyclerView.setAdapter(mHomeAdapter);
        // set custom ScrollListener for loading previous tweets
        mListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                mPresenter.loadPreviousTweets(mHomeAdapter.getTheLowestId());
            }
        };
        mHomeRecyclerView.addOnScrollListener(mListener);
        mHomeRecyclerView.setLayoutManager(linearLayoutManager);

        View newTweetLayout = root.findViewById(R.id.home_included_layout);

        mNewTweetEditView = root.findViewById(R.id.new_tweet_text);

        mNewTweetSendButton = root.findViewById(R.id.new_tweet_send_button);
        mNewTweetSendButton.setOnClickListener(view -> {
            String message = mNewTweetEditView.getText().toString();
            mPresenter.sendTweet(message);
            // disabled button and clear text
            mNewTweetEditView.setText("");
            enableSendTweetButton(false);
        });

        // set up floating action button
        FloatingActionButton fab =
                getActivity().findViewById(R.id.fab_tweet);
        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(new View.OnClickListener() {
            boolean visible;

            @Override
            public void onClick(View view) {
                visible = !visible;
                if (visible) {
                    newTweetLayout.setVisibility(View.VISIBLE);
                    fab.setImageResource(R.drawable.ic_undo);
                } else {
                    newTweetLayout.setVisibility(View.GONE);
                    fab.setImageResource(R.drawable.ic_add);
                }
            }
        });

        //Set up progress indicator
        mSwipeRefreshLayout =
                root.findViewById(R.id.home_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getActivity(), R.color.colorPrimary),
                ContextCompat.getColor(getActivity(), R.color.colorAccent),
                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)
        );

        mSwipeRefreshLayout.setOnRefreshListener(() -> mPresenter.loadRecentTweets(mHomeAdapter.getTheBiggestId()));

        mProgressBar = root.findViewById(R.id.home_progress_bar);

        NavigationView navigationView = getActivity().findViewById(R.id.nav_view);

        View hView = navigationView.getHeaderView(0);
        mAccountProfileImage = hView.findViewById(R.id.nav_header_profile_image);
        mAccountName = hView.findViewById(R.id.nav_header_account_name);
        mAccountScreenName = hView.findViewById(R.id.nav_header_account_screen_name);


        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.unsubscribe();
    }

//    ------------------------------------------
//    HomeContract.View interface
//    ------------------------------------------

    /**
     * @param isActive
     */
    @Override
    public void setWaitingIndicator(boolean isActive) {
        mProgressBar.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void applyAccountProfile(User user) {
        Context context = getActivity();
        if (context != null && mAccountScreenName != null) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .into(mAccountProfileImage);
        }

        if (mAccountName != null) {
            mAccountName.setText(user.getName());
        }

        if (mAccountScreenName != null) {
            mAccountScreenName.setText(user.getScreenName());
        }
    }

    @Override
    public void openSearchScreen() {

    }

    @Override
    public void showErrorMessage(Throwable throwable) {

    }

    @Override
    public void showNewTweetsNotification(int count) {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void setAdapterList(List<Tweet> tweets) {
        mHomeAdapter.replaceData(tweets);
    }

    @Override
    public void insertBeforeToAdapterList(List<Tweet> tweets) {
        mHomeAdapter.replaceData(tweets);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void appendToAdapterList(List<Tweet> tweets) {
        mHomeAdapter.appendData(tweets);
    }

    @Override
    public void enableSendTweetButton(boolean isEnable) {
        mNewTweetSendButton.setEnabled(isEnable);
    }
}
