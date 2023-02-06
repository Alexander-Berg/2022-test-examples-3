#ifndef DOBERMAN_TESTS_WRAP_YIELD_H_
#define DOBERMAN_TESTS_WRAP_YIELD_H_

#include <src/meta/types.h>
#include "spawn_mock.h"

namespace doberman {
namespace testing {

struct Yield {
    mail_errors::error_code* ec_ = nullptr;
    SpawnMock* spawnMock_ = nullptr;
    friend Yield wrap(Yield yield) { return yield; }
    friend Yield wrap(Yield yield, mail_errors::error_code& ec) {
        yield.ec_ = std::addressof(ec);
        return yield;
    }
    void error(error_code ec) {
        if (ec_) {
            *ec_ = ec;
        } else {
            throw mail_errors::system_error(ec);
        }
    }

    template <typename T>
    friend void spawn(Yield yield, T f) {
        if (yield.spawnMock_) {
            yield.spawnMock_->spawn([yield, f] { f(yield); });
        } else {
            throw std::logic_error("spawnMock_ is not initialized");
        }
    }
};


} // namespace testing
} // namespace doberman

#endif /* DOBERMAN_TESTS_WRAP_YIELD_H_ */
