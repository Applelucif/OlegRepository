package com.zorya.gyphyclient.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "data")
data class DataEntity(
    @ColumnInfo(name = "images")
    val images: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "username")
    val username: String,
    @PrimaryKey
    @ColumnInfo(name = "hash")
    val hash: String,
    @ColumnInfo(name = "smallImage")
    val smallImage: String,
    @ColumnInfo(name = "height")
    val height: String,
    @ColumnInfo(name = "width")
    val width: String,
    @ColumnInfo(name = "gif")
    val gif: String
)

@Entity(tableName = "favoriteData")
data class DataFavoriteEntity(
    @ColumnInfo(name = "images")
    val images: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "username")
    val username: String,
    @PrimaryKey
    @ColumnInfo(name = "hash")
    val hash: String,
    @ColumnInfo(name = "smallImage")
    val smallImage: String,
    @ColumnInfo(name = "height")
    val height: String,
    @ColumnInfo(name = "width")
    val width: String,
    @ColumnInfo(name = "gif")
    val gif: String
)