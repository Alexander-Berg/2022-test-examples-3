#include <gtest/gtest.h>

#include <imap_protocol/preparser.h>

using Iterator = std::string::const_iterator;
using Preparser = imap_protocol::Preparser<Iterator>;

struct ExpectedResult
{
    ExpectedResult() = default;
    ExpectedResult(const std::string& buffer, std::vector<std::size_t> literalSizes = {})
        : processedBytes(buffer.size())
    {
        requiredPluses = literalSizes.size();
        for (auto&& size : literalSizes)
        {
            allLiteralsSize += size;
            maxLiteralSize = std::max(maxLiteralSize, size);
        }
        pureSize = buffer.size() - allLiteralsSize;
    }

    void updateProcessedBytes(std::size_t count)
    {
        processedBytes = count;
        pureSize = count - allLiteralsSize;
    }

    bool commandComplete = true;
    std::size_t requiredPluses = 0;
    std::string tag;
    std::size_t processedBytes = 0;
    std::size_t pureSize = 0;
    std::size_t maxLiteralSize = 0;
    std::size_t allLiteralsSize = 0;
};

void extractAndCheck(
    const std::string& buffer,
    const ExpectedResult& expected,
    Preparser& preparser)
{
    auto res = preparser(buffer);

    EXPECT_EQ(expected.commandComplete, res.commandComplete);
    EXPECT_EQ(expected.requiredPluses, res.continuationRequests);
    EXPECT_EQ(expected.tag, res.commandTag);
    EXPECT_EQ(expected.processedBytes, res.totalSize);
    EXPECT_EQ(expected.pureSize, res.pureSize);
    EXPECT_EQ(expected.maxLiteralSize, res.maxLiteralSize);
    EXPECT_EQ(expected.allLiteralsSize, res.allLiteralsSize);
}

void extractAndCheck(const std::string& buffer, const ExpectedResult& expected)
{
    Preparser preparser;
    extractAndCheck(buffer, expected, preparser);
}

TEST(TEST_COMMAND_PREPARSER, BASIC_EXTRACTING)
{
    std::string buffer = "A1 LOGIN QWE ASD\r\n";
    ExpectedResult res(buffer);
    res.tag = "A1";

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, LITERAL_EXTRACTING)
{
    std::string buffer = "A20 LOGIN QWE {3}\r\nASD\r\n";
    ExpectedResult res(buffer, { 3 });
    res.tag = "A20";

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_LITERAL_EXTRACTING)
{
    std::string buffer = "A22 LOGIN {3}\r\nQWE {4}\r\nASDF\r\n";
    ExpectedResult res(buffer, { 3, 4 });
    res.tag = "A22";

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, LITERAL_EXTRACTING_WITHOUT_PLUSES)
{
    std::string buffer = "A005 LOGIN QWE {3+}\r\nASD\r\n";
    ExpectedResult res(buffer, { 3 });
    res.tag = "A005";
    res.requiredPluses = 0;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_LITERAL_EXTRACTING_WITHOUT_PLUSES)
{
    std::string buffer = "PGHA LOGIN {4+}\r\nQWER {3+}\r\nASD\r\n";
    ExpectedResult res(buffer, { 4, 3 });
    res.tag = "PGHA";
    res.requiredPluses = 0;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_LITERAL_EXTRACTING_MIXED_PLUSES)
{
    std::string buffer = "00BG LOGIN {3+}\r\nQWE {4}\r\nASDF\r\n";
    ExpectedResult res(buffer, { 3, 4 });
    res.tag = "00BG";
    res.requiredPluses = 1;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, EMPTY_TAG)
{
    std::string buffer = " CAPABILITY\r\n";
    ExpectedResult res(buffer);
    res.tag = " CAPABILITY";

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, JUST_SPACE)
{
    std::string buffer = " \r\n";
    ExpectedResult res(buffer);
    res.tag = " ";

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, EMPTY_COMMAND)
{
    std::string buffer = "\r\n";

    extractAndCheck(buffer, ExpectedResult(buffer));
}

