SET(OPENAPI_SOURCE ${ARCADIA_ROOT}/market/logistics/logistics4shops/logistics4shops-app/src/main/resources/openapi)
SET(OPENAPI_CONFIG ${ARCADIA_ROOT}/market/logistics/logistics4shops/logistics4shops-app/src/integration-test/resources/openapi/client-config.json)
SET(OPENAPI_CLIENT ${BINDIR}/openapi/client)

RUN_JAVA_PROGRAM(
    org.openapitools.codegen.OpenAPIGenerator
    generate
    -i ${OPENAPI_SOURCE}/logistics4shops-api.yaml
    -c ${OPENAPI_CONFIG}
    -g java
    -psourceFolder=generated
    -s
    -o ${OPENAPI_CLIENT}
    IN_DIR ${OPENAPI_SOURCE}
    IN ${OPENAPI_CONFIG}
    OUT_DIR ${OPENAPI_CLIENT}/generated
    CLASSPATH contrib/java/org/openapitools/openapi-generator-cli/5.2.0
)

JAVA_SRCS(SRCDIR ${OPENAPI_CLIENT}/generated **/*.java)
