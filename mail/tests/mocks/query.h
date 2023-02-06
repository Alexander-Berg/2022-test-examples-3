#pragma once

#include <ymod_pq/call.h>
#include <yplatform/future/future.hpp>
#include <boost/shared_ptr.hpp>
#include <string>
#include <set>

namespace yrpopper::mock {

class query
{
public:
    std::string run_on_shard(const std::string& conninfo, ymod_pq::request_target /*target*/)
    {
        return conninfo;
    }
};

class query_returning_set
{
public:
    using ConninfoSet = std::set<std::string>;
    using ResType = boost::shared_ptr<ConninfoSet>;
    using ResultPromise = yplatform::future::promise<ResType>;
    using ResultFuture = yplatform::future::future<ResType>;

    ResultFuture run_on_shard(const std::string& conninfo, ymod_pq::request_target /*target*/)
    {
        ResultPromise prom;
        auto result = boost::make_shared<ConninfoSet>(ConninfoSet{ conninfo });
        prom.set(result);
        return prom;
    }
};

} // namespace yrpopper::mock