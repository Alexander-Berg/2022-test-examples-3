#pragma once

#include <ymod_blackbox/client.h>

namespace ymod_blackbox {

struct fake_blackbox
    : ymod_blackbox::client
    , yplatform::module
{
    void async_info(
        const string& /*uid*/,
        const address& /*addr*/,
        const info_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_info not implemented");
    }

    void async_info(
        yplatform::task_context_ptr /*ctx*/,
        const string& /*uid*/,
        const address& /*addr*/,
        const info_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_info not implemented");
    }

    void async_info_suid(
        const string& /*suid*/,
        const string& /*sid*/,
        const address& /*addr*/,
        const info_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_info_suid not implemented");
    }

    void async_info_suid(
        yplatform::task_context_ptr /*ctx*/,
        const string& /*suid*/,
        const string& /*sid*/,
        const address& /*addr*/,
        const info_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_info_suid not implemented");
    }

    void async_info_login(
        const string& login,
        const string& /*sid*/,
        const address& /*addr*/,
        const info_callback& cb,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        string raw_resp = responses_by_login[login];
        bbresponse<bb::Response> res(std::move(raw_resp));
        cb(ymod_blackbox::error({}, ""), res);
    }

    void async_info_login(
        yplatform::task_context_ptr /*ctx*/,
        const string& login,
        const string& /*sid*/,
        const address& /*addr*/,
        const info_callback& cb,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        async_info_login(login, "", {}, cb, {}, {}, {});
    }

    void async_login(
        const string& /*login*/,
        const string& /*sid*/,
        const string& /*password*/,
        const address& /*addr*/,
        const login_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_login not implemented");
    }

    void async_login(
        yplatform::task_context_ptr /*ctx*/,
        const string& /*login*/,
        const string& /*sid*/,
        const string& /*password*/,
        const address& /*addr*/,
        const login_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_login not implemented");
    }

    void async_oauth(
        const string& /*oauth_token*/,
        const address& /*addr*/,
        const session_id_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_oauth not implemented");
    }

    void async_oauth(
        yplatform::task_context_ptr /*ctx*/,
        const string& /*oauth_token*/,
        const address& /*addr*/,
        const session_id_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_oauth not implemented");
    }

    void async_session_id(
        const string& /*session_id*/,
        const string& /*hostname*/,
        const address& /*addr*/,
        const session_id_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_session_id not implemented");
    }

    void async_session_id(
        yplatform::task_context_ptr /*ctx*/,
        const string& /*session_id*/,
        const string& /*hostname*/,
        const address& /*addr*/,
        const session_id_callback& /*cb*/,
        const options_list& /*opts*/,
        const db_fields_list& /*fields*/,
        const attribute_list& /*attributes*/) override
    {
        throw std::runtime_error("async_session_id not implemented");
    }

    void async_mhost_find(
        const string& /*scope*/,
        const string& /*prio*/,
        const mhost_find_callback& /*cb*/) override
    {
        throw std::runtime_error("async_mhost_find not implemented");
    }

    void async_mhost_find(
        yplatform::task_context_ptr /*ctx*/,
        const string& /*scope*/,
        const string& /*prio*/,
        const mhost_find_callback& /*cb*/) override
    {
        throw std::runtime_error("async_mhost_find not implemented");
    }

    std::map<string, string> responses_by_login = { { "no-addresses",
                                                      R"XML(<?xml version="1.0" encoding="UTF-8"?>
<doc>
<uid hosted="0">4000526059</uid>
<login>yapoptest302</login>
<have_password>1</have_password>
<have_hint>1</have_hint>
<karma confirmed="0">0</karma>
<karma_status>0</karma_status>
<address-list>
</address-list>
</doc>
)XML" },
                                                    { "non-native-address",
                                                      R"XML(<?xml version="1.0" encoding="UTF-8"?>
<doc>
<uid hosted="0">4000526059</uid>
<login>yapoptest302</login>
<have_password>1</have_password>
<have_hint>1</have_hint>
<karma confirmed="0">0</karma>
<karma_status>0</karma_status>
<address-list>
<address validated="1" default="1" rpop="0" silent="0" unsafe="0" native="0" born-date="2015-02-03 15:37:31">valid@ya.ru</address>
</address-list>
</doc>
)XML" },
                                                    { "non-validated-address",
                                                      R"XML(<?xml version="1.0" encoding="UTF-8"?>
<doc>
<uid hosted="0">4000526059</uid>
<login>yapoptest302</login>
<have_password>1</have_password>
<have_hint>1</have_hint>
<karma confirmed="0">0</karma>
<karma_status>0</karma_status>
<address-list>
<address validated="0" default="1" rpop="0" silent="0" unsafe="0" native="1" born-date="2015-02-03 15:37:31">valid@ya.ru</address>
</address-list>
</doc>
)XML" },
                                                    { "non-default-address",
                                                      R"XML(<?xml version="1.0" encoding="UTF-8"?>
<doc>
<uid hosted="0">4000526059</uid>
<login>yapoptest302</login>
<have_password>1</have_password>
<have_hint>1</have_hint>
<karma confirmed="0">0</karma>
<karma_status>0</karma_status>
<address-list>
<address validated="1" default="0" rpop="0" silent="0" unsafe="0" native="1" born-date="2015-02-03 15:37:31">valid@ya.ru</address>
</address-list>
</doc>
)XML" },
                                                    { "many-different-addresses",
                                                      R"XML(<?xml version="1.0" encoding="UTF-8"?>
<doc>
<uid hosted="0">4000526059</uid>
<login>yapoptest302</login>
<have_password>1</have_password>
<have_hint>1</have_hint>
<karma confirmed="0">0</karma>
<karma_status>0</karma_status>
<address-list>
<address validated="1" default="0" rpop="0" silent="0" unsafe="0" native="1" born-date="2015-02-03 15:37:31">invalid1@ya.ru</address>
<address validated="1" default="0" rpop="0" silent="0" unsafe="0" native="0" born-date="2015-02-03 15:37:31">invalid2@ya.ru</address>
<address validated="0" default="0" rpop="0" silent="0" unsafe="0" native="1" born-date="2015-02-03 15:37:31">invalid3@ya.ru</address>
<address validated="1" default="0" rpop="0" silent="0" unsafe="0" native="1" born-date="2015-02-03 15:37:31">invalid4@ya.ru</address>
<address validated="1" default="1" rpop="0" silent="0" unsafe="0" native="1" born-date="2015-02-03 15:37:31">valid@ya.ru</address>
<address validated="1" default="0" rpop="0" silent="0" unsafe="0" native="1" born-date="2015-02-03 15:37:31">invalid5@ya.ru</address>
</address-list>
</doc>
)XML" } };
};

}

using fake_blackbox = ymod_blackbox::fake_blackbox;
