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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Iterator;

import butterknife.Bind;
import butterknife.ButterKnife;
import name.caiyao.microreader.R;
import name.caiyao.microreader.api.itHome.ItHomeRequest;
import name.caiyao.microreader.bean.itHome.ItHomeItem;
import name.caiyao.microreader.bean.itHome.ItHomeResponse;
import name.caiyao.microreader.config.Config;
import name.caiyao.microreader.ui.activity.ItHomeActivity;
import name.caiyao.microreader.utils.CacheUtil;
import name.caiyao.microreader.utils.DBUtils;
import name.caiyao.microreader.utils.ItHomeUtil;
import name.caiyao.microreader.utils.NetWorkUtil;
import name.caiyao.microreader.utils.SharePreferenceUtil;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by 蔡小木 on 2016/3/24 0024.
 */
public class ItHomeFragment extends BaseFragment implements OnRefreshListener, OnLoadMoreListener {


    @Bind(R.id.progressBar)
    ProgressBar progressBar;
    @Bind(R.id.swipe_target)
    RecyclerView swipeTarget;
    @Bind(R.id.swipeToLoadLayout)
    SwipeToLoadLayout swipeToLoadLayout;

    private ArrayList<ItHomeItem> itHomeItems = new ArrayList<>();
    private ItAdapter itAdapter;
    private CacheUtil cacheUtil;
    private Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_common, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeToLoadLayout.setOnRefreshListener(this);
        swipeToLoadLayout.setOnLoadMoreListener(this);
        swipeTarget.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeTarget.setHasFixedSize(true);
        cacheUtil = CacheUtil.get(getActivity());
        itAdapter = new ItAdapter(itHomeItems);
        swipeTarget.setAdapter(itAdapter);
        getFromCache();
        if (SharePreferenceUtil.isRefreshOnlyWifi(getActivity())) {
            if (NetWorkUtil.isWifiConnected(getActivity())) {
                onRefresh();
            } else {
                Toast.makeText(getActivity(), getString(R.string.toast_wifi_refresh_data), Toast.LENGTH_SHORT).show();
            }
        } else {
            onRefresh();
        }
    }

    private void getFromCache() {
        if (cacheUtil.getAsJSONArray(Config.IT) != null && cacheUtil.getAsJSONArray(Config.IT).length() != 0) {
            ArrayList<ItHomeItem> it = gson.fromJson(cacheUtil.getAsJSONArray(Config.IT).toString(), new TypeToken<ArrayList<ItHomeItem>>() {
            }.getType());
            itHomeItems.addAll(it);
            itAdapter.notifyDataSetChanged();
        }
    }

    private void getIthomeNews() {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);
        ItHomeRequest.getItHomeApi().getItHomeNews()
                .subscribeOn(Schedulers.io())
                .map(new Func1<ItHomeResponse, ArrayList<ItHomeItem>>() {
                    @Override
                    public ArrayList<ItHomeItem> call(ItHomeResponse itHomeResponse) {
                        //过滤广告新闻
                        ArrayList<ItHomeItem> itHomeItems1 = itHomeResponse.getChannel().getItems();
                        Iterator<ItHomeItem> iter = itHomeItems1.iterator();
                        while (iter.hasNext()) {
                            ItHomeItem item = iter.next();
                            if (item.getUrl().contains("digi"))
                                iter.remove();
                        }
                        return itHomeItems1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ArrayList<ItHomeItem>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (progressBar != null)
                            progressBar.setVisibility(View.INVISIBLE);
                        if (swipeToLoadLayout != null) {//不加可能会崩溃
                            swipeToLoadLayout.setRefreshing(false);
                        }
                        if (swipeTarget != null) {
                            getFromCache();
                            Snackbar.make(swipeToLoadLayout, getString(R.string.common_loading_error), Snackbar.LENGTH_SHORT).setAction(getString(R.string.comon_retry), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    getIthomeNews();
                                }
                            }).show();
                        }
                    }

                    @Override
                    public void onNext(ArrayList<ItHomeItem> it) {
                        if (swipeToLoadLayout != null) {//不加可能会崩溃
                            swipeToLoadLayout.setRefreshing(false);
                        }
                        if (progressBar != null)
                            progressBar.setVisibility(View.INVISIBLE);
                        cacheUtil.put(Config.IT, gson.toJson(it));
                        itHomeItems.addAll(it);
                        itAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void getMoreItHome() {
        ItHomeRequest.getItHomeApi().getMoreItHomeNews(ItHomeUtil.getMinNewsId(itHomeItems.get(itHomeItems.size() - 1).getNewsid()))
                .map(new Func1<ItHomeResponse, ArrayList<ItHomeItem>>() {
                    @Override
                    public ArrayList<ItHomeItem> call(ItHomeResponse itHomeResponse) {
                        //过滤广告新闻
                        ArrayList<ItHomeItem> itHomeItems1 = itHomeResponse.getChannel().getItems();
                        Iterator<ItHomeItem> iter = itHomeItems1.iterator();
                        while (iter.hasNext()) {
                            ItHomeItem item = iter.next();
                            if (item.getUrl().contains("digi"))
                                iter.remove();
                        }
                        return itHomeItems1;
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ArrayList<ItHomeItem>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (swipeToLoadLayout != null)
                            swipeToLoadLayout.setLoadingMore(false);
                        if (isAdded())//Fragment not attach to activity
                            Snackbar.make(swipeToLoadLayout, getString(R.string.common_loading_error), Snackbar.LENGTH_SHORT).setAction(getString(R.string.comon_retry), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    getMoreItHome();
                                }
                            }).show();
                    }

                    @Override
                    public void onNext(ArrayList<ItHomeItem> it) {
                        if (swipeToLoadLayout != null)
                            swipeToLoadLayout.setLoadingMore(false);
                        itHomeItems.addAll(it);
                        itAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onRefresh() {
        itHomeItems.clear();
        //2016-04-05修复Inconsistency detected. Invalid view holder adapter positionViewHolder
        itAdapter.notifyDataSetChanged();
        getIthomeNews();
    }

    @Override
    public void onLoadMore() {
        getMoreItHome();
    }

    class ItAdapter extends RecyclerView.Adapter<ItAdapter.ItViewHolder> {

        private ArrayList<ItHomeItem> itHomeItems;

        public ItAdapter(ArrayList<ItHomeItem> itHomeItems) {
            this.itHomeItems = itHomeItems;
        }

        @Override
        public ItViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItViewHolder(getActivity().getLayoutInflater().inflate(R.layout.ithome_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ItViewHolder holder, int position) {
            final ItHomeItem itHomeItem = itHomeItems.get(holder.getAdapterPosition());
            if (DBUtils.getDB(getActivity()).isRead(Config.IT, itHomeItem.getNewsid(), 1))
                holder.tvTitle.setTextColor(Color.GRAY);
            else
                holder.tvTitle.setTextColor(Color.BLACK);
            holder.tvTitle.setText(itHomeItem.getTitle());
            holder.tvTime.setText(itHomeItem.getPostdate());
            holder.tvDescription.setText(itHomeItem.getDescription());
            Glide.with(getActivity()).load(itHomeItem.getImage()).placeholder(R.drawable.bg).into(holder.ivIthome);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DBUtils.getDB(getActivity()).insertHasRead(Config.IT, itHomeItem.getNewsid(), 1);
                    holder.tvTitle.setTextColor(Color.GRAY);
                    startActivity(new Intent(getActivity(), ItHomeActivity.class)
                            .putExtra("item", itHomeItem));
                }
            });
            holder.btnIt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(getActivity(), holder.btnIt);
                    popupMenu.getMenuInflater().inflate(R.menu.pop_menu, popupMenu.getMenu());
                    popupMenu.getMenu().removeItem(R.id.pop_fav);
                    final boolean isRead = DBUtils.getDB(getActivity()).isRead(Config.IT, itHomeItem.getNewsid(), 1);
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
                                        DBUtils.getDB(getActivity()).insertHasRead(Config.IT, itHomeItem.getNewsid(), 0);
                                        holder.tvTitle.setTextColor(Color.BLACK);
                                    } else {
                                        DBUtils.getDB(getActivity()).insertHasRead(Config.IT, itHomeItem.getNewsid(), 1);
                                        holder.tvTitle.setTextColor(Color.GRAY);
                                    }
                                    break;
                                case R.id.pop_share:
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, itHomeItem.getTitle() + " http://ithome.com" + itHomeItem.getUrl() + getString(R.string.share_tail));
                                    shareIntent.setType("text/plain");
                                    //设置分享列表的标题，并且每次都显示分享列表
                                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
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
            return itHomeItems.size();
        }

        public class ItViewHolder extends RecyclerView.ViewHolder {

            @Bind(R.id.iv_ithome)
            ImageView ivIthome;
            @Bind(R.id.tv_title)
            TextView tvTitle;
            @Bind(R.id.tv_description)
            TextView tvDescription;
            @Bind(R.id.tv_time)
            TextView tvTime;
            @Bind(R.id.btn_it)
            Button btnIt;

            public ItViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
