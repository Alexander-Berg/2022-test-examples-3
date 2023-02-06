#include <ymod_smtpserver/commands.h>
#include <gtest/gtest.h>

#include <parser/command.h>
#include <parser/line.h>

#include <boost/algorithm/string.hpp>

namespace {

using namespace ymod_smtpserver;
using namespace testing;

//-------------------- Test EHLO/LHLO --------------------

TEST(ParseEHLO, DomainAddress) {
    std::string ehlo = "Ehlo domain-12345\r\n";
    parse_command(ehlo.begin(), ehlo.end(), [](Command cmd) {
        auto real = commands::Ehlo("domain-12345");
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Ehlo>(cmd).name, real.name);
    });
    ehlo = "Ehlo domain-12345.domain.domain2.domain-3\r\n";
    parse_command(ehlo.begin(), ehlo.end(), [](Command cmd) {
        auto real = commands::Ehlo("domain-12345.domain.domain2.domain-3");
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Ehlo>(cmd).name, real.name);
    });
}

TEST(ParseEHLO, DomainAddressWithSpecialSymbols) {
    std::string ehlo = "Ehlo domain-w!th-[different]-CHARS123==very_different#@!\r\n";
    parse_command(ehlo.begin(), ehlo.end(), [&](Command cmd) {
        auto real = commands::Ehlo("domain-w!th-[different]-CHARS123==very_different#@!");
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Ehlo>(cmd).name, real.name);
    });
}

