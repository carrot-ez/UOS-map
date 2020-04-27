package uos.scg

import android.os.Bundle
import android.os.PersistableBundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import kotlinx.android.synthetic.main.test_layout.*

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_layout)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val drawerView = findViewById<View>(R.id.drawer)
        val btnOpen = findViewById<Button>(R.id.btn_open)
        val btnClose = findViewById<Button>(R.id.btn_close)

        btnOpen.setOnClickListener {
            drawerLayout.openDrawer(drawerView)
        }

        btnClose.setOnClickListener {
            drawerLayout.closeDrawers()
        }
    }
}