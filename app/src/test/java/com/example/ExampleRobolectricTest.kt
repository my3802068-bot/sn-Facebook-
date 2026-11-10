package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.FacebookSampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SN Facebook", appName)
  }

  @Test
  fun `verify sample data availability`() {
    assertTrue(FacebookSampleData.stories.isNotEmpty())
    assertTrue(FacebookSampleData.posts.isNotEmpty())
    assertNotNull(FacebookSampleData.posts.first().content)
  }
}
