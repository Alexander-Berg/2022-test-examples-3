#include <gtest/gtest.h>
#include <server_info.h>

using namespace ymod_smtpclient;
using namespace testing;

TEST(parse_smtp_server_extensions, parseBoolParams) {
    MultiLineResponse resp;
    resp.dataLines = {"Unused 1", "PIPELINING", "Dsn", " CHUnKING ", "STARTtls\t", " Unused Line"};

    auto ext = parse_smtp_server_extensions(resp);
    EXPECT_TRUE(ext.pipelining);
    EXPECT_TRUE(ext.chunking);
    EXPECT_TRUE(ext.dsn);
    EXPECT_TRUE(ext.starttls);

    resp.dataLines.push_back("\t\tEnHANCEDSTATUScODEs ");
    ext = parse_smtp_server_extensions(resp);
    EXPECT_TRUE(ext.enhancedStatusCodes);

    resp.dataLines.push_back("\t\tSmtpUtf8");
    ext = parse_smtp_server_extensions(resp);
    EXPECT_TRUE(ext.utf8Enable);
}

TEST(parse_smtp_server_extensions, parseMaxMessageSize) {
    MultiLineResponse resp;
    resp.dataLines = {" Size   1234567 "};

    auto ext = parse_smtp_server_extensions(resp);
    ASSERT_TRUE(ext.maxSize.is_initialized());
    EXPECT_EQ(*ext.maxSize, 1234567UL);

    resp.dataLines = {"SIZE"};
    ext = parse_smtp_server_extensions(resp);
    EXPECT_FALSE(ext.maxSize.is_initialized());

    resp.dataLines = {"\r\n\t SIZE \r\n"};
    ext = parse_smtp_server_extensions(resp);
    EXPECT_FALSE(ext.maxSize.is_initialized());
}

TEST(parse_smtp_server_extensions, parseMaxMessageSizeInvalid) {
    MultiLineResponse resp;

    resp.dataLines = {" Size 1234567asdf"};
    EXPECT_THROW(parse_smtp_server_extensions(resp), std::runtime_error);

    resp.dataLines = {"Size 100500 200300"};
    EXPECT_THROW(parse_smtp_server_extensions(resp), std::runtime_error);
}

TEST(parse_smtp_server_extensions, parseAuthMechanisms) {
    MultiLineResponse resp;
    resp.dataLines = {"AUTH Login UNKNOWN"};

    auto ext = parse_smtp_server_extensions(resp);
    EXPECT_TRUE(ext.authMechanisms.count(sasl::Mechanism::Login));
    EXPECT_FALSE(ext.authMechanisms.count(sasl::Mechanism::Plain));
    EXPECT_FALSE(ext.authMechanisms.count(sasl::Mechanism::Xoauth2));

    resp.dataLines.push_back("AUTH NoSuch PLAIN");
    ext = parse_smtp_server_extensions(resp);

    EXPECT_TRUE(ext.authMechanisms.count(sasl::Mechanism::Login));
    EXPECT_TRUE(ext.authMechanisms.count(sasl::Mechanism::Plain));
    EXPECT_FALSE(ext.authMechanisms.count(sasl::Mechanism::Xoauth2));

    resp.dataLines.push_back("AUTH Xoauth2");
    ext = parse_smtp_server_extensions(resp);

    EXPECT_TRUE(ext.authMechanisms.count(sasl::Mechanism::Login));
    EXPECT_TRUE(ext.authMechanisms.count(sasl::Mechanism::Plain));
    EXPECT_TRUE(ext.authMechanisms.count(sasl::Mechanism::Xoauth2));
}
