SET(OPENAPI_SOURCE ${ARCADIA_ROOT}/market/mbi/ff4shops/ff4shops-app/src/main/resources/openapi)
SET(SWAGGER_CLIENT ${BINDIR}/swagger/client)

RUN_JAVA_PROGRAM(
    org.openapitools.codegen.OpenAPIGenerator
    generate
    -i ${OPENAPI_SOURCE}/ff4shops.yaml
    -c ${ARCADIA_ROOT}/market/mbi/ff4shops/ff4shops-app/src/test/resources/client-config.json
    -g java
    -psourceFolder=out
    -o ${SWAGGER_CLIENT}
    IN_DIR ${OPENAPI_SOURCE}
    OUT_DIR ${SWAGGER_CLIENT}/out
    CLASSPATH contrib/java/org/openapitools/openapi-generator-cli/4.2.0
)

RUN_JAVA_PROGRAM(
    org.openapitools.codegen.OpenAPIGenerator
    generate
    -i ${OPENAPI_SOURCE}/stocks.yaml
    -c ${ARCADIA_ROOT}/market/mbi/ff4shops/ff4shops-app/src/test/resources/client-config.json
    -g java
    -psourceFolder=out
    --invoker-package "ru.yandex.market.ff4shops.client.stocks"
    -o ${SWAGGER_CLIENT}
    IN_DIR ${OPENAPI_SOURCE}
    OUT_DIR ${SWAGGER_CLIENT}/out
    CLASSPATH contrib/java/org/openapitools/openapi-generator-cli/4.2.0
)

JAVA_SRCS(SRCDIR ${SWAGGER_CLIENT}/out **/*)
