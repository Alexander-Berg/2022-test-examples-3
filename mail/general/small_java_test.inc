INCLUDE_ONCE(yes)

SIZE(SMALL)

JVM_ARGS(
    -Djava.net.preferIPv6Addresses=true
    -Djava.net.preferIPv4Stack=false
)

ENV(ASAN_OPTIONS=detect_leaks=0)

REQUIREMENTS(
    cpu:1
    ram:2
    disk_usage:1
    network:restricted
)

