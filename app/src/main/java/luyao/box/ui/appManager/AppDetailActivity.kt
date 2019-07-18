package luyao.box.ui.appManager

import kotlinx.android.synthetic.main.activity_app_detail.*
import kotlinx.android.synthetic.main.title_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import luyao.box.*
import luyao.box.util.AppUtils
import luyao.box.ui.editor.TextViewerActivity
import luyao.parser.xml.XmlParser
import luyao.util.ktx.base.BaseActivity
import luyao.util.ktx.ext.Hash
import luyao.util.ktx.ext.hash
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by luyao
 * on 2018/12/29 16:35
 */
class AppDetailActivity : BaseActivity() {

    private val mPackageName by lazy { intent.getStringExtra("packageName") }
    private val appName by lazy { AppUtils.getAppName(this@AppDetailActivity, mPackageName) }
    private val filePath by lazy { "$APK_PATH$appName${File.separator}AndroidManifest.xml" }
    private val sourceDir by lazy { applicationContext.packageManager.getApplicationInfo(mPackageName, 0).sourceDir }

    override fun getLayoutResId() = R.layout.activity_app_detail

    override fun initView() {
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        mToolbar.setNavigationOnClickListener { onBackPressed() }
        mToolbar.title = appName
        detailIcon.setImageDrawable(AppUtils.getAppIcon(this, packageManager.getPackageInfo(mPackageName, 0)))
        detailRefresh.isRefreshing = true
        initListener()
    }

    override fun initData() {
        refresh()
    }

    private fun initListener() {
        detailRefresh.setOnRefreshListener { refresh() }
        detailManifest.setOnClickListener {
            startActivity(TextViewerActivity::class.java, "filePath", filePath)
        }
        li_sigMD5.setOnClickListener { copyToClipboard(sigMD5.text.toString()) }
        li_sigSHA1.setOnClickListener { copyToClipboard(sigSHA1.text.toString()) }
        li_sigSHA256.setOnClickListener { copyToClipboard(sig256.text.toString()) }
    }

    private fun refresh() {
        val sig = getAppSignature(this, mPackageName)
        sigMD5.text = sig.hash(Hash.MD5)
        sigSHA1.text = sig.hash(Hash.SHA1)
        sig256.text = sig.hash(Hash.SHA256)

        CoroutineScope(Dispatchers.Main).launch {
            val xmlAsync = async(Dispatchers.IO) {
                var xmlParser: XmlParser
                val zipFile = ZipFile(File(sourceDir))
                val zipEntry: ZipEntry? = zipFile.getEntry("AndroidManifest.xml")
                zipEntry?.run {
                    val inputStream = zipFile.getInputStream(zipEntry)
                    xmlParser = XmlParser(inputStream)
                    return@async xmlParser.parse()
                }
            }
            val xml = xmlAsync.await()
            launch(Dispatchers.IO) {
                val destFile = File(filePath)
                if (!destFile.parentFile.exists()) destFile.parentFile.mkdirs()
                if (!destFile.exists()) destFile.createNewFile()
                xml?.xmlContent?.let { destFile.writeText(it) }
            }

            detailRefresh.isRefreshing = false
            xml?.run {
                detailVersionName.text = versionName ?: "unknown"
                detailVersionCode.text = versionCode
                detailPackageMame.text = packageName
                detailTargetSdk.text = targetSdkVersion
                detailMinSdk.text = minSdkVersion
            }
        }
    }
}