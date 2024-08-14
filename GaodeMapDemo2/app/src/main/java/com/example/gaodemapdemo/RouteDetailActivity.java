package com.example.gaodemapdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.WalkPath;
import com.example.gaodemapdemo.adapter.RideSegmentListAdapter;
import com.example.gaodemapdemo.adapter.WalkSegmentListAdapter;
import com.example.gaodemapdemo.util.MapUtil;

public class RouteDetailActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TextView tvTitle, tvTime;
    private RecyclerView rv;//列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        initView();
    }
    /**
     * 初始化
     */
    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tv_title);
        tvTime = findViewById(R.id.tv_time);
        rv = findViewById(R.id.rv);
        //高亮状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        switch (intent.getIntExtra("type", 0)) {
            case 0://步行
                walkDetail(intent);
                break;
            case 1://骑行
                rideDetail(intent);
                break;
            case 2://驾车

                break;
            case 3://公交

                break;
            default:
                break;
        }
    }
    /**
     * 步行详情
     * @param intent
     */
    private void walkDetail(Intent intent) {
        tvTitle.setText("步行路线规划");
        WalkPath walkPath = intent.getParcelableExtra("path");
        String dur = MapUtil.getFriendlyTime((int) walkPath.getDuration());
        String dis = MapUtil.getFriendlyLength((int) walkPath.getDistance());
        tvTime.setText(dur + "(" + dis + ")");
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new WalkSegmentListAdapter(R.layout.item_segment, walkPath.getSteps()));
    }
    /**
     * 骑行详情
     * @param intent
     */
    private void rideDetail(Intent intent) {
        tvTitle.setText("骑行路线规划");
        RidePath ridePath = intent.getParcelableExtra("path");
        String dur = MapUtil.getFriendlyTime((int) ridePath.getDuration());
        String dis = MapUtil.getFriendlyLength((int) ridePath.getDistance());
        tvTime.setText(dur + "(" + dis + ")");
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new RideSegmentListAdapter(R.layout.item_segment, ridePath.getSteps()));
    }

}