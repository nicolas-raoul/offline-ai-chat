package com.github.nicolas_raoul.offline_ai_chat

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.launch

class AICoreSetupActivity : AppCompatActivity() {
    private var model: GenerativeModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aicore_setup)
        
        // Show app icon in title bar
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setLogo(R.mipmap.ic_launcher)
        supportActionBar?.setDisplayUseLogoEnabled(true)
        
        val statusText = findViewById<TextView>(R.id.status_text)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val instructionsText = findViewById<TextView>(R.id.instructions_text)
        val retryButton = findViewById<Button>(R.id.retry_button)
        
        instructionsText.movementMethod = LinkMovementMethod.getInstance()
        
        fun showSetupInstructions() {
            statusText.setText(R.string.aicore_setup_title)
            progressBar.visibility = View.GONE
            instructionsText.visibility = View.VISIBLE
            retryButton.visibility = View.VISIBLE
        }
        
        retryButton.setOnClickListener {
            statusText.setText(R.string.checking_aicore)
            progressBar.visibility = View.VISIBLE
            instructionsText.visibility = View.GONE
            retryButton.visibility = View.GONE
            
            lifecycleScope.launch {
                if (isAICoreUsable()) {
                    startActivity(Intent(this@AICoreSetupActivity, MainActivity::class.java))
                    finish()
                } else {
                    showSetupInstructions()
                }
            }
        }
        
        // Initial check
        lifecycleScope.launch {
            if (isAICoreUsable()) {
                startActivity(Intent(this@AICoreSetupActivity, MainActivity::class.java))
                finish()
            } else {
                showSetupInstructions()
            }
        }
    }

    private suspend fun isAICoreUsable(): Boolean {
        return try {
            model = GenerativeModel(
                generationConfig {
                    context = applicationContext
                    temperature = 0.2f
                    topK = 16
                    maxOutputTokens = 20
                }
            )
            
            var responseText = ""
            android.util.Log.d("AICoreSetup", "Starting generateContentStream")
            model!!.generateContentStream("Why is the sky blue?")
                .collect { response ->
                    responseText += response.text
                    android.util.Log.d("Offline AI chat AICoreSetup", "Received chunk: ${response.text}")
                }
            
            android.util.Log.d("Offline AI chat AICoreSetup", "Final response text: $responseText")
            responseText.contains("sky", ignoreCase = true)
        } catch (e: Exception) {
            false
        } finally {
            model?.close()
            model = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model?.close()
    }
}