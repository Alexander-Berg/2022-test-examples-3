#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/http_response_parser.h>

using namespace mail_getter::http;
using mail_getter::parseHttpResponse;
using namespace testing;

struct DataTypeFromPlainStringResponseTest : public Test {
    DataTypeFromPlainStringResponseTest()
            : response( "SomePlainString" )
            , mime( "text/html; charset=utf-8" )
            , res( parseHttpResponse( response, mime.toString() ) ) {
    }
    std::string response;
    MimeType mime;
    DataPart res;
};

TEST_F(DataTypeFromPlainStringResponseTest, isPlain_returnsTrue) {
    ASSERT_TRUE( res.isPlain() );
}

TEST_F(DataTypeFromPlainStringResponseTest, isMultipart_returnsFalse) {
    ASSERT_FALSE( res.isMultipart() );
}

TEST_F(DataTypeFromPlainStringResponseTest, getPlainData_returnsInitResponse) {
    ASSERT_EQ( response, res.getPlainData() );
}

TEST_F(DataTypeFromPlainStringResponseTest, getMulripartData_throwsIsNotMultipartData) {
    ASSERT_THROW( res.getMultipartData(), IsNotMultipartData );
}

TEST_F(DataTypeFromPlainStringResponseTest, getContentType_returnInitMime) {
    ASSERT_EQ( mime, res.getContentType() );
}
