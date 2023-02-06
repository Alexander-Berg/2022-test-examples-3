#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <tuple>
#include <mail/barbet/service/include/helpers.h>
#include <mail/barbet/service/include/delivery.h>
#include <mail/ymod_queuedb_worker/include/task_control.h>


using namespace ::testing;

namespace mail_errors {
std::ostream& operator<<(std::ostream& out, const error_code& ec) {
    out << "category=" << ec.category().name() << "\t"
        << "message=" << ec.category().message(ec.value()) << "\t"
        << "reason=" << ec.what();
    return out;
}
}

namespace macs {
bool operator == (const std::optional<Tab::Type>& tab1, const std::optional<Tab::Type>& tab2) {
    if (!tab1 && !tab2) {
        return true;
    } else if (tab1 && tab2) {
        return *tab1 == *tab2;
    } else {
        return false;
    }
}
}

namespace barbet::tests {

struct MockFidService: public FidProvider {
    MOCK_METHOD(macs::Fid, temporaryFid, (YieldCtx), (override));
    MOCK_METHOD(const macs::Fid&, getDefaultFid, (), (const, override));
    MOCK_METHOD(macs::Fid, resolveFid, (const macs::Fid&), (const, override));
    MOCK_METHOD(void, clearTemporaryFids, (YieldCtx), (override));
};

struct MockEnvelopeService: public EnvelopeProvider {
    MOCK_METHOD(macs::Stid, stidByMid, (const macs::Mid&, YieldCtx), (const, override));
    MOCK_METHOD(void, moveMessage, (const macs::Mid&, const std::optional<macs::Tab::Type>&, const macs::Fid&, YieldCtx), (const, override));
    MOCK_METHOD(bool, checkDuplicateMessageMistake, (const macs::Mid&, const macs::BackupMessage&, YieldCtx), (const, override));
};

struct MockDelivery: public Delivery {
    using Delivery::call;
    using Delivery::Delivery;
};

struct AsyncRun {
    MOCK_METHOD(yhttp::response, response, (), (const));
};

yhttp::response successResponse() {
    return yhttp::response{.status=200, .body=R"({"mid": "new_mid"})"};
}

yhttp::response duplicateResponse() {
    return yhttp::response{.status=406, .body=R"({"code": "DuplicateError", "message": "ancient_mid"})"};
}

yhttp::response invalidFidResponse() {
    return yhttp::response{.status=406, .body=R"({"code": "InvalidFid"})"};
}

macs::Mid renewedMid() {
    return "new_mid";
}

macs::Mid ancientMid() {
    return "ancient_mid";
}

macs::Stid stid() {
    return "st_id";
}

macs::Stid anotherStid() {
    return "st_id_";
}

macs::Fid someFid() {
    return "tempFid";
}

const std::string generatedDefaultFid = "1";
macs::Fid defaultFid() {
    return generatedDefaultFid;
}

macs::Fid userFid() {
    return "2";
}

std::optional<macs::Tab::Type> tab() {
    return macs::Tab::Type::fromString("social");
}

macs::BackupFidsMapping mapping() {
    macs::BackupFidsMapping mapping;
    mapping["2"] = "3";
    mapping["4"] = "5";

    return mapping;
}

struct RestoreData {
    macs::BackupMessage msg;
    FillRestoreParams params;
    yplatform::task_context_ptr ctx;
    std::shared_ptr<StrictMock<AsyncRun>> network;
    std::shared_ptr<StrictMock<MockFidService>> fidServicePtr;
    std::shared_ptr<StrictMock<MockEnvelopeService>> envelopeServicePtr;
    boost::asio::io_context context;

    RestoreData()
        : params({
            .logger=getContextLogger("", ""),
            .client=std::make_shared<http_getter::Client> (
                nullptr, http::headers(),
                [this] (http_getter::Request, http_getter::CallbackType cb) {
                    cb(boost::system::error_code(), network->response());
                }, nullptr
            ),
            .store = http_getter::Endpoint::Data {
                .url="http://yandex.ru",
                .method="/mail",
                .tries=1
            },
            .blackbox=http_getter::Endpoint::Data {
                .url="http://yandex.ru",
                .method="/mail",
                .tries=1
            },
        })
    { }

