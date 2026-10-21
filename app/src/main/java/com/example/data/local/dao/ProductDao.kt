package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isOnlineShopVisible = 1 ORDER BY name ASC")
    fun getOnlineProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockAlert ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("""
        SELECT * FROM products 
        WHERE name LIKE '%' || :query || '%' 
           OR barcode LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :quantityChange WHERE id = :productId")
    suspend fun adjustStock(productId: Long, quantityChange: Double)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
