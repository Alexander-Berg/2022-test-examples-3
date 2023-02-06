SYSTEM_PROPERTIES(
    # Временная зона нужна для тестов миграции SQL (в H2 нельзя задать временную зону по умолчанию)
    # Для конвертации JSON (TZ в дате/времени)
    user.timezone Europe/Moscow
)
JVM_ARGS(
    -ea
)
