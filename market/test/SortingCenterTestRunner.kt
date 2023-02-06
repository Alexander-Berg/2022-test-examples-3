package ru.beru.sortingcenter.test

import android.app.Application
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.qameta.allure.android.AllureAndroidLifecycle
import io.qameta.allure.android.runners.AllureAndroidJUnitRunner
import io.qameta.allure.kotlin.FileSystemResultsWriter
import ru.beru.sortingcenter.SortingCenterTestApplication_Application

@Suppress("unused")
class SortingCenterTestRunner : AllureAndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(
            cl,
            SortingCenterTestApplication_Application::class.java.name,
            context,
        )
    }

    override fun createAllureAndroidLifecycle(): AllureAndroidLifecycle {
        return AllureAndroidLifecycle(
            FileSystemResultsWriter {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                requireNotNull(context.getExternalFilesDir(null)).resolve("allure-results")
            }
        )
    }
}
