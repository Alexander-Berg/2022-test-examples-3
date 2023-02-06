INCLUDE(${ARCADIA_ROOT}/mail/micronaut/dependency_versions.inc)

PEERDIR(
    contrib/java/org/junit/jupiter/junit-jupiter-api
    contrib/java/org/junit/jupiter/junit-jupiter-engine
    contrib/java/org/junit/jupiter/junit-jupiter-params
    contrib/java/org/assertj/assertj-core
    contrib/java/org/mockito/mockito-core
    contrib/java/org/mockito/mockito-junit-jupiter
    contrib/java/org/apache/logging/log4j/log4j-1.2-api
    contrib/java/org/apache/logging/log4j/log4j-slf4j-impl
    contrib/java/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml
)

EXCLUDE(
    contrib/java/log4j/log4j
)
