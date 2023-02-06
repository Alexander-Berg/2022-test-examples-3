#include <commands/command_list.hpp>
#include <grammar/examine_grammar.hpp>
#include <grammar/fetch_grammar.hpp>
#include <grammar/list_grammar.hpp>

#include <boost/algorithm/string.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <vector>

using namespace std;
using namespace ymod_imap_client;

//-----------------------------------------------------------------------------
// Test greeting response

TEST(GrammarTest, GREETING_GRAMMAR)
{
    vector<string> greetingLines = {
        "* OK Yandex IMAP4rev1 at imap-dev6.mail.yandex.net:993 ready to talk with "
        "2a02:6b8:0:821::2:56007, 2015-Sep-10 14:56:08, LsU6000loiEs\r\n"
    };

    grammar::Greeting<string::const_iterator> greetingGrammar;

    std::vector<grammar::TaggedResponse> results;
    for (auto greeting : greetingLines)
    {
        grammar::TaggedResponse parsed;
        if (boost::spirit::qi::parse(greeting.cbegin(), greeting.cend(), greetingGrammar, parsed))
        {
            results.push_back(parsed);
        }
    }
    EXPECT_TRUE(results.size() == greetingLines.size());
}

TEST(GrammarTest, EOL_TEST)
{
    std::string notFullEol = "* BYE Have a great time!\r";
    grammar::ResponseDone<string::const_iterator> grammar;

    grammar::ParseResult parsed;
    bool parseOk =
        boost::spirit::qi::parse(notFullEol.cbegin(), notFullEol.cend(), grammar, parsed);
    EXPECT_FALSE(parseOk) << "Bad EOL handling";
}

//-----------------------------------------------------------------------------
// Test common response parsing

TEST(GrammarTest, RESPONSE_GRAMMAR)
{
    grammar::ParseResult badResult;
    badResult.taggedResponse.status = grammar::ResponseStatus::BAD;
    badResult.taggedResponse.code = "CLIENTBUG";
    badResult.taggedResponse.reason = "Unknown command r1mb17735974ldf";

    grammar::ParseResult fatalResult;
    fatalResult.taggedResponse.status = grammar::ResponseStatus::Fatal;
    fatalResult.taggedResponse.reason = "IMAP4rev1 Server logging out";

    grammar::ParseResult noResult;
    noResult.taggedResponse.status = grammar::ResponseStatus::NO;
    noResult.taggedResponse.reason = "Thats all she wrote! s10mb424299040ldu";
    noResult.capabilityData = { "IMAP4rev1",
                                "UNSELECT",
                                "IDLE",
                                "NAMESPACE",
                                "QUOTA",
                                "ID",
                                "XLIST",
                                "CHILDREN",
                                "X-GM-EXT-1",
                                "XYZZY",
                                "SASL-IR",
                                "AUTH=XOAUTH2",
                                "AUTH=PLAIN",
                                "AUTH=PLAIN-CLIENTTOKEN",
                                "AUTH=OAUTHBEARER",
                                "AUTH=XOAUTH" };

    grammar::ParseResult emptyReasonResult;
    emptyReasonResult.taggedResponse.code = "READ-ONLY";

    vector<pair<string, grammar::ParseResult>> responseLines = {
        { ". BAD [CLIENTBUG] Unknown command r1mb17735974ldf\r\n", badResult },
        { "* BYE IMAP4rev1 Server logging out\r\n", fatalResult },
        { "* CAPABILITY IMAP4rev1 UNSELECT IDLE NAMESPACE QUOTA ID XLIST CHILDREN X-GM-EXT-1 XYZZY "
          "SASL-IR AUTH=XOAUTH2 AUTH=PLAIN AUTH=PLAIN-CLIENTTOKEN AUTH=OAUTHBEARER AUTH=XOAUTH\r\n"
          "A00002 NO Thats all she wrote! s10mb424299040ldu\r\n",
          noResult },
        { "A00008 OK [READ-ONLY] \r\n", emptyReasonResult }
    };

    for (auto pair : responseLines)
    {
        grammar::ParseResult parsed;
        string line = pair.first;
        auto parseOk = boost::spirit::qi::parse(
            line.cbegin(), line.cend(), grammar::ResponseDone<string::const_iterator>(), parsed);
        EXPECT_TRUE(parseOk);
        EXPECT_EQ(parsed, pair.second) << parsed.taggedResponse.reason;
    }
}

