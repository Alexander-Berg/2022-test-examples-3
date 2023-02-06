package ru.beru.sortingcenter.util

import android.view.View
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.arch.ext.goneUnless

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class BindingAdaptersKtTest {
    @Mock
    private lateinit var view: View

    @Test
    fun `changes visibility`() {
        goneUnless(view, false)
        verify(view).visibility = View.GONE

        goneUnless(view, true)
        verify(view).visibility = View.VISIBLE
    }
}