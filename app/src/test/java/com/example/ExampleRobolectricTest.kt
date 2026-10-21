package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entities.ProductEntity
import com.example.domain.models.CartItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NKK ERP", appName)
  }

  @Test
  fun `cart item calculation accurate`() {
    val product = ProductEntity(
      id = 1,
      barcode = "12345",
      name = "Test Item",
      category = "Test",
      costPrice = 5.0,
      sellingPrice = 10.0,
      stockQuantity = 50.0,
      taxRatePercent = 5.0
    )
    val cartItem = CartItem(
      product = product,
      quantity = 2.0,
      discountPercent = 10.0
    )
    // subtotal = 10.0 * 2.0 * 0.90 = 18.0
    assertEquals(18.0, cartItem.subtotal, 0.001)
    // tax = 18.0 * 0.05 = 0.90
    assertEquals(0.90, cartItem.taxAmount, 0.001)
    // total = 18.90
    assertEquals(18.90, cartItem.total, 0.001)
  }
}
