#pragma once

#include "web/messages.h"
#include <set>
#include <string>

namespace yxiva::detail {

using std::string;

template <typename Message>
string type_name()
{
    return typeid(Message).name();
}

template <typename... Args>
std::set<string> get_type_names(std::tuple<Args...>* = nullptr)
{
    return std::set<string>{ type_name<Args>()... };
}

auto& global_checked_types()
{
    static std::set<string> checked_types;
    return checked_types;
}

const auto& global_all_types()
{
    static std::set<string> all_types = get_type_names((web::xivaws_special_messages*)nullptr);
    return all_types;
}

}