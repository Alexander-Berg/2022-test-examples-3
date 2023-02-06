#include <crypta/lib/native/thread/threaded.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <util/datetime/base.h>

using namespace NCrypta;

namespace {
    struct TSleeper: public TThreaded {
        void Run() override {
            while (IsRunning()) {
                Sleep(TDuration::Seconds(5));
            }
        }

        ~TSleeper() {
            StopAndJoin();
        }
    };
}

// Intended to be run under thread sanitizer
TEST(TThreaded, NoDataRaceInDestructor) {
    TSleeper sleeper;
    sleeper.Start();

    Sleep(TDuration::Seconds(1));
}

TEST(TThreaded, MultipleStopAndJoin) {
    TSleeper sleeper;
    sleeper.Start();

    Sleep(TDuration::Seconds(1));

    sleeper.StopAndJoin();
    sleeper.StopAndJoin();
    // and one more time in destructor
}
