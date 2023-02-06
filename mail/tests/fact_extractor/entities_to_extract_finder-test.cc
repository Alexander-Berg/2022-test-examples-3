#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/fact_extractor/entities_to_extract_finder.h>
#include <internal/fact_extractor/xml_parser.h>

namespace msg_body {
inline std::ostream & operator << (std::ostream & s, const TextEntitiesToExtractFinder::Entities & entities) {
    return s << '\"' << boost::algorithm::join(entities, ",") << '\"';
}

}

namespace {

struct Envelope {
    enum Type {
        Type_none = 0,
        Type_delivery = 1,
        Type_registration = 2,
        Type_social = 3,
        Type_people = 4,
        Type_eticket = 5,
        Type_eshop = 6,
        Type_notification = 7,
        Type_bounce = 8,
        Type_official = 9,
        Type_script = 10,
        Type_dating = 11,
        Type_greeting = 12,
        Type_news  = 13,
        Type_s_grouponsite = 14,
        Type_s_datingsite  = 15,
        Type_s_aviaeticket = 16,
        Type_s_bank = 17,
        Type_s_social = 18,
        Type_s_travel = 19,
        Type_s_zdticket  = 20,
        Type_s_realty = 21,
        Type_personalnews = 22,
        Type_s_eshop = 23,
        Type_s_company = 24,
        Type_s_job = 25,
        Type_s_game = 26,
        Type_schema = 27,
        Type_cancel = 28,
        Type_s_tech = 29,
        Type_s_media = 30,
        Type_s_advert = 31,
        Type_s_provider = 32,
        Type_s_forum = 33,
        Type_s_mobile = 34,
        Type_hotel = 35,
        Type_yamoney = 36,
    };
};


using namespace testing;
using namespace msg_body;

struct EntitiesToExtractFinderTest : public Test {
    static MessagePart getMessagePart() {
        MessagePart messagePart;
        messagePart.contentType.setMimeType("application/pdf");
        messagePart.realName = "Reservation_Confirmation.pdf";
        messagePart.messageHeader["from"] = "confirm@s7.ru";
        messagePart.messageHeader["subject"] = "Reservation Confirmation";
        return messagePart;
    }

    static TextEntitiesToExtractFinder::MessageTypes getETicketMessageTypes() {
        TextEntitiesToExtractFinder::MessageTypes messageTypes;
        messageTypes.insert(Envelope::Type_eticket);
        messageTypes.insert(Envelope::Type_s_eshop);
        return messageTypes;
    }

    static TextEntitiesToExtractFinder::MessageTypes getTicketMessageTypes() {
        TextEntitiesToExtractFinder::MessageTypes messageTypes;
        messageTypes.insert(Envelope::Type_eticket);
        messageTypes.insert(Envelope::Type_s_aviaeticket);
        messageTypes.insert(Envelope::Type_s_eshop);
        return messageTypes;
    }

    static TextEntitiesToExtractFinder::MessageTypes getHotelMessageTypes() {
        TextEntitiesToExtractFinder::MessageTypes messageTypes;
        messageTypes.insert(Envelope::Type_hotel);
        messageTypes.insert(Envelope::Type_s_travel);
        messageTypes.insert(Envelope::Type_s_eshop);
        return messageTypes;
    }

    static TypeMsg getTicketMultipleEntitiesTypeMsg() {
        TypeMsg typeMsg;
        typeMsg.entities.insert("entity1");
        typeMsg.entities.insert("entity2");
        typeMsg.include.push_back(Envelope::Type_eticket);
        typeMsg.include.push_back(Envelope::Type_s_aviaeticket);
        return typeMsg;
    }

    static TypeMsg getTicketIncludedTypeMsg() {
        TypeMsg typeMsg;
        typeMsg.entities.insert("include");
        typeMsg.include.push_back(Envelope::Type_eticket);
        typeMsg.include.push_back(Envelope::Type_s_aviaeticket);
        return typeMsg;
    }

