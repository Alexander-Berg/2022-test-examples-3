package ru.beru.android.processor.testinstance.adapters

import javax.annotation.processing.Messager
import javax.inject.Inject
import javax.tools.Diagnostic

internal class InstanceAdaptersInitializer @Inject constructor(
    private val registry: InternalInstanceAdaptersRegistry,
    private val availableAdapters: Iterable<AdapterRecord>,
    private val logger: Messager
) {
    fun setupProviders() {
        availableAdapters.forEach { (provider, shortcuts) ->
            try {
                registry.registerProvider(provider)
                shortcuts.forEach {
                    when (val shortcut = it) {

                        is TypeKindShortcut ->
                            registry.registerTypeKindShortcut(provider, shortcut.typeKind)

                        is DeclaredTypeShortcut ->
                            registry.registerDeclaredTypeShortcut(provider, shortcut.canonicalName)
                    }
                }
            } catch (e: Exception) {
                logger.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Произошла ошибка во время инициализации адаптеров: $e."
                )
            }
        }
    }
}