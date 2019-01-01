package luyao.box.adapter

import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import luyao.box.R
import luyao.box.bean.AppBean
import luyao.box.common.util.AppUtils
import luyao.box.ui.appManager.AppDetailActivity
import luyao.box.util.AppManager
import luyao.box.view.SectorProgressView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Created by luyao
 * on 2018/12/29 10:44
 */
class AppAdapter(layoutResId: Int = R.layout.item_app) : BaseQuickAdapter<AppBean, BaseViewHolder>(layoutResId) {

    override fun convert(helper: BaseViewHolder, item: AppBean) {
        helper.run {
            setImageDrawable(R.id.appIcon, item.icon)
            setText(R.id.appName, item.appName)

            getView<ImageButton>(R.id.appPop).setOnClickListener {
                showPopMenu(helper,helper.itemView.context, it, item)
            }
        }
    }

    private fun showPopMenu(helper: BaseViewHolder,context: Context, view: View, appBean: AppBean) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.menu_app, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_open_app -> AppUtils.openApp(context, appBean.packageName)
                R.id.menu_uninstall_app -> AppUtils.uninstallApp(context, appBean.packageName)
                R.id.menu_app_properties -> AppUtils.openAppProperties(context, appBean.packageName)
                R.id.menu_app_detail -> context.startActivity(Intent(context, AppDetailActivity::class.java))
                R.id.menu_save_apk -> saveApk(helper,context, appBean)
                R.id.menu_share_apk -> shareApk()
                R.id.menu_open_in_store -> AppUtils.openInStore(context, appBean.packageName)
            }
            true
        }
        popupMenu.show()
    }

    private fun saveApk(helper: BaseViewHolder,context: Context, appBean: AppBean) {
        GlobalScope.launch(Dispatchers.Main) {
            val progressView = helper.getView<SectorProgressView>(R.id.appProgressView)
            val applicationInfo = context.packageManager.getApplicationInfo(appBean.packageName,0)
            val apkFile=File(applicationInfo.sourceDir)
            val destFile=File("${Environment.getExternalStorageDirectory().path}/${apkFile.name}")
            if (!destFile.exists()) destFile.createNewFile()
            Log.e("box",apkFile.path)
            progressView.visibility=View.VISIBLE
            val channel = Channel<Float>()
            launch(Dispatchers.IO) {
                val input=FileInputStream(apkFile)
                val output=FileOutputStream(destFile)
                val b = ByteArray(1024)
                var hasRead=0f
                var count=0
                while ((input.read(b)) >0){
                   output.write(b)
                    hasRead+=1024f
                    count++
                    if (count%10==0)channel.send(hasRead/apkFile.length())
                }
                channel.send(-1f)
                channel.close()
            }
            for (x in channel) {
                Log.e("box","$x")

                progressView.percent=x*100
                if (x==-1f) progressView.visibility=View.GONE
            }
        }
    }

    private fun shareApk() {

    }

}
