#ifndef DOBERMAN_TESTS_CHANGE_FILTER_MOCK_H_
#define DOBERMAN_TESTS_CHANGE_FILTER_MOCK_H_

#include <src/logic/change.h>
#include <gmock/gmock.h>

namespace doberman {
namespace testing {

using namespace ::testing;

template <typename SF>
struct ChangeFilterMock {
    MOCK_CONST_METHOD2_T(applicable, std::tuple<bool, error_code> (const logic::Change&, const SF&));
    auto operator()(const logic::Change& c, const SF& sf) const { return applicable(c, sf); }
};

} // namespace test
} // namespace doberman

#endif /* DOBERMAN_TESTS_CHANGE_FILTER_MOCK_H_ */
