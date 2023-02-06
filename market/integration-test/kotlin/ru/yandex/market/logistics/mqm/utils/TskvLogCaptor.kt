package ru.yandex.market.logistics.mqm.utils

import org.apache.logging.log4j.Level
import ru.yandex.market.logistics.mqm.logging.TskvLogLayout
import ru.yandex.market.logistics.test.integration.logging.CustomLogCaptor

class TskvLogCaptor(loggerName: String):
    CustomLogCaptor<TskvLogLayout>(loggerName, "TSKV_LOG", Level.INFO)
