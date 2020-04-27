package uos.scg

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class ListViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)

        var mListView = findViewById<ListView>(R.id.userlist)

        /* ListView의 item을 저장하는 배열 */
        val item_list = ArrayList<String>()
        item_list.add("도서관")
        item_list.add("카페")
        item_list.add("편의점")
        item_list.add("프린터")
        item_list.add("흡연 구역")
        item_list.add("주차 가능 구역")
        item_list.add("건물 위치")

        /* List view와 array 사이 처리를 위한 ArrayAdapter */
        val arrayAdapter: ArrayAdapter<*>

        arrayAdapter = ArrayAdapter(this,
            android.R.layout.simple_list_item_1, item_list)

        mListView.adapter = arrayAdapter

        /* ListVIew의 item을 클릭했을때 반응하는 EventListener */
        mListView.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(this, MainActivity::class.java)
            setResult(position)
            finish()
        }

    }
} 