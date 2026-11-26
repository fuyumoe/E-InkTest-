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

    static final int REQUEST_EXPORT_LOG = 1001;

    static final int A2 = 100;
    static final int REGAL = 15;
    static final int PART = 3;
    static final int FULL = 1;
    static final int FAST = 18;

    @Override
    public void onCreate(Bundle b) {

        super.onCreate(b);

        LinearLayout root =
            new LinearLayout(this);

        root.setOrientation(
            LinearLayout.VERTICAL
        );

        root.setPadding(
            8,
            8,
            8,
            8
        );

        // =====================================================
        // 测试画布
        // =====================================================

        v = new TestView(this);

        root.addView(
            v,
            new LinearLayout.LayoutParams(
                -1,
                0,
                1
            )
        );

        // =====================================================
        // 按钮
        // =====================================================

        GridLayout buttons =
            new GridLayout(this);

        buttons.setColumnCount(2);
        buttons.setRowCount(6);

        addButton(
            buttons,
            "深度扫描 E-Ink API",
            x -> deepScan()
        );

        addButton(
            buttons,
            "读取当前模式",
            x -> readModes()
        );

        addButton(
            buttons,
            "A2 100",
            x -> refresh(
                A2,
                "A2"
            )
        );

        addButton(
            buttons,
            "Regal 15",
            x -> refresh(
                REGAL,
                "Regal"
            )
        );

        addButton(
            buttons,
            "Part 3",
            x -> refresh(
                PART,
                "Part"
            )
        );

        addButton(
            buttons,
            "Full 1",
            x -> refresh(
                FULL,
                "Full"
            )
        );

        addButton(
            buttons,
            "Fast 18",
            x -> refresh(
                FAST,
                "Fast"
            )
        );

        addButton(
            buttons,
            "setBy A2",
            x -> refreshBy(
                A2,
                "setBy A2"
            )
        );

        addButton(
            buttons,
            "setBy Regal",
            x -> refreshBy(
                REGAL,
                "setBy Regal"
            )
        );

        addButton(
            buttons,
            "局部刷新测试",
            x -> local()
        );

        addButton(
            buttons,
            "导出日志",
            x -> exportLog()
        );

        addButton(
            buttons,
            "清空日志",
            x -> clearLog()
        );

        root.addView(
            buttons,
            new LinearLayout.LayoutParams(
                -1,
                -2
            )
        );

        // =====================================================
        // 日志
        // =====================================================

        log =
            new TextView(this);

        log.setTextSize(15);

        log.setTextIsSelectable(
            true
        );

        log.setPadding(
            8,
            8,
            8,
            8
        );

        ScrollView scroll =
            new ScrollView(this);

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
                300
            )
        );

        setContentView(root);

        // =====================================================
        // 启动信息
        // =====================================================

        println(
            "================================"
        );

        println(
            "K103 E-Ink Deep Scanner"
        );

        println(
            "================================"
        );

        println(
            "设备 = " +
            Build.MANUFACTURER +
            " " +
            Build.MODEL
        );

        println(
            "平台 = " +
            prop(
                "ro.board.platform"
            )
        );

        println(
            "SDK = " +
            Build.VERSION.SDK_INT
        );

        println(
            "系统 = " +
            Build.VERSION.RELEASE
        );

        println(
            "准备完成，请点击“深度扫描 E-Ink API”"
        );
    }

    // =========================================================
    // 按钮
    // =========================================================

    void addButton(
        GridLayout parent,
        String text,
        View.OnClickListener listener
    ) {

        Button b =
            new Button(this);

        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);

        b.setOnClickListener(
            listener
        );

        GridLayout.LayoutParams lp =
            new GridLayout.LayoutParams();

        lp.width = 0;
        lp.height = 68;

        lp.columnSpec =
            GridLayout.spec(
                GridLayout.UNDEFINED,
                1f
            );

        lp.setMargins(
            4,
            4,
            4,
            4
        );

        parent.addView(
            b,
            lp
        );
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

        log.post(
            () -> {

                if (
                    log.getParent()
                    instanceof ScrollView
                ) {

                    ScrollView sv =
                        (ScrollView)
                        log.getParent();

                    sv.post(
                        () ->
                            sv.fullScroll(
                                View.FOCUS_DOWN
                            )
                    );
                }
            }
        );
    }

    void clearLog() {

        if (log != null) {
            log.setText("");
        }

        println(
            "日志已清空"
        );
    }

    // =========================================================
    // 深度扫描
    // =========================================================

    void deepScan() {

        println("");
        println(
            "========================================"
        );
        println(
            "开始深度扫描 E-Ink API"
        );
        println(
            "========================================"
        );

        println(
            "注意：本次扫描只读取方法/字段信息，"
            +
            "不会自动调用未知刷新接口。"
        );

        println("");

        scanBasicInfo();

        scanMethods();

        scanFields();

        scanInnerClasses();

        scanEinkCandidates();

        scanKnownGetters();

        println("");

        println(
            "========================================"
        );

        println(
            "深度扫描完成"
        );

        println(
            "请点击“导出日志”把完整结果保存。"
        );

        println(
            "========================================"
        );

        Toast.makeText(
            this,
            "深度扫描完成",
            Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // 基本信息
    // =========================================================

    void scanBasicInfo() {

        println("");
        println(
            "----- 基本 API 信息 -----"
        );

        try {

            Method m =
                View.class.getMethod(
                    "setEinkUpdateMode",
                    int.class
                );

            println(
                "setEinkUpdateMode = " +
                m.toGenericString()
            );

        } catch (Throwable e) {

            println(
                "setEinkUpdateMode = NOT FOUND"
            );
        }

        try {

            Method m =
                View.class.getMethod(
                    "setByEinkUpdateMode",
                    int.class
                );

            println(
                "setByEinkUpdateMode = " +
                m.toGenericString()
            );

        } catch (Throwable e) {

            println(
                "setByEinkUpdateMode = NOT FOUND"
            );
        }

        try {

            Method m =
                View.class.getMethod(
                    "getEinkUpdateMode"
                );

            println(
                "getEinkUpdateMode = " +
                m.toGenericString()
            );

        } catch (Throwable e) {

            println(
                "getEinkUpdateMode = NOT FOUND"
            );
        }

        try {

            Method m =
                View.class.getMethod(
                    "getByEinkUpdateMode"
                );

            println(
                "getByEinkUpdateMode = " +
                m.toGenericString()
            );

        } catch (Throwable e) {

            println(
                "getByEinkUpdateMode = NOT FOUND"
            );
        }
    }

    // =========================================================
    // 扫描方法
    // =========================================================

    void scanMethods() {

        println("");
        println(
            "----- View.class 所有声明方法 -----"
        );

        Method[] methods;

        try {

            methods =
                View.class.getDeclaredMethods();

        } catch (Throwable e) {

            println(
                "getDeclaredMethods FAIL: " +
                err(e)
            );

            return;
        }

        println(
            "DeclaredMethods 数量 = " +
            methods.length
        );

        for (Method m : methods) {

            String name =
                m.getName();

            String lower =
                name.toLowerCase(
                    Locale.US
                );

            if (
                isInteresting(
                    lower
                )
            ) {

                println(
                    "[METHOD] " +
                    m.toGenericString()
                );
            }
        }

        println("");
        println(
            "----- View.class 所有公开方法中的 E-Ink 候选 -----"
        );

        try {

            Method[] publicMethods =
                View.class.getMethods();

            for (
                Method m :
                publicMethods
            ) {

                String lower =
                    m.getName()
                        .toLowerCase(
                            Locale.US
                        );

                if (
                    isInteresting(
                        lower
                    )
                ) {

                    println(
                        "[PUBLIC] " +
                        m.toGenericString()
                    );
                }
            }

        } catch (Throwable e) {

            println(
                "getMethods FAIL: " +
                err(e)
            );
        }
    }

    // =========================================================
    // 扫描字段
    // =========================================================

    void scanFields() {

        println("");
        println(
            "----- View.class E-Ink 相关字段 -----"
        );

        Field[] fields;

        try {

            fields =
                View.class.getDeclaredFields();

        } catch (Throwable e) {

            println(
                "getDeclaredFields FAIL: " +
                err(e)
            );

            return;
        }

        println(
            "DeclaredFields 数量 = " +
            fields.length
        );

        for (Field f : fields) {

            String name =
                f.getName();

            String lower =
                name.toLowerCase(
                    Locale.US
                );

            if (
                isInteresting(
                    lower
                )
            ) {

                String value =
                    readStaticField(
                        f
                    );

                println(
                    "[FIELD] " +
                    f.toGenericString() +
                    " = " +
                    value
                );
            }
        }
    }

    // =========================================================
    // 内部类
    // =========================================================

    void scanInnerClasses() {

        println("");
        println(
            "----- View.class 内部类 -----"
        );

        try {

            Class<?>[] classes =
                View.class.getDeclaredClasses();

            println(
                "内部类数量 = " +
                classes.length
            );

            for (
                Class<?> c :
                classes
            ) {

                String name =
                    c.getName();

                String lower =
                    name.toLowerCase(
                        Locale.US
                    );

                if (
                    isInteresting(
                        lower
                    )
                ) {

                    println(
                        "[INNER CLASS] " +
                        name
                    );

                    scanInnerClassDetails(
                        c
                    );
                }
            }

        } catch (Throwable e) {

            println(
                "getDeclaredClasses FAIL: " +
                err(e)
            );
        }
    }

    // =========================================================
    // 内部类详细信息
    // =========================================================

    void scanInnerClassDetails(
        Class<?> c
    ) {

        try {

            Field[] fields =
                c.getDeclaredFields();

            for (
                Field f :
                fields
            ) {

                String lower =
                    f.getName()
                        .toLowerCase(
                            Locale.US
                        );

                if (
                    isInteresting(
                        lower
                    )
                ) {

                    println(
                        "  [INNER FIELD] " +
                        f.toGenericString() +
                        " = " +
                        readStaticField(f)
                    );
                }
            }

        } catch (Throwable e) {

            println(
                "  fields FAIL: " +
                err(e)
            );
        }

        try {

            Method[] methods =
                c.getDeclaredMethods();

            for (
                Method m :
                methods
            ) {

                String lower =
                    m.getName()
                        .toLowerCase(
                            Locale.US
                        );

                if (
                    isInteresting(
                        lower
                    )
                ) {

                    println(
                        "  [INNER METHOD] " +
                        m.toGenericString()
                    );
                }
            }

        } catch (Throwable e) {

            println(
                "  methods FAIL: " +
                err(e)
            );
        }
    }

    // =========================================================
    // 特别扫描 Rect / 多 int 参数方法
    // =========================================================

    void scanEinkCandidates() {

        println("");
        println(
            "----- Rect / 坐标 / 更新参数候选 -----"
        );

        try {

            Method[] methods =
                View.class.getDeclaredMethods();

            for (
                Method m :
                methods
            ) {

                Class<?>[] p =
                    m.getParameterTypes();

                boolean hasRect =
                    false;

                int intCount = 0;

                for (
                    Class<?> type :
                    p
                ) {

                    if (
                        type ==
                        Rect.class
                    ) {

                        hasRect = true;
                    }

                    if (
                        type ==
                        Integer.TYPE
                    ) {

                        intCount++;
                    }
                }

                String name =
                    m.getName()
                        .toLowerCase(
                            Locale.US
                        );

                boolean nameMatch =
                    name.contains("eink") ||
                    name.contains("epd") ||
                    name.contains("epaper") ||
                    name.contains("refresh") ||
                    name.contains("update") ||
                    name.contains("wave") ||
                    name.contains("invalidate");

                if (
                    hasRect ||
                    (
                        nameMatch &&
                        intCount >= 2
                    )
                ) {

                    println(
                        "[RECT CANDIDATE] " +
                        m.toGenericString()
                    );
                }
            }

        } catch (Throwable e) {

            println(
                "Rect candidate scan FAIL: " +
                err(e)
            );
        }

        println("");
        println(
            "----- 所有 invalidate 相关方法 -----"
        );

        try {

            Method[] methods =
                View.class.getDeclaredMethods();

            for (
                Method m :
                methods
            ) {

                if (
                    m.getName()
                        .toLowerCase(
                            Locale.US
                        )
                        .contains(
                            "invalidate"
                        )
                ) {

                    println(
                        "[INVALIDATE] " +
                        m.toGenericString()
                    );
                }
            }

        } catch (Throwable e) {

            println(
                "invalidate scan FAIL: " +
                err(e)
            );
        }
    }

    // =========================================================
    // E-Ink 名称判断
    // =========================================================

    boolean isInteresting(
        String name
    ) {

        return
            name.contains("eink") ||
            name.contains("epd") ||
            name.contains("epaper") ||
            name.contains("wave") ||
            name.contains("waveform") ||
            name.contains("refresh") ||
            name.contains("update") ||
            name.contains("regal") ||
            name.contains("part") ||
            name.contains("full") ||
            name.contains("fast") ||
            name.contains("gc16") ||
            name.contains("gl16") ||
            name.contains("du4") ||
            name.contains("du") ||
            name.contains("dither");
    }

    // =========================================================
    // 读取静态字段
    // =========================================================

    String readStaticField(
        Field f
    ) {

        try {

            if (
                !Modifier.isStatic(
                    f.getModifiers()
                )
            ) {

                return "<non-static>";
            }

            f.setAccessible(
                true
            );

            Object value =
                f.get(null);

            return String.valueOf(
                value
            );

        } catch (Throwable e) {

            return
                "<READ FAIL: " +
                err(e) +
                ">";
        }
    }

    // =========================================================
    // 读取当前 E-Ink 模式
    // =========================================================

    void readModes() {

        println("");
        println(
            "----- 当前 E-Ink 模式 -----"
        );

        try {

            Method m =
                View.class.getMethod(
                    "getEinkUpdateMode"
                );

            Object value =
                m.invoke(null);

            println(
                "getEinkUpdateMode = " +
                String.valueOf(value)
            );

        } catch (Throwable e) {

            println(
                "getEinkUpdateMode FAIL: " +
                err(e)
            );
        }

        try {

            Method m =
                View.class.getMethod(
                    "getByEinkUpdateMode"
                );

            Object value =
                m.invoke(null);

            println(
                "getByEinkUpdateMode = " +
                String.valueOf(value)
            );

        } catch (Throwable e) {

            println(
                "getByEinkUpdateMode FAIL: " +
                err(e)
            );
        }
    }

    // =========================================================
    // setEinkUpdateMode
    // =========================================================

    void refresh(
        int mode,
        String name
    ) {

        try {

            Method m =
                View.class.getMethod(
                    "setEinkUpdateMode",
                    int.class
                );

            m.invoke(
                null,
                mode
            );

            println(
                "setEinkUpdateMode(" +
                mode +
                ") = OK"
            );

            println(
                "模式名称 = " +
                name
            );

            readModes();

            v.flip();

            v.invalidate();

            Toast.makeText(
                this,
                name + " 已调用",
                Toast.LENGTH_SHORT
            ).show();

        } catch (Throwable e) {

            println(
                "setEinkUpdateMode FAIL: " +
                err(e)
            );
        }
    }

    // =========================================================
    // setByEinkUpdateMode
    // =========================================================

    void refreshBy(
        int mode,
        String name
    ) {

        try {

            Method m =
                View.class.getMethod(
                    "setByEinkUpdateMode",
                    int.class
                );

            m.invoke(
                null,
                mode
            );

            println(
                "setByEinkUpdateMode(" +
                mode +
                ") = OK"
            );

            println(
                "模式名称 = " +
                name
            );

            readModes();

            v.flip();

            v.invalidate();

            Toast.makeText(
                this,
                name + " 已调用",
                Toast.LENGTH_SHORT
            ).show();

        } catch (Throwable e) {

            println(
                "setByEinkUpdateMode FAIL: " +
                err(e)
            );
        }
    }

    // =========================================================
    // 局部刷新测试
    // =========================================================

    void local() {

        try {

            Method m =
                View.class.getMethod(
                    "setEinkUpdateMode",
                    int.class
                );

            m.invoke(
                null,
                REGAL
            );

            println(
                "setEinkUpdateMode(15) = OK"
            );

        } catch (Throwable e) {

            println(
                "Regal set FAIL: " +
                err(e)
            );
        }

        v.flip();

        Rect q =
            v.rect();

        v.invalidate(q);

        println(
            "View.invalidate(Rect) = " +
            q.toShortString()
        );

        println(
            "注意：这只是 Android View 层的 Rect invalidate，"
            +
            "暂时不能证明 EPD 硬件执行了区域刷新。"
        );
    }

    // =========================================================
    // 系统属性
    // =========================================================

    String prop(
        String n
    ) {

        try {

            java.lang.Process p =
                Runtime.getRuntime().exec(
                    new String[]{
                        "getprop",
                        n
                    }
                );

            java.io.BufferedReader r =
                new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                        p.getInputStream()
                    )
                );

            String s =
                r.readLine();

            r.close();

            return s == null
                ? "?"
                : s;

        } catch (Exception e) {

            return "?";
        }
    }

    // =========================================================
    // 导出日志
    // =========================================================

    void exportLog() {

        try {

            String text =
                log.getText().toString();

            if (
                text.length() == 0
            ) {

                Toast.makeText(
                    this,
                    "当前没有日志",
                    Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Intent intent =
                new Intent(
                    Intent.ACTION_CREATE_DOCUMENT
                );

            intent.addCategory(
                Intent.CATEGORY_OPENABLE
            );

            intent.setType(
                "text/plain"
            );

            String filename =
                "K103_EInkDeepScan_" +
                new java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(
                    new java.util.Date()
                ) +
                ".txt";

            intent.putExtra(
                Intent.EXTRA_TITLE,
                filename
            );

            println(
                "正在打开系统保存窗口..."
            );

            startActivityForResult(
                intent,
                REQUEST_EXPORT_LOG
            );

        } catch (Throwable e) {

            println(
                "打开保存窗口失败: " +
                err(e)
            );
        }
    }

    // =========================================================
    // 保存日志
    // =========================================================

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        );

        if (
            requestCode ==
                REQUEST_EXPORT_LOG &&
            resultCode ==
                RESULT_OK &&
            data != null
        ) {

            Uri uri =
                data.getData();

            if (uri == null) {

                println(
                    "没有选择文件"
                );

                return;
            }

            try {

                java.io.OutputStream out =
                    getContentResolver()
                        .openOutputStream(
                            uri
                        );

                if (out == null) {

                    throw new Exception(
                        "无法打开文件"
                    );
                }

                String text =
                    log.getText().toString();

                out.write(
                    text.getBytes(
                        "UTF-8"
                    )
                );

                out.flush();
                out.close();

                println("");
                println(
                    "=============================="
                );

                println(
                    "日志导出成功"
                );

                println(
                    "=============================="
                );

                Toast.makeText(
                    this,
                    "日志保存成功",
                    Toast.LENGTH_LONG
                ).show();

            } catch (Throwable e) {

                println(
                    "日志保存失败: " +
                    err(e)
                );
            }

        } else if (
            requestCode ==
                REQUEST_EXPORT_LOG
        ) {

            println(
                "日志导出取消"
            );
        }
    }

    // =========================================================
    // 异常信息
    // =========================================================

    String err(
        Throwable e
    ) {

        Throwable t = e;

        if (
            e instanceof
                InvocationTargetException
            &&
            ((InvocationTargetException)e)
                .getCause() != null
        ) {

            t =
                ((InvocationTargetException)e)
                    .getCause();
        }

        return
            t.getClass()
                .getSimpleName()
            +
            ": "
            +
            String.valueOf(
                t.getMessage()
            );
    }

    // =========================================================
    // 测试 View
    // =========================================================

    class TestView
        extends View {

        Paint p =
            new Paint();

        boolean inv =
            false;

        Rect rr =
            new Rect();

        TestView(
            Context c
        ) {

            super(c);

            setBackgroundColor(
                Color.WHITE
            );
        }

        @Override
        protected void onDraw(
            Canvas c
        ) {

            int w =
                getWidth();

            int h =
                getHeight();

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

            // 黑白棋盘

            p.setColor(
                Color.BLACK
            );

            int z =
                Math.max(
                    18,
                    Math.min(
                        w,
                        h
                    ) / 12
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
                        (
                            (x / z) +
                            (y / z)
                        ) % 2 == 0
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

            p.setTextSize(
                32
            );

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

            p.setTextSize(
                20
            );

            c.drawText(
                inv
                    ? "State B"
                    : "State A",
                24,
                82,
                p
            );

            // 测试矩形

            rr.set(
                w / 4,
                h / 3,
                3 * w / 4,
                2 * h / 3
            );

            p.setStyle(
                Paint.Style.STROKE
            );

            p.setStrokeWidth(
                5
            );

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

            inv =
                !inv;
        }

        Rect rect() {

            return new Rect(
                rr
            );
        }
    }
}
