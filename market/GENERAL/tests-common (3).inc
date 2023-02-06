INCLUDE(${ARCADIA_ROOT}/market/jlibrary/dependency_management.inc)
INCLUDE(${ARCADIA_ROOT}/market/mbo/content-mapping/contrib.ya.make)

PEERDIR(
    contrib/java/junit/junit
)

JAVAC_FLAGS(-parameters)

JVM_ARGS(
    -Djava.net.preferIPv6Addresses=true
    -Djava.awt.headless=true
    -Djava.security.egd=file:/dev/./urandom
    -Dsun.java2d.debugfonts=warning
)

REQUIREMENTS(sb_vault:ETCD_ENDPOINTS=value:MARKETCMS:etcd_endpoints)
REQUIREMENTS(sb_vault:ETCD_USERNAME=value:MARKETCMS:etcd_username)
REQUIREMENTS(sb_vault:ETCD_PASSWORD=value:MARKETCMS:etcd_password)
