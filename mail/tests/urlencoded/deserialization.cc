#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/serialization/urlencoded_writer.h>

#include "structs.h"


using namespace testing;

namespace yamail::data::serialization {

using namespace serialization::urlencoded;

#define P std::make_pair

template<class T>
std::multimap<std::string, std::string> write(T& params, BoolToString bts = boolToYesNo, bool throwOnEmptyPointer = false) {
    std::multimap<std::string, std::string> retval;
    const auto write = [&](const std::string& name, const std::string& val) {
        retval.insert({name, val});
    };

    urlencoded::DefaultOptions opts {
        .dumpValue=write,
        .bts=bts,
        .throwOnEmptyPointer=throwOnEmptyPointer
    };

    toUrlencodedWithOptions(params, std::move(opts));

    return retval;
}


TEST(WriteUsualCases, shouldWriteAllFields) {
    GeneralParams p;
    p.integer = 1;
    p.str = "str";
    p.dob = 1.2;
    p.uns = 2;
    p.stdOpt = std::make_optional<std::string>("stdOpt");
    p.boostOpt = boost::make_optional<std::string>("boostOpt");
    p.strs = {"strs1", "strs2"};
    p.boolean = true;

    const auto req = write(p);

    EXPECT_THAT(req, UnorderedElementsAre(
        P("integer", "1"), P("str", "str"), P("dob", "1.2"),
        P("uns", "2"), P("stdOpt", "stdOpt"), P("boostOpt", "boostOpt"),
        P("strs", "strs1"), P("strs", "strs2"), P("boolean", "yes")
    ));
}


TEST(WritePointerCases, shouldWrite) {
    WithPointers p;
    p.pDouble = std::make_shared<double>(3);
    p.pStr = boost::make_shared<std::string>("str");
    p.pTimet = boost::make_shared<WithTimeT>();
    p.pTimet->time = 1;

    const auto req = write(p);

    EXPECT_THAT(req, UnorderedElementsAre(
        P("pDouble", "3"), P("pStr", "str"), P("time", "1")
    ));
}

TEST(WritePointerCases, shouldNotThrowExceptionIfEmpty) {
    WithPointers p;

    const auto req = write(p, boolToYesNo, false);

    EXPECT_THAT(req, UnorderedElementsAre());
}

TEST(WritePointerCases, shouldNotThrowExceptionIfSpecialOptionIsSet) {
    WithRawPointer p;

    EXPECT_THROW(write(p, boolToYesNo, true), WriterException);
}

TEST(WritePointerCases, shouldNotWriteEmptyRawPointer) {
    WithRawPointer p;
    p.pInt = nullptr;

    const auto req = write(p);

    EXPECT_THAT(req, UnorderedElementsAre());
}

TEST(WritePointerCases, shouldWriteRawPointer) {
    WithRawPointer p;
    p.pInt = new int(0);

    const auto req = write(p);

    EXPECT_THAT(req, UnorderedElementsAre(P("pInt", "0")));
    delete p.pInt;
}



TEST(WriteBoolCases, shouldWriteBooleanParam) {
    WithBool p;
    p.bar = true;

    EXPECT_THAT(write(p), UnorderedElementsAre(P("bar", "yes")));

    p.bar = false;

    EXPECT_THAT(write(p), UnorderedElementsAre(P("bar", "no")));
}

TEST(WriteBoolCases, shouldWriteBoolParamWithCustomFuction) {
    const auto parser = [] (bool val) {
        return val ? "da" : "net";
    };

    WithBool p;
    p.bar = true;

    EXPECT_THAT(write(p, parser), UnorderedElementsAre(P("bar", "da")));

    p.bar = false;

    EXPECT_THAT(write(p, parser), UnorderedElementsAre(P("bar", "net")));
}


TEST(WriteOptionalCases, shouldWrite) {
    WithOptional p;
    p.optInt = std::make_optional<int>(1);

    EXPECT_THAT(write(p), UnorderedElementsAre(P("optInt", "1")));
}

TEST(WriteOptionalCases, shouldNotWriteIfEmpty) {
    WithOptional p;

    EXPECT_THAT(write(p), UnorderedElementsAre());
}

TEST(WriteOptionalCases, shouldWriteOptionalStruct) {
    NestedWithOptional p;
    p.flo = 1.0f;
    p.optStruct = std::make_optional<WithOptional>();
    p.optStruct->optInt = std::make_optional<int>(1);

    EXPECT_THAT(write(p), UnorderedElementsAre(P("flo", "1"), P("optInt", "1")));
}


TEST(WriteSequenceCases, shouldWrite) {
    WithSequences p;
    p.array = {2, 3};

    EXPECT_THAT(write(p), UnorderedElementsAre(P("array", "2"), P("array", "3")));
}


struct TestOptions {
    mutable Request reqGet;
    mutable Request reqHeader;
    mutable Request reqPost;
    mutable Request reqAnything;

    static constexpr bool throwOnEmptyPointer = false;

    template<class Value>
    void write(const std::string& name, const Value& val) const {
        Request* req;

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

        req->insert(std::make_pair(name, boost::lexical_cast<std::string>(val)));
    }
};

TEST(WriteTaggerTest, shouldWrite) {
    TestOptions opts;

    StructForTags p;
    p.header = "header";
    p.get = -12;
    p.post = 12u;
    p.untagged = true;

    toUrlencodedWithOptions(p, opts);

    EXPECT_THAT(opts.reqHeader,   UnorderedElementsAre(P("header", "header")));
    EXPECT_THAT(opts.reqGet,      UnorderedElementsAre(P("get", "-12")));
    EXPECT_THAT(opts.reqPost,     UnorderedElementsAre(P("post", "12")));
    EXPECT_THAT(opts.reqAnything, UnorderedElementsAre(P("untagged", "1")));
}

TEST(WriteSequenceWithTagsTest, shouldWrite) {
    TestOptions opts;

    BigStructWithTags p;
    StructForTags st;
    st.header = "header";
    st.get = -12;
    st.post = 12u;
    st.untagged = true;
    p.headers = {Header<std::string>("header1"), Header<std::string>("header2")};
    p.getPtr = std::make_shared<Get<std::string>>("getPtr");
    p.postOpt = std::make_optional<Post<std::string>>("postOpt");
    p.st = st;

    toUrlencodedWithOptions(p, opts);

    EXPECT_THAT(opts.reqHeader, UnorderedElementsAre(P("headers", "header1"), P("headers", "header2"), P("header", "header")));
    EXPECT_THAT(opts.reqGet, UnorderedElementsAre(P("get", "-12"), P("getPtr", "getPtr")));
    EXPECT_THAT(opts.reqPost, UnorderedElementsAre(P("post", "12"), P("postOpt", "postOpt")));
    EXPECT_THAT(opts.reqAnything, UnorderedElementsAre(P("untagged", "1")));
}

}