TEST(GrammarTest, INFO_RESPONSE_GRAMMAR)
{
    grammar::ParseResult capaResult;
    capaResult.taggedResponse.status = grammar::ResponseStatus::OK;
    capaResult.taggedResponse.reason = "Thats all she wrote! s10mb424299040ldu";
    capaResult.capabilityData = { "IMAP4rev1",
                                  "UNSELECT",
                                  "IDLE",
                                  "NAMESPACE",
                                  "QUOTA",
                                  "ID",
                                  "XLIST",
                                  "CHILDREN",
                                  "X-GM-EXT-1",
                                  "XYZZY",
                                  "SASL-IR",
                                  "AUTH=XOAUTH2",
                                  "AUTH=PLAIN",
                                  "AUTH=PLAIN-CLIENTTOKEN",
                                  "AUTH=OAUTHBEARER",
                                  "AUTH=XOAUTH" };

    grammar::ParseResult idResult;
    idResult.taggedResponse.status = grammar::ResponseStatus::BAD;
    idResult.taggedResponse.reason = "ID Completed.";
    idResult.idData = { "name",   "Yandex Mail", "vendor",
                        "Yandex", "support-url", "http://feedback.yandex.ru/?from=mail" };

    grammar::ParseResult copyuidResult;
    copyuidResult.taggedResponse.status = grammar::ResponseStatus::OK;
    copyuidResult.taggedResponse.reason = "Done";
    copyuidResult.uidplusResponse.uidvalidity = 1447073464;
    copyuidResult.uidplusResponse.newUids = "1720";
    copyuidResult.uidplusResponse.originalUids = "1481";

    grammar::ParseResult appenduidResult;
    appenduidResult.taggedResponse.status = grammar::ResponseStatus::OK;
    appenduidResult.taggedResponse.reason = "APPEND completed";
    appenduidResult.taggedResponse.uidplusResponse.uidvalidity = 38505;
    appenduidResult.taggedResponse.uidplusResponse.newUids = "3955";

    grammar::ParseResult statusResult;
    statusResult.taggedResponse.status = grammar::ResponseStatus::OK;
    statusResult.taggedResponse.reason = "STATUS Completed.";
    statusResult.statusResponse.mailboxName = "&BCMENAQwBDsENQQ9BD0ESwQ1-|abc";
    statusResult.statusResponse.mailboxInfoResponse = MailboxInfoResponse(0, 0, 1, 1448298198, 0);

    grammar::ParseResult statusResultOutlook;
    statusResultOutlook.taggedResponse.status = grammar::ResponseStatus::OK;
    statusResultOutlook.taggedResponse.reason = "STATUS completed.";
    statusResultOutlook.statusResponse.mailboxName = "yapoptest01";
    statusResultOutlook.statusResponse.mailboxInfoResponse =
        MailboxInfoResponse(42, 42, 43, 114, 42);

    grammar::ParseResult statusMultilineResult;
    statusMultilineResult.taggedResponse.status = grammar::ResponseStatus::OK;
    statusMultilineResult.taggedResponse.reason = "STATUS Completed.";
    statusMultilineResult.statusResponse.mailboxName =
        "INBOX|&BCAEPgQ0BDgEQgQ1BDsETARBBDoEMARPAKAEPwQwBD8EOgQw-|\"&"
        "BBQEOwQ4BD0EPQQwBE8AoAQUBD4ERwQ1BEAEPQRPBE8AoAQfBDAEPwQ6BDA-";
    statusMultilineResult.statusResponse.mailboxInfoResponse =
        MailboxInfoResponse(1, 2, 3, 1479470714, 4);

    grammar::ParseResult copyuidGmailResult;
    copyuidGmailResult.taggedResponse.status = grammar::ResponseStatus::OK;
    copyuidGmailResult.taggedResponse.reason = "(Success)";
    copyuidGmailResult.taggedResponse.uidplusResponse.uidvalidity = 3;
    copyuidGmailResult.taggedResponse.uidplusResponse.newUids = "3";
    copyuidGmailResult.taggedResponse.uidplusResponse.originalUids = "4232";

    vector<pair<string, grammar::ParseResult>> responseLines = {
        { "* CAPABILITY IMAP4rev1 UNSELECT IDLE NAMESPACE QUOTA ID XLIST CHILDREN X-GM-EXT-1 XYZZY "
          "SASL-IR AUTH=XOAUTH2 AUTH=PLAIN AUTH=PLAIN-CLIENTTOKEN AUTH=OAUTHBEARER AUTH=XOAUTH\r\n"
          "A00002 OK Thats all she wrote! s10mb424299040ldu\r\n",
          capaResult },
        { "* ID (\"name\" \"Yandex Mail\" \"vendor\" \"Yandex\" \"support-url\" "
          "\"http://feedback.yandex.ru/?from=mail\")\r\n"
          "A00004 BAD ID Completed.\r\n",
          idResult },
        { "* OK [COPYUID 1447073464 1481 1720]\r\n"
          "* 1 EXPUNGE\r\n"
          "* 2 FETCH (FLAGS (\\Seen))\r\n"
          "* 237 EXISTS\r\n"
          "A00008 OK Done\r\n",
          copyuidResult },
        { "A003 OK [APPENDUID 38505 3955] APPEND completed\r\n", appenduidResult },
        { "* STATUS \"&BCMENAQwBDsENQQ9BD0ESwQ1-|abc\" (MESSAGES 0 RECENT 0 UIDNEXT 1 UIDVALIDITY "
          "1448298198 UNSEEN 0)\r\n"
          "A00008 OK STATUS Completed.\r\n",
          statusResult },
        { "* STATUS yapoptest01 (MESSAGES 42 RECENT 42 UIDNEXT 43 UIDVALIDITY 114 UNSEEN 42) \r\n"
          "A00005 OK STATUS completed.\r\n",
          statusResultOutlook },
        { "* STATUS {120}\r\n"
          "INBOX|&BCAEPgQ0BDgEQgQ1BDsETARBBDoEMARPAKAEPwQwBD8EOgQw-|\"&"
          "BBQEOwQ4BD0EPQQwBE8AoAQUBD4ERwQ1BEAEPQRPBE8AoAQfBDAEPwQ6BDA-\" (MESSAGES 1 RECENT 2 "
          "UIDNEXT 3 UIDVALIDITY 1479470714 UNSEEN 4)\r\n"
          "A00012 OK STATUS Completed.\r\n",
          statusMultilineResult },
        { "* 99 EXPUNGE\r\n"
          "* 98 EXISTS\r\n"
          "A00632 OK [COPYUID 3 4232 3] (Success)\r\n",
          copyuidGmailResult }
    };

    for (auto pair : responseLines)
    {
        grammar::ParseResult parsed;
        string line = pair.first;
        auto parseOk = boost::spirit::qi::parse(
            line.cbegin(), line.cend(), grammar::ResponseDone<string::const_iterator>(), parsed);

        EXPECT_TRUE(parseOk);
        EXPECT_EQ(parsed, pair.second);
    }
}

