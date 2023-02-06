package ru.beru.android.processor.testinstance.adapters

import javax.lang.model.type.TypeMirror

interface InstanceAdaptersRegistry {

    fun getProvider(type: TypeMirror): InstanceAdapter?
}