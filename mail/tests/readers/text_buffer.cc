#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/unistat/cpp/include/readers/text_buffer.h>

using namespace ::testing;
using namespace ::unistat;
using namespace std::literals;

struct SourceMock {
    explicit SourceMock(std::string data)
        : _data(std::move(data))
    {}

    template <typename MutableBufferSequence, typename CompletionCondition>
    std::size_t read(
        const MutableBufferSequence& buffers
        , CompletionCondition
        , boost::system::error_code&
    ) {
        std::size_t readSize = read();
        boost::asio::buffer_copy(buffers, boost::asio::buffer(_data));
        return readSize;
    }

    MOCK_METHOD(std::size_t, read, (), ());

private:
    const std::string _data;
};

struct ErrorSourceMock {
    template <typename MutableBufferSequence, typename CompletionCondition>
    std::size_t read(
        const MutableBufferSequence&
        , CompletionCondition
        , boost::system::error_code& ec
    ) {
        std::size_t readSize = read();
        ec = boost::asio::error::make_error_code(boost::asio::error::eof);
        return readSize;
    }

    MOCK_METHOD(std::size_t, read, (), ());
};

TEST(TextBufferTest, initStateShouldBeEmpty) {
    TextBuffer buffer;

    EXPECT_TRUE(buffer.needReadMore());
    EXPECT_TRUE(buffer.empty());
    EXPECT_EQ(0ul, buffer.size());
    EXPECT_EQ(TextBuffer::DEFAULT_BUFFER_SIZE, buffer.capacity());
}

TEST(TextBufferTest, bufferShouldNotSetErrorCodeByItSelf) {
    TextBuffer buffer;
    const std::string data = "abacaba";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;
    EXPECT_EQ(data.size(), buffer.read(source, ec));

    ASSERT_FALSE(ec);
}

TEST(TextBufferTest, bufferShouldPassErrorFromSource) {
    TextBuffer buffer;
    ErrorSourceMock source;

    EXPECT_CALL(source, read()).WillOnce(Return(0));

    boost::system::error_code ec;
    buffer.read(source, ec);

    ASSERT_TRUE(ec);
}

TEST(TextBufferTest, shouldReadAllDataIfBufferSizeGreatEnough) {
    TextBuffer buffer;
    const std::string data = "abacaba";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;
    EXPECT_EQ(data.size(), buffer.read(source, ec));
}

TEST(TextBufferTest, shouldReadOnlyFitDataIfBufferSizeIsNotGreatEnough) {
    TextBuffer buffer(1ul);
    const std::string data = "abacaba";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(buffer.capacity()));

    boost::system::error_code ec;
    EXPECT_EQ(buffer.capacity(), buffer.read(source, ec));
}

TEST(TextBufferTest, needReadMoreShouldReturnFalseAfterReadLine) {
    TextBuffer buffer;
    const std::string data = "abacaba";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;
    buffer.read(source, ec);

    EXPECT_FALSE(buffer.needReadMore());
}

TEST(TextBufferTest, needReadMoreShouldReturnTrueAfterFailedTryGetLine) {
    TextBuffer buffer;
    const std::string data = "abacaba";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;

    EXPECT_FALSE(buffer.tryGetLine(source, ec));
    EXPECT_TRUE(buffer.needReadMore());
}

TEST(TextBufferTest, readAfterFailedTryGetLineShouldResetFailState) {
    TextBuffer buffer;
    const std::string data = "abacaba";
    SourceMock source(data);

    EXPECT_CALL(source, read()).Times(2).WillRepeatedly(Return(data.size()));

    boost::system::error_code ec;
    buffer.tryGetLine(source, ec);
    buffer.read(source, ec);
    EXPECT_FALSE(buffer.needReadMore());
}

TEST(TextBufferTest, tryGetLineShouldReturnNulloptWhenBufferHasNotNewline) {
    TextBuffer buffer;
    const std::string data = "abacaba";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;
    EXPECT_FALSE(buffer.tryGetLine(source, ec));
}

