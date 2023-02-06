NO_LINT()

SET(HAMCREST_VERSION 2.2)
SET(HAMCREST_VERSION 2.2)
SET(MOCKITO_VERSION 3.1.0)
SET(LOG4J_VERSION 2.11.2)
SET(TESTCONTAINERS_VERSION 1.15.1)

INCLUDE(${ARCADIA_ROOT}/market/contrib.ya.make)
INCLUDE(${ARCADIA_ROOT}/market/marketpromo/ciface-promo/ya.versions.inc)

DEPENDENCY_MANAGEMENT(
    contrib/java/org/apache/logging/log4j/log4j-slf4j-impl/${LOG4J_VERSION}
	contrib/java/org/hamcrest/hamcrest-core/${HAMCREST_VERSION}
	contrib/java/org/hamcrest/hamcrest-library/${HAMCREST_VERSION}
	contrib/java/org/mockito/mockito-junit-jupiter/${MOCKITO_VERSION}
	contrib/java/org/mockito/mockito-core/${MOCKITO_VERSION}
	contrib/java/org/testcontainers/testcontainers/${TESTCONTAINERS_VERSION}
    contrib/java/org/testcontainers/junit-jupiter/${TESTCONTAINERS_VERSION}
)

PEERDIR(
    contrib/java/org/junit/jupiter/junit-jupiter
    contrib/java/org/junit/jupiter/junit-jupiter-api
    contrib/java/org/junit/jupiter/junit-jupiter-engine
    contrib/java/org/junit/jupiter/junit-jupiter-params
    contrib/java/org/mockito/mockito-junit-jupiter
    contrib/java/org/springframework/boot/spring-boot-test
    contrib/java/org/springframework/boot/spring-boot-test-autoconfigure
    contrib/java/org/springframework/spring-test
    contrib/java/org/hamcrest/hamcrest-core
    contrib/java/org/hamcrest/hamcrest-library
    contrib/java/org/mockito/mockito-junit-jupiter
    contrib/java/org/mockito/mockito-core
)


