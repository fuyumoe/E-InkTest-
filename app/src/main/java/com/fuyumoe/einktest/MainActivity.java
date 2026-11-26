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

    private void println(String message) {
        if (log == null) return;
        log.append(message);
        log.append("\n");
        log.post(() -> {
            if (log.getParent() instanceof ScrollView) {
                ((ScrollView) log.getParent()).fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void clearLog() {
        if (log != null) log.setText("");
        println("日志已清空");
    }

    // 深度扫描：只读取 API 元数据，不调用未知刷新接口。
    private void deepScan() {
        println("\n========== 开始深度扫描 E-Ink API ==========");
        println("扫描范围：android.view.View 的方法、字段和内部类");
        println("注意：不会自动调用未知刷新接口。");
        scanBasicInfo();
        scanMethods();
        scanFields();
        scanInnerClasses();
        scanEinkCandidates();
        scanKnownGetters();
        println("========== 深度扫描完成 ==========");
        println("请点击“导出日志”保存完整结果。");
        Toast.makeText(this, "深度扫描完成", Toast.LENGTH_LONG).show();
    }

    private void scanBasicInfo() {
        println("\n----- 已知 API 签名 -----");
        String[][] methods = {
            {"setEinkUpdateMode", "int"},
            {"setByEinkUpdateMode", "int"},
            {"getEinkUpdateMode"},
            {"getByEinkUpdateMode"}
        };

        for (String[] item : methods) {
            try {
                Method method = item.length == 2
                    ? View.class.getMethod(item[0], int.class)
                    : View.class.getMethod(item[0]);
                println("[API] " + method.toGenericString());
            } catch (Throwable e) {
                println("[API] " + item[0] + " = NOT FOUND (" + errorText(e) + ")");
            }
        }
    }

    private void scanMethods() {
        println("\n----- View 声明方法中的相关项 -----");
        try {
            Method[] methods = View.class.getDeclaredMethods();
            println("声明方法总数 = " + methods.length);
            for (Method method : methods) {
                if (isInteresting(method.getName())) {
                    println("[METHOD] " + method.toGenericString());
                }
            }
        } catch (Throwable e) {
            println("声明方法扫描失败：" + errorText(e));
        }

        println("\n----- View 公开方法中的相关项 -----");
        try {
            for (Method method : View.class.getMethods()) {
                if (isInteresting(method.getName())) {
                    println("[PUBLIC] " + method.toGenericString());
                }
            }
        } catch (Throwable e) {
            println("公开方法扫描失败：" + errorText(e));
        }
    }

    private void scanFields() {
        println("\n----- View 相关字段 -----");
        try {
            Field[] fields = View.class.getDeclaredFields();
            println("声明字段总数 = " + fields.length);
            for (Field field : fields) {
                if (isInteresting(field.getName())) {
                    println("[FIELD] " + field.toGenericString() + " = " + readStaticField(field));
                }
            }
        } catch (Throwable e) {
            println("字段扫描失败：" + errorText(e));
        }
    }

    private void scanInnerClasses() {
        println("\n----- View 内部类 -----");
        try {
            Class<?>[] classes = View.class.getDeclaredClasses();
            println("内部类总数 = " + classes.length);
            for (Class<?> type : classes) {
                if (!isInteresting(type.getName())) continue;
                println("[INNER CLASS] " + type.getName());

                try {
                    for (Field field : type.getDeclaredFields()) {
                        if (isInteresting(field.getName())) {
                            println("  [FIELD] " + field.toGenericString()
                                + " = " + readStaticField(field));
                        }
                    }
                } catch (Throwable e) {
                    println("  内部类字段扫描失败：" + errorText(e));
                }

                try {
                    for (Method method : type.getDeclaredMethods()) {
                        if (isInteresting(method.getName())) {
                            println("  [METHOD] " + method.toGenericString());
                        }
                    }
                } catch (Throwable e) {
                    println("  内部类方法扫描失败：" + errorText(e));
                }
            }
        } catch (Throwable e) {
            println("内部类扫描失败：" + errorText(e));
        }
    }

    private void scanEinkCandidates() {
        println("\n----- Rect / 坐标参数候选方法 -----");
        try {
            for (Method method : View.class.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                boolean hasRect = false;
                int intCount = 0;

                for (Class<?> param : params) {
                    if (param == Rect.class) hasRect = true;
                    if (param == int.class) intCount++;
                }

                boolean nameMatch = isInteresting(method.getName())
                    || method.getName().toLowerCase(Locale.US).contains("invalidate");

                if (hasRect || (nameMatch && intCount >= 2)) {
                    println("[CANDIDATE] " + method.toGenericString());
                }
            }
        } catch (Throwable e) {
            println("候选方法扫描失败：" + errorText(e));
        }

        println("\n----- invalidate 方法 -----");
        try {
            for (Method method : View.class.getDeclaredMethods()) {
                if (method.getName().toLowerCase(Locale.US).contains("invalidate")) {
                    println("[INVALIDATE] " + method.toGenericString());
                }
            }
        } catch (Throwable e) {
            println("invalidate 扫描失败：" + errorText(e));
        }
    }

    private void scanKnownGetters() {
        println("\n----- 已知 Getter 实际返回值 -----");
        readStaticGetter("getEinkUpdateMode");
        readStaticGetter("getByEinkUpdateMode");
    }

    private void readStaticGetter(String name) {
        try {
            Method method = View.class.getMethod(name);
            Object value = method.invoke(null);
            println(name + "() = " + value);
        } catch (Throwable e) {
            println(name + "() 读取失败：" + errorText(e));
        }
    }

    private boolean isInteresting(String name) {
        String s = name.toLowerCase(Locale.US);
        return s.contains("eink") || s.contains("epd") || s.contains("epaper")
            || s.contains("wave") || s.contains("refresh") || s.contains("update")
            || s.contains("regal") || s.contains("part") || s.contains("full")
            || s.contains("fast") || s.contains("gc16") || s.contains("gl16")
            || s.contains("du4") || s.equals("du") || s.contains("dither");
    }

    private String readStaticField(Field field) {
        if (!Modifier.isStatic(field.getModifiers())) return "<非静态字段>";
        try {
            field.setAccessible(true);
            return String.valueOf(field.get(null));
        } catch (Throwable e) {
            return "<读取失败：" + errorText(e) + ">";
        }
    }

    private void readModes() {
        println("\n----- 当前 E-Ink 模式 -----");
        readStaticGetter("getEinkUpdateMode");
        readStaticGetter("getByEinkUpdateMode");
    }

    private void refresh(int mode, String name) {
        invokeModeSetter("setEinkUpdateMode", mode, name);
    }

    private void refreshBy(int mode, String name) {
        invokeModeSetter("setByEinkUpdateMode", mode, name);
    }

    private void invokeModeSetter(String methodName, int mode, String label) {
        try {
            Method method = View.class.getMethod(methodName, int.class);
            method.invoke(null, mode);
            println(methodName + "(" + mode + ") = OK [" + label + "]");
            readModes();
            testView.flip();
            testView.invalidate();
            Toast.makeText(this, label + " 已调用", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            println(methodName + " 调用失败：" + errorText(e));
        }
    }

    private void localTest() {
        try {
            View.class.getMethod("setEinkUpdateMode", int.class).invoke(null, REGAL);
            println("setEinkUpdateMode(15) = OK");
        } catch (Throwable e) {
            println("Regal 设置失败：" + errorText(e));
        }

        testView.flip();
        Rect rect = testView.getTestRect();
        testView.invalidate(rect);
        println("View.invalidate(Rect) = " + rect.toShortString());
        println("说明：这是 View 层局部失效请求，不能据此认定 EPD 硬件执行了区域刷新。");
    }

    private String prop(String name) {
        try {
            java.lang.Process process = Runtime.getRuntime().exec(new String[]{"getprop", name});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String value = reader.readLine();
            reader.close();
            return value == null ? "?" : value;
        } catch (Exception e) {
            return "?";
        }
    }

    private void exportLog() {
        if (log == null || log.length() == 0) {
            Toast.makeText(this, "当前没有日志", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            String filename = "K103_EInkDeepScan_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new java.util.Date()) + ".txt";
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            println("正在打开系统保存窗口...");
            startActivityForResult(intent, REQUEST_EXPORT_LOG);
        } catch (Throwable e) {
            println("打开保存窗口失败：" + errorText(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT_LOG) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            println("日志导出取消");
            return;
        }

        try {
            Uri uri = data.getData();
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out == null) throw new Exception("无法打开目标文件");
            out.write(log.getText().toString().getBytes("UTF-8"));
            out.flush();
            out.close();
            println("日志导出成功");
            Toast.makeText(this, "日志保存成功", Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            println("日志保存失败：" + errorText(e));
        }
    }

    private String errorText(Throwable error) {
        Throwable cause = error;
        if (error instanceof InvocationTargetException
            && ((InvocationTargetException) error).getCause() != null) {
            cause = ((InvocationTargetException) error).getCause();
        }
        return cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage());
    }

    private class TestView extends View {
        private final Paint paint = new Paint();
        private boolean state;
        private final Rect testRect = new Rect();

        TestView(Context context) {
            super(context);
            setBackgroundColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth(), height = getHeight();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, width, height, paint);

            int cell = Math.max(18, Math.min(width, height) / 12);
            paint.setColor(Color.BLACK);
            for (int y = 0; y < height; y += cell) {
                for (int x = 0; x < width; x += cell) {
                    if (((x / cell) + (y / cell)) % 2 == 0) {
                        canvas.drawRect(x, y, x + cell, y + cell, paint);
                    }
                }
            }

            paint.setTextSize(32);
            paint.setColor(state ? Color.WHITE : Color.BLACK);
            canvas.drawText("K103 E-Ink Test", 24, 50, paint);
            paint.setTextSize(20);
            canvas.drawText(state ? "State B" : "State A", 24, 82, paint);

            testRect.set(width / 4, height / 3, 3 * width / 4, 2 * height / 3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(state ? Color.BLACK : Color.WHITE);
            canvas.drawRect(testRect, paint);
        }

        void flip() {
            state = !state;
        }

        Rect getTestRect() {
            return new Rect(testRect);
        }
    }
}
