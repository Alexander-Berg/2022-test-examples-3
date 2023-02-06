#pragma once
#include <atomic>
#include <util/generic/noncopyable.h>

struct TStatData {
    size_t BytesReceived = 0;
    size_t BytesSent = 0;
};

class TStatCounter: public TNonCopyable {
    public:
        void AdjustBytesReceived(size_t bytes) {
            BytesReceived += bytes;
        }

        void AdjustBytesSent(size_t bytes) {
            BytesSent += bytes;
        }

        void Reset(TStatData& output) {
            output.BytesReceived = BytesReceived.exchange(0);
            output.BytesSent = BytesSent.exchange(0);
        }

    private:
        std::atomic<size_t> BytesReceived = 0;
        std::atomic<size_t> BytesSent = 0;
};
