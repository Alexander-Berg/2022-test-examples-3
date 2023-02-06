#pragma once

#include "service_manager/interface.h"

namespace yxiva {

inline std::shared_ptr<const service_data> make_service_data(const service_properties& data)
{
    service_data service;
    service.properties = data;
    return std::make_shared<const service_data>(service);
}

inline std::shared_ptr<const service_data> make_service_data(
    const string& name,
    const string& owner_id,
    const std::vector<std::tuple<string, tvm_app_info>>& publishers_id = {},
    const std::vector<std::tuple<string, tvm_app_info>>& subscribers_id = {})
{
    service_properties properties;
    properties.name = name;
    properties.owner_id = owner_id;
    for (auto&& [environment, tvm_app] : publishers_id)
        properties.tvm_publishers[environment].insert(tvm_app);
    for (auto&& [environment, tvm_app] : subscribers_id)
        properties.tvm_subscribers[environment].insert(tvm_app);
    return make_service_data(properties);
}

}