TEST(TEST_COMMAND_PREPARSER, INCOMPLETE_COMMAND)
{
    std::string buffer = "A20 LOGIN ";
    ExpectedResult res(buffer);
    res.commandComplete = false;
    res.updateProcessedBytes(0);

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, INCOMPLETE_LITERAL)
{
    std::string buffer = "A20 LOGIN {100}\r\nQWE";
    ExpectedResult res(buffer, { 100 });
    res.commandComplete = false;
    res.tag = "A20";
    res.processedBytes = buffer.find('Q');
    res.pureSize = res.processedBytes;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, COMPLETE_LITERAL_INCOMPLETE_COMMAND)
{
    std::string buffer = "A20 LOGIN {3}\r\nQWE\r";
    ExpectedResult res(buffer, { 3 });
    res.commandComplete = false;
    res.tag = "A20";
    res.updateProcessedBytes(buffer.size() - 1);

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, INCOMPLETE_COMMAND_LIMIT)
{
    std::string buffer = "A20 LOGIN IAMLONGLOGIN";
    ExpectedResult res(buffer);
    res.commandComplete = false;
    res.updateProcessedBytes(0);

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, COMPLETE_COMMAND_LIMIT)
{
    std::string buffer = "A20 LOGIN IAMLONGLOGIN IAMLONGPASS\r\n";
    ExpectedResult res(buffer);
    res.tag = "A20";

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, COMPLETE_LITERAL_PLUS_LIMIT)
{
    std::string buffer = "A20 LOGIN {20+}\r\n12345678901234567890 PASS\r\n";
    ExpectedResult res(buffer, { 20 });
    res.tag = "A20";
    res.requiredPluses = 0;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, INCOMPLETE_LITERAL_PLUS_LIMIT)
{
    std::string buffer = "A20 LOGIN {200+}\r\nQ1234567";
    ExpectedResult res(buffer, { 200 });
    res.commandComplete = false;
    res.tag = "A20";
    res.requiredPluses = 0;
    res.processedBytes = buffer.find('Q');
    res.pureSize = res.processedBytes;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_COMPLETE_LITERAL_PLUS_LIMITS)
{
    std::string buffer = "A {15+}\r\n123456789012345 {16+}\r\nQ234567890123456\r\n";
    ExpectedResult res(buffer, { 15, 16 });
    res.tag = "A";
    res.requiredPluses = 0;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_INCOMPLETE_LITERAL_PLUS_LIMITS)
{
    std::string buffer = "A {15+}\r\n123456789012345 {12+}\r\nQ2345678";
    ExpectedResult res(buffer, { 15, 12 });
    res.commandComplete = false;
    res.tag = "A";
    res.requiredPluses = 0;
    res.processedBytes = buffer.find('Q');
    res.pureSize = res.processedBytes - 15;

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_BASIC_COMMANDS)
{
    std::string buffer = "A20 LOGIN LOG PAS\r\nZ21 CAPABILITIES\r\n";

    ExpectedResult firstRes(buffer);
    firstRes.tag = "A20";
    firstRes.updateProcessedBytes(buffer.find('Z'));

    extractAndCheck(buffer, firstRes);

    buffer.erase(0, firstRes.processedBytes);
    ExpectedResult secondRes(buffer);
    secondRes.tag = "Z21";

    extractAndCheck(buffer, secondRes);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_MIXED_COMMANDS)
{
    std::string buffer = "A20 LOGIN {5}\r\nLOGIN {4+}\r\nPASS\r\nZ21 CAPABILITIES\r\n";

    ExpectedResult firstRes(buffer, { 5, 4 });
    firstRes.tag = "A20";
    firstRes.requiredPluses = 1;
    firstRes.updateProcessedBytes(buffer.find('Z'));

    extractAndCheck(buffer, firstRes);

    buffer.erase(0, firstRes.processedBytes);
    ExpectedResult secondRes(buffer);
    secondRes.tag = "Z21";

    extractAndCheck(buffer, secondRes);
}

TEST(TEST_COMMAND_PREPARSER, LITERAL_LIMIT)
{
    std::string buffer = "A20 LOGIN {20}\r\n";

    ExpectedResult res(buffer, { 20 });
    res.commandComplete = false;
    res.tag = "A20";
    res.pureSize = buffer.size();

    extractAndCheck(buffer, res);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_COMMANDS_WITH_LITERAL_LIMIT)
{
    std::string buffer = "A20 LOGIN {20}\r\nZ24 LOGIN \"qwe\"\r\n";

    ExpectedResult firstRes(buffer, { 20 });
    firstRes.commandComplete = false;
    firstRes.tag = "A20";
    firstRes.processedBytes = buffer.find('Z');
    firstRes.pureSize = firstRes.processedBytes;

    extractAndCheck(buffer, firstRes);

    buffer.erase(0, firstRes.processedBytes);
    ExpectedResult secondRes(buffer);
    secondRes.tag = "Z24";

    extractAndCheck(buffer, secondRes);
}

TEST(TEST_COMMAND_PREPARSER, MULTIPLE_REAL_COMMANDS)
{
    std::string buffer =
        "ibax ID (\"name\" \"Microsoft Outlook\" \"version\" \"14.0.7137.5000\")\r\n"
        "759c SELECT {145}\r\n\r\n&BBoEMARCBDAEOwQ+BDM- &BDAEQARFBDgEMgQw-|5. "
        "&BBQEPgQzBD4EMgQ+BEAEMA-|&BBcEMAQ6BDAENwRHBDgEOgQ4-|3. &BB8EPgQ0BDMEPgRCBD4EMgQ4BEIETA- "
        "&BDQEPgQ6BDg-\r\n";

    ExpectedResult firstRes(buffer);
    firstRes.tag = "ibax";
    firstRes.updateProcessedBytes(buffer.find("75"));

    extractAndCheck(buffer, firstRes);

    buffer.erase(0, firstRes.processedBytes);
    ExpectedResult secondRes(buffer, { 145 });
    secondRes.tag = "759c";

    extractAndCheck(buffer, secondRes);
}