TEST(ParseEHLO, InvalidFormat) {
    // command without EOF in the end
    std::string ehlo = "Ehlo domain-without-eof";
    parse_command(ehlo.begin(), ehlo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(ehlo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // command value consists of several (>1) parts
    ehlo = "Ehlo chunk1 chunk2\r\n";
    parse_command(ehlo.begin(), ehlo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(ehlo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // non-ascii symbols in domain
    ehlo = "Ehlo не-аски\r\n";
    parse_command(ehlo.begin(), ehlo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(ehlo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // non-ascii symbols in general-address
    ehlo = "Ehlo [tag:не-аски]\r\n";
    parse_command(ehlo.begin(), ehlo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(ehlo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
}

//-------------------- Test HELO --------------------

TEST(ParseHELO, DomainAddress) {
    std::string helo = "Helo domain-12345\r\n";
    parse_command(helo.begin(), helo.end(), [](Command cmd) {
        auto real = commands::Helo("domain-12345");
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Helo>(cmd).name, real.name);
    });
    helo = "Helo domain-12345.domain.domain2.domain-3\r\n";
    parse_command(helo.begin(), helo.end(), [](Command cmd) {
        auto real = commands::Helo("domain-12345.domain.domain2.domain-3");
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Helo>(cmd).name, real.name);
    });
}

TEST(ParseHELO, DomainAddressWithSpecialSymbols) {
    std::string helo = "Helo domain-w!th-different-CHARS123==very_different#@!\r\n";
    parse_command(helo.begin(), helo.end(), [&](Command cmd) {
        auto real = commands::Helo("domain-w!th-different-CHARS123==very_different#@!");
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Helo>(cmd).name, real.name);
    });
}

TEST(ParseHELO, InvalidFormat) {
    // command without EOF in the end
    std::string helo = "Helo domain-without-eof";
    parse_command(helo.begin(), helo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(helo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // command value consists of several (>1) parts
    helo = "Helo chunk1 chunk2\r\n";
    parse_command(helo.begin(), helo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(helo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // non-ascii symbols in domain
    helo = "Helo не-аски\r\n";
    parse_command(helo.begin(), helo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(helo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // non-ascii symbols in general-address
    helo = "Helo [tag:не-аски]\r\n";
    parse_command(helo.begin(), helo.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(helo));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
}

//-------------------- Test MAIL FROM --------------------

TEST(ParseMAIL_FROM, OnlyMailBox) {
    // parse empty mailbox
    std::string mailfrom = "Mail From:<>\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains email addr
    mailfrom = "Mail From:<example@yandex.ru>\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains email addr and spaces inside brackets
    mailfrom = "Mail From: <     example@yandex.ru    >\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains email addr without backets
    mailfrom = "Mail From:     example@yandex.ru    \r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains email addr without backets and with \r\n immediately at the end
    mailfrom = "Mail From:     example@yandex.ru\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains any printable characters
    mailfrom = "Mail From:<adf;__#$%^&*()!:,>\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("adf;__#$%^&*()!:,");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains non-ascii symbols
    mailfrom = "Mail From:<юзер@яндекс.рф>\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("юзер@яндекс.рф");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
}

TEST(ParseMAIL_FROM, MailBoxWithEsmtpParams) {
    // parse one esmtp pair
    std::string mailfrom = "Mail From:<mailbox> param-1=!@#$%^&*()0123456789abcdEFG-\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("mailbox", commands::Params{
            {"param-1", "!@#$%^&*()0123456789abcdEFG-"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse several pairs
    mailfrom = "Mail From:<mailbox> key-1=value-1  key-2=value-2  key-3=value-3\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("mailbox", commands::Params{
            {"key-1", "value-1"}, {"key-2", "value-2"}, {"key-3", "value-3"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse several pairs with spaces inside brackets
    mailfrom = "Mail From: <  mailbox   > key-1=value-1  key-2=value-2  key-3=value-3\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("mailbox", commands::Params{
            {"key-1", "value-1"}, {"key-2", "value-2"}, {"key-3", "value-3"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse several pairs with spaces and without brackets
    mailfrom = "Mail From: mailbox@ya.ru   key-1=value-1  key-2=value-2  key-3=value-3\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("mailbox@ya.ru", commands::Params{
            {"key-1", "value-1"}, {"key-2", "value-2"}, {"key-3", "value-3"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse only key
    mailfrom = "Mail From:<mailbox> key-1=value-1  only-key-param\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [](Command cmd) {
        auto real = commands::MailFrom("mailbox", commands::Params{
            {"key-1", "value-1"}, {"only-key-param", ""}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::MailFrom>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
}

TEST(ParseMAIL_FROM, InvalidMailFrom) {
    // mailbox contains '<'
    std::string mailfrom = "Mail From:<ma<il@ya.ru>\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(mailfrom));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // mailbox contains '>'
    mailfrom = "Mail From:<mail@y>a.ru>\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(mailfrom));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // invalid key in esmtp params
    mailfrom = "Mail From:<mail@ya.ru> key_contains_invalid_chars?=value\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(mailfrom));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // No second closing bracket
    mailfrom = "Mail From:<mail@ya.ru key=value\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(mailfrom));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // invalid value in esmtp params
    mailfrom = "Mail From:<mail@ya.ru> key=value=\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(mailfrom));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // command without eof
    mailfrom = "Mail From:<mail@ya.ru> key=value";
    parse_command(mailfrom.begin(), mailfrom.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(mailfrom));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // non-ascii symbols in parameters
    mailfrom = "Mail From:<mail@ya.ru> key=вэлью\r\n";
    parse_command(mailfrom.begin(), mailfrom.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(mailfrom));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
}

//-------------------- Test RCPT TO --------------------

TEST(ParseRCPT_TO, OnlyMailBox) {
    // parse mailbox which contains email addr
    std::string rcpt = "Rcpt To:<example@yandex.ru>\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox with spaces inside brackets
    rcpt = "Rcpt To: <       example@yandex.ru >\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox with spaces and without brackets
    rcpt = "Rcpt To:       example@yandex.ru \r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox with spaces and without brackets and with \r\n immediately at the end
    rcpt = "Rcpt To:       example@yandex.ru\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("example@yandex.ru");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains any characters
    rcpt = "Rcpt to:<adf;__#$%^&*()!:,>\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("adf;__#$%^&*()!:,");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
    // parse mailbox which contains non-ascii symbols
    rcpt = "Rcpt to:<реципиент@тут.бел>\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("реципиент@тут.бел");
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_TRUE(parsed.params.empty());
    });
}

TEST(ParseRCPT_TO, MailBoxWithEsmtpParams) {
    // parse one esmtp pair
    std::string rcpt = "Rcpt To:<mailbox> param-1=!@#$%^&*()0123456789abcdEFG-\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("mailbox", commands::Params{
            {"param-1", "!@#$%^&*()0123456789abcdEFG-"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse several pairs
    rcpt = "Rcpt To:<mailbox> notify=failure,success key-2=value-2  key-3=value-3\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("mailbox", commands::Params{
            {"notify", "failure,success"}, {"key-2", "value-2"}, {"key-3", "value-3"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse several pairs when mailbox have spaces inside brackets
    rcpt = "Rcpt To: <       mailbox   >   notify=failure,success key-2=value-2  key-3=value-3\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("mailbox", commands::Params{
            {"notify", "failure,success"}, {"key-2", "value-2"}, {"key-3", "value-3"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse several pairs when mailbox have spaces and without brackets
    rcpt = "Rcpt To: mailbox@ya.ru     notify=failure,success key-2=value-2  key-3=value-3\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("mailbox@ya.ru", commands::Params{
            {"notify", "failure,success"}, {"key-2", "value-2"}, {"key-3", "value-3"}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
    // parse only key
    rcpt = "Rcpt To:<mailbox> key-1=value-1  only-key-param\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [](Command cmd) {
        auto real = commands::RcptTo("mailbox", commands::Params{
            {"key-1", "value-1"}, {"only-key-param", ""}});
        ASSERT_EQ(cmd.which(), Command(real).which());
        auto parsed = boost::get<commands::RcptTo>(cmd);
        EXPECT_EQ(parsed.addr, real.addr);
        EXPECT_EQ(parsed.params, real.params);
    });
}

TEST(ParseRCPT_TO, InvalidRcptTo) {
    // empty mailbox
    std::string rcpt = "Rcpt To:<>\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(rcpt));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // mailbox contains '<>'
    rcpt = "Rcpt To:<m<ail@y>a.ru>\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(rcpt));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // invalid key in esmtp params
    rcpt = "Rcpt To:<mail@ya.ru> key_contains_invalid_chars?=value\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(rcpt));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // No second closing bracket
    rcpt = "Rcpt TO:<mail@ya.ru key=value\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(rcpt));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // invalid value in esmtp params
    rcpt = "Rcpt TO:<mail@ya.ru> key=value=\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(rcpt));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // command without eof
    rcpt = "Rcpt TO:<mail@ya.ru> key=value";
    parse_command(rcpt.begin(), rcpt.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(rcpt));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
    // non-ascii symbols in parameters
    rcpt = "RCPT TO:<mail@ya.ru> key=вэлью\r\n";
    parse_command(rcpt.begin(), rcpt.end(), [&](Command cmd) {
        auto real = commands::SyntaxError(boost::make_iterator_range(rcpt));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::SyntaxError>(cmd).ctx, real.ctx);
    });
}

//-------------------- Test DATA/QUIT/RSET/STARTTLS/AUTH --------------------

TEST(ParseDATA, CommandWithSymbolsInUpperAndLowerCases) {
    std::string data = "DaTa\r\n";
    parse_command(data.begin(), data.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Data()).which());
    });
    // with spaces in the end
    data = "DATA  \r\n";
    parse_command(data.begin(), data.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Data()).which());
    });
}

TEST(ParseQUIT, CommandWithSymbolsInUpperAndLowerCases) {
    std::string quit = "qUiT\r\n";
    parse_command(quit.begin(), quit.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Quit()).which());
    });
    // with spaces in the end
    quit = "QUIT   \t \r\n";
    parse_command(quit.begin(), quit.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Quit()).which());
    });
}

TEST(ParseRSET, CommandWithSymbolsInUpperAndLowerCases) {
    std::string rset = "Rset\r\n";
    parse_command(rset.begin(), rset.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Rset()).which());
    });
    // with spaces in the end
    rset = "RSET   \r\n";
    parse_command(rset.begin(), rset.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Rset()).which());
    });
}

