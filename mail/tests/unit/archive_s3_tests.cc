#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/barbet/service/include/s3_methods.h>
#include <aws/s3/model/GetObjectRequest.h>
#include <aws/s3/model/ListObjectsV2Request.h>
#include <boost/lexical_cast.hpp>
#include <boost/property_tree/exceptions.hpp>
#include <spdlog/details/format.h>


using namespace ::testing;

namespace barbet::tests {

namespace s3m = Aws::S3::Model;

struct MockS3Client : public ymod_s3::Client {
    MOCK_METHOD(void, asyncGetObject,     (const s3m::GetObjectRequest&, OnGetObjectOutcome),         (const, override));
    MOCK_METHOD(void, asyncListObjectsV2, (const s3m::ListObjectsV2Request&, OnListObjectsV2Outcome), (const, override));
};

struct S3Test : public Test {
    boost::asio::io_context context;
    std::shared_ptr<StrictMock<MockS3Client>> s3;
    std::string s3Bucket;
    std::string uid;

    void SetUp() {
        s3 = std::make_shared<StrictMock<MockS3Client>>();
        s3Bucket = "test_bucket";
        uid = "12345";
    }

    template<class Fn>
    void spawn(Fn&& fn) {
        boost::asio::spawn(context, std::forward<Fn>(fn));
        context.run();
    }
};

struct GetUserArchiveChunksTest: public S3Test { };

struct GetArchiveMessagesTest: public S3Test { };


TEST_F(GetUserArchiveChunksTest, shouldReturnValidChunks) {
    spawn([=, this] (boost::asio::yield_context yield) {
        using Obj = s3m::Object;
        std::string objKey = "1/2_3";
        auto s3Obj = Obj().WithKey(objKey);
        std::vector<Obj> s3Objects = {s3Obj, s3Obj, s3Obj, s3Obj};
        auto result = s3m::ListObjectsV2Result()
                        .WithContents(s3Objects)
                        .WithIsTruncated(false)
                        .WithNextContinuationToken("");

        auto outcome = s3m::ListObjectsV2Outcome(std::move(result));
        EXPECT_CALL(*s3, asyncListObjectsV2(_, _)).WillOnce(InvokeArgument<1>(outcome));

        auto chunks = archive::getUserArchiveChunks(uid, s3, s3Bucket, yield);
        EXPECT_EQ(s3Objects.size(), chunks.size());
        
        auto& chunk = chunks.front();
        EXPECT_EQ(chunk.key, objKey);
        EXPECT_EQ(chunk.count, 3);
        EXPECT_EQ(chunk.last_mid, 2);
    });
}

TEST_F(GetUserArchiveChunksTest, shouldUseNextContinuationToken) {
    spawn([=, this] (boost::asio::yield_context yield) {
        const auto callTimes = 3;

        auto resultFirst = s3m::ListObjectsV2Result()
                        .WithIsTruncated(true)
                        .WithNextContinuationToken("NextContinuationToken");
        
        auto resultLast = s3m::ListObjectsV2Result()
                        .WithIsTruncated(false)
                        .WithNextContinuationToken("");

        EXPECT_CALL(*s3, asyncListObjectsV2(_, _))
            .Times(callTimes)
            .WillOnce(InvokeArgument<1>(s3m::ListObjectsV2Outcome(std::move(resultFirst))))
            .WillRepeatedly(InvokeArgument<1>(s3m::ListObjectsV2Outcome(std::move(resultLast))));

        for(auto i = 0; i < callTimes - 1; ++i) {
            auto chunks = archive::getUserArchiveChunks(uid, s3, s3Bucket, yield);
            EXPECT_TRUE(chunks.empty());
        }
    });
}

TEST_F(GetUserArchiveChunksTest, shouldThrowBecauseOfS3Error) {
    spawn([=, this] (boost::asio::yield_context yield) {
        const auto callTimes = 3;
        auto outcome = s3m::ListObjectsV2Outcome(Aws::S3::S3Error());

        EXPECT_CALL(*s3, asyncListObjectsV2(_, _))
            .Times(callTimes)
            .WillRepeatedly(InvokeArgument<1>(outcome));
        
        for(auto i = 0; i < callTimes; ++i) {
            EXPECT_THROW(archive::getUserArchiveChunks(uid, s3, s3Bucket, yield), mail_errors::system_error);
        }
    });
}

TEST_F(GetUserArchiveChunksTest, shouldThrowBecauseOfWrongKeyFormat) {
    spawn([=, this] (boost::asio::yield_context yield) {
        const auto callTimes = 3;
        std::string objKey = "invalid_key";
        for(auto i = 0; i < callTimes; ++i) {
            using Obj = s3m::Object;
            auto result = s3m::ListObjectsV2Result()
                            .WithContents({Obj().WithKey(objKey)})
                            .WithIsTruncated(false)
                            .WithNextContinuationToken("");

            auto outcome = s3m::ListObjectsV2Outcome(std::move(result));
            EXPECT_CALL(*s3, asyncListObjectsV2(_, _)).WillOnce(InvokeArgument<1>(outcome));
        
            EXPECT_THROW(archive::getUserArchiveChunks(uid, s3, s3Bucket, yield), std::invalid_argument);
            objKey += "/test_test";
        }
    });
}

TEST_F(GetUserArchiveChunksTest, shouldThrowBecauseOfNonNumericKey) {
    spawn([=, this] (boost::asio::yield_context yield) {
        std::string objKey = "1/2_three";
        using Obj = s3m::Object;
        auto result = s3m::ListObjectsV2Result()
                        .WithContents({Obj().WithKey(objKey)})
                        .WithIsTruncated(false)
                        .WithNextContinuationToken("");

        auto outcome = s3m::ListObjectsV2Outcome(std::move(result));
        EXPECT_CALL(*s3, asyncListObjectsV2(_, _)).WillOnce(InvokeArgument<1>(outcome));
    
        EXPECT_THROW(archive::getUserArchiveChunks(uid, s3, s3Bucket, yield), boost::bad_lexical_cast);
    });
}



ACTION_P(PassGetObjectOutcome, outcome) {
    arg1(std::move(*outcome));
}

inline s3m::GetObjectResult createGetObjectResult(const std::string_view body) {
    auto result = s3m::GetObjectResult();
    auto strStream = Aws::New<Aws::StringStream>("");
    *strStream << body;
    result.ReplaceBody(strStream);

    return result;
}

TEST_F(GetArchiveMessagesTest, shouldReturnValidMessages) {
    constexpr auto objBody =
R"([
  {
    "st_id": "320.697023277.E222649:3098331666202109160128265315059",
    "folder_type": "inbox",
    "received_date": 1378212302,
    "is_shared": false
  },
  {
    "st_id": "320.697023277.E222649:3098331666202109160128265315059",
    "folder_type": "inbox",
    "received_date": 1378212302,
    "is_shared": false
  },
  {
    "st_id": "320.697023277.E222649:3098331666202109160128265315059",
    "folder_type": "inbox",
    "received_date": 1378212302,
    "is_shared": false
  }
])";

    spawn([=, this] (boost::asio::yield_context yield) {
        auto outcome = s3m::GetObjectOutcome(createGetObjectResult(objBody));

        EXPECT_CALL(*s3, asyncGetObject(_, _)).WillOnce(PassGetObjectOutcome(&outcome));

        auto archivedMessages = archive::getArchiveMessages("1/2_3", s3, s3Bucket, yield);
        EXPECT_EQ(archivedMessages.size(), 3UL);

        auto& archMsg = archivedMessages.front();
        EXPECT_EQ(archMsg.st_id, "320.697023277.E222649:3098331666202109160128265315059");
        EXPECT_EQ(archMsg.folder_type, "inbox");
        EXPECT_EQ(archMsg.received_date, 1378212302);
        EXPECT_EQ(archMsg.is_shared, false);
    });
}