//-----------------------------------------------------------------------------
// Test * LIST responses

TEST(GrammarTest, LIST_GRAMMAR)
{
    string listLines =
        "* list (\\Unmarked \\HasNoChildren) \"|\" \"&BBIEQQRP- &BB8EPgRHBEIEMA-\"\r\n"
        "* list (\\Unmarked \\HasNoChildren) \"|\" \"&BBIEQQRP- &BD8EPgRHBEIEMA-\"\r\n"
        "* list (\\Unmarked \\HasNoChildren) \"|\" \"&BC0EGgQhBB8EHg-\"\r\n"
        "* list (\\Unmarked \\HasNoChildren) \"|\" \"&BC0EQgRDBEgEOgQw-\"\r\n"
        "* list (\\Unmarked \\HasNoChildren \\Drafts) \"|\" Drafts\r\n"
        "* list (\\Marked \\NoInferiors) \"|\" inBox\r\n"
        "* list (\\Marked \\NoInferiors) \"|\" Inbox|child\r\n"
        "* list (\\Unmarked \\HasNoChildren) \"|\" Outbox\r\n"
        "* list (\\Unmarked \\HasNoChildren \\Sent) \"|\" Sent\r\n"
        "* list (\\Unmarked \\HasNoChildren \\Junk) \"|\" Spam\r\n"
        "* list (\\Marked \\HasNoChildren \\Trash) \"|\" Trash\r\n"
        "* list (\\Unmarked \\HasNoChildren) \"|\" aaa\r\n"
        "A0010 NO reason\r\n";

    std::string requiredNames = "Вся Почта Вся почта ЭКСПО Этушка "
                                "Drafts INBOX INBOX|child Outbox Sent Spam Trash aaa";

    grammar::ParseResult response;
    response.taggedResponse.status = grammar::ResponseStatus::NO;
    response.taggedResponse.reason = "reason";

    std::vector<ListResponse> listResponses;
    grammar::ParseResultPtr parsed = std::make_shared<grammar::ParseResult>();
    grammar::ListResponseGrammar<string::const_iterator> listGrammar(
        [&listResponses](const ListResponse& parsed) {
            listResponses.push_back(parsed);
            Utf8MailboxName name(Utf7ImapMailboxName(parsed.name, parsed.delim));
            L_(info) << "Name:" << name.asString() << " delim:" << parsed.delim
                     << " Flags:" << parsed.flags;
        });

    bool parseOk =
        boost::spirit::qi::parse(listLines.cbegin(), listLines.cend(), listGrammar, *parsed);
    parsed->listResponses = listResponses;
    response.listResponses = listResponses;
    EXPECT_TRUE(parseOk);

    ImapListPtr listResult = makeParsedResult<ImapList>(parsed);
    std::vector<string> names;
    for (auto& mb : listResult->mailboxes)
    {
        names.push_back(mb->name.asString());
    }

    auto namesStr = boost::algorithm::join(names, " ");
    EXPECT_EQ(namesStr, requiredNames) << "Incorrect folder names";
    EXPECT_EQ(response, *parsed) << "Wrong tagged response";
}

