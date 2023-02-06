#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/MetaParser.h>
#include <boost/lexical_cast.hpp>
#include "mime_part.h"
#include "recognizer_instance.h"

namespace {

using namespace testing;

TEST(MetaParser, process_xmlWithOnePart_addsItToMetaParts) {
    MetaParser parser{getRecognizer()};
    MetaParts metaParts;
    const std::string metainfo = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    "<message>"
    "<part id=\"1\" offset=\"100\" length=\"1000\""
    "   content_type.type=\"image\""
    "   content_type.subtype=\"gif\""
    "   content_type.charset=\"US-ASCII\""
    "   content_type.name=\"rrr.gif\""
    "   content_transfer_encoding=\"base64\""
    "   content_disposition.value=\"inline\""
    "   content_disposition.filename=\"91jf5dvh.gif\""
    "   content_id=\"cid91jf5dvh\""
    ">"
    "</part>"
    "</message>";

    parser.process(metainfo, &metaParts);

    auto& actual = metaParts["1"];
    auto expect = macs::MimePartFactory().hid("1").contentType("image").contentSubtype("gif")
                    .name("rrr.gif").charset("US-ASCII").encoding("base64")
                    .contentDisposition("inline").fileName("91jf5dvh.gif").cid("cid91jf5dvh")
                    .offsetBegin(100).offsetEnd(1100).release();

    EXPECT_EQ(actual, expect);
}

TEST(MetaParser, process_xmlWithInlineMessagePart_addsItToMetaParts) {
    MetaParser parser{getRecognizer()};
    MetaParts metaParts;
    const std::string metainfo = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    "<message>"
    "<part id=\"1\" offset=\"100\" length=\"1000\""
    "   content_type.type=\"message\""
    "   content_type.subtype=\"rfc822\""
    ">"
    "</part>"
    "</message>";

    parser.process(metainfo, &metaParts);

    auto& actual = metaParts["1"];
    auto expect = macs::MimePartFactory().hid("1").contentType("message").contentSubtype("rfc822")
                    .offsetBegin(100).offsetEnd(1100).release();

    EXPECT_EQ(actual, expect);
}

TEST(MetaParser, process_xmlWithPartWithoutOffsets_addsPartWithNullOffsetsToMetaParts) {
    MetaParser parser{getRecognizer()};
    MetaParts metaParts;
    const std::string metainfo = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    "<message>"
    "<part id=\"1\">"
    "</part>"
    "</message>";

    parser.process(metainfo, &metaParts);

    auto& actual = metaParts["1"];
    auto expect = macs::MimePartFactory().hid("1").release();

    EXPECT_EQ(actual, expect);
}

TEST(MetaParser, process_xmlWithPartWithBadFormattedOffsets_throwsException) {
    MetaParser parser{getRecognizer()};
    MetaParts metaParts;
    const std::string metainfo = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    "<message>"
    "<part id=\"1\" offset=\"a100\" length=\"1000\">"
    "</part>"
    "</message>";

    EXPECT_THROW(parser.process(metainfo, &metaParts), std::runtime_error);
}

TEST(MetaParser, process_xmlWithPartWithNegativeOffsets_throwsException) {
    MetaParser parser{getRecognizer()};
    MetaParts metaParts;
    const std::string metainfo = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
    "<message>"
    "<part id=\"1\" offset=\"100\" length=\"-1000\">"
    "</part>"
    "</message>";

    EXPECT_THROW(parser.process(metainfo, &metaParts), std::runtime_error);
}

}
