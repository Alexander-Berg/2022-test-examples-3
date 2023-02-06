package ru.beru.android.processor.testinstance.adapters

data class AdapterRecord(
    val adapter: InstanceAdapter,
    val shortcuts: Iterable<AdapterShortcut>
)