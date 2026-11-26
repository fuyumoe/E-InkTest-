package com.fuyumoe.einktest;

import android.app.Activity;
import android.os.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.lang.reflect.*;
import java.util.*;

public class MainActivity extends Activity {

    TestView v;
    TextView log;

    static final int A2 = 100;
    static final int REGAL = 15;
    static final int PART = 3;
    static final int FULL = 1;
    static final int FAST = 18;

    public void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 8, 8, 8);

        // E-Ink 测试画布
        v = new TestView(this);
        root.addView(
            v,
            new LinearLayout.LayoutParams(
                -1,
                0,
                1
            )
        );

        // 按钮区域
        GridLayout buttons = new GridLayout(this);
        buttons.setColumnCount(2);
        buttons.setRowCount(5);

        addButton(buttons, "API 探测", x -> probe());
        addButton(buttons, "A2 100", x -> refresh(A2, "A2"));
        addButton(buttons, "Regal 15", x -> refresh(REGAL, "Regal"));
        addButton(buttons, "Part 3", x -> refresh(PART, "Part"));
        addButton(buttons, "Full 1", x -> refresh(FULL, "Full"));
        addButton(buttons, "Fast 18", x -> refresh(FAST, "Fast"));
        addButton(buttons, "局部刷新测试", x -> local());
        addButton(buttons, "A2 → Regal", x -> a2regal());
        addButton(buttons, "导出日志", x -> exportLog());
        addButton(buttons, "清空日志", x -> clearLog());

        root.addView(
            buttons,
            new LinearLayout.LayoutParams(
                -1,
                -2
            )
        );

        // 日志区域
        log = new TextView(this);
        log.setTextSize(16);
        log.setTextIsSelectable(true);
        log.setPadding(8, 8, 8, 8);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(
            log,
            new ScrollView.LayoutParams(
                -1,
                -2
            )
        );

        root.addView(
            scroll,
            new LinearLayout.LayoutParams(
                -1,
                260
            )
        );

        setContentView(root);

        println(
            "设备=" + Build.MANUFACTURER +
            " " + Build.MODEL +
            " platform=" + prop("ro.board.platform") +
            " SDK=" + Build.VERSION.SDK_INT
        );

        println("=== K103 E-Ink Test ===");
        println("按钮：API探测 / A2 / Regal / Part / Full / Fast");
        println("支持：局部刷新 / A2→Regal / 导出日志");
    }

    // =========================================================
    // 按钮
    // =========================================================

    void addButton(GridLayout parent, String text, View.OnClickListener listener) {

        Button b = new Button(this);

        b.setText(text);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setOnClickListener(listener);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();

        lp.width = 0;
        lp.height = 68;

        lp.columnSpec = GridLayout.spec(
            GridLayout.UNDEFINED,
            1f
        );

        lp.setMargins(4, 4, 4, 4);

        parent.addView(b, lp);
    }

    // =========================================================
    // 日志
    // =========================================================

    void println(String s) {

        if (log == null) {
            return;
        }

        log.append(s);
        log.append("\n");

        log.post(() -> {

            if (log.getParent() instanceof ScrollView) {

                ScrollView sv = (ScrollView) log.getParent();

                sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    void clearLog() {

        if (log != null) {
            log.setText("");
        }

        println("日志已清空");
    }

    // =========================================================
    // 导出日志
    // =========================================================

    void exportLog() {

        try {

            String text = log.getText().toString();

            if (text.length() == 0) {
                Toast.makeText(
                    this,
                    "当前没有日志",
                    Toast.LENGTH_SHORT
                ).show();

                return;
            }

            java.io.File dir =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                );

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename =
                "K103_EInkTest_log_" +
                new java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(new java.util.Date()) +
                ".txt";

            java.io.File file =
                new java.io.File(dir, filename);

            java.io.FileWriter writer =
                new java.io.FileWriter(file, false);

            writer.write(text);
            writer.flush();
            writer.close();

            println("");
            println("================================");
            println("日志导出成功");
            println("文件：" + file.getAbsolutePath());
            println("================================");

            Toast.makeText(
                this,
                "日志已导出到 Download",
                Toast.LENGTH_LONG
            ).show();

        } catch (Throwable e) {

            println("日志导出失败：" + err(e));

            Toast.makeText(
                this,
                "导出失败：" + e.getMessage(),
                Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // 系统属性
    // =========================================================

    String prop(String n) {

        try {

            java.lang.Process p =
                Runtime.getRuntime().exec(
                    new String[]{"getprop", n}
                );

            java.io.BufferedReader r =
                new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                        p.getInputStream()
                    )
                );

            String s = r.readLine();

            r.close();

            return s == null ? "?" : s;

        } catch (Exception e) {

            return "?";
        }
    }

    // =========================================================
    // E-Ink 刷新模式
    // =========================================================

    void refresh(int mode, String name) {

        boolean ok = false;

        StringBuilder s =
            new StringBuilder(
                name + "=" + mode
            );

        // 第一种可能的 RK3368 API
        try {

            Method m =
                View.class.getMethod(
                    "setEinkUpdateMode",
                    int.class
                );

            m.invoke(v, mode);

            s.append("\nsetEinkUpdateMode: OK");

            ok = true;

        } catch (NoSuchMethodException e) {

            s.append(
                "\nsetEinkUpdateMode: 不存在"
            );

        } catch (Throwable e) {

            s.append(
                "\nsetEinkUpdateMode: FAIL "
            );

            s.append(err(e));
        }

        // 第二种可能的 API
        if (!ok) {

            try {

                Method m =
                    View.class.getMethod(
                        "setByEinkUpdateMode",
                        int.class
                    );

                m.invoke(v, mode);

                s.append(
                    "\nsetByEinkUpdateMode: OK"
                );

                ok = true;

            } catch (NoSuchMethodException e) {

                s.append(
                    "\nsetByEinkUpdateMode: 不存在"
                );

            } catch (Throwable e) {

                s.append(
                    "\nsetByEinkUpdateMode: FAIL "
                );

                s.append(err(e));
            }
        }

        v.flip();
        v.invalidate();

        println(s.toString());

        Toast.makeText(
            this,
            ok
                ? name + " 已调用"
                : "未找到可用API",
            Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // API 探测
    // =========================================================

    void probe() {

        println("");
        println("================================");
        println("=== E-Ink API Probe ===");
        println("================================");

        String[] names = {
            "setEinkUpdateMode",
            "setByEinkUpdateMode"
        };

        for (String n : names) {

            try {

                Method m =
                    View.class.getMethod(
                        n,
                        int.class
                    );

                println(
                    n +
                    " FOUND " +
                    m.toString()
                );

            } catch (Exception e) {

                println(
                    n +
                    " NOT FOUND"
                );
            }
        }

        println("");
        println("扫描 View.class 中的 E-Ink 相关方法：");

        for (Method m : View.class.getMethods()) {

            String n =
                m.getName().toLowerCase(
                    Locale.US
                );

            if (
                n.contains("eink") ||
                n.contains("epd") ||
                n.contains("epaper") ||
                n.contains("wave")
            ) {

                println(
                    "FOUND: " +
                    m.toString()
                );
            }
        }

        println("=== Probe 完成 ===");
    }

    // =========================================================
    // 局部刷新
    // =========================================================

    void local() {

        try {

            Method m =
                View.class.getMethod(
                    "setEinkUpdateMode",
                    int.class
                );

            m.invoke(v, REGAL);

            println(
                "setEinkUpdateMode(15) = OK"
            );

        } catch (Throwable e) {

            println(
                "Regal set FAIL " +
                err(e)
            );
        }

        v.flip();

        Rect q = v.rect();

        v.invalidate(q);

        println(
            "invalidate Rect " +
            q.toShortString()
        );

        println(
            "注意：这里是 Android View 的 Rect invalidate，"
            + "不等于已经证明底层 EPD 使用了真正的区域刷新 API。"
        );
    }

    // =========================================================
    // A2 → Regal
    // =========================================================

    void a2regal() {

        println("开始 A2 → Regal 测试");

        refresh(A2, "A2");

        v.postDelayed(
            () -> refresh(REGAL, "Regal"),
            180
        );

        v.postDelayed(
            () -> refresh(REGAL, "Regal"),
            360
        );
    }

    // =========================================================
    // 异常信息
    // =========================================================

    String err(Throwable e) {

        Throwable t = e;

        if (
            e instanceof InvocationTargetException &&
            ((InvocationTargetException)e).getCause() != null
        ) {

            t =
                ((InvocationTargetException)e)
                    .getCause();
        }

        return
            t.getClass().getSimpleName() +
            ": " +
            String.valueOf(
                t.getMessage()
            );
    }

    // =========================================================
    // 测试画布
    // =========================================================

    class TestView extends View {

        Paint p = new Paint();

        boolean inv = false;

        Rect rr = new Rect();

        TestView(Context c) {

            super(c);

            setBackgroundColor(
                Color.WHITE
            );
        }

        @Override
        protected void onDraw(Canvas c) {

            int w = getWidth();
            int h = getHeight();

            // 白色背景
            p.setStyle(
                Paint.Style.FILL
            );

            p.setColor(
                Color.WHITE
            );

            c.drawRect(
                0,
                0,
                w,
                h,
                p
            );

            // 黑白棋盘格
            p.setColor(
                Color.BLACK
            );

            int z =
                Math.max(
                    18,
                    Math.min(w, h) / 12
                );

            for (
                int y = 0;
                y < h;
                y += z
            ) {

                for (
                    int x = 0;
                    x < w;
                    x += z
                ) {

                    if (
                        ((x / z) +
                         (y / z)) % 2 == 0
                    ) {

                        c.drawRect(
                            x,
                            y,
                            x + z,
                            y + z,
                            p
                        );
                    }
                }
            }

            // 标题
            p.setTextSize(32);

            p.setColor(
                inv
                    ? Color.WHITE
                    : Color.BLACK
            );

            c.drawText(
                "K103 E-Ink Test",
                24,
                50,
                p
            );

            // 状态
            p.setTextSize(20);

            c.drawText(
                inv
                    ? "State B"
                    : "State A",
                24,
                82,
                p
            );

            // 中央测试区域
            rr.set(
                w / 4,
                h / 3,
                3 * w / 4,
                2 * h / 3
            );

            p.setStyle(
                Paint.Style.STROKE
            );

            p.setStrokeWidth(5);

            p.setColor(
                inv
                    ? Color.BLACK
                    : Color.WHITE
            );

            c.drawRect(
                rr,
                p
            );
        }

        void flip() {

            inv = !inv;
        }

        Rect rect() {

            return new Rect(rr);
        }
    }
}
