#pragma once

#include <common/random_uint_generator.h>

namespace yrpopper::mock {

class random_uint_generator
{
public:
    size_t operator()(size_t min, size_t max) const
    {
        last_generated = (min + max) / 2;
        return last_generated;
    }

    static size_t get_last_generated_uint()
    {
        return last_generated;
    }

private:
    inline static size_t last_generated = 100500;
};

} // namespace yrpopper::mock
