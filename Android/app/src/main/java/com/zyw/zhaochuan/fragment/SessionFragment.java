package com.zyw.zhaochuan.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.activity.SessionActivity;

/**
 * Created by zyw on 2016/6/4.
 * 属于SessionActivity
 * 两个标签卡的容器
 */
public class SessionFragment extends Fragment {
    private  View rootView;
    private LayoutInflater mLayoutInflater;
    private FragmentTabHost mTabHost;
    private Class[] mFragmentArray={LocalListFragment.class, RemoteListFragment.class};
    private String[] mTextArray={"本地目录","远程目录"};
    private Context context;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView=inflater.inflate(R.layout.session_layout,null);

        context=SessionActivity.thiz;
        initView();
        return rootView;
    }

    private  void initView()
    {

        mLayoutInflater = LayoutInflater.from(SessionActivity.thiz);
        // 找到TabHost
        mTabHost = (FragmentTabHost) rootView.findViewById(android.R.id.tabhost);
        mTabHost.setup(context, getChildFragmentManager(), R.id.realtabcontent);//getChildFragmentManager()这里要用这个方法，因为这个fragment被其他fragment包着
        int count = mFragmentArray.length;
        for (int i = 0; i < count; i++) {
            // 给每个Tab按钮设置图标、文字和内容
            TabHost.TabSpec tabSpec = mTabHost.newTabSpec(mTextArray[i]).setIndicator(mTextArray[i]);
            // 将Tab按钮添加进Tab选项卡中
            mTabHost.addTab(tabSpec, mFragmentArray[i], null);
        }
    }
}
