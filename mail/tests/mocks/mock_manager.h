#pragma once

#include <mocks/mock.h>
#include <vector>
#include <memory>
#include <type_traits>
#include <typeinfo>

namespace yrpopper::mock {

class mock_manager
{
public:
    using mock_ptr = std::shared_ptr<yrpopper::mock::mock>;

    mock_manager(const std::initializer_list<mock_ptr>& mocks) : mocks_(mocks)
    {
    }

    void init_mock()
    {
        for (auto&& mock : mocks_)
        {
            mock->init_mock();
        }
    }

    void reset()
    {
        for (auto&& mock : mocks_)
        {
            mock->reset();
        }
    }

private:
    std::vector<mock_ptr> mocks_;
};

}
