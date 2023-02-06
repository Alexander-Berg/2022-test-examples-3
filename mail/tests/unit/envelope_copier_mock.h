#ifndef DOBERMAN_TESTS_ENVELOPE_COPIER_MOCK_H_
#define DOBERMAN_TESTS_ENVELOPE_COPIER_MOCK_H_

#include <src/meta/types.h>
#include <gmock/gmock.h>

namespace doberman {
namespace testing {

using namespace ::testing;

template <typename SF, typename SBF>
struct EnvelopeCopierMock {
    MOCK_CONST_METHOD2_T(copy, void (const SF&, SBF&));
    void operator()(const SF& src, SBF& dst) const {
        copy(src, dst);
    }
};

} // namespace test
} // namespace doberman

#endif /* DOBERMAN_TESTS_ENVELOPE_COPIER_MOCK_H_ */
