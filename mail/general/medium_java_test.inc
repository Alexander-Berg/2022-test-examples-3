INCLUDE_ONCE(yes)

SIZE(MEDIUM)

JVM_ARGS(
    -Djava.net.preferIPv6Addresses=true
    -Djava.net.preferIPv4Stack=false
    -Xmx4G
    -Xms1G
)

ENV(ASAN_OPTIONS=detect_leaks=0)

REQUIREMENTS(
    cpu:4
    ram:8
    disk_usage:2
    network:restricted
)

