#pragma once

#include <list>
#include <boost/asio.hpp>
#include "../src/pool_manager.h"
#include "../src/session/messenger_session.h"
#include "../src/session_factory.h"
#include "stub_session.h"
#include "stub_resolver.h"
#include "defines.h"

namespace ymod_messenger {

struct stub_session_factory : public session_factory_interface
{

    stub_session_factory() : bad(false), calls(0)
    {
    }

    virtual void create_session(
        const host_info& address,
        const time_traits::duration&,
        const connect_hook_t& connect_hook,
        const timeout_hook_t& timeout_hook)
    {
        calls++;
        if (!bad) connect_hook(session_ptr(new stub_session(address)), error_code());
        else
            timeout_hook();
    }

    bool bad;
    int calls;
};

struct manual_stub_session_factory : public session_factory_interface
{

    manual_stub_session_factory()
    {
    }

    virtual void create_session(
        const host_info& address,
        const time_traits::duration&,
        const connect_hook_t& connect_hook,
        const timeout_hook_t& timeout_hook)
    {
        request_t request = { address, connect_hook, timeout_hook };
        requests.push_back(request);
    }

    session_ptr make_session(const host_info& address)
    {
        return session_ptr(new stub_session(address));
    }

    void clear()
    {
        requests.clear();
    }

    struct request_t
    {
        host_info address;
        connect_hook_t connect_hook;
        timeout_hook_t timeout_hook;
    };

    std::vector<request_t> requests;
};

}