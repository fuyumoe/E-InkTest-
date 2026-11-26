package com.fuyumoe.einktest;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
    private final Handler handler = new Handler();

    // 翻页测试参数
    private int animationMode = A2;
    // 0=不清理，15=Regal，1=Full
    private int cleanupMode = REGAL;
    private long animationDuration = 650;
    private boolean autoPaging;
    private final Runnable autoPageTask = new Runnable() {
        @Override public void run() {
            if (!autoPaging) return;
            if (testView != null && !testView.isAnimating()) {
                startPageTurn();
            }
            handler.postDelayed(this, animationDuration + 500);
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(6, 6, 6, 6);

        testView = new TestView(this);
        root.addView(testView, new LinearLayout.LayoutParams(-1, 0, 1));

        ScrollView controlsScroll = new ScrollView(this);
        GridLayout buttons = new GridLayout(this);
        buttons.setColumnCount(3);

        addButton(buttons, "翻页一次", v -> startPageTurn());
        addButton(buttons, "连续翻页", v -> startAutoPaging());
        addButton(buttons, "停止连续", v -> stopAutoPaging());

        addButton(buttons, "动画 A2", v -> setAnimationMode(A2, "A2"));
        addButton(buttons, "动画 Part", v -> setAnimationMode(PART, "Part"));
        addButton(buttons, "动画 Regal", v -> setAnimationMode(REGAL, "Regal"));

        addButton(buttons, "不清理", v -> setCleanupMode(0, "不清理"));
        addButton(buttons, "结束 Regal", v -> setCleanupMode(REGAL, "Regal"));
        addButton(buttons, "结束 Full", v -> setCleanupMode(FULL, "Full"));

        addButton(buttons, "慢速 1200ms", v -> setSpeed(1200));
        addButton(buttons, "中速 650ms", v -> setSpeed(650));
        addButton(buttons, "快速 300ms", v -> setSpeed(300));

        addButton(buttons, "深度扫描 API", v -> deepScan());
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

        controlsScroll.addView(buttons);
        root.addView(controlsScroll, new LinearLayout.LayoutParams(-1, 220));

        log = new TextView(this);
        log.setTextSize(12);
        log.setTextIsSelectable(true);
        log.setPadding(6, 4, 6, 4);
        ScrollView logScroll = new ScrollView(this);
        logScroll.addView(log);
        root.addView(logScroll, new LinearLayout.LayoutParams(-1, 115));

        setContentView(root);

        println("================================");
        println("K103 E-Ink Page Turn Lab");
        println("设备 = " + Build.MANUFACTURER + " " + Build.MODEL);
        println("平台 = " + prop("ro.board.platform"));
        println("SDK = " + Build.VERSION.SDK_INT);
        println("系统 = " + Build.VERSION.RELEASE);
        println("翻页默认模式=A2；结束后默认Regal清理；动画时长=650ms");
        println("提示：动画采用从右向左的页面揭示效果，逐帧只请求重绘移动边界附近的窄条区域。");
        println("硬件是否真正按区域刷新，仍需观察屏幕，invalidate(Rect) 本身不能证明硬件局刷。");
    }

    private void addButton(GridLayout parent, String title, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = 52;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(2, 2, 2, 2);
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

    // -------------------- 翻页测试 --------------------

    private void setAnimationMode(int mode, String name) {
        animationMode = mode;
        println("翻页动画模式设为 " + name + " (" + mode + ")");
        Toast.makeText(this, "翻页模式：" + name, Toast.LENGTH_SHORT).show();
    }

    private void setCleanupMode(int mode, String name) {
        cleanupMode = mode;
        println("翻页结束清理方式设为：" + name);
        Toast.makeText(this, "结束清理：" + name, Toast.LENGTH_SHORT).show();
    }

    private void setSpeed(long duration) {
        animationDuration = duration;
        println("翻页动画时长 = " + duration + "ms");
        Toast.makeText(this, "动画时长 " + duration + "ms", Toast.LENGTH_SHORT).show();
    }

    private void startPageTurn() {
        if (testView == null) return;
        if (testView.isAnimating()) {
            println("上一轮翻页尚未结束，忽略本次请求");
            return;
        }

        if (!setEinkMode(animationMode)) {
            println("设置翻页刷新模式失败，仍继续执行动画以供观察");
        }

        println(String.format(Locale.US,
            "开始翻页：page=%d，mode=%d，duration=%dms",
            testView.getPageNumber(), animationMode, animationDuration));
        testView.startTurn(animationDuration, new Runnable() {
            @Override public void run() {
                println("翻页动画结束，当前页=" + testView.getPageNumber());
                applyCleanup();
            }
        });
    }

    private void applyCleanup() {
        if (cleanupMode == 0) {
            println("结束清理：关闭（不切换刷新模式、不主动全屏重绘）");
            return;
        }

        String label = cleanupMode == REGAL ? "Regal" : "Full";
        if (setEinkMode(cleanupMode)) {
            // 这是有意请求一次完整 View 重绘，观察结束清理模式的效果。
            testView.invalidate();
            println("结束清理请求：" + label + " (" + cleanupMode + ") + View 全区域重绘");
            println("注意：这不等于已确认硬件采用特定波形或全屏闪烁。");
        } else {
            println("结束清理模式设置失败：" + label);
        }
    }

    private boolean setEinkMode(int mode) {
        try {
            Method method = View.class.getMethod("setEinkUpdateMode", int.class);
            method.invoke(null, mode);
            println("setEinkUpdateMode(" + mode + ") = OK");
            return true;
        } catch (Throwable e) {
            println("setEinkUpdateMode(" + mode + ") 失败：" + errorText(e));
            return false;
        }
    }

    private void startAutoPaging() {
        if (autoPaging) return;
        autoPaging = true;
        println("开始连续翻页");
        handler.removeCallbacks(autoPageTask);
        autoPageTask.run();
    }

    private void stopAutoPaging() {
        autoPaging = false;
        handler.removeCallbacks(autoPageTask);
        println("已停止连续翻页");
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoPaging();
        if (testView != null) testView.cancelTurn();
    }

    // -------------------- 原有 E-Ink API 扫描功能 --------------------

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
            {"getByEinkUpdateMode"},
            {"byFullRefresh"}
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
                if (isInteresting(method.getName())) println("[METHOD] " + method.toGenericString());
            }
        } catch (Throwable e) {
            println("声明方法扫描失败：" + errorText(e));
        }

        println("\n----- View 公开方法中的相关项 -----");
        try {
            for (Method method : View.class.getMethods()) {
                if (isInteresting(method.getName())) println("[PUBLIC] " + method.toGenericString());
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
                            println("  [FIELD] " + field.toGenericString() + " = " + readStaticField(field));
                        }
                    }
                } catch (Throwable e) {
                    println("  内部类字段扫描失败：" + errorText(e));
                }
                try {
                    for (Method method : type.getDeclaredMethods()) {
                        if (isInteresting(method.getName())) println("  [METHOD] " + method.toGenericString());
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
        setEinkMode(REGAL);
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
            String filename = "K103_EInkPageTurn_"
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

    // -------------------- 页面绘制与局部动画 --------------------

    private class TestView extends View {
        private final Paint paint = new Paint();
        private final Rect testRect = new Rect();
        private final Rect dirtyRect = new Rect();
        private final int density;
        private int pageNumber = 1;
        private int fromPage = 1;
        private int toPage = 2;
        private int boundary;
        private int previousBoundary;
        private boolean animating;
        private ValueAnimator animator;
        private Runnable endAction;

        TestView(Context context) {
            super(context);
            density = Math.max(1, (int) getResources().getDisplayMetrics().density);
            setBackgroundColor(Color.WHITE);
            paint.setAntiAlias(false);
            paint.setFilterBitmap(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            if (!animating) {
                drawPage(canvas, pageNumber, w, h);
                return;
            }

            // 先画旧页，再将新页裁剪到翻页边界右侧，形成逐步揭页效果。
            drawPage(canvas, fromPage, w, h);
            int save = canvas.save();
            canvas.clipRect(Math.max(0, boundary), 0, w, h);
            drawPage(canvas, toPage, w, h);
            canvas.restoreToCount(save);

            // 用窄灰阶折痕模拟纸页翻动边缘，便于观察边界残影。
            if (boundary >= 0 && boundary < w) {
                int band = 5 * density;
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(
                    boundary - band, 0, boundary + band, 0,
                    new int[]{Color.WHITE, Color.LTGRAY, Color.DKGRAY, Color.WHITE},
                    null, Shader.TileMode.CLAMP));
                canvas.drawRect(boundary - band, 0, boundary + band, h, paint);
                paint.setShader(null);
            }
        }

        private void drawPage(Canvas canvas, int number, int w, int h) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, w, h, paint);

            int margin = 22 * density;
            paint.setColor(Color.BLACK);
            paint.setTextSize(19 * density);
            canvas.drawText("K103 E-INK PAGE TEST", margin, 28 * density, paint);

            paint.setStrokeWidth(1 * density);
            canvas.drawLine(margin, 38 * density, w - margin, 38 * density, paint);

            paint.setTextSize(23 * density);
            canvas.drawText("Page " + number, margin, 72 * density, paint);

            paint.setTextSize(15 * density);
            String[] lines = {
                "The quick brown fox jumps over the lazy dog.",
                "0123456789  ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                "局部刷新测试：观察文字边缘和页面边界。",
                "连续翻页时，注意上一页内容是否残留。",
                "A2 快速刷新 / Part 局部 / Regal 清理",
                "这是用于观察拖影、闪烁和刷新延迟的页面。",
                "每一页使用不同编号和文字排列。",
                "请特别观察移动折痕附近的灰阶残影。"
            };
            int y = 105 * density;
            int lineHeight = 25 * density;
            for (int i = 0; i < lines.length; i++) {
                String text = lines[(i + number - 1) % lines.length];
                canvas.drawText(text, margin, y + i * lineHeight, paint);
            }

            // 高对比度黑白条纹，用于观察残影，不铺满全屏。
            int barTop = Math.min(h - 70 * density, y + lines.length * lineHeight + 12 * density);
            int barHeight = 22 * density;
            int barWidth = Math.max(1, (w - 2 * margin) / 12);
            for (int i = 0; i < 12; i++) {
                paint.setColor((i + number) % 2 == 0 ? Color.BLACK : Color.WHITE);
                canvas.drawRect(margin + i * barWidth, barTop,
                    margin + (i + 1) * barWidth, barTop + barHeight, paint);
            }

            paint.setColor(Color.BLACK);
            paint.setTextSize(13 * density);
            canvas.drawText("PAGE " + number + " / K103 TEST", margin,
                h - 18 * density, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2 * density);
            canvas.drawRect(1, 1, w - 2, h - 2, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        void startTurn(long duration, Runnable onEnd) {
            if (animating || getWidth() <= 0) return;
            fromPage = pageNumber;
            toPage = pageNumber + 1;
            boundary = getWidth();
            previousBoundary = boundary;
            animating = true;
            endAction = onEnd;

            animator = ValueAnimator.ofInt(getWidth(), 0);
            animator.setDuration(duration);
            animator.addUpdateListener(valueAnimator -> {
                int next = (Integer) valueAnimator.getAnimatedValue();
                previousBoundary = boundary;
                boundary = next;

                // 只请求重绘移动折痕附近的窄条区域，而不是每帧 invalidate 整个 View。
                int band = 9 * density;
                int left = Math.max(0, Math.min(previousBoundary, boundary) - band);
                int right = Math.min(getWidth(), Math.max(previousBoundary, boundary) + band);
                dirtyRect.set(left, 0, Math.max(left + 1, right), getHeight());
                invalidate(dirtyRect);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }

                @Override public void onAnimationEnd(Animator animation) {
                    animator = null;
                    if (cancelled) {
                        animating = false;
                        return;
                    }
                    pageNumber = toPage;
                    animating = false;
                    // 最后一帧 boundary=0 已经显示完整新页，不额外整屏 invalidate。
                    Runnable action = endAction;
                    endAction = null;
                    if (action != null) action.run();
                }
            });
            animator.start();
        }

        void cancelTurn() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            animating = false;
        }

        boolean isAnimating() {
            return animating;
        }

        int getPageNumber() {
            return pageNumber;
        }

        void flip() {
            pageNumber++;
            animating = false;
        }

        Rect getTestRect() {
            if (testRect.isEmpty()) {
                testRect.set(getWidth() / 4, getHeight() / 3,
                    3 * getWidth() / 4, 2 * getHeight() / 3);
            }
            return new Rect(testRect);
        }
    }
}
