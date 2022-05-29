package id.maskology.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productRemoteKeys")
data class ProductRemoteKeys(
    @PrimaryKey
    val id: String,
    val prevKey: Int?,
    val nextKey: Int?
)
