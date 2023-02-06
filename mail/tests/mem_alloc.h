#pragma once

#include <cstdlib>
#include <iostream>
#include <boost/aligned_storage.hpp>
#include <boost/array.hpp>
#include <boost/bind.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/asio.hpp>

// Class to manage the memory to be used for handler-based custom allocation.
// It contains a single block of memory which may be returned for allocation
// requests. If the memory is in use when an allocation request is made, the
// allocator delegates allocation to the global heap.
template <int SIZE = 1024>
class handler_allocator : private boost::noncopyable
{
public:
    handler_allocator() : in_use_(false)
    {
    }

    void* allocate(std::size_t size)
    {
        if (!in_use_ && size < storage_.size)
        {
            in_use_ = true;
            return storage_.address();
        }
        else
        {
            std::cerr << "handler_allocator using new to allocate, size = " << size << "\n";

            return ::operator new(size);
        }
    }

    void deallocate(void* pointer)
    {
        if (pointer == storage_.address())
        {
            in_use_ = false;
        }
        else
        {
            ::operator delete(pointer);
        }
    }

    void* address()
    {
        return storage_.address();
    }

private:
    // Storage space used for handler-based custom memory allocation.
    boost::aligned_storage<SIZE> storage_;

    // Whether the handler-based custom allocation storage has been used.
    bool in_use_;
};

// Wrapper class template for handler objects to allow handler memory
// allocation to be customised. Calls to operator() are forwarded to the
// encapsulated handler.
template <typename Allocator, typename Handler>
class custom_alloc_handler
{
public:
    typedef custom_alloc_handler<Allocator, Handler> this_type;

    custom_alloc_handler(Allocator& a, Handler h) : allocator_(a), handler_(h)
    {
    }

    template <typename Arg1>
    void operator()(Arg1 arg1)
    {
        handler_(arg1);
    }

    template <typename Arg1, typename Arg2>
    void operator()(Arg1 arg1, Arg2 arg2)
    {
        handler_(arg1, arg2);
    }

    friend void* asio_handler_allocate(std::size_t size, this_type* this_handler)
    {
        void* ptr = this_handler->allocator_.allocate(size);
        return ptr;
    }

    friend void asio_handler_deallocate(void* ptr, std::size_t size, this_type* this_handler)
    {
        this_handler->allocator_.deallocate(ptr);
    }

    template <typename Function>
    friend void asio_handler_invoke(const Function& function, this_type* context)
    {
        using boost::asio::asio_handler_invoke;
        asio_handler_invoke(function, boost::addressof(context->handler_));
    }

private:
    Allocator& allocator_;
    Handler handler_;
};

// Helper function to wrap a handler object to add custom allocation.
template <typename Allocator, typename Handler>
inline custom_alloc_handler<Allocator, Handler> make_custom_alloc_handler(Allocator& a, Handler h)
{
    return custom_alloc_handler<Allocator, Handler>(a, h);
}