TEST(GrammarTest, UNTAGGED_LIST_PARTIAL_GRAMMAR)
{
    string listLines =
        "* list (\\Unmarked \\HasNoChildren) \"|\" \"&BBIEQQRP- &BB8EPgRHBEIEMA-\"\r\n"
        "* LIST (\\Unmarked \\HasNoChildren) \"|\" {53}\r\n"
        "&BCEEQgQwBEAESwQ1-|&BB4";

    std::string secondResponse = "* LIST (\\Unmarked \\HasNoChildren) \"|\" {53}";

    std::vector<ListResponse> listResponses;
    grammar::ListResponseGrammar<string::const_iterator> listGrammar(
        [&listResponses](const ListResponse& parsed) { listResponses.push_back(parsed); });

    grammar::ParseResult parsed;
    auto beg = listLines.cbegin();
    EXPECT_TRUE(
        boost::spirit::qi::parse(beg, listLines.cend(), listGrammar.untaggedResponses, parsed));
    EXPECT_TRUE(
        boost::equals(secondResponse, boost::make_iterator_range_n(beg, secondResponse.size())));
    EXPECT_EQ(listResponses.size(), 1u) << "Handler called for partial data";
}

//-----------------------------------------------------------------------------
// Test * EXAMINE responses

TEST(GrammarTest, EXAMINE_GRAMMAR)
{
    string examineLines = "* FLAGS (\\Answered \\Seen \\Draft \\Deleted $Forwarded)\r\n"
                          "* 25 EXISTS\r\n"
                          "* 1 RECENT\r\n"
                          "* OK [UNSEEN 25]\r\n"
                          "* OK [PERMANENTFLAGS (\\Answered \\Seen \\Draft \\Flagged \\Deleted "
                          "$Forwarded \\*)] Limited\r\n"
                          "* OK [UIDNEXT 742] Ok\r\n"
                          "* OK [UIDVALIDITY 1409141021] Ok\r\n"
                          "A0001 OK [READ-ONLY] examine Completed.\r\n";

    grammar::ParseResult resp;
    resp.taggedResponse.status = grammar::ResponseStatus::OK;
    resp.taggedResponse.code = "READ-ONLY";
    resp.taggedResponse.reason = "examine Completed.";

    MailboxInfoResponse correctResponse;
    correctResponse.exists = 25;
    correctResponse.recent = 1;
    correctResponse.uidnext = 742;
    correctResponse.uidvalidity = 1409141021;
    correctResponse.unseen = 25;
    resp.mailboxInfoResponse = correctResponse;

    grammar::ExamineResponseGrammar<string::const_iterator> examineGrammar;
    grammar::ParseResult parsed;
    bool parseOk = boost::spirit::qi::parse(
        examineLines.cbegin(), examineLines.cend(), examineGrammar, parsed);
    EXPECT_TRUE(parseOk);
    EXPECT_EQ(resp, parsed) << "Examine responses are different";
}

