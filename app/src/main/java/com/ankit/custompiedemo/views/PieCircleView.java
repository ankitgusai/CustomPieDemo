package com.ankit.custompiedemo.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.util.Pair;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.ankit.custompiedemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Code is fairy elaborate.
 * Created by Ankit on 05-04-2016.
 */
public class PieCircleView extends View {
    private static final boolean PRINT_LOG = true;
    private static final String LOG_TAG = "PieCircleView";

    private static final int CLICK_ACTION_THRESHOLD = 25;
    private int line_color;
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
     * Each drawn arc's start and stop angles references
     */
    private ArrayList<ArcCoordinate> arcCoordinates;

    /*
     * Helper variables
     ***********************************
     */

    //Only for layout editor
    int debug_section_count;
    private int pieRadius;
    private int pie_stroke_width_px;

    /*
    *center of the view
     */
    private int centerX;
    private int centerY;

    //New Parameters
    private List<Item> items;

    private OnItemCliCkListener itemCliCkListener;

    /**
     * Item Click event listener
     */

    public interface OnItemCliCkListener {
        /**
         * @param pos clockwise pos starting from top
         */
        void onItemClick(int pos);
    }


    public PieCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

    }

    public PieCircleView(Context context, AttributeSet attrs,
                         int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public PieCircleView(Context context) {
        super(context);
        init(null);

    }




    /*
    *
    *   Public interface
    * ****************************************
     */

    public void setOnItemClickListeners(OnItemCliCkListener itemClickListeners) {
        this.itemCliCkListener = itemClickListeners;
    }

    /**
     * set The data that inflates the view
     *
     * @param items list on the item to inflate
     */
    public void setData(List<Item> items) {
        this.items = items;

        //set the "debug_section_count"  to 8.
        //we will calculate arcs' start point and sweep angle.
        //what's that you say? i'll pretend you said no.

        // the View coordinates looks something like this(below). 0 is at right most X axis.
        // We want to start from the Top. well, you can start from anywhere.
        // But we don't want to start from 270 EXACTLY, for more natural look we want out first piece to be centered at 270


       /*   (270-(arc/2))     270        (x2, y2)
                       \       |         /
                               |        (x1, y1)
                               |
                               |
                               |
                               |
                               |
         180 ------------------|---------------------- 0
                               |
                               |
                               |
                               |
                               |
                               |
                               |
                               90


            From start to end we calculate start angle and swipe angle for all items.
            and we save them into array list.
            We will need this in UI drawing and click listener

            */


        for (int i = 0; i < items.size(); i++) {
            int arcSweepAngle = 360 / items.size();
            int arcStartAngle = (arcSweepAngle * i) + 270 - (arcSweepAngle / 2);

            //saving Coordinates of all items
            ArcCoordinate arcCoordinate = createArcCoordinate(arcStartAngle, arcSweepAngle);
            arcCoordinates.add(arcCoordinate);
        }
    }


    private void init(AttributeSet attrs) {
        //First we read the xml tags
        readXmlTags(attrs);

        //init all required paints
        paintArc = getArcPaint();
        paintLine = getDividerPaint();

        //init global vars
        arcCoordinates = new ArrayList<>();
        ovalArc = new RectF();

        //In android studio layout viewer we set dummy data base no XML tags. we also set background color to  get exact idea of view span
        if (isInEditMode()) {
            setDummyData();
            setBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(getContext(), android.R.color.black), 64));
        }
    }

    /**
     * Read pie thickness, divider color and section count for preview purpose
     */
    private void readXmlTags(AttributeSet attrs) {
        if (attrs == null) return;
        final TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.PieCircleView);
        debug_section_count = array.getInteger(R.styleable.PieCircleView_debug_section_count, 0);
        pie_stroke_width_px = array.getDimensionPixelSize(R.styleable.PieCircleView_pie_width, 0);
        line_color = array.getColor(R.styleable.PieCircleView_divider_color, ContextCompat.getColor(getContext(), android.R.color.black));

        array.recycle();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width;
        int height;

        //First thing first. we need to check how much space we can have. and base on that we will decide view size.
        Pair<Integer, Integer> pair = measureHeightWidth(widthMeasureSpec, heightMeasureSpec);
        width = pair.first;
        height = pair.second;

        printLog(" Width " + width + "  Height " + height);

        //MUST CALL THIS
        setMeasuredDimension(width, height);

        //since we have view dimension that we can work with, let create our variable out of it
        calculatePieVariables(width, height);

        //So we measured stuff, that's the first part.
    }

    //Simple Text book view measure stuff
    private Pair<Integer, Integer> measureHeightWidth(int widthMeasureSpec, int heightMeasureSpec) {
        //we wanna be as big as possible
        int desiredWidth = Integer.MAX_VALUE;
        int desiredHeight = Integer.MAX_VALUE;

        //what parents says,
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

        return new Pair<>(width, height);

    }

    private void calculatePieVariables(int width, int height) {
        //we calculate the view center
        centerX = width / 2;
        centerY = height / 2;

        //We know that we will be drawing our circle inside a square.
        int squareSize;


        //We select the biggest square that perfectly fits inside the view
        //Landscape mode
        if (width > height) {
            squareSize = height;

            //portrait
        } else {
            squareSize = width;
        }

        /*
                    if pie width is 1dp

                    -------------------



                    if we increase that to 15, then thickness increase like this

                    ******************
                    -------------------
                    ******************



                    if we mke it 30, then

                    ******************
                    ******************
                    -------------------
                    ******************
                    ******************


                    in short, thickness increases on both side of the line, hence we also have to account for that


         */


        //radius of pie
        pieRadius = squareSize / 2 - pie_stroke_width_px;
        //^pieRadius corresponds to R in below diagram

            /*      __________________________
                    |           |            |
                    |           |t           |
                    |       ____|____        |
                    |   w   |    R  |   r    |
                    |-------|   ----|--------|
                    |       |       |        |
                    |           |            |
                    |           |            |
                    |         bo|            |
                    |           |            |
                    |           |            |

             */


        int w = (width - squareSize) / 2;
        int t = (height - squareSize) / 2;
        int r = w + squareSize;
        int bo = t + squareSize;

        ovalArc.set(w + pie_stroke_width_px, t + pie_stroke_width_px, r - pie_stroke_width_px, bo - pie_stroke_width_px);

        //So now we have desired square, inside which we can draw stuff :). with view dimension kept in mind.

    }


    @Override
    protected void onDraw(Canvas canvas) {

        //If we don't have anything to display, we don't execute out logic
        if (items == null || items.isEmpty()) {
            super.onDraw(canvas);
            return;

        }

        //This. we should hve done it in init.
        paintLine.setColor(line_color);

        ArcCoordinate arcCoordinate;
        int halfPieStrokeWidthPx = pie_stroke_width_px / 2;
        int arcSweepAngle = 360 / items.size();

        for (int i = 0; i < items.size(); i++) {
            //for each arc we have seperate color to fill
            paintArc.setColor(ContextCompat.getColor(getContext(), items.get(i).color));

            //we get the coordinate for this particular item
            arcCoordinate = arcCoordinates.get(i);

            /*
            * draw arc with background and divider
            *********
             */
            canvas.drawArc(ovalArc, arcCoordinate.startAngle,
                    arcSweepAngle, false, paintArc);

            int countDegree = arcCoordinate.startAngle;

            /*

            Now check the graph from setData method, (x1, y1) and (x2, y2). that's what we gonna calculate now.
               why? because we need to draw divider line and to do that we need two points.
               there is some math involved here.

             */


            float innerX = getCirclePerimeterPointX(
                    centerX,
                    pieRadius + halfPieStrokeWidthPx,
                    countDegree
            );

            float innerY = getCirclePerimeterPointY(
                    centerY,
                    pieRadius + halfPieStrokeWidthPx,
                    countDegree
            );

            float outerX = getCirclePerimeterPointX(
                    centerX,
                    pieRadius - halfPieStrokeWidthPx,
                    countDegree
            );

            float outerY = getCirclePerimeterPointY(
                    centerY,
                    pieRadius - halfPieStrokeWidthPx,
                    countDegree
            );

            //We got those points, noe we draw line

            canvas.drawLine(innerX, innerY, outerX, outerY, paintLine);

            //That's done, arc background color set and divider line drawn. whats next? thumb in center.


            /*
            *Draw thumb in center of the arc
            * ****************
             */
            int countDegree2 = arcCoordinate.startAngle + (360 / (items.size() * 2));
            //                              whats this ^??

            /*
                imagine this is a first piece of pie. and we want the angle of  "*" from the center.

                        \               /
                         \      *      /
                          \           /



                As we got the angle we can get the exact coordinate (X,Y) using the formula mentioned little bit above.

                its basically does something like this,
                            "If you want coordinate(X,Y) of a point on the periphery of a circle,
                             then, give me radius of a circle and angle of that point from the center"


            */

            float x = getCirclePerimeterPointX(
                    centerX,
                    pieRadius,
                    countDegree2
            );

            float y = getCirclePerimeterPointY(
                    centerY,
                    pieRadius,
                    countDegree2
            );

            //We got the center point coordinate of arc where we want to draw our image.

            //we ge the bitmap,
            //FIXME This need to be done only once when user sets item.
            Bitmap b = BitmapFactory.decodeResource(getResources(), items.get(i).getIcon());

            //And draw it
            canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintLine);

        }
        super.onDraw(canvas);

        //That is the second part, Drawing.
        //Wanna click on each arc? see onTouchEvent
    }



    /*
    * Here we handle click event of all arcs.
    * What we have,
    *           -User touched coordinate.
    *
    * What we want,
    *           -which arc falls under that coordinate.
    *
    * How do we do that?
    *           -Math.
    *
    *
    * Well, apart from User touched coordinate, We also have the center of view.
    *
    *     A point(x,y) and a center. does that ring a bell?
    *           yup, we gonna do two things.
    *               1)Measure the angle of the point respect to center.
    *               2)Measure the distance between point and center.
    *
    *          voila, Now we know where it is and how far it is from the center. what we also know is, where and how far each arc is.
    *
    *          See the magic below.
    *
    * */

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
                    //               this^ get the angle we talked about

                    double radius = getPointRadiusFromCenterPoint(event.getX(), event.getY());
                    //               this^ gets the distance

                    int itemClickPos = getPositionOfItem(radius, angle);
                    //                 this is just simple comparison with all recorded coordinates

                    if (itemCliCkListener != null) {
                        if (itemClickPos >= 0) {
                            itemCliCkListener.onItemClick(itemClickPos);
                        }
                    }
