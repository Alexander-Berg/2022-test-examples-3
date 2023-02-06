SET(SWAGGER_SOURCE ${ARCADIA_ROOT}/market/logistics/logistics4go/logistics4go-app/src/main/resources/static/openapi)
SET(SWAGGER_CONFIG ${ARCADIA_ROOT}/market/delivery/test-common/l4g-client/src/main/resources/swagger/config-client.json)
SET(SWAGGER_CLIENT ${BINDIR}/swagger/client)

RUN_JAVA_PROGRAM(
    org.openapitools.codegen.OpenAPIGenerator
    generate
    -i ${SWAGGER_SOURCE}/logistics4go-api.yaml
    -c ${SWAGGER_CONFIG}
    -g java
    -psourceFolder=generated
    -papiPackage=ru.yandex.market.logistics4go.client.api
    -pmodelPackage=ru.yandex.market.logistics4go.client.model
    -v
    -s
    -o ${SWAGGER_CLIENT}
    IN_DIR ${SWAGGER_SOURCE}
    IN ${SWAGGER_CONFIG}
    OUT_DIR ${SWAGGER_CLIENT}/generated
    CLASSPATH contrib/java/org/openapitools/openapi-generator-cli/4.2.0
)

JAVA_SRCS(SRCDIR ${SWAGGER_CLIENT}/generated **/*.java)
