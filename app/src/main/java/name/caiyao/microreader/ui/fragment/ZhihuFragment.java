package name.caiyao.microreader.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.apkfuns.logutils.LogUtils;
import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import name.caiyao.microreader.R;
import name.caiyao.microreader.api.zhihu.ZhihuRequest;
import name.caiyao.microreader.bean.zhihu.ZhihuDaily;
import name.caiyao.microreader.bean.zhihu.ZhihuDailyItem;
import name.caiyao.microreader.config.Config;
import name.caiyao.microreader.presenter.IZhihuPresenter;
import name.caiyao.microreader.presenter.impl.ZhihuPresenterImpl;
import name.caiyao.microreader.ui.activity.ZhihuStoryActivity;
import name.caiyao.microreader.ui.iView.IZhihuFragment;
import name.caiyao.microreader.utils.CacheUtil;
import name.caiyao.microreader.utils.DBUtils;
import name.caiyao.microreader.utils.NetWorkUtil;
import name.caiyao.microreader.utils.ScreenUtil;
import name.caiyao.microreader.utils.SharePreferenceUtil;
import name.caiyao.microreader.utils.TimeUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class ZhihuFragment extends BaseFragment implements OnRefreshListener, OnLoadMoreListener, IZhihuFragment {

    @Bind(R.id.swipe_target)
    RecyclerView swipeTarget;
    @Bind(R.id.swipeToLoadLayout)
    SwipeToLoadLayout swipeToLoadLayout;
    @Bind(R.id.progressBar)
    ProgressBar progressBar;

    private String currentLoadedDate;
    private ZhihuAdapter zhihuAdapter;
    private IZhihuPresenter mZhihuPresenter;
    private ArrayList<ZhihuDailyItem> zhihuStories = new ArrayList<>();
    CacheUtil cacheUtil;
    Gson gson = new Gson();

    public ZhihuFragment() {
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
        mZhihuPresenter = new ZhihuPresenterImpl(this, getActivity());
    }

    private void initView() {
        swipeToLoadLayout.setOnRefreshListener(this);
        swipeToLoadLayout.setOnLoadMoreListener(this);
        swipeTarget.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeTarget.setHasFixedSize(true);
        zhihuAdapter = new ZhihuAdapter(zhihuStories);
        swipeTarget.setAdapter(zhihuAdapter);
        cacheUtil = CacheUtil.get(getActivity());
        mZhihuPresenter.getLastFromCache();
        if (SharePreferenceUtil.isRefreshOnlyWifi(getActivity())) {
            if (NetWorkUtil.isWifiConnected(getActivity())) {
                onRefresh();
            }
        } else {
            onRefresh();
        }
    }

    private void getFromCache() {
        if (cacheUtil.getAsJSONObject(Config.ZHIHU) != null) {
            zhihuStories.clear();
            ZhihuDaily zhihuDaily = gson.fromJson(cacheUtil.getAsJSONObject(Config.ZHIHU).toString(), ZhihuDaily.class);
            currentLoadedDate = zhihuDaily.getDate();
            zhihuStories.addAll(zhihuDaily.getStories());
            zhihuAdapter.notifyDataSetChanged();
        }
    }

    private void getZhihuDaily() {
        zhihuStories.clear();
        //2016-04-05修复Inconsistency detected. Invalid view holder adapter positionViewHolder
        zhihuAdapter.notifyDataSetChanged();
        ZhihuRequest.getZhihuApi().getLastDaily()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ZhihuDaily>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (progressBar != null)
                            progressBar.setVisibility(View.INVISIBLE);
                        if (swipeToLoadLayout != null)
                            swipeToLoadLayout.setRefreshing(false);
                        if (swipeTarget != null) {
                            getFromCache();
                            Snackbar.make(swipeTarget, getString(R.string.common_loading_error), Snackbar.LENGTH_SHORT).setAction("重试", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    getZhihuDaily();
                                }
                            }).show();
                        }
                    }

                    @Override
                    public void onNext(ZhihuDaily zhihuDaily) {
                        if (progressBar != null)
                            progressBar.setVisibility(View.INVISIBLE);
                        if (swipeToLoadLayout != null) {//不加可能会崩溃
                            swipeToLoadLayout.setRefreshing(false);
                        }
                        cacheUtil.put(Config.ZHIHU, gson.toJson(zhihuDaily));
                        currentLoadedDate = zhihuDaily.getDate();
                        zhihuStories.addAll(zhihuDaily.getStories());
                        zhihuAdapter.notifyDataSetChanged();
                    }
                });

    }

    private void getMoreZhihuDaily() {
        if (!TextUtils.isEmpty(currentLoadedDate)) {
            ZhihuRequest.getZhihuApi().getTheDaily(TimeUtil.getSpecifiedDayBefore(currentLoadedDate))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ZhihuDaily>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            LogUtils.i(e.toString());
                        }

                        @Override
                        public void onNext(ZhihuDaily zhihuDaily) {
                            if (swipeToLoadLayout != null) {//不加可能会崩溃
                                swipeToLoadLayout.setLoadingMore(false);
                            }
                            currentLoadedDate = zhihuDaily.getDate();
                            zhihuStories.addAll(zhihuDaily.getStories());
                            zhihuAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onRefresh() {
        getZhihuDaily();
    }

    @Override
    public void onLoadMore() {
        getMoreZhihuDaily();
    }

    @Override
    public void showProgressDialog() {

    }

    @Override
    public void hidProgressDialog() {
        if (progressBar != null)
            progressBar.setVisibility(View.INVISIBLE);
        if (swipeToLoadLayout != null) {
            swipeToLoadLayout.setRefreshing(false);
            swipeToLoadLayout.setLoadingMore(false);
        }
    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void updateList(ZhihuDaily zhihuDaily) {

    }

    class ZhihuAdapter extends RecyclerView.Adapter<ZhihuAdapter.ZhihuViewHolder> {


        private ArrayList<ZhihuDailyItem> zhihuStories;

        public ZhihuAdapter(ArrayList<ZhihuDailyItem> zhihuStories) {
            this.zhihuStories = zhihuStories;
        }

        @Override
        public ZhihuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ZhihuViewHolder(getActivity().getLayoutInflater().inflate(R.layout.zhihu_daily_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ZhihuViewHolder holder, int position) {
            final ZhihuDailyItem zhihuDailyItem = zhihuStories.get(holder.getAdapterPosition());
            if (DBUtils.getDB(getActivity()).isRead(Config.ZHIHU, zhihuDailyItem.getId(), 1))
                holder.tvZhihuDaily.setTextColor(Color.GRAY);
            else
                holder.tvZhihuDaily.setTextColor(Color.BLACK);
            holder.tvZhihuDaily.setText(zhihuDailyItem.getTitle());
            holder.cvZhihu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DBUtils.getDB(getActivity()).insertHasRead(Config.ZHIHU, zhihuDailyItem.getId(), 1);
                    holder.tvZhihuDaily.setTextColor(Color.GRAY);
                    Intent intent = new Intent(getActivity(), ZhihuStoryActivity.class);
                    intent.putExtra("type", ZhihuStoryActivity.TYPE_ZHIHU);
                    intent.putExtra("id", zhihuDailyItem.getId());
                    intent.putExtra("title", zhihuDailyItem.getTitle());
                    startActivity(intent);
                }
            });
            holder.btnZhihu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(getActivity(), holder.btnZhihu);
                    popupMenu.getMenuInflater().inflate(R.menu.pop_menu, popupMenu.getMenu());
                    popupMenu.getMenu().removeItem(R.id.pop_share);
                    popupMenu.getMenu().removeItem(R.id.pop_fav);
                    final boolean isRead = DBUtils.getDB(getActivity()).isRead(Config.ZHIHU, zhihuDailyItem.getId(), 1);
                    if (!isRead)
                        popupMenu.getMenu().findItem(R.id.pop_unread).setTitle("标记为已读");
                    else
                        popupMenu.getMenu().findItem(R.id.pop_unread).setTitle("标记为未读");
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.pop_unread:
                                    if (isRead) {
                                        DBUtils.getDB(getActivity()).insertHasRead(Config.ZHIHU, zhihuDailyItem.getId(), 0);
                                        holder.tvZhihuDaily.setTextColor(Color.BLACK);
                                    } else {
                                        DBUtils.getDB(getActivity()).insertHasRead(Config.ZHIHU, zhihuDailyItem.getId(), 1);
                                        holder.tvZhihuDaily.setTextColor(Color.GRAY);
                                    }
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                }
            });
            runEnterAnimation(holder.itemView);
            if (zhihuStories.get(position).getImages() != null)
                Glide.with(getActivity()).load(zhihuDailyItem.getImages()[0]).placeholder(R.drawable.icon).into(holder.ivZhihuDaily);
        }

        private void runEnterAnimation(View view) {
            view.setTranslationX(ScreenUtil.getScreenWidth(getActivity()));
            view.animate()
                    .translationX(0)
                    .setStartDelay(100)
                    .setInterpolator(new DecelerateInterpolator(3.f))
                    .setDuration(700)
                    .start();
        }

        @Override
        public int getItemCount() {
            return zhihuStories.size();
        }

        public class ZhihuViewHolder extends RecyclerView.ViewHolder {

            @Bind(R.id.iv_zhihu_daily)
            ImageView ivZhihuDaily;
            @Bind(R.id.tv_zhihu_daily)
            TextView tvZhihuDaily;
            @Bind(R.id.cv_zhihu)
            CardView cvZhihu;
            @Bind(R.id.tv_time)
            TextView tvTime;
            @Bind(R.id.btn_zhihu)
            Button btnZhihu;

            public ZhihuViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
