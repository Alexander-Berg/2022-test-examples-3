#include <gtest/gtest.h>

#include <mail/hound/include/internal/wmi/windat.h>
#include <yamail/data/serialization/json_writer.h>
#include "../mimes.h"

namespace {

using namespace testing;
using namespace hound;
using namespace macs;

TEST(MakeMimesTest, for_empty_should_return_empty) {
    const MidsWithMimes midStidMimeParts;
    const MidsWithMimes windatMidStidMimeParts;
    const Mimes result = makeMimes(midStidMimeParts, windatMidStidMimeParts);
    EXPECT_EQ(result, Mimes {});
}

TEST(MakeMimesTest, for_not_empty_mid_stid_mime_parts_without_mime_parts_should_return_root_for_mid_with_only_stid) {
    const Mid mid("42");
    const Stid stid("root.stid");
    const MidsWithMimes midStidMimeParts {std::make_tuple(mid, stid, MimeParts())};
    const MidsWithMimes windatMidStidMimeParts;
    const Mimes expected {
        {
            mid,
            MessageParts {
                RootMessagePart {
                    stid,
                    StidHidMimeParts()
                },
                StidMessageParts {}
            }
        }
    };
    const Mimes result = makeMimes(midStidMimeParts, windatMidStidMimeParts);
    EXPECT_EQ(result, expected);
}

TEST(MakeMimesTest, for_not_empty_mid_stid_mime_parts_should_return_not_empty_root) {
    const Mid mid("42");
    const Stid stid("root.stid");
    const Hid hid1("root.hid.1");
    const Hid hid2("root.hid.2");
    const MimePart mimePart1 = MimePartFactory().hid(hid1).release();
    const MimePart mimePart2 = MimePartFactory().hid(hid2).release();
    const MidsWithMimes midStidMimeParts {std::make_tuple(mid, stid, MimeParts({mimePart1, mimePart2}))};
    const MidsWithMimes windatMidStidMimeParts;
    const Mimes expected {
        {
            mid,
            MessageParts {
                RootMessagePart {
                    stid,
                    StidHidMimeParts({{hid1, mimePart1}, {hid2, mimePart2}})
                },
                StidMessageParts {}
            }
        }
    };
    const Mimes result = makeMimes(midStidMimeParts, windatMidStidMimeParts);
    EXPECT_EQ(result, expected);
}

TEST(MakeMimesTest, for_not_empty_windat_mid_stid_mime_parts_should_return_not_empty_other) {
    const Mid mid("42");
    const Stid stid1("root.stid.1");
    const Stid stid2("root.stid.2");
    const Hid hid1("other.hid.1");
    const Hid hid2("other.hid.2");
    const MimePart mimePart1 = MimePartFactory().hid(hid1).release();
    const MimePart mimePart2 = MimePartFactory().hid(hid2).release();
    const MidsWithMimes midStidMimeParts;
    const MidsWithMimes windatMidStidMimeParts {
        std::make_tuple(mid, stid1, MimeParts({mimePart1})),
        std::make_tuple(mid, stid2, MimeParts({mimePart2})),
    };
    const Mimes expected {
        {
            mid,
            MessageParts {
                RootMessagePart(),
                StidMessageParts {
                    {
                        stid1,
                        StidHidMessageParts {
                            {
                                hid1,
                                StidHidMessagePart {windatMulcaHid, mimePart1}
                            }
                        }
                    },
                    {
                        stid2,
                        StidHidMessageParts {
                            {
                                hid2,
                                StidHidMessagePart {windatMulcaHid, mimePart2}
                            }
                        }
                    }
                }
            }
        }
    };
    const Mimes result = makeMimes(midStidMimeParts, windatMidStidMimeParts);
    EXPECT_EQ(result, expected);
}

TEST(MakeMimesTest, for_not_empty_all_should_return_not_empty_root_and_other) {
    const Mid mid("42");
    const Stid rootStid("root.stid");
    const Stid otherStid("other.stid");
    const Hid rootHid("root.hid");
    const Hid otherHid("other.hid");
    const MimePart rootMimePart = MimePartFactory().hid(rootHid).release();
    const MimePart otherMimePart = MimePartFactory().hid(otherHid).release();
    const MidsWithMimes midStidMimeParts {std::make_tuple(mid, rootStid, MimeParts({rootMimePart}))};
    const MidsWithMimes windatMidStidMimeParts {std::make_tuple(mid, otherStid, MimeParts({otherMimePart}))};
    const Mimes expected {
        {
            mid,
            MessageParts {
                RootMessagePart {
                    rootStid,
                    StidHidMimeParts({{rootHid, rootMimePart}})
                },
                StidMessageParts {
                    {
                        otherStid,
                        StidHidMessageParts {
                            {
                                otherHid,
                                StidHidMessagePart {windatMulcaHid, otherMimePart}
                            }
                        }
                    }
                }
            }
        }
    };
    const Mimes result = makeMimes(midStidMimeParts, windatMidStidMimeParts);
    EXPECT_EQ(result, expected);
}

TEST(MimesReflectionTest, to_json_should_return_json) {
    using yamail::data::serialization::toJson;
    const Mid mid("42");
    const Stid rootStid("root.stid.1");
    const Stid otherStid1("other.stid.1");
    const Stid otherStid2("other.stid.2");
    const Hid rootHid1("root.hid.1");
    const Hid rootHid2("root.hid.2");
    const Hid otherHid1("other.hid.1");
    const Hid otherHid2("other.hid.2");
    const MimePart baseMimePart = MimePartFactory()
        .hid("hid")
        .contentType("content_type")
        .contentSubtype("content_subtype")
        .boundary("boundary")
        .name("name")
        .charset("charset")
        .encoding("encoding")
        .contentDisposition("content_disposition")
        .fileName("file_name")
        .cid("cid")
        .offsetBegin(1)
        .offsetEnd(2)
        .release();
    const MimePart rootMimePart1 = MimePartFactory(baseMimePart).hid(rootHid1).release();
    const MimePart rootMimePart2 = MimePartFactory(baseMimePart).hid(rootHid2).release();
    const MimePart otherMimePart1 = MimePartFactory(baseMimePart).hid(otherHid1).release();
    const MimePart otherMimePart2 = MimePartFactory(baseMimePart).hid(otherHid2).release();
    const Mimes mimes {
        {
            mid,
            MessageParts {
                RootMessagePart {
                    rootStid,
                    StidHidMimeParts({{rootHid1, rootMimePart1}, {rootHid2, rootMimePart2}})
                },
                StidMessageParts {
                    {
                        otherStid1,
                        StidHidMessageParts {
                            {
                                otherHid1,
                                StidHidMessagePart {windatMulcaHid, otherMimePart1}
                            }
                        }
                    },
                    {
                        otherStid2,
                        StidHidMessageParts {
                            {
                                otherHid2,
                                StidHidMessagePart {windatMulcaHid, otherMimePart2}
                            }
                        }
                    }
                }
            }
        }
    };
    const std::string expected(
        "{"
            "\"42\":{"
                "\"root\":{"
                    "\"stid\":\"root.stid.1\","
                    "\"mimeParts\":{"
                        "\"root.hid.1\":{"
                            "\"hid\":\"root.hid.1\","
                            "\"contentType\":\"content_type\","
                            "\"contentSubtype\":\"content_subtype\","
                            "\"boundary\":\"boundary\","
                            "\"name\":\"name\","
                            "\"charset\":\"charset\","
                            "\"encoding\":\"encoding\","
                            "\"contentDisposition\":\"content_disposition\","
                            "\"fileName\":\"file_name\","
                            "\"cid\":\"cid\","
                            "\"offsetBegin\":1,"
                            "\"offsetEnd\":2"
                        "},"
                        "\"root.hid.2\":{"
                            "\"hid\":\"root.hid.2\","
                            "\"contentType\":\"content_type\","
                            "\"contentSubtype\":\"content_subtype\","
                            "\"boundary\":\"boundary\","
                            "\"name\":\"name\","
                            "\"charset\":\"charset\","
                            "\"encoding\":\"encoding\","
                            "\"contentDisposition\":\"content_disposition\","
                            "\"fileName\":\"file_name\","
                            "\"cid\":\"cid\","
                            "\"offsetBegin\":1,"
                            "\"offsetEnd\":2"
                        "}"
                    "}"
                "},"
                "\"other\":{"
                    "\"other.stid.1\":{"
                        "\"other.hid.1\":{"
                            "\"stidHid\":\"1\","
                            "\"mimePart\":{"
                                "\"hid\":\"other.hid.1\","
                                "\"contentType\":\"content_type\","
                                "\"contentSubtype\":\"content_subtype\","
                                "\"boundary\":\"boundary\","
                                "\"name\":\"name\","
                                "\"charset\":\"charset\","
                                "\"encoding\":\"encoding\","
                                "\"contentDisposition\":\"content_disposition\","
                                "\"fileName\":\"file_name\","
                                "\"cid\":\"cid\","
                                "\"offsetBegin\":1,"
                                "\"offsetEnd\":2"
                            "}"
                        "}"
                    "},"
                    "\"other.stid.2\":{"
                        "\"other.hid.2\":{"
                            "\"stidHid\":\"1\","
                            "\"mimePart\":{"
                                "\"hid\":\"other.hid.2\","
                                "\"contentType\":\"content_type\","
                                "\"contentSubtype\":\"content_subtype\","
                                "\"boundary\":\"boundary\","
                                "\"name\":\"name\","
                                "\"charset\":\"charset\","
                                "\"encoding\":\"encoding\","
                                "\"contentDisposition\":\"content_disposition\","
                                "\"fileName\":\"file_name\","
                                "\"cid\":\"cid\","
                                "\"offsetBegin\":1,"
                                "\"offsetEnd\":2"
                            "}"
                        "}"
                    "}"
                "}"
            "}"
        "}"
    );
    const auto result = toJson(mimes).str();
    EXPECT_EQ(result, expected);
}

} // namespace