//-----------------------------------------------------------------------------
// Test FETCH responses

TEST(GrammarTest, FETCH_GRAMMAR)
{
    vector<string> fetchLines = {
        "* 62883 EXISTS\r\n"
        "* 3008 RECENT\r\n"
        "* 1 FETCH (UID 10 FLAGS (\\Seen))\r\n"
        "A00001 OK UID FETCH Completed\r\n",

        "* 2 FETCH (FLAGS (\\Deleted) UID 15)\r\n"
        "A00001 OK UID FETCH Completed\r\n",

        "* 3 FETCH (UID 16 FLAGS (\\Recent) INTERNALDATE \"14-Jul-2017 05:40:00 +0300\")\r\n"
        "A00001 OK UID FETCH Completed\r\n",

        "* 4 FETCH (UID 20 FLAGS () X-GM-LABELS (\"\\\\Inbox\" \"\\\\Important\" "
        "&BBwENQRCBDoEMA-))\r\n"
        "A00001 OK UID FETCH Completed\r\n",

        "* 5 FETCH (UID 11779 INTERNALDATE \"14-Jul-2017 02:40:00 +0000\")\r\n"
        "A00001 OK UID FETCH Completed\r\n",

        "* 6 FETCH (UID 26 FLAGS (\\Unseen \\Draft) INTERNALDATE \"24-Jul-2017 10:48:38 +0000\" "
        "RFC822.SIZE 164115)\r\n"
        "A00001 OK UID FETCH Completed\r\n"
    };

    std::vector<uint32_t> requiredUids{ 10, 15, 16, 20, 11779, 26 };
    std::vector<uint32_t> requiredNums{ 1, 2, 3, 4, 5, 6 };
    std::vector<std::time_t> requiredDates{ 0, 0, 1500000000, 0, 1500000000, 1500893318 };
    std::vector<uint64_t> requiredSizes{ 0, 0, 0, 0, 0, 164115 };

    std::vector<FetchResponse> messages;
    std::vector<std::string> xgmlabels;

    grammar::FetchResponseGrammar<string::const_iterator> fetchGrammar(
        [&messages, &xgmlabels](const FetchResponse& fetched) {
            messages.push_back(fetched);
            L_(info) << "Num:" << fetched.num << " Uid:" << fetched.uid
                     << " flags:" << fetched.flags << " xgm:" << fetched.xgmlabels.size();

            xgmlabels.insert(xgmlabels.end(), fetched.xgmlabels.begin(), fetched.xgmlabels.end());
        });

    for (auto line : fetchLines)
    {
        grammar::ParseResult parsed;
        EXPECT_TRUE(boost::spirit::qi::parse(line.cbegin(), line.cend(), fetchGrammar, parsed));
    }

    std::vector<uint32_t> nums(messages.size());
    std::transform(messages.begin(), messages.end(), nums.begin(), [](const FetchResponse& resp) {
        return resp.num;
    });

    EXPECT_TRUE(nums.size() == requiredNums.size()) << "Wrong numbers count";
    EXPECT_TRUE(std::equal(nums.begin(), nums.end(), requiredNums.begin()))
        << "Numbers are different";

    std::vector<std::time_t> dates(messages.size());
    std::transform(messages.begin(), messages.end(), dates.begin(), [](const FetchResponse& resp) {
        return resp.internaldate_to_time();
    });
    EXPECT_TRUE(dates.size() == requiredDates.size()) << "Wrong dates count";
    EXPECT_TRUE(std::equal(dates.begin(), dates.end(), requiredDates.begin()))
        << "Dates are different";

    std::vector<uint32_t> uids(messages.size());
    std::transform(messages.begin(), messages.end(), uids.begin(), [](const FetchResponse& resp) {
        return resp.uid;
    });

    EXPECT_TRUE(uids.size() == requiredUids.size()) << "Wrong uids count";
    EXPECT_TRUE(std::equal(uids.begin(), uids.end(), requiredUids.begin())) << "Uids are different";

    std::vector<uint64_t> sizes(messages.size());
    std::transform(messages.begin(), messages.end(), sizes.begin(), [](const FetchResponse& resp) {
        return resp.size;
    });

    EXPECT_TRUE(sizes.size() == requiredSizes.size()) << "Wrong numbers count";
    EXPECT_TRUE(std::equal(sizes.begin(), sizes.end(), requiredSizes.begin()))
        << "Numbers are different";

    auto xgmString = boost::algorithm::join(xgmlabels, " ");
    EXPECT_EQ(xgmString, "\\Inbox \\Important Метка") << "Wrong X-GM-LABELS. Got: " << xgmString;

    EXPECT_TRUE(messages.size() == fetchLines.size()) << "Wrong messages count";
}

