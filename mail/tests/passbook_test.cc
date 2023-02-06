#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/property_tree/json_parser.hpp>
#include <internal/passbook_parse.h>

namespace msg_body {

using namespace testing;

TEST(PassbookTest, testParseDict_simpleJson_dumpCorrectJson) {
    const std::string passDict = "{"
        "\"passTypeIdentifier\":\"pass.rambler.kassa.movies\","
        "\"formatVersion\":1,"
        "\"serialNumber\":\"5227151\""
    "}";
    PassbookPackage passbook = parsePassbookDict(passDict);
    std::ostringstream outstream;
    boost::property_tree::json_parser::write_json(outstream, passbook.dict, false);
    const std::string outJson = outstream.str();

    const std::string expected = "{"
        "\"passTypeIdentifier\":\"pass.rambler.kassa.movies\","
        "\"formatVersion\":\"1\","
        "\"serialNumber\":\"5227151\""
    "}\n";
    ASSERT_EQ(expected, outJson);
}

TEST(PassbookTest, testParseDict_simpleJsonWithArray_dumpCorrectJson) {
    const std::string passDict = "{"
        "\"associatedStoreIdentifiers\" : ["
           "531324961,"
           "12345678"
        "]"
    "}";
    PassbookPackage passbook = parsePassbookDict(passDict);
    std::ostringstream outstream;
    boost::property_tree::json_parser::write_json(outstream, passbook.dict, false);
    const std::string outJson = outstream.str();

    const std::string expected = "{"
        "\"associatedStoreIdentifiers\":["
           "\"531324961\","
           "\"12345678\""
        "]"
    "}\n";
    ASSERT_EQ(expected, outJson);
}

TEST(PassbookTest, testParseDict_jsonWithArrayAndMap_dumpCorrectJson) {
    const std::string passDict = "{"
        "\"eventTicket\" : {"
           "\"auxiliaryFields\" : ["
              "{"
                 "\"key\" : \"tickets\","
                 "\"label\" : \"Seats\""
              "},"
              "{"
                 "\"label\" : \"726 euro\","
                 "\"key\" : \"price\""
              "}"
           "]"
        "}"
    "}";
    PassbookPackage passbook = parsePassbookDict(passDict);
    std::ostringstream outstream;
    boost::property_tree::json_parser::write_json(outstream, passbook.dict, false);
    const std::string outJson = outstream.str();

    const std::string expected = "{"
        "\"eventTicket\":{"
           "\"auxiliaryFields\":["
              "{"
                 "\"key\":\"tickets\","
                 "\"label\":\"Seats\""
              "},"
              "{"
                 "\"label\":\"726 euro\","
                 "\"key\":\"price\""
              "}"
           "]"
        "}"
    "}\n";
    ASSERT_EQ(expected, outJson);
}

TEST(PassbookTest, testParseDict_realLifePkpass_dumpCorrectJson) {
    const std::string passDict = "{"
"       \"organizationName\" : \"Cleartrip\","
"       \"teamIdentifier\" : \"466YHJ5Q96\","
"       \"passTypeIdentifier\" : \"pass.cleartrip.ticket\","
"       \"associatedStoreIdentifiers\" : ["
"          531324961"
"       ],"
"       \"suppressStripShine\" : true,"
"       \"foregroundColor\" : \"rgb(0,0,0)\","
"       \"boardingPass\" : {"
"          \"transitType\" : \"PKTransitTypeAir\","
"          \"backFields\" : ["
"             {"
"                \"label\" : \"DEL - MAA (Sat, 28 Dec 2013)\","
"                \"key\" : \"DEL\","
"                \"value\" : \"Terminal 3\\nAir India AI 142\\n13:55 - 16:45  (02h 50m)\""
"             },"
"             {"
"                \"value\" : \"01h 30m\","
"                \"key\" : \"DEL_layover\","
"                \"label\" : \"Layover (MAA)\""
"             },"
"             {"
"                \"key\" : \"MAA\","
"                \"label\" : \"MAA - GOI (Sat, 28 Dec 2013)\","
"                \"value\" : \"Terminal I\\nAir India AI 975\\n18:15 - 19:35  (01h 20m)\""
"             },"
"             {"
"                \"value\" : \"H4VTJ\","
"                \"key\" : \"PNR\","
"                \"label\" : \"PNR\""
"             },"
"             {"
"                \"value\" : \"Artem Zaika\\nKateryna Mordvynova\","
"                \"label\" : \"Travellers\","
"                \"key\" : \"pax\""
"             },"
"             {"
"                \"value\" : \"1308080477\","
"                \"key\" : \"trip_id\","
"                \"label\" : \"Trip ID\""
"             },"
"             {"
"                \"value\" : \"Support: 180030003004\\nNote: Unfortunately, this is NOT a boarding pass. You will have to collect that from the airline counter. You seem to be ahead of your time.\","
"                \"label\" : \"Thank you for booking with Cleartrip\","
"                \"key\" : \"ct_note\""
"             }"
"          ]"
"       },"
"       \"description\" : \"Demo pass\","
"       \"barcode\" : {"
"          \"message\" : \"Air India AI 142\","
"          \"format\" : \"PKBarcodeFormatPDF417\","
"          \"messageEncoding\" : \"iso-8859-1\""
"       },"
"       \"relevantDate\" : \"2013-12-28T13:55+05:30\","
"       \"logoText\" : \" \","
"       \"serialNumber\" : \"17158637\","
"       \"backgroundColor\" : \"rgb(255,204,0)\","
"       \"formatVersion\" : 1"
    "}";
    PassbookPackage passbook = parsePassbookDict(passDict);
    std::ostringstream outstream;
    boost::property_tree::json_parser::write_json(outstream, passbook.dict, true);
    const std::string outJson = outstream.str();

    const std::string expected =
"{\n"
"    \"organizationName\": \"Cleartrip\",\n"
"    \"teamIdentifier\": \"466YHJ5Q96\",\n"
"    \"passTypeIdentifier\": \"pass.cleartrip.ticket\",\n"
"    \"associatedStoreIdentifiers\": [\n"
"        \"531324961\"\n"
"    ],\n"
"    \"suppressStripShine\": \"1\",\n"
"    \"foregroundColor\": \"rgb(0,0,0)\",\n"
"    \"boardingPass\": {\n"
"        \"transitType\": \"PKTransitTypeAir\",\n"
"        \"backFields\": [\n"
"            {\n"
"                \"label\": \"DEL - MAA (Sat, 28 Dec 2013)\",\n"
"                \"key\": \"DEL\",\n"
"                \"value\": \"Terminal 3\\nAir India AI 142\\n13:55 - 16:45  (02h 50m)\"\n"
"            },\n"
"            {\n"
"                \"value\": \"01h 30m\",\n"
"                \"key\": \"DEL_layover\",\n"
"                \"label\": \"Layover (MAA)\"\n"
"            },\n"
"            {\n"
"                \"key\": \"MAA\",\n"
"                \"label\": \"MAA - GOI (Sat, 28 Dec 2013)\",\n"
"                \"value\": \"Terminal I\\nAir India AI 975\\n18:15 - 19:35  (01h 20m)\"\n"
"            },\n"
"            {\n"
"                \"value\": \"H4VTJ\",\n"
"                \"key\": \"PNR\",\n"
"                \"label\": \"PNR\"\n"
"            },\n"
"            {\n"
"                \"value\": \"Artem Zaika\\nKateryna Mordvynova\",\n"
"                \"label\": \"Travellers\",\n"
"                \"key\": \"pax\"\n"
"            },\n"
"            {\n"
"                \"value\": \"1308080477\",\n"
"                \"key\": \"trip_id\",\n"
"                \"label\": \"Trip ID\"\n"
"            },\n"
"            {\n"
"                \"value\": \"Support: 180030003004\\nNote: Unfortunately, this is NOT a boarding pass. You will have to collect that from the airline counter. You seem to be ahead of your time.\",\n"
"                \"label\": \"Thank you for booking with Cleartrip\",\n"
"                \"key\": \"ct_note\"\n"
"            }\n"
"        ]\n"
"    },\n"
"    \"description\": \"Demo pass\",\n"
"    \"barcode\": {\n"
"        \"message\": \"Air India AI 142\",\n"
"        \"format\": \"PKBarcodeFormatPDF417\",\n"
"        \"messageEncoding\": \"iso-8859-1\"\n"
"    },\n"
"    \"relevantDate\": \"2013-12-28T13:55+05:30\",\n"
"    \"logoText\": \" \",\n"
"    \"serialNumber\": \"17158637\",\n"
"    \"backgroundColor\": \"rgb(255,204,0)\",\n"
"    \"formatVersion\": \"1\"\n"
    "}\n";
    ASSERT_EQ(expected, outJson);
}


}
