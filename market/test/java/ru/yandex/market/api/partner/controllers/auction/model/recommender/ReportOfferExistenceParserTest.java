package ru.yandex.market.api.partner.controllers.auction.model.recommender;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.AbstractParserTest;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiReportOfferExistenceValidator.AmbiguousReportAnswerException;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiReportOfferExistenceValidator.ReportOfferExistenceParser;
import ru.yandex.market.api.partner.controllers.auction.model.recommender.ApiReportOfferExistenceValidator.ReportOfferInfoMinimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;

public class ReportOfferExistenceParserTest extends AbstractParserTest {
    private static final String FILE_OK = "ok.xml";
    private static final String FILE_BAD_NOT_FOUND = "bad_not_found.xml";
    private static final String FILE_BAD_NO_TITLE = "bad_no_title.xml";
    private static final String FILE_BAD_MULTIPLE_OFFERS = "bad_multiple_offers.xml";
    private static final String FILE_OK_NO_MODEL = "no_model_card.xml";
    private static ReportOfferExistenceParser PARSER;

    @BeforeEach
    void before() {
        PARSER = new ReportOfferExistenceParser();
    }

    @Test
    void test_parser_should_parseOffer_when_xmlIsOk() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_OK)) {
            PARSER.parseXmlStream(in);
            ReportOfferInfoMinimal actual = PARSER.getResult();

            assertThat(actual.getOfferName(), is("Часы Casio AQ-S810W-2A"));
            assertThat(actual.getCategoryId(), is(91259L));
            assertThat(actual.getModelId(), is(555666777L));
        }
    }

    @Test
    void test_parser_should_throw_when_multipleOfferBlocks() {
        Assertions.assertThrows(AmbiguousReportAnswerException.class,
                () -> {
                    try (InputStream in = getContentStream(FILE_BAD_MULTIPLE_OFFERS)) {
                        PARSER.parseXmlStream(in);
                        PARSER.getResult();
                    }
                });
    }

    @Test
    void test_parser_should_returnNull_when_notFound() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_BAD_NOT_FOUND)) {
            PARSER.parseXmlStream(in);
            ReportOfferInfoMinimal actual = PARSER.getResult();
            assertThat(actual, nullValue());
        }
    }

    @Test
    void test_parser_should_returnNull_when_offerBlockHasNoTitle() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_BAD_NO_TITLE)) {
            PARSER.parseXmlStream(in);
            ReportOfferInfoMinimal actual = PARSER.getResult();
            assertThat(actual, nullValue());
        }
    }

    @Test
    void test_parser_should_parseOffer_when_xmlIsOk_butNoModel() throws IOException, SAXException {
        try (InputStream in = getContentStream(FILE_OK_NO_MODEL)) {
            PARSER.parseXmlStream(in);
            ReportOfferInfoMinimal actual = PARSER.getResult();

            assertThat(actual.getOfferName(), is("Часы Casio AQ-S810W-2A"));
            assertThat(actual.getCategoryId(), is(91259L));
            assertNull(actual.getModelId());
        }
    }

}