TEST_F(GetArchiveMessagesTest, shouldThrowBecauseOfS3Error) {
    spawn([=, this] (boost::asio::yield_context yield) {
        const auto callTimes = 3;
        auto outcome = s3m::GetObjectOutcome(Aws::S3::S3Error());

        EXPECT_CALL(*s3, asyncGetObject(_, _))
            .Times(callTimes)
            .WillRepeatedly(PassGetObjectOutcome(&outcome));
        
        for(auto i = 0; i < callTimes; ++i) {
            EXPECT_THROW(archive::getArchiveMessages("1/2_3", s3, s3Bucket, yield), mail_errors::system_error);
        }
    });
}

TEST_F(GetArchiveMessagesTest, shouldThrowBecauseOfWrongResponsePath) {
    constexpr auto objBodyMissing_st_id =
R"([
  {
    "st_id_wrong": "THIS FIELD SHOUD HAS <st_id> NAME",
    "folder_type": "inbox",
    "received_date": 1378212302,
    "is_shared": false
  }
])";

    spawn([=, this] (boost::asio::yield_context yield) {
        auto outcome = s3m::GetObjectOutcome(createGetObjectResult(objBodyMissing_st_id));

        EXPECT_CALL(*s3, asyncGetObject(_, _)).WillOnce(PassGetObjectOutcome(&outcome));

        EXPECT_THROW(archive::getArchiveMessages("1/2_3", s3, s3Bucket, yield), boost::property_tree::ptree_bad_path);
    });
}

TEST_F(GetArchiveMessagesTest, shouldThrowBecauseOfWrongResponseType) {
    constexpr auto objBodyFieldsHaveWrongTypes = 
R"([
  {
    "st_id": "320.697023277.E222649:3098331666202109160128265315059",
    "folder_type": {"arr":[]},
    "received_date": "a1378212302",
    "is_shared": "1false"
  }
])";

    spawn([=, this] (boost::asio::yield_context yield) {
        auto outcome = s3m::GetObjectOutcome(createGetObjectResult(objBodyFieldsHaveWrongTypes));

        EXPECT_CALL(*s3, asyncGetObject(_, _)).WillOnce(PassGetObjectOutcome(&outcome));

        EXPECT_THROW(archive::getArchiveMessages("1/2_3", s3, s3Bucket, yield), boost::property_tree::ptree_bad_data);
    });
}


}
