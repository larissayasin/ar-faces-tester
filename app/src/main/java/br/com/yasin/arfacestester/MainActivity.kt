package br.com.yasin.arfacestester

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        test_it.setOnClickListener {
            if (url_text.text.toString().isEmpty()) {
                til_url.error = "URL missing"
            } else {
                til_url.error = null
                val intent = Intent(this, AugmentedFacesActivity::class.java)
                intent.putExtra("url", url_text.text.toString())
                intent.putExtra("isGltf", source_type.isChecked)
                startActivity(intent)
            }
        }
    }
}