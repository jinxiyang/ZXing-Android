package com.yang.zxing.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.yang.zxing.R;
import com.yang.zxing.camera.CameraManager;
import com.yang.zxing.util.UnitUtils;

import java.util.Collection;
import java.util.HashSet;


/**
 *此视图覆盖在摄像机预览的顶部。它增加了取景器。
 *矩形和部分透明度以外，以及激光扫描仪
 *动画和结果点。
 */
public final class ViewfinderView extends View {

	/**
	 * 刷新界面的时间
	 */
	private static final long ANIMATION_DELAY = 10L;
	private static final int OPAQUE = 0xFF;

	/**
	 * 四个绿色边角对应的长度
	 */
	private int ScreenRate;

	/**
	 * 四个绿色边角对应的宽度
	 */
	private static final int CORNER_WIDTH = 10;
	/**
	 * 扫描框中的中间线的宽度
	 */
	private static final int MIDDLE_LINE_WIDTH = 6;


	/**
	 * 中间那条线每次刷新移动的距离
	 */
	private static final int SPEEN_DISTANCE = 5;

	/**
	 * 手机的屏幕密度
	 */
	private static float density;
	/**
	 * 字体距离扫描框下面的距离
	 */
	private static final int TEXT_PADDING_TOP = 30;

	/**
	 * 画笔对象的引用
	 */
	private Paint paint;

	/**
	 * 中间滑动线的最顶端位置
	 */
	private int slideTop;

	private final int maskColor;

	private final int resultPointColor;
	private Collection<ResultPoint> possibleResultPoints;
	private Collection<ResultPoint> lastPossibleResultPoints;
	boolean isFirst;

	private int scanningCodeStrip= R.drawable.zxing_scanning_code_strip;
	private int hornColor=Color.parseColor("#FF774F");
	private int edgeColor=Color.WHITE;
	private String text="";
	private float textSize= UnitUtils.sp2px(12);

	private Rect frame;//扫码方框

	public ViewfinderView(Context context){
		this(context,null);
	}

	public ViewfinderView(Context context, AttributeSet attrs) {
		this(context,attrs,0);
	}


	public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ViewfinderView, defStyleAttr, 0);
		int n = a.getIndexCount();
		for (int i = 0; i < n; i++) {
			int attr = a.getIndex(i);
			if (attr == R.styleable.ViewfinderView_vfvText) {
				text= a.getString(attr);
			}else if (attr == R.styleable.ViewfinderView_vfvTextSize) {
				textSize= UnitUtils.sp2px(a.getFloat(attr,12));
			}else if (attr == R.styleable.ViewfinderView_edgeColor) {
				edgeColor= a.getColor(attr,Color.WHITE);
			}else if (attr == R.styleable.ViewfinderView_hornColor) {
				hornColor= a.getColor(attr,Color.parseColor("#FF774F"));
			}else if (attr == R.styleable.ViewfinderView_scanningCodeStrip) {
				scanningCodeStrip= a.getResourceId(attr,R.drawable.zxing_scanning_code_strip);
			}
		}

		density = context.getResources().getDisplayMetrics().density;
		//将像素转换成dp
		ScreenRate = (int)(20 * density);

		paint = new Paint();
		maskColor = Color.parseColor("#60000000");
		resultPointColor = Color.parseColor("#c0ffff00");
		possibleResultPoints = new HashSet<ResultPoint>(5);
	}

	@Override
	public void onDraw(Canvas canvas) {
		//中间的扫描框，你要修改扫描框的大小，去CameraManager里面修改
		frame = CameraManager.get().getFramingRect((int) (canvas.getHeight()*0.35));
		if (frame == null) {
			postInvalidateDelayed(200);
			return;
		}

		//初始化中间线滑动的最上边和最下边
		if(!isFirst){
			isFirst = true;
			slideTop = frame.top;
		}

		//获取屏幕的宽和高
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		paint.setColor(maskColor);

		//画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		//扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		float shape_whidt= CORNER_WIDTH/3;
		paint.setColor(edgeColor);
		canvas.drawRect(frame.right-shape_whidt, frame.top ,
				frame.right, frame.bottom, paint);
		canvas.drawRect(frame.left, frame.top ,
				frame.left+shape_whidt, frame.bottom, paint);
		canvas.drawRect(frame.left, frame.top ,
				frame.right, frame.top+shape_whidt, paint);
		canvas.drawRect(frame.left, frame.bottom-shape_whidt ,
				frame.right, frame.bottom, paint);

		//画扫描框边上的角，总共8个部分
		paint.setColor(hornColor);
		canvas.drawRect(frame.left, frame.top, frame.left + ScreenRate,
				frame.top + CORNER_WIDTH, paint);
		canvas.drawRect(frame.left, frame.top, frame.left + CORNER_WIDTH, frame.top
				+ ScreenRate, paint);
		canvas.drawRect(frame.right - ScreenRate, frame.top, frame.right,
				frame.top + CORNER_WIDTH, paint);
		canvas.drawRect(frame.right - CORNER_WIDTH, frame.top, frame.right, frame.top
				+ ScreenRate, paint);
		canvas.drawRect(frame.left, frame.bottom - CORNER_WIDTH, frame.left
				+ ScreenRate, frame.bottom, paint);
		canvas.drawRect(frame.left, frame.bottom - ScreenRate,
				frame.left + CORNER_WIDTH, frame.bottom, paint);
		canvas.drawRect(frame.right - ScreenRate, frame.bottom - CORNER_WIDTH,
				frame.right, frame.bottom, paint);
		canvas.drawRect(frame.right - CORNER_WIDTH, frame.bottom - ScreenRate,
				frame.right, frame.bottom, paint);

		//绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
		slideTop += SPEEN_DISTANCE;
		if(slideTop >= frame.bottom){
			slideTop = frame.top;
		}

		//画扫描框下面的字
		paint.setColor(Color.parseColor("#FFFFFFFF"));
		paint.setTextSize(textSize);
		float x=(width -textSize*(text.length()-1))/2;
		canvas.drawText(text, x, (frame.bottom + (float)TEXT_PADDING_TOP *density), paint);

		Collection<ResultPoint> currentPossible = possibleResultPoints;
		Collection<ResultPoint> currentLast = lastPossibleResultPoints;
		if (currentPossible.isEmpty()) {
			lastPossibleResultPoints = null;
		} else {
			possibleResultPoints = new HashSet<ResultPoint>(5);
			lastPossibleResultPoints = currentPossible;
			paint.setAlpha(OPAQUE);
			paint.setColor(resultPointColor);
			for (ResultPoint point : currentPossible) {
				canvas.drawCircle(frame.left + point.getX(), frame.top
						+ point.getY(), 6.0f, paint);
			}
		}
		if (currentLast != null) {
			paint.setAlpha(OPAQUE / 2);
			paint.setColor(resultPointColor);
			for (ResultPoint point : currentLast) {
				canvas.drawCircle(frame.left + point.getX(), frame.top
						+ point.getY(), 3.0f, paint);
			}
		}
		if (scanningCodeStrip!=-1){
			Bitmap bitmap= BitmapFactory.decodeResource(getResources(), scanningCodeStrip);
			float left=(width-bitmap.getWidth())/2;
			canvas.drawBitmap(bitmap,left, slideTop - MIDDLE_LINE_WIDTH/2,paint);
		}
		//只刷新扫描框的内容，其他地方不刷新
		postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}

	public Rect getFrame() {
		return frame;
	}


}