TEST(TextBufferTest, shouldReadTwoLinesFromOneChunk) {
    TextBuffer buffer;
    const std::string data = "abacaba\ncabababa\nY";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;

    const std::optional<std::string_view> abacaba = buffer.tryGetLine(source, ec);
    ASSERT_TRUE(abacaba);
    EXPECT_EQ("abacaba", *abacaba);

    const std::optional<std::string_view> cabababa = buffer.tryGetLine(source, ec);
    ASSERT_TRUE(cabababa);
    EXPECT_EQ("cabababa", *cabababa);

    EXPECT_FALSE(buffer.tryGetLine(source, ec));
}

TEST(TextBufferTest, shouldReadTwoLinesFromTwoChunk) {
    TextBuffer buffer;
    const std::string data = "abacaba\ncabab";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;

    std::optional<std::string_view> abacaba = buffer.tryGetLine(source, ec);
    ASSERT_TRUE(abacaba);
    EXPECT_EQ("abacaba"sv, *abacaba);

    ASSERT_FALSE(buffer.tryGetLine(source, ec));

    const std::string str2 = "a\nyand\n";
    SourceMock source2(str2);

    EXPECT_CALL(source2, read()).WillOnce(Return(str2.size()));

    std::optional<std::string_view> cababa = buffer.tryGetLine(source2, ec);
    ASSERT_TRUE(cababa);
    EXPECT_EQ("cababa"sv, *cababa);

    std::optional<std::string_view> yand = buffer.tryGetLine(source2, ec);
    ASSERT_TRUE(yand);
    EXPECT_EQ("yand"sv, *yand);

    EXPECT_FALSE(buffer.tryGetLine(source2, ec));
}

TEST(TextBufferTest, readTwoBlocksCarryNewLineSymbol) {
    TextBuffer buffer;
    const std::string firstBlock = "abacaba\ncabab";
    SourceMock source(firstBlock);

    EXPECT_CALL(source, read()).WillOnce(Return(firstBlock.size()));

    boost::system::error_code ec;

    std::optional<std::string_view> abacaba = buffer.tryGetLine(source, ec);
    ASSERT_TRUE(abacaba);
    EXPECT_EQ("abacaba", *abacaba);

    ASSERT_FALSE(buffer.tryGetLine(source, ec));

    const std::string secondBlock = "\nyand\n";
    SourceMock source2(secondBlock);

    EXPECT_CALL(source2, read()).WillOnce(Return(secondBlock.size()));

    std::optional<std::string_view> cababa = buffer.tryGetLine(source2, ec);
    ASSERT_TRUE(cababa);
    EXPECT_EQ("cabab"sv, *cababa);

    std::optional<std::string_view> yand = buffer.tryGetLine(source2, ec);
    ASSERT_TRUE(yand);
    EXPECT_EQ("yand"sv, *yand);

    EXPECT_FALSE(buffer.tryGetLine(source2, ec));
}

TEST(TextBufferTest, shouldThrowExceptionOnLineWithSizeMoreMaxBufferSize) {
    TextBuffer buffer(TextBuffer::MAX_BUFFER_SIZE);
    const std::string data(TextBuffer::MAX_BUFFER_SIZE + 1, 'A');
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(TextBuffer::MAX_BUFFER_SIZE));

    boost::system::error_code ec;
    EXPECT_THROW(buffer.tryGetLine(source, ec), std::runtime_error);
}

TEST(TextBufferTest, resetShouldSetZeroSize) {
    TextBuffer buffer;
    const std::string data = "abacaba\ncabab";
    SourceMock source(data);

    EXPECT_CALL(source, read()).WillOnce(Return(data.size()));

    boost::system::error_code ec;
    buffer.read(source, ec);
    buffer.reset();
    EXPECT_EQ(0ul, buffer.size());
}
