#ifndef TESTS_MAPPER_MOCK_H_115424092015
#define TESTS_MAPPER_MOCK_H_115424092015

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/utility.hpp>

namespace boost {

template <typename T>
std::ostream & operator << ( std::ostream & s, const optional<T> & v ) {
    if(v) {
        s << *v;
    } else {
        s << "NIL";
    }
    return s;
}

}

using namespace testing;

template <typename T>
struct MockMapValue {
    MOCK_METHOD2_T(mapValue, void(const T &, const std::string & ));
};

class MockMapper {
    template <typename T>
    using MockPtr = std::unique_ptr<MockMapValue<T>>;

    template <typename T>
    static MockPtr<T> & mockPtr() {
        static MockPtr<T> retval;
        return retval;
    }

    template <typename T>
    MockMapValue<T> & mock() const {
        if(nullptr == mockPtr<T>()) {
            mockPtr<T>().reset(new MockMapValue<T>());
            EXPECT_CALL(*mockPtr<T>(), mapValue(_, _)).WillRepeatedly(Return());
        }
        return *mockPtr<T>();
    }
public:
    template <typename T>
    void mapValue(const T & value, const std::string & name) const {
        mock<T>().mapValue(value, name);
    }

    template <typename T>
    class Guard : boost::noncopyable {
        MockPtr<T> & ptr;
    public:
        Guard(MockPtr<T> & ptr) : ptr(ptr) {}
        MockMapValue<T> & mock() const { return *ptr; }
        ~Guard() { ptr = nullptr; }
    };

    template <typename T>
    std::unique_ptr<Guard<T>> mapValueMock() const {
        mockPtr<T>().reset(new MockMapValue<T>());
        return std::unique_ptr<Guard<T>>( new Guard<T>(mockPtr<T>()));
    }
};

#endif /* TESTS_MAPPER_MOCK_H_115424092015 */