    void SetUp() {
        ctx = boost::make_shared<yplatform::task_context>();
        network = std::make_shared<StrictMock<AsyncRun>>();

        msg.st_id = stid();
        msg.fid = defaultFid();
        msg.tab = tab();

        fidServicePtr = std::make_shared<StrictMock<MockFidService>>();
        envelopeServicePtr = std::make_shared<StrictMock<MockEnvelopeService>>();

        ON_CALL(*fidServicePtr, resolveFid)
                .WillByDefault([](const macs::Fid& fid) { return helpers::resolveFid(fid, defaultFid(), mapping()); });
    }

    auto mockDelivery() const {
        return std::make_shared<StrictMock<MockDelivery>>(
            "default@ya.ru", Delivery::SymbolLabels{}, fidServicePtr, envelopeServicePtr, params, ctx
        );
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(context, fn);
        context.run();
    }

    void restoreToUserFid() {
        msg.fid = userFid();
    }

    void restoreToDefaultFid() {
        msg.fid = defaultFid();
    }
};

struct ParseDeliveryResponse:
    public RestoreData,
    public TestWithParam<std::tuple<int, macs::Mid, mail_errors::error_code, std::string>>
{
    void SetUp() override {
        RestoreData::SetUp();
        Test::SetUp();
    }
};

INSTANTIATE_TEST_SUITE_P(DeliveryResponse, ParseDeliveryResponse,
    Values(
        std::make_tuple(successResponse()   .status, renewedMid(), mail_errors::error_code(),                 successResponse().body),
        std::make_tuple(invalidFidResponse().status, "",           make_error(DeliveryError::folderIssues),   invalidFidResponse().body),
        std::make_tuple(duplicateResponse() .status, ancientMid(), make_error(DeliveryError::duplicateFound), duplicateResponse().body),

        std::make_tuple(500, "", make_error(DeliveryError::temporaryFail),  ""),
        std::make_tuple(300, "", make_error(DeliveryError::temporaryFail),  ""),
        std::make_tuple(900, "", make_error(DeliveryError::temporaryFail),  ""),

        std::make_tuple(400, "", make_error(DeliveryError::permanentFail),  ""),

        std::make_tuple(406, "", make_error(DeliveryError::permanentFail),  R"({"code": "strange_code"})"),
        std::make_tuple(406, "", make_error(DeliveryError::temporaryFail),  R"({"code": "ServiceUnavaliable"})")
    )
);

TEST_P(ParseDeliveryResponse, shouldCheckDeliveryResponse) {
    int status;
    macs::Mid expectedMid;
    mail_errors::error_code expectedEc;
    std::string body;
    std::tie(status, expectedMid, expectedEc, body) = GetParam();

    spawn([=, this] (boost::asio::yield_context yield) {
        mail_errors::error_code ec;
        macs::Mid mid;

        EXPECT_CALL(*network, response())
            .WillOnce(Return(yhttp::response{.status=status, .body=body}));

        std::tie(mid, ec) = mockDelivery()->call(defaultFid(), msg, yield);

        EXPECT_EQ(mid, expectedMid);
        EXPECT_EQ(ec, expectedEc);
    });
}

struct RestoreFlowTest: public RestoreData, public Test {
    void SetUp() override {
        RestoreData::SetUp();
        Test::SetUp();
    }
};

TEST_F(RestoreFlowTest, shouldRestoreMessage) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(successResponse()));

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_EQ(mockDelivery()->restore(msg, yield).value(), renewedMid());
    });
}

TEST_F(RestoreFlowTest, shouldReturnErrorInCaseOfMissingDefaultFolder) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(invalidFidResponse()));

    restoreToDefaultFid();

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_CALL(Const(*fidServicePtr), getDefaultFid()).
                WillRepeatedly(ReturnRef(generatedDefaultFid));
        EXPECT_EQ(mockDelivery()->restore(msg, yield).error(), make_error(DeliveryError::folderIssues));
    });
}

