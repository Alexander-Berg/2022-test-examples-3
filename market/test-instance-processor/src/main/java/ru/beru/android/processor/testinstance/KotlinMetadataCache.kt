package ru.beru.android.processor.testinstance

import com.squareup.kotlinpoet.metadata.toKmClass
import kotlinx.metadata.KmClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KotlinMetadataCache @Inject constructor() {
    private val metadataToKmClassCache = mutableMapOf<Metadata, KmClass>()

    fun toImmutableKmClass(metadata: Metadata): KmClass {
        return metadataToKmClassCache.getOrPut(metadata) {
            metadata.toKmClass()
        }
    }
}