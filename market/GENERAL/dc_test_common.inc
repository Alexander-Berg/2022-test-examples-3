JUNIT5()
IF(JDK_VERSION == "")
    JDK_VERSION(11)
ENDIF()
SIZE(MEDIUM)

OWNER(g:market-mbi)
NEED_CHECK()

DEPENDENCY_MANAGEMENT(
    contrib/java/com/googlecode/protobuf-java-format/protobuf-java-format/1.2
)

REQUIREMENTS(cpu:4)

INCLUDE(${ARCADIA_ROOT}/market/mbi/delivery-calculator/dc_dependency_management.inc)
INCLUDE(${ARCADIA_ROOT}/market/jlibrary/common-postgres-test/pg_embedded.ya.make)

CHECK_JAVA_DEPS(yes)
NO_LINT()

JAVA_SRCS(SRCDIR java **/*)
JAVA_SRCS(SRCDIR resources **/*)

SET(JVM_ARGS
    -Xmx512M
    -XX:-OmitStackTraceInFastThrow
    -Duser.language=en
)

PEERDIR(
    contrib/java/io/github/benas/random-beans
    contrib/java/net/javacrumbs/json-unit/json-unit
    contrib/java/org/apache/logging/log4j/log4j-core
    contrib/java/org/apache/logging/log4j/log4j-slf4j-impl
    contrib/java/org/assertj/assertj-core
    contrib/java/org/mockito/mockito-core
    contrib/java/org/mockito/mockito-junit-jupiter
    contrib/java/org/hamcrest/hamcrest-library
    contrib/java/org/junit/jupiter/junit-jupiter-api
    contrib/java/org/junit/jupiter/junit-jupiter-params
    contrib/java/org/junit/jupiter/junit-jupiter-engine
    contrib/java/org/unitils/unitils-core
    contrib/java/org/xmlunit/xmlunit-core
    contrib/java/org/springframework/spring-test
    contrib/java/org/mock-server/mockserver-netty
    contrib/java/org/liquibase/liquibase-core
    contrib/java/com/googlecode/protobuf-java-format/protobuf-java-format
    market/jlibrary/common-test
)

EXCLUDE(
    contrib/java/javax/mail/mailapi
    contrib/java/ch/qos/logback/logback-classic
    contrib/java/com/vaadin/external/google/android-json
    contrib/java/com/zaxxer/HikariCP-java7
    contrib/java/org/glassfish/jaxb/jaxb-runtime
)
