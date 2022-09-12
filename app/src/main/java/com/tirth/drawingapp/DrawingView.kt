package com.tirth.drawingapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mCanvasPaint: Paint? = null
    private var mDrawPaint: Paint? = null
    private var mBrushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private var mPaths = ArrayList<CustomPath>()
    private var mUndoPaths = ArrayList<CustomPath>()
    private var redoPath: CustomPath?= null

    init{
        setUpDrawing()
    }

    fun onClickUndo(){
        if(mPaths.size > 0){
//            redoPath = mPaths[mPaths.size - 1]
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
//            invalidate() will internally call onDraw function
            invalidate()
        }
    }
    fun onClicKRedo(){
        if(mPaths.size > 0){
            mPaths.add(mUndoPaths[mUndoPaths.size - 1])
            mUndoPaths.removeAt(mUndoPaths.size - 1)
//            invalidate() will internally call onDraw function
            invalidate()
        }
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        for(path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        if(!(mDrawPath!!.isEmpty)){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

   override fun onTouchEvent(event: MotionEvent?): Boolean {
        var touchX = event?.x
        var touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                //pressed on screen
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{//When touch/finger removed from the screen
                if (touchX != null) {
                    if (touchY != null) {
                        mPaths.add(mDrawPath!!)
                        mDrawPath = CustomPath(color, mBrushSize)
                    }
                }
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setBrushSize(newBrushSize:Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newBrushSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setBrushColor(newBrushColor:String){
        color = Color.parseColor(newBrushColor)
        mDrawPaint!!.color = color
    }

//    fun setBrushColor(newBrushColor: Int){
//        color = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
//            newBrushColor.toFloat(), resources.displayMetrics).toInt()
//        mDrawPaint!!.color = color
//    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path(){

    }
}