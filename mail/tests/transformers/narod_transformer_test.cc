#include <library/cpp/testing/unittest/env.h>

#include <internal/transformers/narod_transformer.h>
#include <internal/transformer_attributes.h>
#include "content_type_detector_mock.h"
#include "alias_class_list_mock.h"

#include <stdexcept>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace ::testing;
using namespace msg_body::testing;

const TString workPath = GetWorkPath();
const TString languageDict = GetWorkPath() + "/queryrec.dict";
const TString languageWeights = GetWorkPath() + "/queryrec.weights";
const TString encodingDict = GetWorkPath() + "/dict.dict";

struct NarodTransformerTest : public Test {
    const ContentTypeDetectorMock contentTypeDetector = ContentTypeDetectorMock();
    const AliasClassListMock aliasClassList = AliasClassListMock();
    msg_body::NarodTransformer narodTrasformer{
        msg_body::TransformerAttributes(),
        contentTypeDetector,
        aliasClassList,
        *Recognizer::create(languageDict.data(), languageWeights.data(), encodingDict.data())};
    std::string fakeHid = "fakeHid";
};

TEST_F(NarodTransformerTest, for_url_with_hash_successful_returned_hash) {
    std::string html = R"(lalala&href="https://yadi.sk/mail/?hash=its_hash"\sdata\-preview="blablabla"lululu>kek (bububu))";
    std::string result = "its_hash";

    ASSERT_EQ(result, narodTrasformer.transformAttach(html, fakeHid)->hash);
}

TEST_F(NarodTransformerTest, for_url_with_hash_and_other_params_successful_returned_hash) {
    std::string html = R"(lalala&href="https://yadi.sk/mail/?hash=its_hash&not_hash=its_not_hash"\sdata\-preview="blablabla"lululu>kek (bububu))";
    std::string result = "its_hash";

    ASSERT_EQ(result, narodTrasformer.transformAttach(html, fakeHid)->hash);
}

TEST_F(NarodTransformerTest, for_missing_hash_in_url_returned_empty_hash) {
    std::string html = R"(lalala&href="https://yadi.sk/mail/?not_hash=its_not_hash"\sdata\-preview="blablabla"lululu>kek (bububu))";
    std::string result = "";

    ASSERT_EQ(result, narodTrasformer.transformAttach(html, fakeHid)->hash);
}

TEST_F(NarodTransformerTest, get_hash_from_data_attr) {
    std::string html = R"(<a href="https://yadi.sk/mail/?not_hash=its_not_hash" data-hash="hashfromdataattr">attach name (size01)</a>)";
    ASSERT_EQ("hashfromdataattr", narodTrasformer.transformAttach(html, fakeHid)->hash);
}

TEST_F(NarodTransformerTest, hash_from_data_must_overwrite_hash_from_url) {
    std::string html = R"(<a href="https://yadi.sk/mail/?hash=its_not_hash" data-preview="12345" data-hash="hashfromdataattr">attach name (size01)</a>)";
    ASSERT_EQ("hashfromdataattr", narodTrasformer.transformAttach(html, fakeHid)->hash);
}

}
