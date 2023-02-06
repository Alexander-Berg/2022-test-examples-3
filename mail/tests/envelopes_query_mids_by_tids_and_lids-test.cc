#include <macs/tests/mocking-macs.h>

namespace {

using namespace macs;
using namespace macs::io;
using namespace testing;

class EnvelopesQueryMidsByTidsAndLidsTest : public Test {
protected:
    EnvelopesQueryMidsByTidsAndLidsTest() : query(envelopes) {
    }

    const MockEnvelopesRepository envelopes;
    EnvelopesQueryMidsByTidsAndLids query;
    const Lids lids{"1", "2","3"};
    const Tids tids{"100", "101","102"};
    static const uint32_t handlerPosition{2u};
    error_code actualError;
};

TEST_F(EnvelopesQueryMidsByTidsAndLidsTest, correctWorkflow) {
    const Mids expectedMids{"1", "2"};
    EXPECT_CALL(envelopes, syncGetMidsByTidsAndLids(tids, lids, _)).WillOnce(WithArg<handlerPosition>(Invoke(
            [&expectedMids](const auto& handler){handler(expectedMids);})));

    const auto actualMids(query.withTids(tids).withLids(lids).get(use_sync[actualError]));
    EXPECT_FALSE(actualError);
    EXPECT_EQ(expectedMids, actualMids);
}

TEST_F(EnvelopesQueryMidsByTidsAndLidsTest, erroneousWorkflow) {
    const error_code expectedError{error::invalidArgument, "Message"};
    EXPECT_CALL(envelopes, syncGetMidsByTidsAndLids(tids, lids, _)).WillOnce(WithArg<handlerPosition>(Invoke(
            [&expectedError](const auto& handler){handler(expectedError);})));

    const auto actualMids(query.withTids(tids).withLids(lids).get(use_sync[actualError]));
    EXPECT_EQ(expectedError, actualError);
    EXPECT_EQ(Mids{}, actualMids);
}

}
