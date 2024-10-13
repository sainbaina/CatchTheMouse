package com.example.nethology

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.slider.Slider

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.start_screen)
        val startButt = findViewById<Button>(R.id.startButt)
        val scoreButt = findViewById<Button>(R.id.scoreButt)
        val sizeSlider = findViewById<Slider>(R.id.sizeSlider)
        val speedSlider = findViewById<Slider>(R.id.speedSlider)
        val numSlider = findViewById<Slider>(R.id.numSlider)

        scoreButt.setOnClickListener() {
            val helper = DbHelper(this)
            val games = helper.getStats()
            var content = ""
            for ((i, game) in games.withIndex()) {
                content += "--${i+1}--\ntaps: ${game.taps}\nscore: ${game.score}\npercentage: ${game.score.toFloat()/game.taps*100}\nduration: ${game.duration}\n\n"
            }
            setContentView(R.layout.score_screen)
            val statsView = findViewById<TextView>(R.id.statsView)
            statsView.movementMethod = ScrollingMovementMethod()
            statsView.text = content

            val backButt = findViewById<Button>(R.id.backButton)
            backButt.setOnClickListener{
                val intent = Intent(this, StartActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        startButt.setOnClickListener() {
            speed = speedSlider.value
            size = sizeSlider.value
            num = numSlider.value.toInt()

            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("speed", speed)
            intent.putExtra("size", size)
            intent.putExtra("num", num)
            startActivity(intent)
            finish()
        }
    }
}