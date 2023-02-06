#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/range/algorithm/copy.hpp>
#include <boost/range/adaptor/map.hpp>

#include <yamail/data/deserialization/urlencoded_reader.h>

#include "structs.h"


using namespace testing;

using boost::fusion::operators::operator==;

namespace yamail::data::deserialization {

using boost::fusion::operators::operator<<;
using boost::fusion::operators::operator==;

using namespace deserialization::urlencoded;

template<class T>
auto get(const Request& req, T retval = T(), StringToBool stb = yesNoToString, bool init = false) {
    const auto oneArg = [&req](const std::string& name) -> std::optional<std::string> {
        if (auto iter = req.find(name); iter != req.end()) {
            return std::make_optional(iter->second);
        } else {
            return std::nullopt;
        }
    };

    const auto manyArgs = [&req](const std::string& name) -> std::optional<std::vector<std::string>> {
        std::vector<std::string> ret;
        boost::copy(req.equal_range(name)
                    | boost::adaptors::map_values
                    , std::back_inserter(ret));

        if (ret.empty()) {
            return std::nullopt;
        }

        return ret;
    };

    urlencoded::DefaultOptions<decltype(oneArg), decltype(manyArgs)> options {
        .getOne=oneArg, .getMany=manyArgs, .stringToBool=stb, .initEmptyRawPointer = init
    };

    fromUrlencodedWithOptions(retval, options);

    return retval;
}


TEST(ReadPointerCases, shouldParse) {
    Request req = {
        {"pDouble", "3.14"},
        {"pStr", "str"},
        {"time", "1"}
    };

    const auto params = get<WithPointers>(req);

    EXPECT_EQ(*params.pDouble, 3.14);
    EXPECT_EQ(*params.pStr, "str");
    EXPECT_EQ(params.pTimet->time, 1);
}

TEST(ReadPointerCases, shouldNotThrowExceptionIfEmpty) {
    Request req;

    const auto params = get<WithPointers>(req);

    EXPECT_EQ(params.pDouble, nullptr);
    EXPECT_EQ(params.pStr, nullptr);
    EXPECT_EQ(params.pTimet, nullptr);
}

TEST(ReadPointerCases, shouldThrowAnExceptionOnEmptyRawPointer) {
    Request req;

    EXPECT_THROW(get<WithRawPointer>(req), ReaderException);
}

TEST(ReadPointerCases, shouldNotThrowAnExceptionOnEmptyRawPointerWithSpectialOption) {
    Request req = {
        {"pInt", "1"}
    };

    const auto params = get<WithRawPointer>(req, WithRawPointer(), yesNoToString, true);

    EXPECT_EQ(*params.pInt, 1);

    delete params.pInt;
}

TEST(ReadPointerCases, shouldReadIntoRawPointer) {
    Request req = {
        {"pInt", "1"}
    };

    const auto ptr = std::make_unique<int>(0);
    WithRawPointer val;
    val.pInt = ptr.get();

    EXPECT_EQ(*get<WithRawPointer>(req, val).pInt, 1);
}


TEST(ReadBoolCases, shouldParsePositiveBooleanParam) {
    Request req = {
        {"bar", "yes"}
    };

    EXPECT_EQ(get<WithBool>(req).bar, true);
}

TEST(ReadBoolCases, shouldParseNegativeBooleanParam) {
    Request req = {
        {"bar", "no"}
    };

    EXPECT_EQ(get<WithBool>(req).bar, false);
}

TEST(ReadBoolCases, shouldThrowAnExceptionOnBadBooleanParam) {
    Request req = {
        {"bar", "da"}
    };

    EXPECT_THROW(get<WithBool>(req), BadCast);
}

TEST(ReadBoolCases, shouldParseBoolParamWithCustomFuction) {
    const auto parser = [] (const std::string& str, bool& val) {
        if (str == "da") {
            val = true;
        } else if (str == "net") {
            val = false;
        } else {
            return false;
        }
        return true;
    };

    Request req = {
        {"bar", "da"}
    };

    EXPECT_EQ(get<WithBool>(req, WithBool(), parser).bar, true);
}

TEST(ReadUsualCases, shouldParseAllFields) {
    Request req = {
        {"integer", "1"},
        {"str", "str"},
        {"dob", "1.2"},
        {"uns", "2"},
        {"stdOpt", "stdOpt"},
        {"boostOpt", "boostOpt"},
        {"strs", "strs1"},
        {"strs", "strs2"},
        {"boolean", "yes"}
    };

    const auto params = get<GeneralParams>(req);

    EXPECT_EQ(params.integer, 1);
    EXPECT_EQ(params.str, "str");
    EXPECT_EQ(params.dob, 1.2);
    EXPECT_EQ(params.uns, 2u);
    EXPECT_EQ(params.boolean, true);
    EXPECT_EQ(*params.stdOpt, "stdOpt");
    EXPECT_EQ(*params.boostOpt, "boostOpt");
    EXPECT_THAT(params.strs, UnorderedElementsAre("strs1", "strs2"));
}

TEST(ReadUsualCases, shouldThrowAnExcetionOnMissingRequiredParam) {
    Request req;

    EXPECT_THROW(get<WithInt>(req), NoSuchEntry);
}

TEST(ReadUnsignedCases, shouldThrowAnExcetionOnNegativeUnsignedParam) {
    Request req = { {"unsCh", "-1"} };
    EXPECT_THROW(get<WithUnsigned>(req), BadCast);
}

TEST(ReadUnsignedCases, shouldParse) {
    Request req = { {"unsCh", "1"} };
    EXPECT_EQ(get<WithUnsigned>(req).unsCh, static_cast<unsigned char>(1));
}

TEST(ReadUnsignedCases, shouldParseTimeTAsUnsignedAndThrowAnExceptionIfNegative) {
    Request req = { {"time", "-1"} };
    EXPECT_THROW(get<WithTimeT>(req), BadCast);
}

TEST(ReadOptionalCases, shouldParse) {
    Request req = { {"optInt", "1"} };
    EXPECT_EQ(get<WithOptional>(req).optInt, 1);
}

TEST(ReadOptionalCases, shouldReturnEmpty) {
    Request req = { {"optAnotherVar", "1"} };
    EXPECT_EQ(get<WithOptional>(req).optInt, std::nullopt);
}

TEST(ReadOptionalCases, shouldThrowOnBadCast) {
    Request req = { {"optInt", "qwer"} };
    EXPECT_THROW(get<WithOptional>(req), BadCast);
}

TEST(ReadOptionalCases, shouldParseOptionalStruct) {
    Request req = {
        {"flo", "1.2"},
        {"optInt", "1"},
    };
    const auto params = get<NestedWithOptional>(req);

    EXPECT_EQ(params.flo, 1.2f);
    EXPECT_EQ(params.optStruct->optInt.value(), 1);
}

TEST(ReadOptionalCases, shouldSetValueToTopLevelOptionalStruct) {
    Request req = {
        {"flo", "1.2"}
    };
    const auto params = get<NestedWithOptional>(req);

    EXPECT_EQ(params.flo, 1.2f);
    EXPECT_NE(params.optStruct, std::nullopt);
    EXPECT_EQ(params.optStruct->optInt, std::nullopt);
}

TEST(ReadOptionalCases, shouldSetNoneToOptionalStructIfThereAreSomeRequiredParamIsMissing) {
    Request req = {
        {"optInt", "1"}
    };
    const auto params = get<GeneralWithOptional>(req);

    EXPECT_EQ(params.nested, std::nullopt);
}

TEST(ReadSequenceCases, shouldParse) {
    Request req = {
        {"array", "2.71"},
        {"array", "3.14"}
    };

    const auto params = get<WithSequences>(req);

    EXPECT_THAT(params.array, UnorderedElementsAre(2.71, 3.14));
}

TEST(ReadSequenceCases, shouldThrowAnExceptionIfThereAreNoSequenceParams) {
    Request req;
    EXPECT_THROW(get<WithSequences>(req), NoSuchEntry);
}

struct TestOptions {
    Request reqGet;
    Request reqHeader;
    Request reqPost;
    Request reqAnything;

