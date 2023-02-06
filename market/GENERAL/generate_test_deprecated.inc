SET(MAIN_DIR ${CURDIR}/../..)
SET(RESOURCES_DIR ${MAIN_DIR}/src/main/resources)
SET(SERVICE_YAML ${MAIN_DIR}/service.yaml)
SET(OPENAPI_CONFIGS ${ARCADIA_ROOT}/market/infra/java-application/mj/v1/src/main/resources/openapi)
SET(OPENAPI_TEMPLATES ${ARCADIA_ROOT}/market/infra/java-application/mj/v1/openapi-templates)
SET(FRAMES ${ARCADIA_ROOT}/market/infra/java-application/mj/v1/frames)
SET(OUTPUT ${BINDIR}/generated_test)
RUN_JAVA_PROGRAM(
    ru.yandex.market.framework.generator.MarketTestJavaGenerator
    ${MAIN_DIR}
    ${ARCADIA_ROOT}
    ${OUTPUT}
    IN ${SERVICE_YAML} ${CLIENT_DEPENDENT_FILES}
    IN_DIR ${RESOURCES_DIR} ${OPENAPI_CONFIGS} ${OPENAPI_TEMPLATES} ${FRAMES}
    OUT_DIR ${OUTPUT}
    CLASSPATH market/infra/java-application/mj/v1
)
JAVA_SRCS(SRCDIR ${OUTPUT} **/*.java yaml-properties/*.yaml)
