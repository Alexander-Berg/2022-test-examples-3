INCLUDE(${ARCADIA_ROOT}/market/adv/inc/dependency_management.inc)

PEERDIR(
    market/jlibrary/common-test

    #Test framework
    contrib/java/org/junit/jupiter/junit-jupiter-api
    contrib/java/org/junit/jupiter/junit-jupiter-engine
    contrib/java/org/junit/jupiter/junit-jupiter-params

    #Asserts
    contrib/java/org/assertj/assertj-core

    #Mocks
    contrib/java/org/mockito/mockito-core
    contrib/java/org/mockito/mockito-junit-jupiter

    #Spring boot test
    contrib/java/org/springframework/boot/spring-boot-test
    contrib/java/org/springframework/boot/spring-boot-starter-test
    contrib/java/org/mock-server/mockserver-netty
    contrib/java/org/mock-server/mockserver-junit-jupiter
)

EXCLUDE(
    contrib/java/ch/qos/logback/logback-classic
    contrib/java/org/apache/logging/log4j/log4j-to-slf4j

    contrib/java/junit/junit

    contrib/java/org/springframework/boot/spring-boot-starter-logging
)
