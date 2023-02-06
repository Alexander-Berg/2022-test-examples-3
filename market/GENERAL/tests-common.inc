INCLUDE(${ARCADIA_ROOT}/market/jlibrary/dependency_management.inc)
INCLUDE(${ARCADIA_ROOT}/market/cms/contrib.ya.make)

PEERDIR(
    contrib/java/junit/junit
)

EXCLUDE(
    <!--code owners have to solve this themselves-->
    contrib/java/com/sun/activation/javax.activation
    contrib/java/javax/activation/
    contrib/java/javax/xml/bind
    contrib/java/javax/activation/javax.activation-api
    market/jlibrary/request
)

#etcd data for tests
REQUIREMENTS(sb_vault:ETCD_ENDPOINTS=value:MARKETCMS:etcd_endpoints)
REQUIREMENTS(sb_vault:ETCD_USERNAME=value:MARKETCMS:etcd_username)
REQUIREMENTS(sb_vault:ETCD_PASSWORD=value:MARKETCMS:etcd_password)

IF(JDK_VERSION == "")
    JDK_VERSION(17)
ENDIF()

JAVAC_FLAGS(
    -parameters
    --release $JDK_REAL_VERSION
)

JVM_ARGS(
    -Djava.net.preferIPv6Addresses=true
    -Djava.awt.headless=true
    -Djava.security.egd=file:/dev/./urandom
    -Dsun.java2d.debugfonts=warning
    --add-opens java.base/java.util=ALL-UNNAMED
    --add-opens java.base/java.nio=ALL-UNNAMED
    --add-opens java.base/java.lang=ALL-UNNAMED
    --add-opens java.base/sun.security.action=ALL-UNNAMED
)
