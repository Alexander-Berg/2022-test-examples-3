package ru.yandex.market.logistics.mqm.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.logistics.mqm.configuration.properties.StatisticsReportGrafanaProperties
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.AssemblyStatisticsReportQueryBuilder
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.IntakeStatisticsReportQueryBuilder
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.MarketCourierIntakeStatisticsReportQueryBuilder
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.ShipmentStatisticsReportQueryBuilder

@Configuration
class StatisticsReportConfiguration {

    @Bean
    fun ffAssemblyQueryBuilder(grafanaProperties: StatisticsReportGrafanaProperties) =
        AssemblyStatisticsReportQueryBuilder(SegmentType.FULFILLMENT, grafanaProperties.ffAssemblyUrl)

    @Bean
    fun ffShipmentQueryBuilder(grafanaProperties: StatisticsReportGrafanaProperties) =
        ShipmentStatisticsReportQueryBuilder(PartnerType.FULFILLMENT, grafanaProperties.ffShipmentUrl)

    @Bean
    fun ffDsIntakeQueryBuilder(grafanaProperties: StatisticsReportGrafanaProperties) =
        IntakeStatisticsReportQueryBuilder(
            PartnerType.FULFILLMENT,
            PartnerType.DELIVERY,
            grafanaProperties.ffDsIntakeUrl
        )

    @Bean
    fun mcIntakeQueryBuilder(grafanaProperties: StatisticsReportGrafanaProperties) =
        MarketCourierIntakeStatisticsReportQueryBuilder(grafanaProperties.mcIntakeUrl)
}
