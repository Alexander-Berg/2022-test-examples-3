#pragma once

#include <gmock/gmock.h>
#include <mail/sendbernar/client/include/internal/reader.h>


namespace boost {

inline std::ostream& operator<<(std::ostream& out, const boost::optional<std::vector<std::string>>& vec) {
    if (vec) {
        out << "[ ";
        for(const std::string& v : *vec) {
            out << v << " ";
        }
        out << "]";
    } else {
        out << "empty optional";
    }
    return out;
}

} // namespace boost

namespace sendbernar {
namespace tests {

namespace ydr = yamail::data::reflection;

struct TestRequest: public Request {
    MOCK_METHOD(boost::optional<std::vector<std::string>>, optionalArgs, (const std::string&), (const, override));
    MOCK_METHOD(boost::optional<std::string>, optionalArg, (const std::string&), (const, override));
    MOCK_METHOD(boost::optional<std::string>, optionalHeader, (const std::string&), (const, override));
    MOCK_METHOD(std::string, rawBody, (), (const, override));
};

inline std::shared_ptr<::testing::StrictMock<TestRequest>> make_req() {
    return std::make_shared<::testing::StrictMock<TestRequest>>();
}

#define REQ *req
#define CREATE_REQ auto req = ::sendbernar::tests::make_req()

#define RETURN_ARG(name, val) \
    EXPECT_CALL(REQ, optionalArg(name))\
        .WillOnce(testing::Return(boost::make_optional(std::string(val))))

#define RETURN_ARG_OPT(name, val) \
    EXPECT_CALL(REQ, optionalArg(name))\
        .Times(2)\
        .WillRepeatedly(testing::Return(boost::make_optional(std::string(val))))

#define RETURN_ARG_OPT_EMPTY(name) \
    EXPECT_CALL(REQ, optionalArg(name))\
        .WillRepeatedly(testing::Return(boost::make_optional(false, std::string())))

#define RETURN_VECTOR_ARGS(name, val) \
    EXPECT_CALL(REQ, optionalArgs(name))\
        .WillOnce(testing::Return(boost::make_optional( (std::vector<std::string> val) )))

#define RETURN_ARGS(name, val) \
    EXPECT_CALL(REQ, optionalArg(name))\
        .WillOnce(testing::Return(boost::make_optional(std::string(""))));\
    EXPECT_CALL(REQ, optionalArgs(name))\
        .WillOnce(testing::Return(boost::make_optional( (std::vector<std::string> val) )))

#define RETURN_HEADER(name, val) \
    EXPECT_CALL(REQ, optionalHeader(name))\
        .WillOnce(testing::Return(boost::make_optional(std::string(val))))

#define RETURN_HEADER_TWICE(name, val) \
    EXPECT_CALL(REQ, optionalHeader(name))\
        .Times(2)\
        .WillRepeatedly(testing::Return(boost::make_optional(std::string(val))))

#define RETURN_HEADER_OPT_EMPTY(name) \
    EXPECT_CALL(REQ, optionalHeader(name))\
        .WillOnce(testing::Return(boost::make_optional(false, std::string())))

#define RETURN_HEADER_OPT_EMPTY_TWICE(name) \
    EXPECT_CALL(REQ, optionalHeader(name))\
        .Times(2)\
        .WillRepeatedly(testing::Return(boost::make_optional(false, std::string())))

#define RETURN_BODY(val) \
    EXPECT_CALL(REQ, rawBody())\
        .WillOnce(testing::Return(val))

inline std::string partJson() {
    return R"r({"mid":"mid","hid":"hid"})r";
}

inline auto messagePart() {
    return params::IdetifiableMessagePart("mid", "hid");
}

inline std::string diskAttachJson() {
    return R"r({"path":"/path","name":"filename","size":65535})r";
}

inline auto diskAttach() {
    return params::DiskAttach {
        "/path",
        "filename",
        boost::none,
        boost::none,
        boost::make_optional<std::size_t>(65535),
        boost::none
    };
}

} // namespace test
} // namespace sendbernar

