#pragma once

#include <passport/client.h>

using namespace collectors::passport;

struct fake_passport : client
{
    using aliases = std::set<std::string>;
    using uid_aliases = std::map<uid, aliases>;

    void get_userinfo_by_suid(const suid& suid, const user_info_cb& cb) override
    {
        cb(code::ok, { suid, suid + "@imap.yandex.ru" });
    }

    void get_userinfo_by_uid(const uid& uid, const user_info_cb& cb) override
    {
        cb(code::ok, { uid, uid + "@imap.yandex.ru" });
    }

    void get_userinfo_by_login(const std::string& login, const user_info_cb& cb) override
    {
        auto delim_pos = login.find("@");
        cb(code::ok, { login.substr(0, delim_pos), login });
    }

    void check_auth_token(const std::string& token, address /*user_addr*/, const user_info_cb& cb)
        override
    {
        cb(code::ok, { token, token + "@imap.yandex.ru" });
    }

    void add_alias(
        const uid& uid,
        const std::string& alias,
        const std::string& /*consumer*/,
        const no_data_cb& cb) override
    {
        aliases_[uid].insert(alias);
        cb(code::ok);
    }

    void remove_alias(
        const uid& uid,
        const std::string& alias,
        const std::string& /*consumer*/,
        const no_data_cb& cb) override
    {
        auto uid_it = aliases_.find(uid);
        if (uid_it == aliases_.end())
        {
            return cb(code::ok);
        }

        auto alias_it = uid_it->second.find(alias);
        if (alias_it == uid_it->second.end())
        {
            return cb(code::ok);
        }
        uid_it->second.erase(alias_it);
        if (uid_it->second.empty())
        {
            aliases_.erase(uid_it);
        }
        cb(code::ok);
    }

    void get_suid(const uid& uid, const suid_cb& cb) override
    {
        cb(code::ok, uid);
    }

    void set_aliases(const uid_aliases& uid_aliases)
    {
        aliases_ = uid_aliases;
    }

    uid_aliases aliases_;
};

using fake_passport_ptr = std::shared_ptr<fake_passport>;

struct fake_passport_err_get_userinfo : fake_passport
{
    using fake_passport::fake_passport;

    void get_userinfo_by_suid(const std::string& /*suid*/, const user_info_cb& cb) override
    {
        cb(code::passport_error, {});
    }

    void get_userinfo_by_uid(const std::string& /*suid*/, const user_info_cb& cb) override
    {
        cb(code::passport_error, {});
    }
};

struct fake_passport_internal_err_check_auth_token : fake_passport
{
    using fake_passport::fake_passport;

    void check_auth_token(
        const std::string& /*token*/,
        address /*user_addr*/,
        const user_info_cb& cb) override
    {
        cb(code::passport_error, {});
    }
};

struct fake_passport_err_invalid_auth_token : fake_passport
{
    using fake_passport::fake_passport;

    void check_auth_token(
        const std::string& /*token*/,
        address /*user_addr*/,
        const user_info_cb& cb) override
    {
        cb(code::invalid_auth_token, {});
    }
};

struct fake_passport_err_alias_operations : fake_passport
{
    using fake_passport::fake_passport;

    void add_alias(
        const uid& /*uid*/,
        const std::string& /*alias*/,
        const std::string& /*consumer*/,
        const no_data_cb& cb) override
    {
        cb(code::passport_error);
    }

    void remove_alias(
        const uid& /*uid*/,
        const std::string& /*alias*/,
        const std::string& /*consumer*/,
        const no_data_cb& cb) override
    {
        cb(code::passport_error);
    }
};

enum class fake_passport_type
{
    type_normal,
    type_err_get_userinfo,
    type_internal_err_check_auth_token,
    type_err_invalid_auth_token,
    type_err_alias_operations
};
