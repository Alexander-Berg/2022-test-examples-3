#include <macs/tests/mocking-macs.h>

namespace {

using namespace macs;
using namespace macs::io;
using namespace testing;

class EnvelopesQueryDeletedTest : public Test {
protected:
    EnvelopesQueryDeletedTest() : query(envelopes) {
    }

    const MockEnvelopesRepository envelopes;
    EnvelopesQueryDeleted query;
};

TEST_F(EnvelopesQueryDeletedTest, withTimeIntervalNotSet_shouldCallSimpleQuery) {
    const std::size_t from = 100;
    const std::size_t count = 10;

    EXPECT_CALL(envelopes, syncGetDeletedMessages(from, count, _))
            .WillOnce(InvokeArgument<2>());
    query.from(from).count(count).get();
}

TEST_F(EnvelopesQueryDeletedTest, withTimeIntervalSet_shouldCallRangeQuery) {
    const std::size_t from = 100;
    const std::size_t count = 10;
    const std::time_t dateFrom = 10000;
    const std::time_t dateTo = 20000;

    EXPECT_CALL(envelopes, syncGetDeletedMessagesInInterval(from, count, dateFrom, dateTo, _))
            .WillOnce(InvokeArgument<4>());
    query.from(from).count(count).withinInterval(std::make_pair(dateFrom, dateTo)).get();
}

}
