#pragma once

#include <boost/smart_ptr.hpp>

#include <ymod_paxos/caller.h>
#include <ymod_paxos/db.h>
#include <ymod_paxos/packing.hpp>
#include <memory>

namespace ymod_paxos {

class test_sync_manager : public sync_manager
{
public:
    boost::optional<serialized_data_type> make_delta_request()
    {
        serialized_data_type data = pack(string("get-delta"));
        return data;
    }

    void apply_delta(const serialized_data_type& /*message*/)
    {
        ++done;
    }

    size_t remained() const
    {
        return total - done;
    }

    bool finished() const
    {
        return total == done;
    }

    size_t total;
    size_t done;
    string session_id;
};

class test_database : public abstract_database
{
public:
    iid_t revision_ = 0;
    bool events_are_modifying = true;
    std::map<string, int> data_;

public:
    bool is_ok() override
    {
        return true;
    }

    void apply(iid_t iid, operation op, std::weak_ptr<ymod_paxos::icaller> caller) override
    {
        if (auto pcaller = caller.lock())
        {
            pcaller->set_result(op.uniq_id(), "ok");
        }
        if (iid != -1)
        {
            revision_ = iid;
        }
    }

    bool is_modifying(const operation&) override
    {
        return events_are_modifying;
    }

    iid_t get_revision() override
    {
        return revision_;
    }

    sync_manager_ptr start_sync(const serialized_data_type& /*snapshot*/) override
    {
        return std::make_shared<test_sync_manager>();
    }

    void finish_sync(sync_manager_ptr /*diff*/) override
    {
    }

    serialized_data_type get_snapshot() override
    {
        return serialized_data_type();
    }

    serialized_data_type get_delta(const serialized_data_type& /*request*/) override
    {
        return serialized_data_type();
    }
};

}
