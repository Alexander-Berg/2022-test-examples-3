#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/unistat/cpp/include/readers/readers.h>

using namespace ::testing;
using namespace ::unistat;

struct FileMock {
    MOCK_METHOD(void, reopen, (), ());
    MOCK_METHOD(void, close, (), ());
    void open(bool fastForward = false) {(void)fastForward;}
    MOCK_METHOD(std::size_t, alignment, (), (const));
    MOCK_METHOD(bool, isOpen, (), (const));
    MOCK_METHOD(const std::string&, getPath, (), (const));
    MOCK_METHOD(bool, shouldReopenFile, (), (const));
};

struct FileMockWrapper {
    FileMock& mock;

    explicit FileMockWrapper(FileMock& mock_)
        : mock(mock_)
    {}

    void reopen() {
        mock.reopen();
    }

    void close() {
        mock.close();
    }

    void open() {
        mock.open();
    }

    void open(bool fastForward) {
        mock.open(fastForward);
    }

    std::size_t alignment() const {
        return mock.alignment();
    }

    bool isOpen() const {
        return mock.isOpen();
    }

    std::string getPath() const {
        return mock.getPath();
    }

    bool shouldReopenFile() const {
        return mock.shouldReopenFile();
    }
};

struct BufferMock {
    MOCK_METHOD(void, reset, (), ());
    MOCK_METHOD(std::optional<std::string_view>, tryGetLine, (FileMockWrapper& source, boost::system::error_code& ec), ());
    MOCK_METHOD(bool, empty, (), (const));
    MOCK_METHOD(bool, needReadMore, (), (const));
    MOCK_METHOD(std::size_t, capacity, (), (const));
    MOCK_METHOD(std::size_t, size, (), (const));
};

struct BufferMockWrapper {
    StrictMock<BufferMock>& mock;

    explicit BufferMockWrapper(StrictMock<BufferMock>& mock_)
        : mock(mock_)
    {}

    void reset() {
        mock.reset();
    }

    std::optional<std::string_view> tryGetLine(FileMockWrapper& source, boost::system::error_code& ec) {
        return mock.tryGetLine(source, ec);
    }

    bool empty() const {
        return mock.empty();
    }

    bool needReadMore() const {
        return mock.needReadMore();
    }

    std::size_t capacity() const {
        return mock.capacity();
    }

    std::size_t size() const {
        return mock.size();
    }
};

using TextFileReaderMocked = TextFileReader<FileMockWrapper, BufferMockWrapper, logdog::none_t>;


struct TextFileReaderTest : public Test {
    FileMock fileMock;
    FileMockWrapper fileMockWrapper = FileMockWrapper(fileMock);

    StrictMock<BufferMock> bufferMock;
    BufferMockWrapper bufferMockWrapper = BufferMockWrapper(bufferMock);
};

TEST_F(TextFileReaderTest, getSourceNameShouldPassFilePath) {
    TextFileReaderMocked reader(std::move(fileMockWrapper), true, std::move(bufferMockWrapper));

    const std::string fileName = "/path/name.log";
    EXPECT_CALL(fileMock, getPath()).WillOnce(ReturnRef(fileName));

    EXPECT_EQ(reader.getSourceName(), fileName);
}

TEST_F(TextFileReaderTest, shouldPassGottenLineFromBuffer) {
    TextFileReaderMocked reader(std::move(fileMockWrapper), true, std::move(bufferMockWrapper));

    reader.setLogger(logdog::none);
    boost::system::error_code ec;

    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Return("some line"));
    EXPECT_CALL(fileMock, isOpen()).WillOnce(Return(true));

    EXPECT_EQ(reader(), "some line");
}

TEST_F(TextFileReaderTest, shouldTryReadMoreWhenTryGetLineFails) {
    TextFileReaderMocked reader(std::move(fileMockWrapper), true, std::move(bufferMockWrapper));

    reader.setLogger(logdog::none);
    boost::system::error_code ec;

    InSequence s;

    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Return(std::nullopt));
    EXPECT_CALL(fileMock, isOpen()).WillOnce(Return(true));
    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Return("some line"));
    EXPECT_CALL(fileMock, isOpen()).WillOnce(Return(true));

    EXPECT_EQ(reader(), "some line");
}

TEST_F(TextFileReaderTest, shouldReopenFile) {
    TextFileReaderMocked reader(std::move(fileMockWrapper), true, std::move(bufferMockWrapper));

    reader.setLogger(logdog::none);
    boost::system::error_code ec;

    InSequence s;

    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Invoke([] (auto&, auto& ec) {
        ec = boost::asio::error::eof;
        return std::nullopt;
    }));
    EXPECT_CALL(fileMock, isOpen()).WillOnce(Return(true));
    EXPECT_CALL(fileMock, shouldReopenFile()).WillOnce(Return(true));
    EXPECT_CALL(fileMock, reopen()).WillOnce(Return());
    EXPECT_CALL(bufferMock, reset()).WillOnce(Return());
    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Return("some line"));
    EXPECT_CALL(fileMock, isOpen()).WillOnce(Return(true));

    EXPECT_EQ(reader(), "some line");
}

TEST_F(TextFileReaderTest, shouldResetBufferWhenTryGetLineThrowException) {
    TextFileReaderMocked reader(std::move(fileMockWrapper), true, std::move(bufferMockWrapper));

    reader.setLogger(logdog::none);
    boost::system::error_code ec;

    InSequence s;

    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Invoke(
            [](auto&, auto&) -> std::optional<std::string_view> {throw std::runtime_error("");}
    ));
    EXPECT_CALL(bufferMock, reset()).WillOnce(Return());
    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Return("some line"));
    EXPECT_CALL(fileMock, isOpen()).WillOnce(Return(true));

    EXPECT_EQ(reader(), "some line");
}

TEST_F(TextFileReaderTest, shouldThrowExceptionWhenLoggerWasNotSet) {
    TextFileReaderMocked reader(std::move(fileMockWrapper), true, std::move(bufferMockWrapper));

    boost::system::error_code ec;

    EXPECT_CALL(bufferMock, tryGetLine(_, ec)).WillOnce(Invoke(
            [](auto&, auto&) -> std::optional<std::string_view> {throw std::runtime_error("");}
    ));

    EXPECT_THROW(reader(), std::logic_error);
}
