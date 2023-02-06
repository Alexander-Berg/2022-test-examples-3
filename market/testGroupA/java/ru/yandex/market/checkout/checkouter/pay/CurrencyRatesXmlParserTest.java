package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;

public class CurrencyRatesXmlParserTest {

    private static final String XML_WITH_UNKNOWN_BANK =
            "<exchange>\n" +
                    " <currencies>\n" +
                    "  <currency name=\"RUR\">\n" +
                    "   <alias>RUB</alias>\n" +
                    "  </currency>\n" +
                    "  <currency name=\"USD\"/>\n" +
                    "  <currency name=\"EUR\"/>\n" +
                    "  <currency name=\"UAH\"/>\n" +
                    "  <currency name=\"BYR\"/>\n" +
                    "  <currency name=\"KZT\"/>\n" +
                    "  <currency name=\"UE\"/>\n" +
                    " </currencies>\n" +
                    " <banks>\n" +
                    "  <bank name=\"CBRF\">\n" +
                    "   <region>225</region>\n" +
                    "   <currency>RUR</currency>\n" +
                    "   <rates>\n" +
                    "    <rate from=\"RUR\" to=\"RUR\">1.0</rate>\n" +
                    "    <rate from=\"USD\" to=\"RUR\">62.4583</rate>\n" +
                    "    <rate from=\"EUR\" to=\"RUR\">70.0782</rate>\n" +
                    "    <rate from=\"UAH\" to=\"RUR\">2.41105</rate>\n" +
                    "    <rate from=\"BYR\" to=\"RUR\">0.00325983</rate>\n" +
                    "    <rate from=\"KZT\" to=\"RUR\">0.187887</rate>\n" +
                    "   </rates>\n" +
                    "  </bank>\n" +
                    "  <bank name=\"NBRB\">\n" +
                    "   <region>149</region>\n" +
                    "   <currency>BYR</currency>\n" +
                    "   <rates>\n" +
                    "    <rate from=\"RUR\" to=\"BYR\">307.01</rate>\n" +
                    "    <rate from=\"USD\" to=\"BYR\">19176.0</rate>\n" +
                    "    <rate from=\"EUR\" to=\"BYR\">21510.0</rate>\n" +
                    "    <rate from=\"UAH\" to=\"BYR\">743.05</rate>\n" +
                    "    <rate from=\"BYR\" to=\"BYR\">1.0</rate>\n" +
                    "    <rate from=\"KZT\" to=\"BYR\">57.656</rate>\n" +
                    "   </rates>\n" +
                    "  </bank>\n" +
                    "  <bank name=\"NBU\">\n" +
                    "   <region>187</region>\n" +
                    "   <currency>UAH</currency>\n" +
                    "   <rates>\n" +
                    "    <rate from=\"RUR\" to=\"UAH\">0.41465</rate>\n" +
                    "    <rate from=\"USD\" to=\"UAH\">25.8983</rate>\n" +
                    "    <rate from=\"EUR\" to=\"UAH\">29.0346</rate>\n" +
                    "    <rate from=\"UAH\" to=\"UAH\">1.0</rate>\n" +
                    "    <rate from=\"BYR\" to=\"UAH\">0.00135</rate>\n" +
                    "    <rate from=\"KZT\" to=\"UAH\">0.077831</rate>\n" +
                    "   </rates>\n" +
                    "  </bank>\n" +
                    "  <bank name=\"NBK\">\n" +
                    "   <region>159</region>\n" +
                    "   <currency>KZT</currency>\n" +
                    "   <rates>\n" +
                    "    <rate from=\"RUR\" to=\"KZT\">5.33</rate>\n" +
                    "    <rate from=\"USD\" to=\"KZT\">332.75</rate>\n" +
                    "    <rate from=\"EUR\" to=\"KZT\">373.25</rate>\n" +
                    "    <rate from=\"UAH\" to=\"KZT\">12.88</rate>\n" +
                    "    <rate from=\"BYR\" to=\"KZT\">0.0174</rate>\n" +
                    "    <rate from=\"KZT\" to=\"KZT\">1.0</rate>\n" +
                    "   </rates>\n" +
                    "  </bank>\n" +
                    "  <bank name=\"YNDX\">\n" +
                    "   <region>-1</region>\n" +
                    "   <currency>UE</currency>\n" +
                    "   <rates>\n" +
                    "    <rate from=\"RUR\" to=\"UE\">0.033333335</rate>\n" +
                    "    <rate from=\"UAH\" to=\"UE\">0.083333336</rate>\n" +
                    "    <rate from=\"KZT\" to=\"UE\">0.00952381</rate>\n" +
                    "    <rate from=\"UE\" to=\"UE\">1.0</rate>\n" +
                    "   </rates>\n" +
                    "  </bank>\n" +
                    "  <bank name=\"ECB\">\n" +
                    "   <region>315</region>\n" +
                    "   <currency>EUR</currency>\n" +
                    "   <rates>\n" +
                    "    <rate from=\"RUR\" to=\"EUR\">0.01427</rate>\n" +
                    "    <rate from=\"USD\" to=\"EUR\">0.892</rate>\n" +
                    "    <rate from=\"EUR\" to=\"EUR\">1.0</rate>\n" +
                    "    <rate from=\"UAH\" to=\"EUR\">0.034442</rate>\n" +
                    "    <rate from=\"BYR\" to=\"EUR\">4.6E-5</rate>\n" +
                    "    <rate from=\"KZT\" to=\"EUR\">0.002679</rate>\n" +
                    "   </rates>\n" +
                    "  </bank>\n" +
                    "  <bank name=\"BUSD\">\n" +
                    "   <region>201</region>\n" +
                    "   <currency>USD</currency>\n" +
                    "   <rates>\n" +
                    "    <rate from=\"RUR\" to=\"USD\">0.016011</rate>\n" +
                    "    <rate from=\"USD\" to=\"USD\">1.0</rate>\n" +
                    "    <rate from=\"EUR\" to=\"USD\">1.121076</rate>\n" +
                    "    <rate from=\"UAH\" to=\"USD\">0.038613</rate>\n" +
                    "    <rate from=\"BYR\" to=\"USD\">5.2E-5</rate>\n" +
                    "    <rate from=\"KZT\" to=\"USD\">0.003005</rate>\n" +
                    "   </rates>\n" +
                    "  </bank>\n" +
                    " </banks>\n" +
                    "</exchange>\n";

    @Test
    public void shouldNotFailOnUnknownBank() throws IOException, SAXException {
        CurrencyRatesXmlParser currencyRatesXmlParser = new CurrencyRatesXmlParser();
        currencyRatesXmlParser.parseXmlReader(new StringReader(XML_WITH_UNKNOWN_BANK));
        Assert.notEmpty(currencyRatesXmlParser.getRates());
    }

}
