package com.example.livetvapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class SplashStarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] x, y, r, alpha, speed;
    private final int COUNT = 60;
    private final Random rnd = new Random();

    public SplashStarView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        paint.setColor(0xFFFFFFFF);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        x = new float[COUNT]; y = new float[COUNT];
        r = new float[COUNT]; alpha = new float[COUNT]; speed = new float[COUNT];
        for (int i = 0; i < COUNT; i++) {
            x[i] = rnd.nextFloat() * w;
            y[i] = rnd.nextFloat() * h;
            r[i] = rnd.nextFloat() * 2f + 0.5f;
            alpha[i] = rnd.nextFloat();
            speed[i] = rnd.nextFloat() * 0.02f + 0.005f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < COUNT; i++) {
            alpha[i] += speed[i];
            if (alpha[i] > 1f || alpha[i] < 0f) speed[i] = -speed[i];
            paint.setAlpha((int)(Math.abs(alpha[i]) * 255));
            canvas.drawCircle(x[i], y[i], r[i], paint);
        }
        postInvalidateDelayed(16);
    }
}