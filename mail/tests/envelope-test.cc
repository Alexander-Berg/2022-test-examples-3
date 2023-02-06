#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <macs/macs.h>
#include <macs/tests/mocking-macs.h>
#include <chrono>

namespace {

using time_point = std::chrono::high_resolution_clock::time_point;
inline auto now() { return std::chrono::high_resolution_clock::now();}

using namespace ::testing;
using namespace ::macs;

struct EnvelopeTest : public Test {
    EnvelopeData data;
    EnvelopeTest() {
        data.mid = "1234567890";
        data.fid = "1";
        data.threadId = "1234567890";
        data.revision = 100;
        data.date = 0;
        data.receiveDate = 0;
        data.from = "volozh@yandex-team.ru";
        data.replyTo = "volozh@yandex-team.ru";
        data.subject = "We will not to force the Arcadia usage";
        data.cc = "pg@yandex-team.ru";
        data.bcc = "imperator@yandex-team.ru";
        data.to = "staff@yandex-team.ru";
        data.uidl = "16565312";
        data.imapId = "134293094763";
        data.size = 4096;
        data.stid = "167562358721369821370921";
        data.firstline = std::string(512, '8');
        data.labels = { "1", "2", "3", "4", "5", "6", "7", "8"};
        data.types = {1, 2, 3, 4, 5};
        data.inReplyTo = "";
        data.references = "";
        data.rfcId = "some_ugly_string@with_many_symbols";
        data.attachments = {};
        data.threadCount = 0;
        data.attachmentsCount = 0;
        data.attachmentsFullSize = 0;
        data.newCount = 0;
        data.extraData = "";
    }
};


TEST_F(EnvelopeTest, copyEnvelopeData_mustBeSlowerThan_copyEnvelope) {
    const int nTry = 100;
    std::vector<EnvelopeData> dataDst;
    dataDst.reserve(nTry);
    auto start = now();
    for(int i = 0; i!=nTry; ++i) {
        dataDst.push_back(data);
    }
    dataDst.clear();
    const auto copyTime = std::chrono::nanoseconds(now().time_since_epoch() - start.time_since_epoch());
    dataDst = {nTry, data};
    start = now();
    std::vector<EnvelopeData> moveDst{std::make_move_iterator(dataDst.begin()),
        std::make_move_iterator(dataDst.end())};
    moveDst.clear();
    const auto moveTime = std::chrono::nanoseconds(now().time_since_epoch() - start.time_since_epoch());
    std::vector<Envelope> envDst;
    envDst.reserve(nTry);
    Envelope env{std::make_shared<EnvelopeData>(data)};
    start = now();
    for(int i = 0; i!=nTry; ++i) {
        envDst.push_back(env);
    }
    env = Envelope{};
    envDst.clear();
    const auto shareTime = std::chrono::nanoseconds(now().time_since_epoch() - start.time_since_epoch());
    env = Envelope{std::make_shared<EnvelopeData>(data)};
    start = now();
    for(int i = 0; i!=nTry/2; ++i) {
        Envelope tmp{std::move(env)};
        env = std::move(tmp);
    }
    env = Envelope{};
    const auto shareMoveTime = std::chrono::nanoseconds(now().time_since_epoch() - start.time_since_epoch());
    std::cout << "Copy c-tor:" << copyTime.count() << std::endl
            << "Move c-tor:" << moveTime.count() << std::endl
            << "Copy shared_ptr:" << shareTime.count() << std::endl
            << "Move shared_ptr:" << shareMoveTime.count() << std::endl;
    ASSERT_TRUE(shareTime < moveTime);
}

}

