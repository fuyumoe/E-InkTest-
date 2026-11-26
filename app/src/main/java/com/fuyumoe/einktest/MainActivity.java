java
package com.fuyumoe.einktest;

import android.app.Activity;
import android.os.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.content.Context;
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

        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(8, 8, 8, 8);

        // 测试画布
        v = new TestView(this);
        r.addView(v, new LinearLayout.LayoutParams(-1, 0, 1));

        // 两列按钮
        GridLayout bs = new GridLayout(this);
        bs.setColumnCount(2);
        bs.setRowCount(4);

        addButton(bs, "API 探测", x -> probe());
        addButton(bs, "A2 100", x -> refresh(A2, "A2"));
        addButton(bs, "Regal 15", x -> refresh(REGAL, "Regal"));
        addButton(bs, "Part 3", x -> refresh(PART, "Part"));
        addButton(bs, "Full 1", x -> refresh(FULL, "Full"));
        addButton(bs, "Fast 18", x -> refresh(FAST, "Fast"));
        addButton(bs, "局部刷新", x -> local());
        addButton(bs, "A2 → Regal", x -> a2regal());

        r.addView(bs, new LinearLayout.LayoutParams(-1, -2));

        // 日志
        log = new TextView(this);
        log.setTextSize(16);
        log.setTextColor(Color.BLACK);
        log.setPadding(6, 6, 6, 6);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(log);

        r.addView(scroll, new LinearLayout.LayoutParams(-1, 170));

        setContentView(r);

        println(
            "设备=" + Build.MANUFACTURER +
            " " + Build.MODEL +
            " platform=" + prop("ro.board.platform") +
            " SDK=" + Build.VERSION.SDK_INT
        );
    }

    // 添加大按钮
    void addButton(GridLayout parent, String text, View.OnClickListener listener) {

        Button b = new Button(this);

        b.setText(text);

        // 墨水屏上字体明显放大
        b.setTextSize(20);

        // 防止文字上下被裁剪
        b.setGravity(Gravity.CENTER);

        b.setSingleLine(true);

        b.setPadding(4, 4, 4, 4);

        b.setOnClickListener(listener);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();

        lp.width = 0;
        lp.height = 70;

        lp.columnSpec = GridLayout.spec(
            GridLayout.UNDEFINED,
            1f
        );

        lp.setMargins(3, 3, 3, 3);

        parent.addView(b, lp);
    }

    void println(String s) {

        if (log == null) return;

        log.append("\n" + s);

        log.post(() -> {
            ViewParent p = log.getParent();

            if (p instanceof ScrollView) {
                ((ScrollView)p).fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    String prop(String n) {

        try {

            java.lang.Process p =
                Runtime.getRuntime().exec(
                    new String[]{"getprop", n}
                );

            return new java.io.BufferedReader(
                new java.io.InputStreamReader(
                    p.getInputStream()
                )
            ).readLine();

        } catch (Exception e) {

            return "?";
        }
    }

    void refresh(int mode, String name) {

        boolean ok = false;

        StringBuilder s =
            new StringBuilder(name + " = " + mode);

        for (String mn :
            new String[]{
                "setEinkUpdateMode",
                "setByEinkUpdateMode"
            }) {

            try {

                Method m =
                    View.class.getMethod(
                        mn,
                        int.class
                    );

                m.invoke(v, mode);

                s.append("\n")
                    .append(mn)
                    .append(": OK");

                ok = true;

                break;

            } catch (NoSuchMethodException e) {

                s.append("\n")
                    .append(mn)
                    .append(": 不存在");

            } catch (Throwable e) {

                s.append("\n")
                    .append(mn)
                    .append(": FAIL ")
                    .append(err(e));
            }
        }

        v.flip();
        v.invalidate();

        println(s.toString());

        Toast.makeText(
            this,
            ok ? name + " 已调用" : "未找到可用API",
            Toast.LENGTH_SHORT
        ).show();
    }

    void probe() {

        println("=== API Probe ===");

        for (
            String n :
            new String[]{
                "setEinkUpdateMode",
                "setByEinkUpdateMode"
            }
        ) {

            try {

                Method m =
                    View.class.getMethod(
                        n,
                        int.class
                    );

                println(
                    n + " FOUND " + m
                );

            } catch (Exception e) {

                println(
                    n + " NOT FOUND"
                );
            }
        }

        for (Method m :
            View.class.getMethods()) {

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
                    "FOUND: " + m
                );
            }
        }
    }

    void local() {

        try {

            Method m =
                View.class.getMethod(
                    "setEinkUpdateMode",
                    int.class
                );

            m.invoke(v, REGAL);

            println(
                "Regal mode set"
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
    }

    void a2regal() {

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

    String err(Throwable e) {

        Throwable t =
            e instanceof InvocationTargetException &&
            ((InvocationTargetException)e).getCause() != null
                ? ((InvocationTargetException)e).getCause()
                : e;

        return
            t.getClass().getSimpleName() +
            ": " +
            String.valueOf(
                t.getMessage()
            );
    }

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

        protected void onDraw(Canvas c) {

            int w = getWidth();
            int h = getHeight();

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

            // 棋盘格
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
            p.setTextSize(24);

            c.drawText(
                inv
                    ? "State B"
                    : "State A",
                24,
                88,
                p
            );

            // 测试区域
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
