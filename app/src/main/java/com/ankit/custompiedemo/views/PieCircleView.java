package com.ankit.custompiedemo.views;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.ankit.custompiedemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ankit on 05-04-2016.
 */
public class PieCircleView extends ImageView {
    private static final boolean PRINT_LOG = true;
    private static final String LOG_TAG = "PieCircleView";

    private static final int CLICK_ACTION_THRESHOLD = 25;
    private Context mContext;
    private int lineColor;
    private int mAnimationSpeed;
    private int arcValue;
    private int percentage;

    /*
    *Different paint objects used
     */
    private Paint paintArc;
    private RectF ovalArc;
    private Paint paintLine;

    /*
    *touch down position holder for (possible) click
     */
    private float clickStartX;
    private float clickStartY;

    /**
     * Radius of the doughnut.
     */
    private int doughnutRadius;

    /**
     * Radius of the drawn image on parent ImageView
     */
    private int sourceImageRadious;

    /**
     * Actually width (in dp) over which item arcs will be drawn
     */
    private int doughnutStrokeWidthPx;

    /**
     * Each drawn arc's start and stop angles references
     */
    private ArrayList<ArcCoordinate> arcCoordinates;

    /*
     * Helper variables
     ***********************************
     */

    private int halfDoughnutStrokeWidthPx;

    /*
    *center of the view
     */
    private int centerX;
    private int centerY;

    private OnItemCliCkListener itemCliCkListener;


    /**
     * Arc view square sides
     */
    private int squareSize;


    //New Parameters
    int[] section_colors;
    int[] section_drawables;
    int section_count;
    int pie_width;
    private DisplayMetrics metrics;


    /**
     * Item Click event listener
     */

    public interface OnItemCliCkListener {
        /**
         * @param pos clockwise pos starting from top
         */
        void onItemClick(int pos);

        void onSourceClick();
    }