    static constexpr bool initEmptyRawPointer = false;

    bool stringToBool(const std::string& str, bool& val) const {
        return urlencoded::yesNoToString(str, val);
    }

    template<class Value>
    std::optional<std::string> one(const std::string& str) const {
        const Request* req;

        if constexpr (tagged_v<Value>) {
            if constexpr (Value::tag == Tag::get) {
                req = &reqGet;
            } else if constexpr (Value::tag == Tag::post) {
                req = &reqPost;
            } else if constexpr (Value::tag == Tag::header) {
                req = &reqHeader;
            }
        } else {
            req = &reqAnything;
        }

        if (auto iter = req->find(str); iter != req->end()) {
            return std::make_optional(iter->second);
        } else {
            return std::nullopt;
        }
    }

    template<class Value>
    std::optional<std::vector<std::string>> many(const std::string& name) const {
        const Request* req;

        if constexpr (tagged_v<Value>) {
            if constexpr (Value::tag == Tag::get) {
                req = &reqGet;
            } else if constexpr (Value::tag == Tag::post) {
                req = &reqPost;
            } else if constexpr (Value::tag == Tag::header) {
                req = &reqHeader;
            }
        } else {
            req = &reqAnything;
        }

        std::vector<std::string> ret;
        boost::copy(req->equal_range(name)
                    | boost::adaptors::map_values
                    , std::back_inserter(ret));

        if (ret.empty()) {
            return std::nullopt;
        }

        return ret;
    }
};

TEST(ReadTaggerTest, shouldParse) {
    TestOptions opts {
        .reqGet = {
            {"get", "-12"}
        },
        .reqHeader = {
            {"header", "header"}
        },
        .reqPost = {
            {"post", "12"}
        },
        .reqAnything = {
            {"untagged", "yes"}
        }
    };

    StructForTags params;
    fromUrlencodedWithOptions(params, opts);

    EXPECT_EQ(params.header, "header");
    EXPECT_EQ(params.get, -12);
    EXPECT_EQ(params.post, 12u);
    EXPECT_EQ(params.untagged, true);
}

TEST(ReadSequenceWithTagsTest, shouldParse) {
    TestOptions opts {
        .reqGet = {
            {"get", "-12"},
            {"getPtr", "getPtr"}
        },
        .reqHeader = {
            {"headers", "header1"},
            {"headers", "header2"},
            {"header", "header"}
        },
        .reqPost = {
            {"post", "12"},
            {"postOpt", "postOpt"}
        },
        .reqAnything = {
            {"untagged", "yes"}
        }
    };

    BigStructWithTags params;
    fromUrlencodedWithOptions(params, opts);

    EXPECT_THAT(params.headers, UnorderedElementsAre("header1", "header2"));
    EXPECT_EQ(*params.getPtr, "getPtr");
    EXPECT_EQ(*params.postOpt, "postOpt");
    EXPECT_EQ(params.st.header, "header");
    EXPECT_EQ(params.st.get, -12);
    EXPECT_EQ(params.st.post, 12u);
    EXPECT_EQ(params.st.untagged, true);
}

}
