#pragma once

#include <db/interface.h>
#include <yplatform/module.h>

namespace botserver::db {

struct fake_db
    : otp
    , links
    , yplatform::module
{
    struct code_entry
    {
        mail_account mail_account;
        string code = std::to_string(std::rand());
        std::time_t issued_at = std::time(nullptr);
        std::time_t ttl = 10;
    };

    void add(task_context_ptr, link link) override
    {
        mapping_[link.botpeer] = link.mail_account;
    }

    optional<link> lookup(task_context_ptr, botpeer botpeer) override
    {
        if (mapping_.count(botpeer))
        {
            return link{ botpeer, mapping_[botpeer] };
        }
        return {};
    }

    string gen_code(task_context_ptr, botpeer botpeer, mail_account mail_account) override
    {
        auto entry = code_entry{ .mail_account = mail_account };
        codes_[botpeer] = entry;
        return entry.code;
    }

    otp_check_result check_code(task_context_ptr, botpeer botpeer, string code) override
    {
        if (codes_.count(botpeer))
        {
            auto stored = codes_[botpeer];
            codes_.erase(botpeer);
            bool valid = stored.code == code;
            ;
            bool expired = stored.issued_at + stored.ttl <= std::time(nullptr);
            return otp_check_result{ valid && !expired, stored.mail_account };
        }
        return otp_check_result{ false, {} };
    }

    map<botpeer, mail_account> mapping_;
    map<botpeer, code_entry> codes_;
};

}
