package ru.yandex.market.mbo.ydb.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ydb")
class TestYdbProperties : BaseYdbProperties() {
}
