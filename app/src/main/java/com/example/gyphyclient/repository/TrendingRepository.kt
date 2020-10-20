package com.example.gyphyclient.repository

import KEY
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import com.example.gyphyclient.GiphyApplication
import com.example.gyphyclient.data.database.*
import com.example.gyphyclient.data.network.GiphyApi
import com.example.gyphyclient.di.DaggerAppComponent
import com.example.gyphyclient.internal.LIMIT
import com.example.gyphyclient.internal.SEARCH_LIMIT
import com.example.gyphyclient.model.Data
import com.example.gyphyclient.model.Result
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class TrendingRepository {

    var offset = 0

    @Inject
    lateinit var giphyApiService: GiphyApi

    private val downloadsDisposable by lazy { CompositeDisposable() }

    init {
        DaggerAppComponent.create().inject(this)
    }

    fun insertData(offset: Int = 0): Disposable {
        var rating = ""
        val preferences = PreferenceManager.getDefaultSharedPreferences(GiphyApplication.getAppContext())
        preferences.apply {
            rating = getString("RATING", "").toString()
        }

        return giphyApiService.getTrending(KEY, LIMIT, rating, offset.toString())
            .subscribeOn(Schedulers.io())
            .subscribeWith(subscribeToDatabase())
    }

    fun insertFavoriteData(gif: Data) {
        Thread {
            GiphyApplication.database.dataDao().insertFavoriteData(gif.toDataFavoriteEntity())
        }.start()
    }

    private fun searchGif(searchTerm: String): Single<Result> {
        var rating = ""
        val preferences = PreferenceManager.getDefaultSharedPreferences(GiphyApplication.getAppContext())
        preferences.apply {
            rating = getString("RATING", "").toString()
        }
        return giphyApiService.getSeach(KEY, SEARCH_LIMIT, rating, searchTerm)
    }

    fun gifShare(data: Data, context: Context) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, data.images.original?.webp)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Поделиться гифкой")
        context.startActivity(shareIntent)
    }

    fun gifSave(data: Data, context: Context) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uriGif: Uri = Uri.parse(data.images.original?.webp.toString())
        val aExtDcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val request = DownloadManager
            .Request(uriGif)
            .setTitle(data.title.substringBefore("GIF"))
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.parse("file://" + aExtDcimDir.path + "/${data.title}.gif"))
       /* val dialog = ProgressDialog(context)
        dialog.setMessage("Идет сохранение гифки, пожалуйста, подождите...")
        dialog.setCancelable(false)
        dialog.max = 100
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.show()*/
        val downloadId = downloadManager.enqueue(request)

        val progressFlow = Flowable
            .interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val query = DownloadManager.Query()
                query.setFilterById(downloadId)

                val c: Cursor = downloadManager.query(query)
                c.use { c ->
                    if (c.moveToFirst()) {
                        val sizeIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val downloadedIndex =
                            c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val size = c.getInt(sizeIndex).toLong()
                        val downloaded = c.getInt(downloadedIndex).toLong()
                        var progress = 0.0
                        if (size != -1L)
                            progress = downloaded * 100.0 / size
                        //dialog.progress = progress.toInt()

                        if (progress == 100.0) {
                         //   dialog.cancel()
                            downloadsDisposable.clear()
                        }
                    }
                }
            }
        downloadsDisposable.add(progressFlow)
    }

    private fun subscribeToDatabase(): DisposableSubscriber<Result> {
        return object : DisposableSubscriber<Result>() {
            override fun onNext(result: Result?) {
                if (result != null) {
                    val entityList = result.data.toList().toDataEntityList()
                    GiphyApplication.database.apply {
                        dataDao().insertData(entityList)
                    }
                }
            }

            override fun onError(t: Throwable?) {
                Log.e("insertData()", "TrendingResult error: ${t?.message}")
            }

            override fun onComplete() {
                getTrendingQuery()
            }
        }
    }

    fun getFavoriteQuery(): Flowable<List<DataFavoriteEntity>> {
        return GiphyApplication.database.dataDao()
            .queryFavoriteData()
    }

    fun getTrendingQuery(): Flowable<List<DataEntity>> {
        return GiphyApplication.database.dataDao()
            .queryData()
    }

    fun querySearchData(searchTerm: String): Single<List<Data>> {
        return searchGif(searchTerm).map {
            it.data
        }
    }
}