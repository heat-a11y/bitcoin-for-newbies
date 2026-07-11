package com.bitcoinwidget

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var priceView: TextView
    private lateinit var blockView: TextView
    private lateinit var hashrateView: TextView
    private lateinit var lastUpdatedView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        priceView = findViewById(R.id.activity_price)
        blockView = findViewById(R.id.activity_block)
        hashrateView = findViewById(R.id.activity_hashrate)
        lastUpdatedView = findViewById(R.id.activity_last_updated)

        findViewById<Button>(R.id.btn_refresh).setOnClickListener { fetchData() }

        fetchData()
    }

    private fun fetchData() {
        Thread {
            val price = BitcoinApiClient.fetchPriceUsd()
            val block = BitcoinApiClient.fetchBlockHeight()
            val hashrate = BitcoinApiClient.fetchHashrateEh()

            runOnUiThread {
                priceView.text = BitcoinApiClient.formatPrice(price)
                blockView.text = BitcoinApiClient.formatBlockHeight(block)
                hashrateView.text = BitcoinApiClient.formatHashrate(hashrate)
                lastUpdatedView.text = "Last updated: ${
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                }"
            }
        }.start()
    }
}
