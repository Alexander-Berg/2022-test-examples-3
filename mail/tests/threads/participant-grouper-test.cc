#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/thread/structures/participant_grouper.h>
#include <macs/thread_participants_factory.h>
#include <boost/algorithm/string/join.hpp>
#include <boost/range/adaptors.hpp>
#include <boost/function.hpp>

namespace {

using namespace testing;
using namespace macs::pg;

using ThreadParticipants = macs::ThreadParticipants;
using ThreadParticipantsList = macs::ThreadParticipantsList;
using Participant = ThreadParticipants::Participant;
using Lids = std::initializer_list<int32_t>;
using reflection::ParticipantWithTid;

ParticipantWithTid makeParticipantWithTid(const int64_t mid, const int64_t tid,
                                          const Lids& lids, const std::string& from) {
    return {mid, tid, lids, from};
}

template<class Cont, class F>
std::set<std::string> unique(const Cont& c, F f) {
    std::set<std::string> res;
    for(const auto& i : c) {
        res.insert( f(i) );
    }
    return res;
}

struct ParticipantGrouperTest : public Test
{ };

TEST_F(ParticipantGrouperTest, testResultContainsOnlyUniqueTids) {
    using CRef = const ThreadParticipants&;
    using namespace boost;
    using namespace boost::adaptors;
    std::vector<ParticipantWithTid> data = {
        makeParticipantWithTid(0, 1, {}, "a1@ya.ru"),
        makeParticipantWithTid(1, 2, {}, "a2@ya.ru"),
        makeParticipantWithTid(2, 3, {}, "a3@ya.ru"),
        makeParticipantWithTid(3, 3, {}, "a3@ya.ru") };

    std::vector<std::string> tids;
    function<std::string(CRef)> getTid = [](CRef a){ return a.threadId(); };

    copy(ParticipantGrouper().groupByTid(data) | transformed(getTid),
         std::back_inserter(tids));

    ASSERT_THAT(tids, ElementsAre("1", "2", "3"));
}

TEST_F(ParticipantGrouperTest, checkGroupContainsFirstMidFromDataWithEqualEmails) {
    std::vector<ParticipantWithTid> data = {
        makeParticipantWithTid(0, 1, {0}, "a1@ya.ru"),
        makeParticipantWithTid(1, 1, {},  "a1@ya.ru") };

    auto group = ParticipantGrouper().groupByTid(data)[0];
    auto letter = group.participants()[0];
    EXPECT_EQ(letter.mid(), "0");
}

TEST_F(ParticipantGrouperTest, checkGroupContainsFirstLidsFromLettersWithEqualEmails) {
    std::vector<ParticipantWithTid> data = {
        makeParticipantWithTid(0, 1, {0}, "a1@ya.ru"),
        makeParticipantWithTid(1, 1, {},  "a1@ya.ru") };

    auto group = ParticipantGrouper().groupByTid(data)[0];
    auto letter = group.participants()[0];
    ASSERT_THAT(letter.types(), ElementsAre(0));
}

}
