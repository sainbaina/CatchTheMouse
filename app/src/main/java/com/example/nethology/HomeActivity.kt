package com.example.nethology

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.health.connect.datatypes.units.Percentage
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


data class Pos(var x: Float, var y: Float)

var WIDTH = 0
var HEIGHT = 0
var MOUSE_WIDTH = 0
var MOUSE_HEIGHT = 0

var speed = 0f
var size = 0f
var num = 0

var taps = 0
var score = 0
lateinit var tapView: TextView
lateinit var scoreView: TextView

data class Game(val taps: Int, val score: Int, val duration: String)

class Mouse() {
    private var _selfPos = Pos(0f, 0f)
    private var _posTo = Pos(0f, 0f)
    private val _speed = 0.5f * speed
    var isAlive = true
    var angle = 0f
    private var delta = 20f;

    fun setPosTo(pos: Pos) {
        _posTo.x = pos.x
        _posTo.y = pos.y
    }

    private fun distanceToTarget(): Float {
        val dx = _posTo.x - _selfPos.x
        val dy = _posTo.y - _selfPos.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateAngle(): Float {
        val dx = _posTo.x - _selfPos.x
        val dy = _posTo.y - _selfPos.y
        return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    }

    fun move(): Pos {
        val distance = distanceToTarget()

        if (distance > _speed) {
            val dx = (_posTo.x - _selfPos.x) / distance
            val dy = (_posTo.y - _selfPos.y) / distance

            _selfPos.x += dx * (_speed + Math.random().toFloat() * delta)
            _selfPos.y += dy * (_speed + Math.random().toFloat() * delta)

            angle = calculateAngle()

            checkBounds()
        } else {
            _selfPos.x = _posTo.x
            _selfPos.y = _posTo.y
        }

        return Pos(_selfPos.x, _selfPos.y)
    }

    fun step(): Pos {
        move()
        if (distanceToTarget() < 1) {
            setPosTo(
                Pos(
                    (Math.random() * (WIDTH - MOUSE_WIDTH)).toFloat(),
                    (Math.random() * (HEIGHT - MOUSE_HEIGHT)).toFloat()
                )
            )
        }
        return Pos(_selfPos.x, _selfPos.y)
    }

    private fun checkBounds() {
        _selfPos.x = _selfPos.x.coerceIn(0f, (WIDTH - MOUSE_WIDTH).toFloat())
        _selfPos.y = _selfPos.y.coerceIn(0f, (HEIGHT - MOUSE_HEIGHT).toFloat())
    }

    fun checkClick(clickX: Int, clickY: Int): Boolean {
        val translatedX = clickX - (_selfPos.x + MOUSE_WIDTH / 2 * 0.02 * size)
        val translatedY = clickY - (_selfPos.y + MOUSE_HEIGHT / 2 * 0.02 * size)

        val cosAngle = cos(-Math.toRadians(angle.toDouble())).toFloat()
        val sinAngle = sin(-Math.toRadians(angle.toDouble())).toFloat()
        val localX = translatedX * cosAngle - translatedY * sinAngle
        val localY = translatedX * sinAngle + translatedY * cosAngle

        val halfWidth = (MOUSE_WIDTH * 0.02 * size) / 2
        val halfHeight = (MOUSE_HEIGHT * 0.02 * size) / 2

        return localX >= -halfWidth && localX <= halfWidth &&
                localY >= -halfHeight && localY <= halfHeight
    }
}

class Animation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    val helper = DbHelper(this.context)
    val db = helper.writableDatabase
    val startTime = System.currentTimeMillis()
    private lateinit var mouseBitmap: Bitmap
    private val mice = mutableListOf<Mouse>()

    init {
        holder.addCallback(this)

        mouseBitmap = makeWhiteTransparent(
            createScaledBitmap(
                BitmapFactory.decodeResource(context.resources, R.drawable.mouse),

                (MOUSE_WIDTH * 0.02f * size).toInt(),
                (MOUSE_HEIGHT * 0.02f * size).toInt(),
                true
            )
        )

        for (i in 0 until num) {
            val mouse = Mouse()
            mouse.setPosTo(
                Pos(
                    (Math.random() * (WIDTH - MOUSE_WIDTH)).toFloat(),
                    (Math.random() * (HEIGHT - MOUSE_HEIGHT)).toFloat()
                )
            )
            mice.add(mouse)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread {
            while (mice.any { it.isAlive }) {
                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        canvas.drawColor(Color.WHITE)

                        for (mouse in mice) {
                            val pos = mouse.step()

                            canvas.save()

                            canvas.translate(pos.x + MOUSE_WIDTH / 2 * 0.02f * size, pos.y + MOUSE_HEIGHT / 2 * 0.02f * size)
                            canvas.rotate(mouse.angle)
                            canvas.drawBitmap(
                                mouseBitmap,
                                -MOUSE_WIDTH / 2 * 0.02f * size,
                                -MOUSE_HEIGHT / 2 * 0.02f * size,
                                null
                            )

                            val paint = Paint().apply {
                                color = Color.RED
                                style = Paint.Style.STROKE
                                strokeWidth = 3f
                            }

                            canvas.restore()
                        }
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }

                Thread.sleep(16)
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mouseBitmap.recycle()
        val gameDuration = ((System.currentTimeMillis() - startTime) / 1000).toString()
        helper.addStat(gameDuration)
    }

//    @SuppressLint("SetTextI18n")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (i in mice) {
                    if(i.checkClick(x, y)){
                        score++
                        scoreView.text = "Score : $score"
                    }
                }
                taps++
                tapView.text = "Taps : $taps"
                return true
            }
        }
        return false
    }
}

fun makeWhiteTransparent(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = bitmap.getPixel(x, y)
            if (pixel == Color.WHITE) {
                result.setPixel(x, y, Color.TRANSPARENT)
            } else {
                result.setPixel(x, y, pixel)
            }
        }
    }

    return result
}


class DbHelper(context: Context) : SQLiteOpenHelper(context, "game_statistics.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE game_statistics (id INTEGER PRIMARY KEY AUTOINCREMENT,  taps INTEGER, score INTEGER, game_duration TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS game_statistics")
        onCreate(db)
    }

    fun addStat(duration: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("taps", taps)
            put("score", score)
            put("game_duration", duration)
        }
        db.insert("game_statistics", null, values)
    }

    fun getStats(): List<Game> {
        val games = mutableListOf<Game>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM game_statistics ORDER BY id DESC LIMIT 10", null)
        with(cursor) {
            while (moveToNext()) {
                val taps = getInt(getColumnIndexOrThrow("taps"))
                val score = getInt(getColumnIndexOrThrow("score"))
                val duration = getString(getColumnIndexOrThrow("game_duration"))
                games.add(Game(taps, score, duration))
            }
        }
        cursor.close()
        return games
    }
}

class HomeActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        speed = intent.getFloatExtra("speed", 1f)
        size = intent.getFloatExtra("size", 1f)
        num = intent.getIntExtra("num", 1)

        taps = 0
        score = 0

        val displayMetrics = this.resources.displayMetrics
        WIDTH = displayMetrics.widthPixels
        HEIGHT = displayMetrics.heightPixels
        MOUSE_WIDTH = 300
        MOUSE_HEIGHT = 150

        setContentView(R.layout.home_activity)
        tapView = findViewById(R.id.tapView)
        scoreView = findViewById(R.id.scoreView)

        val toStartButt = findViewById<Button>(R.id.toStartButton)
        toStartButt.setOnClickListener(){
            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}