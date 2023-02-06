#ifndef DOBERMAN_TESTS_MOCK_EXTENTIONS_H_
#define DOBERMAN_TESTS_MOCK_EXTENTIONS_H_

#include <stdexcept>

namespace doberman {
namespace testing {

class InterruptAlgorithm : public std::exception {
    const char* what() const noexcept { return "algorithm interrupted by test"; }
};

#define EXPECT_INTERRUPTED(statement) EXPECT_THROW(statement, InterruptAlgorithm)

#define INTERRUPT_ON(a1, a2) EXPECT_CALL(a1, a2).WillOnce(Throw(InterruptAlgorithm{}))


template <typename T>
struct MockSingleton {
    static T* instance;
    MockSingleton() {
        if (!instance) {
            instance = static_cast<T*>(this);
        }
    }

    ~MockSingleton() {
        if (instance == static_cast<T*>(this)) {
            instance = nullptr;
        }
    }
};

template <typename T>
T* MockSingleton<T>::instance = nullptr;

#define PROXY_TO_MOCK_SINGLETON(method) \
    template <typename ... Args>\
    static auto method(Args&& ... args) { \
        return instance->method##_(std::forward<Args>(args)...); \
    }


} // namespace testing
} // namespace doberman



#endif /* DOBERMAN_TESTS_MOCK_EXTENTIONS_H_ */
