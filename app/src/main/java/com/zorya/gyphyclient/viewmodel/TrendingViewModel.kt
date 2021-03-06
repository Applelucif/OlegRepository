package com.zorya.gyphyclient.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zorya.gyphyclient.data.database.toDataList
import com.zorya.gyphyclient.di.DaggerAppComponent
import com.zorya.gyphyclient.model.Data
import com.zorya.gyphyclient.repository.TrendingRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class TrendingViewModel : ViewModel() {

    @Inject
    lateinit var repository: TrendingRepository

    private val compositeDisposable by lazy { CompositeDisposable() }

    private val _data by lazy { MutableLiveData<List<Data>>() }
    val data: MutableLiveData<List<Data>>
        get() = _data

    val _isInProgress by lazy { MutableLiveData<Boolean>() }
    val isInProgress: MutableLiveData<Boolean>
        get() = _isInProgress

    val _isError by lazy { MutableLiveData<Boolean>() }
    val isError: MutableLiveData<Boolean>
        get() = _isError

    init {
        DaggerAppComponent.create().inject(this)
        compositeDisposable.add(getListTrending())
    }

    fun getData(offset: Int) {
        repository.insertData(offset)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun addToFavorite(gif: Data) {
        Thread {
            repository.insertFavoriteData(gif)
        }.start()
    }

    private fun getListTrending(): Disposable {
        return repository.getTrendingQuery()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { dataEntityList ->
                    _isInProgress.postValue(true)
                    if (dataEntityList != null && dataEntityList.isNotEmpty()) {
                        _isError.postValue(false)
                        _data.postValue(dataEntityList.toDataList())
                    } else {
                        repository.insertData(repository.offset)
                    }
                    _isInProgress.postValue(false)
                },
                {
                    _isInProgress.postValue(true)
                    _isError.postValue(true)
                    _isInProgress.postValue(false)
                }
            )
    }
}
