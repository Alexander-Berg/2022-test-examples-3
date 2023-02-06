package ru.yandex.market.checkout.util.report;

import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.report.ColorUtils;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 16.09.2020
 */
@Component
public class ColoredReportConfigurer {

    private final ReportConfigurer reportConfigurer;
    private final ReportConfigurer reportConfigurerWhite;
    private final ReportConfigurer reportConfigurerTurbo;

    public ColoredReportConfigurer(ReportConfigurer reportConfigurer,
                                   ReportConfigurer reportConfigurerWhite,
                                   ReportConfigurer reportConfigurerTurbo) {
        this.reportConfigurer = reportConfigurer;
        this.reportConfigurerWhite = reportConfigurerWhite;
        this.reportConfigurerTurbo = reportConfigurerTurbo;
    }

    public ReportConfigurer getBy(Color color) {
        if (color == null) {
            // default=green in api
            return reportConfigurerWhite;
        }

        var reportColor = ColorUtils.mapCheckouterColorToReport(color);
        return reportColor == ru.yandex.market.common.report.model.Color.TURBO ? reportConfigurerTurbo :
                reportColor == ru.yandex.market.common.report.model.Color.GREEN
                        || reportColor == ru.yandex.market.common.report.model.Color.WHITE
                        ? reportConfigurerWhite : reportConfigurer;
    }
}
