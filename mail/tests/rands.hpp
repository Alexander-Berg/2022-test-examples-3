/**
 * @file  rands.hpp
 * @brief  random genereator utilities for testd
 * @author Sergey Nikishin
 * © 2011 Yandex LLC.
 */
#ifndef MAXIMACS_TESTS_RANDS_HPP
#define MAXIMACS_TESTS_RANDS_HPP

#include <stdlib.h>
#include <string>
#include <time.h>

// generating random values
class Rands
{
public:
    /// get random int value
    static int getInt()
    {
        static bool initialized = false;
        if (!initialized)
        {
            srand(static_cast<unsigned>(time(0)));
            initialized = true;
        }
        return rand();
    }

    /// get random int value between min<->max
    static int getInt(int min_length, int max_length)
    {
        int result = min_length;
        if (max_length > min_length)
        {
            result += getInt() % (max_length - min_length);
        }
        return result;
    }

    /// get string of random letters with length in [min;max]
    static std::string getString(int min_length, int max_length)
    {
        int size = getInt(min_length, max_length);
        std::string result;
        result.reserve(max_length);
        for (int i = 0; i < size; ++i)
        {
            result = result + char(getInt() % 25 + 65);
        }
        return result;
    }
};

#endif /* MAXIMACS_TESTS_RANDS_HPP */