/**/
                    printLog("Angle " + angle + "   Radius " + radius + " pos " + itemClickPos);
                }
                break;
            }
        }
        return true;
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
//        int x = moduleAngle - 180;
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
        mPaint.setColor(line_color);
        mPaint.setAntiAlias(true);
        return mPaint;
    }

    /**
     * @return paint to draw the arc. Color is set dynamically
     */
    private Paint getArcPaint() {
        Paint mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(pie_stroke_width_px);
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
     * -2 represent no item click.
     *
     * @param pointRadius point rad from center of view
     * @param pointAngle  angle from center point
     * @return position of arc drawn earlier
     */
    private int getPositionOfItem(double pointRadius, double pointAngle) {

        //if the distance(pointRadius) is too far or too close, that is if it isn't between thickness of arc(pie), we are NOT interested. period.
        if (pointRadius < pieRadius - pie_stroke_width_px / 2
                || pointRadius > pieRadius + pie_stroke_width_px / 2) {
            return -2;
        }

        //if it does, it must be between start angle and swipe angle of one of the arcs.
        for (int i = 0; i < arcCoordinates.size(); i++) {
            if (pointAngle >= arcCoordinates.get(i).startAngle
                    && pointAngle <= arcCoordinates.get(i).stopAngle) {
                return i;
            }
        }

        return -1;
    }


    public static class Item {
        int color;
        int icon;

        public Item(int color, int icon) {
            this.color = color;
            this.icon = icon;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public int getIcon() {
            return icon;
        }

        public void setIcon(int icon) {
            this.icon = icon;
        }
    }


    /*
     *  Dummy data
     *********************************
     */

    private void setDummyData() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < debug_section_count; i++) {
            items.add(new Item(R.color.colorAccent, R.drawable.space));
        }

        setData(items);
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
