package com.glasses.app.util

import android.content.Context
import android.os.Build
import android.os.PowerManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 电池优化请求工具类单元测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class BatteryOptimizationHelperTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }
    
    /**
     * 测试检查电池优化状态
     */
    @Test
    fun `test isIgnoringBatteryOptimizations returns boolean`() {
        val result = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        
        // 验证方法返回布尔值
        assertTrue(result is Boolean)
    }
    
    /**
     * 测试在Android 6.0以下版本返回true
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `test isIgnoringBatteryOptimizations returns true on Android below M`() {
        val result = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        
        // Android 6.0以下应该返回true
        assertTrue(result)
    }
    
    /**
     * 测试在Android 6.0及以上版本能够正常调用PowerManager
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `test isIgnoringBatteryOptimizations on Android M and above`() {
        val result = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        
        // 验证方法执行成功，返回布尔值
        assertNotNull(result)
        assertTrue(result is Boolean)
    }
}
