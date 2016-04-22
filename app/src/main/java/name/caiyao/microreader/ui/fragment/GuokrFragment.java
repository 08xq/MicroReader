package name.caiyao.microreader.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import name.caiyao.microreader.R;
import name.caiyao.microreader.bean.guokr.GuokrHotItem;
import name.caiyao.microreader.config.Config;
import name.caiyao.microreader.presenter.IGuokrPresenter;
import name.caiyao.microreader.presenter.impl.GuokrPresenterImpl;
import name.caiyao.microreader.ui.activity.ZhihuStoryActivity;
import name.caiyao.microreader.ui.iView.IGuokrFragment;
import name.caiyao.microreader.utils.DBUtils;
import name.caiyao.microreader.utils.NetWorkUtil;
import name.caiyao.microreader.utils.SharePreferenceUtil;

public class GuokrFragment extends BaseFragment implements OnRefreshListener, OnLoadMoreListener, IGuokrFragment {

    @Bind(R.id.swipe_target)
    RecyclerView swipeTarget;
    @Bind(R.id.swipeToLoadLayout)
    SwipeToLoadLayout swipeToLoadLayout;
    @Bind(R.id.progressBar)
    ProgressBar progressBar;

    private ArrayList<GuokrHotItem> guokrHotItems = new ArrayList<>();
    private GuokrAdapter guokrAdapter;
    private IGuokrPresenter mGuokrPresenter;
    private int currentOffset;

    public GuokrFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_common, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        initView();
    }

    private void initData() {
        mGuokrPresenter = new GuokrPresenterImpl(this, getActivity());
    }

    private void initView() {
        swipeToLoadLayout.setOnRefreshListener(this);
        swipeToLoadLayout.setOnLoadMoreListener(this);
        swipeTarget.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeTarget.setHasFixedSize(true);
        guokrAdapter = new GuokrAdapter(guokrHotItems);
        swipeTarget.setAdapter(guokrAdapter);
        mGuokrPresenter.getGuokrHotFromCache(0);
        if (SharePreferenceUtil.isRefreshOnlyWifi(getActivity())) {
            if (NetWorkUtil.isWifiConnected(getActivity())) {
                onRefresh();
            } else {
                Toast.makeText(getActivity(), R.string.toast_wifi_refresh_data, Toast.LENGTH_SHORT).show();
            }
        } else {
            onRefresh();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onRefresh() {
        currentOffset = 0;
        guokrHotItems.clear();
        //2016-04-05修复Inconsistency detected. Invalid view holder adapter positionViewHolder
        guokrAdapter.notifyDataSetChanged();
        mGuokrPresenter.getGuokrHot(currentOffset);
    }

    @Override
    public void onLoadMore() {
        mGuokrPresenter.getGuokrHot(currentOffset);
    }

    @Override
    public void showProgressDialog() {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidProgressDialog() {
        if (swipeToLoadLayout != null) {//不加可能会崩溃
            swipeToLoadLayout.setRefreshing(false);
            swipeToLoadLayout.setLoadingMore(false);
        }
        if (progressBar != null)
            progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showError(String error) {
        if (swipeTarget != null) {
            mGuokrPresenter.getGuokrHotFromCache(currentOffset);
            Snackbar.make(swipeToLoadLayout, getString(R.string.common_loading_error) + error, Snackbar.LENGTH_SHORT).setAction(getString(R.string.comon_retry), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mGuokrPresenter.getGuokrHot(currentOffset);
                }
            }).show();
        }
    }

    @Override
    public void updateList(ArrayList<GuokrHotItem> guokrHotItems) {
        currentOffset++;
        this.guokrHotItems.addAll(guokrHotItems);
        guokrAdapter.notifyDataSetChanged();
    }

    class GuokrAdapter extends RecyclerView.Adapter<GuokrAdapter.GuokrViewHolder> {

        private ArrayList<GuokrHotItem> guokrHotItems;

        public GuokrAdapter(ArrayList<GuokrHotItem> guokrHotItems) {
            this.guokrHotItems = guokrHotItems;
        }

        @Override
        public GuokrViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new GuokrViewHolder(getActivity().getLayoutInflater().inflate(R.layout.ithome_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final GuokrViewHolder holder, int position) {
            final GuokrHotItem guokrHotItem = guokrHotItems.get(holder.getAdapterPosition());
            if (DBUtils.getDB(getActivity()).isRead(Config.GUOKR, guokrHotItem.getId(), 1))
                holder.mTvTitle.setTextColor(Color.GRAY);
            else
                holder.mTvTitle.setTextColor(Color.BLACK);
            holder.mTvTitle.setText(guokrHotItem.getTitle());
            holder.mTvDescription.setText(guokrHotItem.getSummary());
            holder.mTvTime.setText(guokrHotItem.getTime());
            Glide.with(getActivity()).load(guokrHotItem.getSmallImage()).into(holder.mIvIthome);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DBUtils.getDB(getActivity()).insertHasRead(Config.GUOKR, guokrHotItem.getId(), 1);
                    holder.mTvTitle.setTextColor(Color.GRAY);
                    Intent intent = new Intent(getActivity(), ZhihuStoryActivity.class);
                    intent.putExtra("type", ZhihuStoryActivity.TYPE_GUOKR);
                    intent.putExtra("id", guokrHotItem.getId());
                    intent.putExtra("title", guokrHotItem.getTitle());
                    startActivity(intent);
                }
            });
            holder.mBtnIt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(getActivity(), holder.mBtnIt);
                    popupMenu.getMenuInflater().inflate(R.menu.pop_menu, popupMenu.getMenu());
                    popupMenu.getMenu().removeItem(R.id.pop_share);
                    popupMenu.getMenu().removeItem(R.id.pop_fav);
                    final boolean isRead = DBUtils.getDB(getActivity()).isRead(Config.GUOKR, guokrHotItem.getId(), 1);
                    if (!isRead)
                        popupMenu.getMenu().findItem(R.id.pop_unread).setTitle("标记为已读");
                    else
                        popupMenu.getMenu().findItem(R.id.pop_unread).setTitle("标记为未读");
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.pop_fav:

                                    break;
                                case R.id.pop_unread:
                                    if (isRead) {
                                        DBUtils.getDB(getActivity()).insertHasRead(Config.GUOKR, guokrHotItem.getId(), 0);
                                        holder.mTvTitle.setTextColor(Color.BLACK);
                                    } else {
                                        DBUtils.getDB(getActivity()).insertHasRead(Config.GUOKR, guokrHotItem.getId(), 1);
                                        holder.mTvTitle.setTextColor(Color.GRAY);
                                    }
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return guokrHotItems.size();
        }

        public class GuokrViewHolder extends RecyclerView.ViewHolder {

            @Bind(R.id.tv_title)
            TextView mTvTitle;
            @Bind(R.id.iv_ithome)
            ImageView mIvIthome;
            @Bind(R.id.tv_description)
            TextView mTvDescription;
            @Bind(R.id.tv_time)
            TextView mTvTime;
            @Bind(R.id.btn_it)
            Button mBtnIt;

            public GuokrViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