TEST_F(RestoreFlowTest, shouldRestoreMessageInDefaultFolderInCaseOfUnexistentUserFolder) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(invalidFidResponse()))
        .WillOnce(Return(successResponse()));

    restoreToUserFid();

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_CALL(Const(*fidServicePtr), getDefaultFid()).
                WillRepeatedly(ReturnRef(generatedDefaultFid));
        EXPECT_EQ(mockDelivery()->restore(msg, yield).value(), renewedMid());
    });
}

TEST_F(RestoreFlowTest, shouldReturnErrorInCaseOfStrangeError) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(yhttp::response{.status=500, .body=""}));

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_EQ(mockDelivery()->restore(msg, yield).error(), make_error(DeliveryError::temporaryFail));
    });
}

TEST_F(RestoreFlowTest, shouldReturnOldMidInCaseOfDuplicate) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(duplicateResponse()));

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_CALL(*envelopeServicePtr, checkDuplicateMessageMistake(ancientMid(), _, _))
                .WillOnce(Return(false));

        EXPECT_EQ(mockDelivery()->restore(msg, yield).value(), ancientMid());
    });
}

TEST_F(RestoreFlowTest, shouldRestoreMessageToTemporaryFolderInCaseOfWrongDuplicate) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(duplicateResponse()))
        .WillOnce(Return(successResponse()));

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_CALL(*fidServicePtr, temporaryFid(_))
                .WillOnce(Return(someFid()));
        EXPECT_CALL(*envelopeServicePtr, moveMessage(renewedMid(), tab(), defaultFid(), _));
        EXPECT_CALL(*envelopeServicePtr, checkDuplicateMessageMistake(ancientMid(), _, _))
                .WillOnce(Return(true));

        EXPECT_EQ(mockDelivery()->restore(msg, yield).value(), renewedMid());
    });
}

TEST_F(RestoreFlowTest, shouldRestoreMessageToTemporaryFolderEvenWithInvalidFidError) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(invalidFidResponse()))
        .WillOnce(Return(duplicateResponse()))
        .WillOnce(Return(successResponse()));

    restoreToUserFid();

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_CALL(Const(*fidServicePtr), getDefaultFid()).
                WillRepeatedly(ReturnRef(generatedDefaultFid));
        EXPECT_CALL(*fidServicePtr, temporaryFid(_))
                .WillOnce(Return(someFid()));
        EXPECT_CALL(*envelopeServicePtr, moveMessage(renewedMid(), tab(), defaultFid(), _));
        EXPECT_CALL(*envelopeServicePtr, checkDuplicateMessageMistake(ancientMid(), _, _))
                .WillOnce(Return(true));

        EXPECT_EQ(mockDelivery()->restore(msg, yield).value(), renewedMid());
    });
}

TEST_F(RestoreFlowTest, shouldReturnErrorInCaseOfSeveralDuplicateErrors) {
    EXPECT_CALL(*network, response())
        .WillOnce(Return(duplicateResponse()))
        .WillOnce(Return(duplicateResponse()));

    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_CALL(*fidServicePtr, temporaryFid(_))
                .WillOnce(Return(someFid()));
        EXPECT_CALL(*envelopeServicePtr, checkDuplicateMessageMistake(ancientMid(), _, _))
                .Times(2)
                .WillRepeatedly(Return(true));

        EXPECT_EQ(mockDelivery()->restore(msg, yield).error(), make_error(DeliveryError::duplicateFound));
    });
}

TEST_F(RestoreFlowTest, shouldSpecialThrowAnExceptionInCaseOfCancelledContext) {
    ctx->cancel();
    spawn([=, this] (boost::asio::yield_context yield) {
        EXPECT_CALL(*fidServicePtr, resolveFid(_));
        EXPECT_THROW(mockDelivery()->restore(msg, yield).value(), ymod_queuedb::TaskControlDelayException);
    });
}

}
