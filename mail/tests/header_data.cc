#define BOOST_TEST_MODULE header_field
#include <tests_common.h>

#include <mimeparser/HeaderField.h>
#include <mimeparser/HeaderData.h>

BOOST_AUTO_TEST_SUITE(header_data)

BOOST_AUTO_TEST_CASE(decoded_headers_data)
{
    const string field="Content-Disposition: attachment; \n filename*0*=utf-8''%D0%98%D0%94%20%D0%A7%D1%83%D0%B4%D0%BE%D0%B2%D0%BE%2E; \n filename*1*=%70%64%66 \r\n";
    HeaderField headerField(field.begin(), field.end());
    HeaderData data(true);

    data.parseContentDisposition(headerField);
    BOOST_REQUIRE(data.content_disposition()=="attachment");
    BOOST_REQUIRE(data.filename().contents=="ИД Чудово.pdf");
}

BOOST_AUTO_TEST_CASE(raw_headers_data)
{
    const string field="Content-Disposition: attachment; \n filename*0*=utf-8''%D0%98%D0%94%20%D0%A7%D1%83%D0%B4%D0%BE%D0%B2%D0%BE%2E; \n filename*1*=%70%64%66 \r\n";
    HeaderField headerField(field.begin(), field.end());
    HeaderData data(true);
    data.setRawHeaderList({"filename"});

    data.parseContentDisposition(headerField);
    BOOST_REQUIRE(data.content_disposition()=="attachment");
    BOOST_REQUIRE(data.filename().contents=="utf-8''%D0%98%D0%94%20%D0%A7%D1%83%D0%B4%D0%BE%D0%B2%D0%BE%2E%70%64%66");
}

BOOST_AUTO_TEST_SUITE_END()
