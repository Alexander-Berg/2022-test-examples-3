INCLUDE(${ARCADIA_ROOT}/market/jlibrary/dependency_management.inc)
INCLUDE(${ARCADIA_ROOT}/market/mbo/content-lab/contrib.ya.make)

JAVAC_FLAGS(-parameters)

JVM_ARGS(
    -Djava.net.preferIPv6Addresses=true
    -Djava.awt.headless=true
    -Djava.security.egd=file:/dev/./urandom
    -Dsun.java2d.debugfonts=warning
)
