package ru.beru.android.processor.testinstance

import dagger.Binds
import dagger.Module
import dagger.Provides
import ru.beru.android.processor.testinstance.adapters.AdapterRecord
import ru.beru.android.processor.testinstance.adapters.AdapterShortcut
import ru.beru.android.processor.testinstance.adapters.BigDecimalInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.BooleanInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.DateInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.DeclaredTypeShortcut
import ru.beru.android.processor.testinstance.adapters.DoubleInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.DurationInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.EnumInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.FactoryMethodInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.FloatInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.Function0InstanceAdapter
import ru.beru.android.processor.testinstance.adapters.GeneratedInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.InstanceAdapter
import ru.beru.android.processor.testinstance.adapters.InstanceAdaptersRegistry
import ru.beru.android.processor.testinstance.adapters.IntegerInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.InternalInstanceAdaptersRegistry
import ru.beru.android.processor.testinstance.adapters.ListInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.LocalDateInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.LocalTimeInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.LongInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.MapInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.ObjectInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.RootInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.SealedClassInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.SetInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.StringInstanceAdapter
import ru.beru.android.processor.testinstance.adapters.TypeKindShortcut
import ru.beru.android.processor.testinstance.adapters.ZonedDateTimeInstanceAdapter
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Date
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind

@Module
abstract class ProcessorModule {

    @Binds
    internal abstract fun bindsInstanceAdaptersRegistry(registry: InternalInstanceAdaptersRegistry?): InstanceAdaptersRegistry?

    @Binds
    internal abstract fun bindsInstanceAdapter(instanceAdapter: RootInstanceAdapter?): InstanceAdapter?

    companion object {
        private const val DEFAULT_METHOD_NAME_SUFFIX = "TestInstance"
        private const val DEFAULT_FILE_NAME_SUFFIX = "TestFactory"
        private const val DEFAULT_HAND_WRITTEN_METHOD_NAME = "testInstance"
        private const val DEFAULT_JVM_METHOD_NAME = "create"

        @Provides
        fun provideProcessorConfiguration(): ProcessorConfiguration {
            return ProcessorConfiguration(
                DEFAULT_METHOD_NAME_SUFFIX,
                DEFAULT_HAND_WRITTEN_METHOD_NAME,
                DEFAULT_FILE_NAME_SUFFIX,
                DEFAULT_FILE_NAME_SUFFIX,
                DEFAULT_JVM_METHOD_NAME
            )
        }

        @Provides
        internal fun provideAvailableAdapterRecords(
            listInstanceAdapter: ListInstanceAdapter,
            enumInstanceAdapter: EnumInstanceAdapter,
            integerInstanceAdapter: IntegerInstanceAdapter,
            longInstanceAdapter: LongInstanceAdapter,
            stringInstanceAdapter: StringInstanceAdapter,
            booleanInstanceAdapter: BooleanInstanceAdapter,
            floatInstanceAdapter: FloatInstanceAdapter,
            doubleInstanceAdapter: DoubleInstanceAdapter,
            bigDecimalInstanceAdapter: BigDecimalInstanceAdapter,
            setInstanceAdapter: SetInstanceAdapter,
            mapInstanceAdapter: MapInstanceAdapter,
            function0InstanceAdapter: Function0InstanceAdapter,
            dateInstanceAdapter: DateInstanceAdapter,
            generatedInstanceAdapter: GeneratedInstanceAdapter,
            zonedDateTimeInstanceAdapter: ZonedDateTimeInstanceAdapter,
            sealedClassInstanceAdapter: SealedClassInstanceAdapter,
            objectInstanceAdapter: ObjectInstanceAdapter,
            factoryMethodInstanceAdapter: FactoryMethodInstanceAdapter,
            localDateInstanceAdapter: LocalDateInstanceAdapter,
            localTimeInstanceAdapter: LocalTimeInstanceAdapter,
            durationInstanceAdapter: DurationInstanceAdapter,
        ): Iterable<AdapterRecord> {

            return listOf(
                record(generatedInstanceAdapter),
                record(
                    integerInstanceAdapter,
                    DeclaredTypeShortcut(Int::class.java), TypeKindShortcut(TypeKind.INT)
                ),
                record(
                    longInstanceAdapter,
                    DeclaredTypeShortcut(Long::class.java), TypeKindShortcut(TypeKind.LONG)
                ),
                record(
                    booleanInstanceAdapter,
                    DeclaredTypeShortcut(Boolean::class.java),
                    TypeKindShortcut(TypeKind.BOOLEAN)
                ),
                record(
                    floatInstanceAdapter,
                    DeclaredTypeShortcut(Float::class.java),
                    TypeKindShortcut(TypeKind.FLOAT)
                ),
                record(
                    doubleInstanceAdapter,
                    DeclaredTypeShortcut(Double::class.java),
                    TypeKindShortcut(TypeKind.DOUBLE)
                ),
                record(
                    stringInstanceAdapter,
                    DeclaredTypeShortcut(String::class.java),
                    DeclaredTypeShortcut(CharSequence::class.java)
                ),
                record(
                    dateInstanceAdapter,
                    DeclaredTypeShortcut(Date::class.java)
                ),
                record(
                    function0InstanceAdapter,
                    DeclaredTypeShortcut(Function0::class.java)
                ),
                record(
                    bigDecimalInstanceAdapter,
                    DeclaredTypeShortcut(BigDecimal::class.java)
                ),
                record(
                    listInstanceAdapter,
                    DeclaredTypeShortcut(MutableList::class.java),
                    DeclaredTypeShortcut(MutableCollection::class.java)
                ),
                record(
                    setInstanceAdapter,
                    DeclaredTypeShortcut(MutableSet::class.java)
                ),
                record(
                    mapInstanceAdapter,
                    DeclaredTypeShortcut(MutableMap::class.java)
                ),
                record(
                    zonedDateTimeInstanceAdapter,
                    DeclaredTypeShortcut(ZonedDateTime::class.java)
                ),
                record(
                    localTimeInstanceAdapter,
                    DeclaredTypeShortcut(LocalTime::class.java)
                ),
                record(
                    localDateInstanceAdapter,
                    DeclaredTypeShortcut(LocalDate::class.java)
                ),
                record(
                    durationInstanceAdapter,
                    DeclaredTypeShortcut(Duration::class.java)
                ),
                record(enumInstanceAdapter),
                record(sealedClassInstanceAdapter),
                record(objectInstanceAdapter),
                record(factoryMethodInstanceAdapter),
            )
        }

        @Provides
        fun provideMessager(processingEnvironment: ProcessingEnvironment): Messager {
            return processingEnvironment.messager
        }

        private fun record(
            adapter: InstanceAdapter,
            shortcut: AdapterShortcut,
        ): AdapterRecord {
            return record(adapter, listOf(shortcut))
        }

        private fun record(
            adapter: InstanceAdapter,
            vararg shortcuts: AdapterShortcut,
        ): AdapterRecord {
            return record(adapter, listOf(*shortcuts))
        }

        private fun record(
            adapter: InstanceAdapter,
            shortcuts: List<AdapterShortcut> = emptyList(),
        ): AdapterRecord {
            return AdapterRecord(adapter, shortcuts)
        }
    }
}