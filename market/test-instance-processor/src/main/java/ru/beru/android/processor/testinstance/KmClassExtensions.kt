package ru.beru.android.processor.testinstance

import com.squareup.kotlinpoet.metadata.isSealed
import kotlinx.metadata.KmClass

val KmClass.isSealed: Boolean get() = flags.isSealed