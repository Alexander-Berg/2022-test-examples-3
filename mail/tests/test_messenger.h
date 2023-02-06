#pragma once

#include <set>
#include <map>

#include <ymod_messenger/ymod_messenger.h>
#include <ymod_messenger/types.h>
#include "../../ymod_messenger/src/notifier.h"

//#include "backtrace.h"

using std::string;
using std::set;

using ymod_messenger::hook_id_t;
using ymod_messenger::message_hook_t;
using ymod_messenger::message_type;
using ymod_messenger::address_t;
using ymod_messenger::message_type_NONE;
using ymod_messenger::event_hook_t;
using ymod_messenger::pool_type_t;
using ymod_messenger::segment_t;

// TODO move to ymod_messenger?

typedef ymod_messenger::module imessenger;

class test_pool
{
private:
    std::map<address_t, std::shared_ptr<ymod_messenger::messages_notifier>> notifiers;

public:
    static test_pool& instance()
    {
        static test_pool instance;
        return instance;
    }

    static void clear()
    {
        instance().notifiers.clear();
    }

    hook_id_t add_hook(const address_t from, const message_hook_t& hook, const message_type type)
    {
        if (!notifiers[from])
            notifiers[from] = std::make_shared<ymod_messenger::messages_notifier>();
        return notifiers[from]->add_hook(hook, type);
    }

    void send(const address_t from, const string& address, segment_t seg, const message_type type)
    {
        YLOG_G(info) << from << " send to " << address;
        if (notifiers.count(address))
        {
            notifiers[address]->notify(from, type, seg.to_shared_buffers());
        }
    }

    void send_all(
        const address_t from,
        set<address_t>& connections,
        segment_t seg,
        const message_type type)
    {
        for (auto iter = connections.begin(); iter != connections.end(); iter++)
        {
            YLOG_G(info) << from << " sendall to " << *iter;
            if (notifiers.count(*iter))
            {
                auto b = seg.to_shared_buffers();
                notifiers[*iter]->notify(from, type, b);
            }
        }
    }

private:
    test_pool()
    {
    }
    test_pool(test_pool const&);
    void operator=(test_pool const&);
};

class test_messenger : public imessenger
{
private:
    address_t address_;
    set<string> connections;

public:
    test_messenger(address_t my_address) : address_(my_address)
    {
    }

    const address_t& my_address() const
    {
        return address_;
    }

    void connect(const string& address)
    {
        connections.insert(address);
        for (auto iter = connections.begin(); iter != connections.end(); iter++)
        {
            YLOG_G(info) << address_ << " connected to " << *iter;
        }
    }

    void disconnect(const string& address)
    {
        connections.erase(address);
    }

    void connect_to_cluster(const std::set<address_t>& /*peers*/)
    {
        //
    }

    hook_id_t bind_messages(const message_hook_t& hook, const message_type type = message_type_NONE)
    {
        return test_pool::instance().add_hook(address_, hook, type);
    }

    hook_id_t bind_events(const event_hook_t& /*hook*/)
    {
        throw "Not implemented yet";
    }

    void do_send(const string& address, segment_t seg, const message_type type = message_type_NONE)
    {
        test_pool::instance().send(address_, address, seg, type);
    }

    void do_send_all(
        pool_type_t /*pool_type*/,
        segment_t seg,
        const message_type type = message_type_NONE)
    {
        test_pool::instance().send_all(address_, connections, seg, type);
    }
};
