#include <parser/request.h>
#include <parser/body.h>
#include <validator.h>
#include <boost/thread.hpp>
#include <catch.hpp>

ymod_webserver::context_ptr default_ctx(new ymod_webserver::context);

void verify(ymod_webserver::request_ptr req)
{
    REQUIRE(req->url.params.find("p")->second == "2238.2240495528094");
    REQUIRE(req->content.boundary == "----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3");
    REQUIRE(std::string(req->body.begin(), req->body.end()) == "This is multipart test");
    REQUIRE(!req->childs.empty());
    REQUIRE(req->childs.front().headers["content-disposition"] == "form-data; name=\"Filename\"");
    REQUIRE(
        req->childs.back().headers["content-disposition"] ==
        "form-data; name=\"attachment\"; filename=\"dogovor.doc\"");
    REQUIRE(req->childs.back().headers["content-type"] == "application/octet-stream");
    REQUIRE(
        std::string(req->childs.front().body.begin(), req->childs.front().body.end()) ==
        "dogovor.doc");
    REQUIRE(std::string(req->childs.back().body.begin(), req->childs.back().body.end()) == "Test");
}

TEST_CASE("body_multipart")
{
    yplatform::zerocopy::streambuf buffer;
    std::ostream stream(&buffer);

    stream << "POST /api/upload_attachment.xml?p=2238.2240495528094 HTTP/1.1\r\n"
              "Accept: text/*\r\n"
              "Content-Type: mUlTiPaRt/fOrM-dAtA; "
              "bOuNdArY=----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3\r\n"
              "User-Agent: Shockwave Flash\r\n"
              "Host: upload.mail.yandex.net\r\n"
              "Content-Length: 133062\r\n"
              "Connection: Keep-Alive\r\n"
              "Cache-Control: no-cache\r\n"
              "\r\n"
              "This is multipart test\r\n"
              "------------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3\r\n"
              "Content-Disposition: form-data; name=\"Filename\"\r\n"
              "\r\n"
              "dogovor.doc\r\n"
              "------------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3\r\n"
              "Content-Disposition: form-data; name=\"attachment\"; filename=\"dogovor.doc\"\r\n"
              "Content-Type: application/octet-stream\r\n"
              "\r\n"
              "Test\r\n"
              "------------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3--\r\n";

    ymod_webserver::parser::request_parser<yplatform::zerocopy::streambuf::iterator> parser;
    parser.reset(default_ctx);
    yplatform::zerocopy::streambuf::iterator i_start = buffer.begin();
    parser(buffer.begin(), i_start, buffer.end());
    ymod_webserver::validator v;
    v(ymod_webserver::endpoint(), parser.req());
    buffer.detach(i_start);
    parser.req()->raw_body = buffer.detach(buffer.end());
    ymod_webserver::parser::parse_body(parser.req());
    verify(parser.req());
}