SET(OPENAPI_SOURCE ${ARCADIA_ROOT}/market/logistics/lrm/lrm-app/src/main/resources/openapi)
SET(OPENAPI_CONFIG ${ARCADIA_ROOT}/market/logistics/lrm/lrm-app/src/integration-test/resources/client-config.json)
SET(SWAGGER_CLIENT ${BINDIR}/swagger/client)
RUN_JAVA_PROGRAM(
    org.openapitools.codegen.OpenAPIGenerator
    generate
    -i ${OPENAPI_SOURCE}/lrm-api.yaml
    -c ${OPENAPI_CONFIG}
    -g java
    -psourceFolder=out
    -o ${SWAGGER_CLIENT}
    -s
    IN_DIR ${OPENAPI_SOURCE}
    IN ${OPENAPI_CONFIG}
    OUT_DIR ${SWAGGER_CLIENT}/out
    CLASSPATH contrib/java/org/openapitools/openapi-generator-cli/5.2.0
)

JAVA_SRCS(SRCDIR ${SWAGGER_CLIENT}/out **/*)
