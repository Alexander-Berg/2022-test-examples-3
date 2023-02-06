#pragma once

namespace york {
namespace tests {

struct TimerMock {
    template <typename Duration>
    void wait(Duration&&, int) {}
};

} // namespace tests
} // namespace york

