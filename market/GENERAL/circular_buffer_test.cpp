#include <market/robotics/cv/library/cpp/tracking/circular_buffer.h>
#include <library/cpp/testing/unittest/registar.h>

#include <atomic>
#include <thread>

using namespace NWarehouseSDK;

/**
 * @brief TCircularBufferTest - test class for TCircularBuffer
 */
class TCircularBufferTest: public TTestBase {
    UNIT_TEST_SUITE(TCircularBufferTest);

    UNIT_TEST(CreationTest);
    UNIT_TEST(CreationZeroSizeTest);
    UNIT_TEST(PutAndTakeTest);
    UNIT_TEST(TwoThreadTest);
    UNIT_TEST(WaitTest);

    UNIT_TEST_SUITE_END();

public:
    void CreationTest() {
        TCircularBuffer<int> buffer(100);

        UNIT_ASSERT(buffer.IsEmpty());
        UNIT_ASSERT(!buffer.IsFull());
        UNIT_ASSERT_EQUAL(0, buffer.GetCount());
        UNIT_ASSERT_EQUAL(100, buffer.GetMaxSize());

        // No elements for now - get should fail
        int value{0};
        UNIT_ASSERT(!buffer.Take(value));
    }

    void CreationZeroSizeTest() {
        TCircularBuffer<int> buffer(0);

        UNIT_ASSERT(buffer.IsEmpty());
        UNIT_ASSERT(buffer.IsFull());
        UNIT_ASSERT_EQUAL(0, buffer.GetCount());
        UNIT_ASSERT_EQUAL(0, buffer.GetMaxSize());

        // No elements for now - get should fail
        int value{0};
        UNIT_ASSERT(!buffer.Take(value));

        // Put should fail because of zero size
        UNIT_ASSERT(!buffer.Put(value));
    }

    void PutAndTakeTest() {
        const int maxSize = 100;
        TCircularBuffer<int> buffer(maxSize);

        TVector<int> values(maxSize);
        for (int i = 0; i < maxSize; ++i)
            values.at(i) = i;

        // Put maxSize elements to the buffer
        for (auto& entry : values)
            UNIT_ASSERT(buffer.Put(entry));
        // Buffer should not overflow
        UNIT_ASSERT(!buffer.Put(42));

        // Get elements from the buffer with expected queue order
        for (auto& entry : values) {
            int taken = -1;
            UNIT_ASSERT(buffer.Take(taken));
            UNIT_ASSERT_EQUAL(entry, taken);
        }
        // Attempt to get more elements should fail
        int takenFinal = -1;
        UNIT_ASSERT(!buffer.Take(takenFinal));
    }

    void TwoThreadTest() {
        // One producer and one consumer
        // Producer puts more than maxCount elements to the buffer
        // Consumer takes elements from the buffer with order control

        const size_t maxSize = 10;
        const size_t maxCount = maxSize * 10;
        TCircularBuffer<size_t> buffer(maxSize);
        std::atomic<bool> workFlag{false};

        std::thread producer{
            [&buffer, &workFlag]() {
                while (!workFlag)
                    std::this_thread::yield();

                for (std::size_t i = 0; i < maxCount; ++i) {
                    while (!buffer.Put(i))
                        std::this_thread::yield();
                }
            }};

        std::thread consumer{
            [&buffer, &workFlag]() {
                while (!workFlag)
                    std::this_thread::yield();

                for (std::size_t i = 0; i < maxCount; ++i) {
                    std::size_t entry{0};
                    while (!buffer.Take(entry))
                        std::this_thread::yield();
                    UNIT_ASSERT_EQUAL(i, entry);
                    std::this_thread::yield();
                }
            }};

        workFlag = true;

        producer.join();
        consumer.join();
    }

    void WaitTest() {
        // One producer and one consumer
        // Producer puts more than maxCount elements to the buffer with waiting
        // Consumer takes elements from the buffer with order control

        const size_t maxSize = 10;
        const size_t maxCount = maxSize * 100;
        TCircularBuffer<size_t> buffer(maxSize);
        std::atomic<bool> workFlag{false};

        std::thread consumer{
            [&buffer, &workFlag]() {
                while (!workFlag)
                    std::this_thread::yield();

                for (std::size_t i = 0; i < maxCount; ++i) {
                    std::size_t entry{0};
                    buffer.TakeWait(entry);
                    UNIT_ASSERT_EQUAL(i, entry);
                    std::this_thread::yield();
                }
            }};
        std::this_thread::sleep_for(std::chrono::milliseconds(10));

        std::thread producer{
            [&buffer, &workFlag]() {
                while (!workFlag)
                    std::this_thread::yield();

                for (std::size_t i = 0; i < maxCount; ++i) {
                    buffer.PutWait(i);
                }
            }};

        workFlag = true;

        producer.join();
        consumer.join();
    }
};

UNIT_TEST_SUITE_REGISTRATION(TCircularBufferTest);