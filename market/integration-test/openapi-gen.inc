SET(OPENAPI_SOURCE ${ARCADIA_ROOT}/market/logistics/logistics4go/logistics4go-app/src/main/resources/static/openapi)
SET(OPENAPI_CONFIG ${ARCADIA_ROOT}/market/logistics/logistics4go/logistics4go-app/src/integration-test/resources/client-config.json)
SET(OPENAPI_CLIENT ${BINDIR}/openapi/client)

RUN_JAVA_PROGRAM(
    org.openapitools.codegen.OpenAPIGenerator
    generate
    -i ${OPENAPI_SOURCE}/logistics4go-api.yaml
    -c ${OPENAPI_CONFIG}
    -g java
    -psourceFolder=generated
    -o ${OPENAPI_CLIENT}
    IN_DIR ${OPENAPI_SOURCE}
    OUT_DIR ${OPENAPI_CLIENT}/generated
    CLASSPATH contrib/java/org/openapitools/openapi-generator-cli/5.2.0
)

JAVA_SRCS(SRCDIR ${OPENAPI_CLIENT}/generated **/*)