TEST(ParseSTARTTLS, CommandWithSymbolsInUpperAndLowerCases) {
    std::string starttls = "STARTTlS\r\n";
    parse_command(starttls.begin(), starttls.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::StartTls()).which());
    });
    // with spaces in the end
    starttls = "STARTTLS   \r\n";
    parse_command(starttls.begin(), starttls.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::StartTls()).which());
    });
}

TEST(ParseSTARTTLS, CommandWithoutEOF) {
    std::string starttls = "STARTTLS";
    parse_command(starttls.begin(), starttls.end(), [&](Command cmd) {
        auto real = commands::Unknown(boost::make_iterator_range(starttls));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Unknown>(cmd).ctx, real.ctx);
    });
}

TEST(ParseNOOP, CommandWithSymbolsInUpperAndLowerCases) {
    std::string noop = "NOOP\r\n";
    parse_command(noop.begin(), noop.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Noop()).which());
    });
    // with spaces in the end
    noop = "nOoP   \r\n";
    parse_command(noop.begin(), noop.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Noop()).which());
    });
}

TEST(ParseNOOP, CommandWithUselessString) {
    std::string noop = "NOOP UseLess Staff\r\n";
    parse_command(noop.begin(), noop.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Noop()).which());
    });
    // with spaces in the end
    noop = "nOoP useless stuff \r\n";
    parse_command(noop.begin(), noop.end(), [](Command cmd) {
        EXPECT_EQ(cmd.which(), Command(commands::Noop()).which());
    });
}

