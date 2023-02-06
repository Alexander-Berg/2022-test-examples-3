#ifndef DOBERMAN_TESTS_CHANGE_MOCK_H_
#define DOBERMAN_TESTS_CHANGE_MOCK_H_

#include <src/logic/change.h>
#include <gmock/gmock.h>
#include "change_io.h"

namespace doberman {
namespace testing {

using namespace ::testing;

struct ChangeMock{
    using SubscribedFolder = logic::Change::SubscribedFolder;
    MOCK_METHOD(error_code, apply, (const SubscribedFolder&), (const));
};

inline auto makeChange(ChangeId id, Revision r, ChangeMock& mock) {
    return std::make_shared<logic::Change>(id, r,
            [&mock](auto& arg) { return mock.apply(arg); });
}

inline auto ReturnChange(ChangeId id, Revision r, ChangeMock& mock) {
    return Return(makeChange(id, r, mock));
}

inline auto ReturnChange(ChangeMock& mock) {
    return Return(makeChange({}, {}, mock));
}

} // namespace test

} // namespace doberman

#endif /* DOBERMAN_TESTS_CHANGE_MOCK_H_ */