TEST(GrammarTest, UNTAGGED_PARTIAL_FETCH_GRAMMAR)
{
    std::string data = "* 1 FETCH (UID 10 FLAGS (\\Seen))\r\n"
                       "* 2 FETCH (FLAGS (\\Deleted) UID 15)";

    std::vector<FetchResponse> messages;

    grammar::FetchResponseGrammar<string::const_iterator> fetchGrammar(
        [&messages](const FetchResponse& fetched) { messages.push_back(fetched); });

    grammar::ParseResult parsed;
    EXPECT_TRUE(boost::spirit::qi::parse(
        data.cbegin(), data.cend(), fetchGrammar.untaggedResponses, parsed));
    EXPECT_EQ(messages.size(), 1u) << "Handler called for partial data";
}

//-----------------------------------------------------------------------------
// Test FETCH BODY prefix

TEST(GrammarTest, FETCH_BODY_PREFIX_GRAMMAR)
{
    vector<string> fetchLines = {
        "* 1 FETCH (BODY[] {2217}\r\n",
        "* 2 FETCH (FLAGS (\\Deleted) UID 15 BODY[] {1014}\r\n",
        "* 3 FETCH (UID 16 X-GM-LABELS (\"\\\\Inbox\") FLAGS (\\Deleted) BODY[] {500614}\r\n",
        "* 17 FETCH (UID 4124 BODY[] {50374}\r\n"
    };
    std::vector<uint32_t> requiredSizes = { 2217, 1014, 500614, 50374 };
    std::vector<uint32_t> parsedSizes;

    grammar::FetchBodyPrefixGrammar<string::const_iterator> fetchPrefixGrammar(
        [&parsedSizes](const FetchBodyPrefixResponse& fetched) {
            parsedSizes.push_back(fetched.size);
            L_(info) << "Size:" << fetched.size;
        });

    for (auto line : fetchLines)
    {
        EXPECT_TRUE(boost::spirit::qi::parse(line.cbegin(), line.cend(), fetchPrefixGrammar));
    }

    EXPECT_TRUE(requiredSizes.size() == parsedSizes.size()) << "Wrong responses count";
    EXPECT_TRUE(std::equal(parsedSizes.begin(), parsedSizes.end(), requiredSizes.begin()))
        << "Parsed sizes are incorrect";
}

TEST(GrammarTest, FETCH_BODY_PREFIX_GRAMMAR_UNFINISHED)
{
    vector<string> fetchLines = { "* 2 FETCH (FLAGS (\\Deleted))\r\n" };

    grammar::FetchBodyPrefixGrammar<string::const_iterator> fetchPrefixGrammar(
        [](const FetchBodyPrefixResponse& fetched) { L_(info) << "Size:" << fetched.size; });

    for (auto line : fetchLines)
    {
        EXPECT_FALSE(boost::spirit::qi::parse(line.cbegin(), line.cend(), fetchPrefixGrammar));
    }
}