struct TTestAuthDifferentTypes : TestWithParam<std::tuple<std::string, commands::AuthMethod>> {
};

TEST_P(TTestAuthDifferentTypes, BasicCase) {
    const auto& methodStr = std::get<0>(GetParam());
    const auto& methodShouldBe = std::get<1>(GetParam());
    std::string cmdText = "AuTh   " + methodStr + "\r\n";
    parse_command(cmdText.begin(), cmdText.end(), [=](Command cmd) {
        auto real = commands::Auth(methodShouldBe, std::string{});
        ASSERT_EQ(cmd.which(), Command(real).which());
        ASSERT_EQ(boost::get<commands::Auth>(cmd).Method.which(), methodShouldBe.which());
        ASSERT_EQ(boost::get<commands::Auth>(cmd).InitialResponse, std::string{});
    });
}

INSTANTIATE_TEST_SUITE_P(AuthTestBasicCase, TTestAuthDifferentTypes,
    Values(
        std::make_tuple("lOgIn", commands::AuthMethods::Login{}),
        std::make_tuple("PLaIn", commands::AuthMethods::Plain{}),
        std::make_tuple("  PLaIn  ", commands::AuthMethods::Plain{}),
        std::make_tuple("XoauTh2", commands::AuthMethods::XOAuth2{}),
        std::make_tuple("  XoauTh2  \t", commands::AuthMethods::XOAuth2{}),
        std::make_tuple("Smth", commands::AuthMethods::NotSupported{}),
        std::make_tuple("Smth  ", commands::AuthMethods::NotSupported{})
    )
);

struct TTestAuthDifferentTypesWithInitialResponse : TestWithParam<
    std::tuple<std::string, std::string, std::string, commands::AuthMethod>> {
};

TEST_P(TTestAuthDifferentTypesWithInitialResponse, BasicCaseWithInitialValue) {
    const auto& methodStr = std::get<0>(GetParam());
    const auto& initialValue = std::get<1>(GetParam());
    const auto& initialValueShoudBe = std::get<2>(GetParam());
    const auto& methodShouldBe = std::get<3>(GetParam());
    std::string cmdText = "AuTh   " + methodStr + "    " + initialValue + "\r\n";
    parse_command(cmdText.begin(), cmdText.end(), [=](Command cmd) {
        auto real = commands::Auth(methodShouldBe, std::string{});
        ASSERT_EQ(cmd.which(), Command(real).which());
        ASSERT_EQ(boost::get<commands::Auth>(cmd).Method.which(), real.Method.which());
        ASSERT_EQ(boost::get<commands::Auth>(cmd).InitialResponse, initialValueShoudBe);
    });
}

INSTANTIATE_TEST_SUITE_P(AuthDifferentTypesWithInitialResponse, TTestAuthDifferentTypesWithInitialResponse,
    Values(
        std::make_tuple("lOgIn", "Zm9vYmFy", "Zm9vYmFy", commands::AuthMethods::Login{}),
        std::make_tuple("PLaIn", "Zm9vYmFy", "Zm9vYmFy", commands::AuthMethods::Plain{}),
        std::make_tuple("XoauTh2", "Zm9vYmFy", "Zm9vYmFy", commands::AuthMethods::XOAuth2{}),
        std::make_tuple("Smth", "Zm9vYmFy", "Zm9vYmFy", commands::AuthMethods::NotSupported{}),
        std::make_tuple("Smth", "  Zm9vYmFy   ", "Zm9vYmFy", commands::AuthMethods::NotSupported{}),
        std::make_tuple("Smth", "  Zm9vYmFy\t", "Zm9vYmFy", commands::AuthMethods::NotSupported{})
    )
);

//-------------------- Test unknown command --------------------

TEST(ParseUnknownCommand, Unknown) {
    // unknown command
    std::string unknown = "UnknownCommand\r\n";
    parse_command(unknown.begin(), unknown.end(), [&](Command cmd) {
        auto real = commands::Unknown(boost::make_iterator_range(unknown));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Unknown>(cmd).ctx, real.ctx);
    });
    // command starts with spaces
    unknown = "  DATA\r\n";
    parse_command(unknown.begin(), unknown.end(), [&](Command cmd) {
        auto real = commands::Unknown(boost::make_iterator_range(unknown));
        ASSERT_EQ(cmd.which(), Command(real).which());
        EXPECT_EQ(boost::get<commands::Unknown>(cmd).ctx, real.ctx);
    });
}

}