    static TypeMsg getHotelExcludedTypeMsg() {
        TypeMsg typeMsg;
        typeMsg.entities.insert("exclude");
        typeMsg.exclude.push_back(Envelope::Type_hotel);
        typeMsg.exclude.push_back(Envelope::Type_s_travel);
        return typeMsg;
    }
};

TEST_F( EntitiesToExtractFinderTest, textPart_satisfies_RuleWithMultipleEntities ) {
    EntitiesToExtractFinder::Entities expectedEntities;
    expectedEntities.insert("entity1");
    expectedEntities.insert("entity2");

    ParserConfig parserConfig;
    parserConfig.rules.push_back(getTicketMultipleEntitiesTypeMsg());

    EntitiesToExtractFinder::MessageTypes messageTypes = getTicketMessageTypes();
    TextEntitiesToExtractFinder entitiesToExtractFinder(messageTypes);
    EntitiesToExtractFinder::Entities actualEntities =
            entitiesToExtractFinder.find(parserConfig);
    EXPECT_EQ(expectedEntities, actualEntities);
}

TEST_F( EntitiesToExtractFinderTest, textPart_satisfies_includeRestriction ) {
    EntitiesToExtractFinder::Entities expectedEntities;
    expectedEntities.insert("include");

    ParserConfig parserConfig;
    parserConfig.rules.push_back(getTicketIncludedTypeMsg());

    EntitiesToExtractFinder::MessageTypes messageTypes = getTicketMessageTypes();
    TextEntitiesToExtractFinder entitiesToExtractFinder(messageTypes);
    EntitiesToExtractFinder::Entities actualEntities =
            entitiesToExtractFinder.find(parserConfig);
    EXPECT_EQ(expectedEntities, actualEntities);
}

TEST_F( EntitiesToExtractFinderTest, textPart_not_satisfies_includeRestriction ) {
    EntitiesToExtractFinder::Entities expectedEntities;

    ParserConfig parserConfig;
    parserConfig.rules.push_back(getTicketIncludedTypeMsg());

    EntitiesToExtractFinder::MessageTypes messageTypes = getHotelMessageTypes();
    TextEntitiesToExtractFinder entitiesToExtractFinder(messageTypes);
    EntitiesToExtractFinder::Entities actualEntities =
            entitiesToExtractFinder.find(parserConfig);
    EXPECT_EQ(expectedEntities, actualEntities);
}

TEST_F( EntitiesToExtractFinderTest, textPart_satisfies_excludeRestriction ) {
    EntitiesToExtractFinder::Entities expectedEntities;
    expectedEntities.insert("exclude");

    ParserConfig parserConfig;
    parserConfig.rules.push_back(getHotelExcludedTypeMsg());

    EntitiesToExtractFinder::MessageTypes messageTypes = getTicketMessageTypes();
    TextEntitiesToExtractFinder entitiesToExtractFinder(messageTypes);
    EntitiesToExtractFinder::Entities actualEntities =
            entitiesToExtractFinder.find(parserConfig);
    EXPECT_EQ(expectedEntities, actualEntities);
}

TEST_F( EntitiesToExtractFinderTest, textPart_not_satisfies_excludeRestriction ) {
    EntitiesToExtractFinder::Entities expectedEntities;

    ParserConfig parserConfig;
    parserConfig.rules.push_back(getHotelExcludedTypeMsg());

    EntitiesToExtractFinder::MessageTypes messageTypes = getHotelMessageTypes();
    TextEntitiesToExtractFinder entitiesToExtractFinder(messageTypes);
    EntitiesToExtractFinder::Entities actualEntities =
            entitiesToExtractFinder.find(parserConfig);
    EXPECT_EQ(expectedEntities, actualEntities);
}

TEST_F( EntitiesToExtractFinderTest, textPart_satisfies_multipleRestriction ) {
    EntitiesToExtractFinder::Entities expectedEntities;
    expectedEntities.insert("include");
    expectedEntities.insert("exclude");

    ParserConfig parserConfig;
    parserConfig.rules.push_back(getTicketIncludedTypeMsg());
    parserConfig.rules.push_back(getHotelExcludedTypeMsg());

    EntitiesToExtractFinder::MessageTypes messageTypes = getTicketMessageTypes();
    TextEntitiesToExtractFinder entitiesToExtractFinder(messageTypes);
    EntitiesToExtractFinder::Entities actualEntities =
            entitiesToExtractFinder.find(parserConfig);
    EXPECT_EQ(expectedEntities, actualEntities);
}

}