    public PieCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);

    }

    public PieCircleView(Context context, AttributeSet attrs,
                         int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(attrs);
    }

    public PieCircleView(Context context) {
        super(context);
        mContext = context;
        init(null);

    }

    private void init(AttributeSet attrs) {
        metrics = new DisplayMetrics();
        readXmlTags(attrs);

        arcCoordinates = new ArrayList<>();
        doughnutStrokeWidthPx = (int) getPixelFromDp(pie_width);
        halfDoughnutStrokeWidthPx = doughnutStrokeWidthPx / 2;

        paintArc = getArcPaint();
        paintLine = getDividerPaint();

        ovalArc = new RectF();

        if (isInEditMode()) {
            setDummyData();
        }

    }

    private void readXmlTags(AttributeSet attrs) {
        if (attrs == null) return;
        final TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.PieCircleView);
        section_count = array.getInteger(R.styleable.PieCircleView_section_count, 0);
        pie_width = array.getInteger(R.styleable.PieCircleView_pie_width, 0);

        final int section_color_ids = array.getResourceId(R.styleable.PieCircleView_section_colors, 0);
        final int section_color_drawables = array.getResourceId(R.styleable.PieCircleView_section_drawables, 0);

        if (section_color_ids != 0) {
            section_colors = getResources().getIntArray(section_color_ids);
        }

        if (section_color_ids != 0) {
            section_drawables = getResources().getIntArray(section_color_drawables);
        }

        array.recycle();
    }


    public void setOnItemClickListeners(OnItemCliCkListener itemClickListeners) {
        this.itemCliCkListener = itemClickListeners;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        /*
                 ____________
                |            |
                |------------|
                |  |      |  |
                |  |      |  |
                |------------|
                |            |
                |            |
                |            |



            ____________________
           |                   |
           |   |           |   |
           |   |           |   |
           |   |           |   |
           |                   |


                Ideally we would want something like this for both orientation. our pie would just
                fit inside the square


         */


        int desiredWidth = Integer.MAX_VALUE;
        int desiredHeight = Integer.MAX_VALUE;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        printLog(pie_width + " Width " + width + "  Height " + height);

        centerX = width / 2;
        centerY = height / 2;

        if (width > height) {
            //Landscape mode
            width = height;
            squareSize = height;

        } else {
            //portrait
            height = width / 2;
            squareSize = height;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);


        doughnutRadius = squareSize / 2 - doughnutStrokeWidthPx;


        int w = getWidth() / 2 - squareSize / 2;
        int t = getHeight() / 2 - squareSize / 2;
        int r = w + squareSize;
        int bo = t + squareSize;

        ovalArc.set(w + doughnutStrokeWidthPx, t + doughnutStrokeWidthPx, r - doughnutStrokeWidthPx, bo - doughnutStrokeWidthPx);



    }

    /*@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get image matrix values and place them in an array
        float[] f = new float[9];
        getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        //final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = getDrawable();
        final int origW = d.getIntrinsicWidth();
        //final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int actW = Math.round(origW * scaleX);
        //final int actH = Math.round(origH * scaleY);

        //as the source image we set is square we only require one dimension
        sourceImageRadious = actW / 2;

        //Log.e("DBG", "["+origW+","+origH+"] -> ["+actW+","+actH+"] & scales: x="+scaleX+" y="+scaleY);
    }*/

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clickStartX = event.getX();
                clickStartY = event.getY();
                break;
            case MotionEvent.ACTION_UP: {
                float endX = event.getX();
                float endY = event.getY();
                if (isAClick(clickStartX, endX, clickStartY, endY)) {

                    double angle = getPointAngleRespectToCenterPoint(event.getX(), event.getY());
                    double radius = getPointRadiusFromCenterPoint(event.getX(), event.getY());

                    int itemClickPos = getPositionOfItem(radius, angle);

                    if (itemCliCkListener != null) {
                        if (itemClickPos >= 0) {
                            itemCliCkListener.onItemClick(itemClickPos);

                            //Source item clicked
                        } else if (itemClickPos == -1) {
                            itemCliCkListener.onSourceClick();

                        }
                    }
/**/
                    Log.d("X", "Angle " + angle + "   Radius " + radius + " pos " + itemClickPos);
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (section_count == 0) {
            super.onDraw(canvas);
            return;
            //No Items to inflate
        }

        paintLine.setColor(lineColor);

        ArcCoordinate arcCoordinate;
        int arcSweepAngle = 360 / section_count;

        for (int i = 0; i < section_count; i++) {
            paintArc.setColor(ContextCompat.getColor(mContext, section_colors[i]));

            arcCoordinate = arcCoordinates.get(i);

            /*
            * draw arc with background and divider
            *********
             */
            canvas.drawArc(ovalArc, arcCoordinate.startAngle,
                    arcSweepAngle, false, paintArc);

            int countDegree = arcCoordinate.startAngle;

            float line1InX = getCirclePerimeterPointX(
                    centerX,
                    doughnutRadius + halfDoughnutStrokeWidthPx,
                    countDegree
            );

            float line1InY = getCirclePerimeterPointY(
                    centerY,
                    doughnutRadius + halfDoughnutStrokeWidthPx,
                    countDegree
            );

            float line1OutX = getCirclePerimeterPointX(
                    centerX,
                    doughnutRadius - halfDoughnutStrokeWidthPx,
                    countDegree
            );

            float line1OutY = getCirclePerimeterPointY(
                    centerY,
                    doughnutRadius - halfDoughnutStrokeWidthPx,
                    countDegree
            );

            canvas.drawLine(line1InX, line1InY, line1OutX, line1OutY, paintLine);

            /*
            *Draw thumb in center of the arc
            * ****************
             */
            int countDegree2 = arcCoordinate.startAngle + (360 / (section_count * 2));

            float x = getCirclePerimeterPointX(
                    centerX,
                    doughnutRadius,
                    countDegree2
            );

            float y = getCirclePerimeterPointY(
                    centerY,
                    doughnutRadius,
                    countDegree2
            );

            //Bitmap b = getRespectiveBitmap(i);
            //FIXME This need to be done only once when user sets item.
            Bitmap b = BitmapFactory.decodeResource(getResources(), section_drawables[i]);

            canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintLine);

        }
        super.onDraw(canvas);
    }

    public void setData(int section_count, int[] section_colors, int[] section_drawables) {

        for (int i = 0; i < section_count; i++) {

            int arcSweepAngle = 360 / section_count;
            int arcStartAngle = (arcSweepAngle * i) + 270 - (arcSweepAngle / 2);

            //saving Coordinates of all items
            ArcCoordinate arcCoordinate = createArcCoordinate(arcStartAngle, arcSweepAngle);
            arcCoordinates.add(arcCoordinate);
        }
    }

    /*
    *Coordinate stuff
    ******************************
    */
    private class ArcCoordinate {
        int startAngle;
        int stopAngle;
    }

    /**
     * This will create arc coordinate object and convert given angle which is in 0 to 2pi to angle which will be in -pi to pi
     *
     * @return arc coordinate
     */
    private ArcCoordinate createArcCoordinate(int startAngle, int sweepAngle) {
        ArcCoordinate arcCoordinate = new ArcCoordinate();

        int moduleAngle = startAngle % 360;
        int x = moduleAngle - 180;
        arcCoordinate.startAngle = moduleAngle;
        arcCoordinate.stopAngle = moduleAngle + sweepAngle;

        //angle is < pi
//        if (x < 0) {
//            arcCoordinate.startAngle = moduleAngle;
//            arcCoordinate.stopAngle = moduleAngle + sweepAngle;
//        } else {
//            startAngle = -180 + x;
//            arcCoordinate.startAngle = startAngle;
//            arcCoordinate.stopAngle = startAngle + sweepAngle;
//        }

        return arcCoordinate;
    }

    /*
    *
    * ****************************************************************************
     */

    /**
     * @return paint to draw between the dividers.
     */
    private Paint getDividerPaint() {
        Paint mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(getPixelFromDp(1));
        mPaint.setColor(lineColor);
        mPaint.setAntiAlias(true);
        return mPaint;
    }

    /**
     * @return paint to draw the arc. Color is set dynamically
     */
    private Paint getArcPaint() {
        Paint mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(doughnutStrokeWidthPx);
        mPaint.setAntiAlias(true);
        return mPaint;
    }

    /*
    *
    *                                   Calculations
    * *****************************************************************************
     */

    private float getPixelFromDp(int dpValue) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, r.getDisplayMetrics());
    }

    private boolean isAClick(float startX, float endX, float startY, float endY) {
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        return !(differenceX > CLICK_ACTION_THRESHOLD || differenceY > CLICK_ACTION_THRESHOLD);
    }

    /**
     * Applying formula px = cx * r * cos(angleInRadian)
     *
     * @param centerX       x coordinate or center
     * @param radius        circle radius
     * @param angleInDegree angle in degree
     * @return point on circle
     */
    private float getCirclePerimeterPointX(int centerX, int radius, int angleInDegree) {
        return (float) (centerX + radius * Math.cos(angleInDegree * Math.PI / 180));
    }

    /**
     * Applying formula px = cx * r * sin(angleInRadian)
     *
     * @param centerY       x coordinate or center
     * @param radius        circle radius
     * @param angleInDegree angle in degree
     * @return point on circle
     */
    private float getCirclePerimeterPointY(int centerY, int radius, int angleInDegree) {
        return (float) (centerY + radius * Math.sin(angleInDegree * Math.PI / 180));
    }

    /**
     * Applies AngleIsRad = arctan(x/y)
     *
     * @param px point x
     * @param py point y
     * @return Angle in degree
     */
    private double getPointAngleRespectToCenterPoint(float px, float py) {
        /*
        * arctan will return value 0 to 180 and from there instead of 181 to 360 it returns -180 to 0;
         */

        double arcAngle = 180 * Math.atan2(py - centerY, px - centerX) / Math.PI;

        if (arcAngle > 0 && arcAngle < 180) {
            return arcAngle;

        } else {
            return 180 + (180 + arcAngle);
        }
    }

    /**
     * Uses standard circle formula formula (X-a)2 + (Y-b)2 = r2 to get value of radius
     *
     * @param px point X
     * @param py point Y
     * @return radius
     */
    private double getPointRadiusFromCenterPoint(float px, float py) {
        float x_a = px - centerX;
        float x_b = py - centerY;

        return Math.sqrt(x_a * x_a + x_b * x_b);
    }

    /**
     * -1 indicates source image click. -2 represent no item click.
     *
     * @param pointRadius point rad from center of view
     * @param pointAngle  angle from center point
     * @return position of arc drawn earlier
     */
    private int getPositionOfItem(double pointRadius, double pointAngle) {
        if (pointRadius < sourceImageRadious) {
            return -1;
        }

        if (pointRadius < doughnutRadius - doughnutStrokeWidthPx / 2
                || pointRadius > doughnutRadius + doughnutStrokeWidthPx / 2) {
            return -2;
        }

        for (int i = 0; i < arcCoordinates.size(); i++) {
            if (pointAngle >= arcCoordinates.get(i).startAngle
                    && pointAngle <= arcCoordinates.get(i).stopAngle) {
                return i;
            }
        }

        return -1;
    }


    /*
     *  Dummy data
     *********************************
     */

    private void setDummyData() {
        pie_width = 20;
        int[] dummy_section_colors = new int[]{
                R.color.item_one_color,
                R.color.item_two_color,
                R.color.item_three_color,
                R.color.item_four_color,
                R.color.item_five_color,
                R.color.item_six_color,
                R.color.item_seven_color,
                R.color.item_eight_color,
                R.color.item_nine_color};

        int[] dummy_section_drawables = new int[]{
                R.drawable.space,
                R.drawable.space,
                R.drawable.space,
                R.drawable.space,
                R.drawable.space,
                R.drawable.space,
                R.drawable.space,
                R.drawable.space,
                R.drawable.space
        };
        int dummy_section_count = dummy_section_colors.length;

        setData(dummy_section_count, dummy_section_colors, dummy_section_drawables);

    }


    /*
     *
     * Logs
     **********************
     */

    private static void printLog(String msg) {
        if (PRINT_LOG) {
            Log.d(LOG_TAG, msg);
        }
    }
}
