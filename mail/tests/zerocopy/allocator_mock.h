#pragma once
/*
 * allocator_mock.h
 *
 *  Allocator mock class and necessary stuff for mocking
 */

#include <gtest.h>

namespace testing {

template <typename T>
struct AllocatorMock
{
    typedef T value_type;
    typedef std::size_t size_type;
    typedef T* pointer;
    MOCK_METHOD1_T(allocate, pointer(size_type size));
    MOCK_METHOD2_T(deallocate, void(pointer buf, size_type size));
};

template <typename Mock>
struct AllocatorMockWrapper
{
    Mock& mock;
    typedef typename Mock::value_type value_type;
    typedef typename Mock::size_type size_type;
    typedef typename Mock::pointer pointer;

    AllocatorMockWrapper(Mock& mock) : mock(mock)
    {
    }

    pointer allocate(size_type size)
    {
        return mock.allocate(size);
    }
    void deallocate(pointer buf, size_type size)
    {
        mock.deallocate(buf, size);
    }

    template <typename Other>
    struct rebind
    {
        typedef AllocatorMockWrapper<AllocatorMock<Other>> other;
    };
};

} // namespace testing