TEST(GrammarTest, FETCH_BODY_PREFIX_EMPTY_RESPONSE)
{
    vector<string> fetchLines = { "A00027 OK UID FETCH Completed\r\n" };

    grammar::FetchBodyPrefixGrammar<string::const_iterator> fetchPrefixGrammar(
        [](const FetchBodyPrefixResponse& fetched) { L_(info) << "Size:" << fetched.size; });

    for (auto line : fetchLines)
    {
        EXPECT_TRUE(boost::spirit::qi::parse(line.cbegin(), line.cend(), fetchPrefixGrammar));
    }
}

//-----------------------------------------------------------------------------
// Test FETCH BODY

TEST(GrammarTest, FETCH_BODY)
{
    vector<string> fetchLines = { "* 1 FETCH (BODY[] {310}\r\n"
                                  "Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\r\n"
                                  "From: Fred Foobar <foobar@Blurdybloop.COM>\r\n"
                                  "Subject: afternoon meeting\r\n"
                                  "To: mooch@owatagu.siam.edu\r\n"
                                  "Message-Id: <B27397-0100000@Blurdybloop.COM>\r\n"
                                  "MIME-Version: 1.0\r\n"
                                  "Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\r\n"
                                  "\r\n"
                                  "Hello Joe, do you think we can meet at 3:30 tomorrow?\r\n"
                                  " FLAGS (\\Seen))\r\n"
                                  "* 2 FETCH (FLAGS (\\Deleted) UID 15)\r\n"
                                  "A00027 OK UID FETCH Completed\r\n" };

    std::vector<std::pair<uint32_t, uint32_t>> required = {
        { 310, 8 } // { Size, Flags } pair
    };

    std::vector<std::pair<uint32_t, uint32_t>> parsed;

    grammar::FetchResponseGrammar<string::const_iterator> fetchBodyGrammar(
        [&parsed](const FetchResponse& fetched) {
            if (!fetched.body) return;
            parsed.push_back({ fetched.body->size(), fetched.flags });
            L_(info) << "Size:" << fetched.body->size() << " flags:" << fetched.flags;
        });

    for (auto line : fetchLines)
    {
        EXPECT_TRUE(boost::spirit::qi::parse(line.cbegin(), line.cend(), fetchBodyGrammar));
    }

    EXPECT_TRUE(parsed.size() == fetchLines.size()) << "Wrong responses count";
    EXPECT_TRUE(std::equal(parsed.begin(), parsed.end(), required.begin()))
        << "Parsed sizes are incorrect";
}

TEST(GrammarTest, COMPLETE_FETCH_BODY)
{
    vector<string> fetchLines = { "* 62883 EXISTS\r\n"
                                  "* 3008 RECENT\r\n"
                                  "* 1 FETCH (BODY[] {310}\r\n"
                                  "Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\r\n"
                                  "From: Fred Foobar <foobar@Blurdybloop.COM>\r\n"
                                  "Subject: afternoon meeting\r\n"
                                  "To: mooch@owatagu.siam.edu\r\n"
                                  "Message-Id: <B27397-0100000@Blurdybloop.COM>\r\n"
                                  "MIME-Version: 1.0\r\n"
                                  "Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\r\n"
                                  "\r\n"
                                  "Hello Joe, do you think we can meet at 3:30 tomorrow?\r\n"
                                  ")\r\n"
                                  "A00027 OK UID FETCH Completed\r\n" };

    std::vector<uint32_t> required = {
        310 // Size
    };

    std::vector<std::size_t> parsed;

    grammar::FetchResponseGrammar<string::const_iterator> fetchBodyGrammar(
        [&parsed](const FetchResponse& fetched) {
            if (!fetched.body) return;
            parsed.push_back(fetched.body->size());
            L_(info) << "Size:" << fetched.body->size();
        });

    for (auto line : fetchLines)
    {
        EXPECT_TRUE(boost::spirit::qi::parse(line.cbegin(), line.cend(), fetchBodyGrammar))
            << "qi::parse failed";
    }

    EXPECT_TRUE(parsed.size() == fetchLines.size()) << "Wrong responses count";
    EXPECT_TRUE(std::equal(parsed.begin(), parsed.end(), required.begin()))
        << "Parsed sizes are incorrect";
}
