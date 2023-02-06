#pragma once

#include <memory>
#include <tuple>

template <typename... Args>
class callback
{
    using args_tuple = std::tuple<Args...>;

public:
    void operator()(const Args&... args)
    {
        if (*called_) throw std::runtime_error("callback called twice");
        *called_ = true;
        *args_ = std::tuple{ args... };
    }

    const auto& args()
    {
        if (!called()) throw std::runtime_error("callback not called");
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
    std::shared_ptr<args_tuple> args_ = std::make_shared<args_tuple>();
};
