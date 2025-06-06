package com.example.meetingassistant.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.meetingassistant.R;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DayView extends TextView {
    private Calendar date;
    private boolean hasMeeting;
    private boolean isSelected;
    private boolean isCurrentMonth;
    private boolean isToday;
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint todayPaint;
    private static final float TEXT_SIZE_DP = 14f;
    private static final float CIRCLE_PADDING_DP = 4f;
    private long meetingTimeInMillis;

    public DayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setGravity(Gravity.CENTER);
        setPadding(8, 8, 8, 8);
        setTextColor(ContextCompat.getColor(getContext(), R.color.white));

        float density = getResources().getDisplayMetrics().density;
        float textSizePx = TEXT_SIZE_DP * density;

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSizePx);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);

        todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        todayPaint.setStyle(Paint.Style.STROKE);
        todayPaint.setStrokeWidth(2 * density);
        todayPaint.setColor(ContextCompat.getColor(getContext(), R.color.current_day_border));
    }

    public void setDate(Calendar date) {
        this.date = (Calendar) date.clone();
        invalidate();
    }

    public void setHasMeeting(boolean hasMeeting) {
        this.hasMeeting = hasMeeting;
        if (hasMeeting && !isSelected) {
            setBackgroundResource(R.drawable.meeting_day_background);
        }
        invalidate();
    }

    public void setMeetingTime(long timeInMillis) {
        this.meetingTimeInMillis = timeInMillis;
        invalidate();
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
        if (selected) {
            setTextAppearance(getContext(), R.style.SelectedDayTheme);
        } else {
            setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        }
        invalidate();
    }

    public void setCurrentMonth(boolean currentMonth) {
        this.isCurrentMonth = currentMonth;
        invalidate();
    }

    public void setIsToday(boolean isToday) {
        this.isToday = isToday;
        invalidate();
    }

    private int getMeetingColor() {
        if (!hasMeeting) return ContextCompat.getColor(getContext(), R.color.button_gray);

        Calendar now = Calendar.getInstance();
        long diffInMillis = meetingTimeInMillis - now.getTimeInMillis();

        if (diffInMillis < 0) {
            return ContextCompat.getColor(getContext(), R.color.meeting_past);
        } else if (diffInMillis < TimeUnit.HOURS.toMillis(24)) {
            return ContextCompat.getColor(getContext(), R.color.meeting_soon);
        } else {
            return ContextCompat.getColor(getContext(), R.color.meeting_future);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (date == null) return;

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        float density = getResources().getDisplayMetrics().density;
        float circlePadding = CIRCLE_PADDING_DP * density;

        // Draw background
        float radius = Math.min(width, height) / 2f - circlePadding;
        if (isSelected && isCurrentMonth) {
            backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.calendar_selected_day_bg));
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint);
        } else if (hasMeeting && isCurrentMonth) {
            backgroundPaint.setColor(getMeetingColor());
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint);
        }

        // Draw today indicator
        if (isToday) {
            canvas.drawCircle(centerX, centerY, radius, todayPaint);
        }

        // Set text color
        if (!isCurrentMonth) {
            textPaint.setAlpha(128); // 50% transparency
        } else {
            textPaint.setAlpha(255); // full opacity
        }

        // Draw day number
        String dayText = String.valueOf(date.get(Calendar.DAY_OF_MONTH));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textHeight = fm.bottom - fm.top;
        float textOffset = textHeight / 2 - fm.bottom;
        canvas.drawText(dayText, centerX, centerY + textOffset, textPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width); // Square view
    }

    public Calendar getDate() {
        return date;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setTextColor(ContextCompat.getColor(getContext(), 
            enabled ? R.color.white : R.color.white));
        setAlpha(enabled ? 1.0f : 0.3f);
    }
} 