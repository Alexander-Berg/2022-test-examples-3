#pragma once

#include <boost/asio/error.hpp>
#include <map>
#include <vector>

struct resolver_fake
{
    struct iterator
    {
        bool operator==(const iterator& other)
        {
            if (!other.impl)
            {
                return impl == end;
            }
            return impl == other.impl;
        }

        bool operator!=(const iterator& other)
        {
            return !(*this == other);
        }

        iterator& operator++()
        {
            ++impl;
            return *this;
        }

        std::string operator*()
        {
            return *impl;
        }

        std::vector<std::string>::iterator impl, end;
    };

    using iterator_a = iterator;
    using iterator_aaaa = iterator;
    using handler_type = std::function<void(boost::system::error_code, iterator)>;
    using broken_hosts_map = std::map<std::string, boost::system::error_code>;
    using known_hosts_map = std::map<std::string, std::vector<std::string>>;

    void async_resolve_a(const std::string& host, const handler_type& handler)
    {
        if (broken_ipv4_hosts.count(host)) return handler(broken_ipv4_hosts[host], {});
        if (known_ipv4_hosts.count(host)) return handler({}, make_iterator(host, known_ipv4_hosts));
        handler(boost::asio::error::host_not_found, {});
    }

    void async_resolve_aaaa(const std::string& host, const handler_type& handler)
    {
        if (broken_ipv6_hosts.count(host)) return handler(broken_ipv6_hosts[host], {});
        if (known_ipv6_hosts.count(host)) return handler({}, make_iterator(host, known_ipv6_hosts));
        handler(boost::asio::error::host_not_found, {});
    }

    iterator make_iterator(const std::string& host, known_hosts_map known_hosts)
    {
        return { known_hosts[host].begin(), known_hosts[host].end() };
    }

    broken_hosts_map broken_ipv4_hosts, broken_ipv6_hosts;
    known_hosts_map known_ipv4_hosts, known_ipv6_hosts;
};
