package ru.yandex.market.common.report.parser;

import org.springframework.beans.factory.annotation.Required;
import ru.yandex.market.common.report.parser.xml.MainMarketReportXmlParserSettings;

/**
 * @author kukabara
 */
public class MainReportXmlParserFactory implements MarketReportParserFactory<MainReportXmlParser> {

    private MainMarketReportXmlParserSettings parserSettings;

    @Required
    public void setParserSettings(MainMarketReportXmlParserSettings parserSettings) {
        this.parserSettings = parserSettings;
    }

    @Override
    public MainReportXmlParser newParser() {
        return new MainReportXmlParser(parserSettings);
    }
}
