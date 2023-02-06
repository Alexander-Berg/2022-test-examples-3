#pragma once

#include <catch.hpp>

template <typename... Args>
class callback
{
    using args_t = std::tuple<Args...>;

public:
    void operator()(const Args&... args)
    {
        *called_ = true;
        *args_ = std::tuple{ args... };
    }

    const auto& args()
    {
        REQUIRE(called());
        return *args_;
    }

    bool called()
    {
        return *called_;
    }

    void reset()
    {
        *called_ = false;
    }

private:
    std::shared_ptr<bool> called_ = std::make_shared<bool>(false);
    std::shared_ptr<args_t> args_ = std::make_shared<args_t>();
};
