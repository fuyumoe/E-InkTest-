package com.fuyumoe.einktest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_EXPORT_LOG = 1001;
    private static final int A2 = 100, REGAL = 15, PART = 3, FULL = 1, FAST = 18;

    private TestView testView;
    private TextView log;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 8, 8, 8);

        testView = new TestView(this);
        root.addView(testView, new LinearLayout.LayoutParams(-1, 0, 1));

        GridLayout buttons = new GridLayout(this);
        buttons.setColumnCount(2);
        addButton(buttons, "深度扫描 E-Ink API", v -> deepScan());
        addButton(buttons, "读取当前模式", v -> readModes());
        addButton(buttons, "A2 100", v -> refresh(A2, "A2"));
        addButton(buttons, "Regal 15", v -> refresh(REGAL, "Regal"));
        addButton(buttons, "Part 3", v -> refresh(PART, "Part"));
        addButton(buttons, "Full 1", v -> refresh(FULL, "Full"));
        addButton(buttons, "Fast 18", v -> refresh(FAST, "Fast"));
        addButton(buttons, "setBy A2", v -> refreshBy(A2, "setBy A2"));
        addButton(buttons, "setBy Regal", v -> refreshBy(REGAL, "setBy Regal"));
        addButton(buttons, "局部刷新测试", v -> localTest());
        addButton(buttons, "导出日志", v -> exportLog());
        addButton(buttons, "清空日志", v -> clearLog());
        root.addView(buttons, new LinearLayout.LayoutParams(-1, -2));

        log = new TextView(this);
        log.setTextSize(14);
        log.setTextIsSelectable(true);
        log.setPadding(8, 8, 8, 8);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(log);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 280));
        setContentView(root);

        println("================================");
        println("K103 E-Ink Deep Scanner");
        println("设备 = " + Build.MANUFACTURER + " " + Build.MODEL);
        println("平台 = " + prop("ro.board.platform"));
        println("SDK = " + Build.VERSION.SDK_INT);
        println("系统 = " + Build.VERSION.RELEASE);
        println("准备完成，请点击“深度扫描 E-Ink API”");
    }

    private void addButton(GridLayout parent, String title, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setOnClickListener(listener);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = 62;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(3, 3, 3, 3);
        parent.addView(button, lp);
    }
