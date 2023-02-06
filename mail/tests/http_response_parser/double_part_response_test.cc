#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail_getter/http_response_parser.h>

#include <boost/algorithm/string/erase.hpp>

using namespace mail_getter::http;
using mail_getter::parseHttpResponse;
using namespace testing;

struct DataTypeFromDoublePartResponseTest : public Test {
    DataTypeFromDoublePartResponseTest()
            : dataForFirstPart( "[[22,10,2],[50,10,2],[74,31,1],[118,33,3]]" )
            , mimeForFirstPart( "application/json; charset=utf-8" )
            , dataForSecondPart( "<div><div background=\"cid:bg.png\"></div><img src=\"cid:bg.png\" />"
                "<img src=\"http://somehost.com/someimg.jpg\" />"
                "<a href=\"http://somehost.com/somelink.html\"></a></div>" )
            , mimeForSecondPart( "text/html; charset=utf-8" )
            , boundary( "c2FuaXRpemUtcmVwbHktYm91bmRhcnkK" )
            , mime( "multipart/mixed; boundary=" + boundary )
    {}

    void formResponse() {
        response = ("\r\n" "--" + boundary + "\r\n"
                "Content-Type: " + mimeForFirstPart.toString() + "\r\n"
                "\r\n"
                + dataForFirstPart + "\r\n"
                "--" + boundary + "\r\n"
                "Content-Type: " + mimeForSecondPart.toString() + "\r\n"
                "\r\n"
                + dataForSecondPart + "\r\n"
                "--" + boundary + "--");
    }

    const DataPart& getRes() {
        if( response.empty() ) {
            formResponse();
        }
        return res = parseHttpResponse( response, mime.toString() );
    }

    const MultipartData& data() {
        return getRes().getMultipartData();
    }
    const DataPart& firstPart() {
        return data().parts.at(0);
    }
    const DataPart& secondPart() {
        return data().parts.at(1);
    }

    std::string dataForFirstPart;
    MimeType mimeForFirstPart;

    std::string dataForSecondPart;
    MimeType mimeForSecondPart;

    std::string boundary;

    std::string response;

    MimeType mime;
    DataPart res;
};

TEST_F(DataTypeFromDoublePartResponseTest, isPlain_returnsFalse) {
    ASSERT_FALSE( getRes().isPlain() );
}

TEST_F(DataTypeFromDoublePartResponseTest, isMultipart_returnsTrue) {
    ASSERT_TRUE( getRes().isMultipart() );
}

TEST_F(DataTypeFromDoublePartResponseTest, getPlainData_throwsIsNotPlainData) {
    ASSERT_THROW( getRes().getPlainData(), IsNotPlainData );
}

TEST_F(DataTypeFromDoublePartResponseTest, getContentType_returnInitMime) {
    ASSERT_EQ( mime, getRes().getContentType() );
}

TEST_F(DataTypeFromDoublePartResponseTest, multipartData_sizeEqualsTwo) {
    ASSERT_EQ( data().parts.size(), 2u );
}

TEST_F(DataTypeFromDoublePartResponseTest, firstPart_mimeEqualsInit) {
    ASSERT_EQ( mimeForFirstPart, firstPart().getContentType() );
}

TEST_F(DataTypeFromDoublePartResponseTest, firstPart_plainDataEqualsInit) {
    ASSERT_EQ( dataForFirstPart, firstPart().getPlainData() );
}

TEST_F(DataTypeFromDoublePartResponseTest, secondPart_mimeEqualsInit) {
    ASSERT_EQ( mimeForSecondPart, secondPart().getContentType() );
}

TEST_F(DataTypeFromDoublePartResponseTest, secondPart_plainDataEqualsInit) {
    ASSERT_EQ( dataForSecondPart, secondPart().getPlainData() );
}

TEST_F(DataTypeFromDoublePartResponseTest, doubleNewLine_doNotTrim) {
    dataForSecondPart.append("\r\n\r\nSomeData");
    ASSERT_EQ( dataForSecondPart, secondPart().getPlainData() );
}

TEST_F(DataTypeFromDoublePartResponseTest, noContentType_ThrowInvalidHttpResponse) {
    formResponse();
    boost::erase_first(response, "Content-Type: " + mimeForFirstPart.toString() + "\r\n");
    ASSERT_THROW( getRes(), InvalidHttpResponse );
}
