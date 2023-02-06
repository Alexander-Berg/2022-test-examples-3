#pragma once

#include <iostream>
#include <sstream>

class t_logger
{
public:
    t_logger(const char* severity)
    {
        ss << "[" << severity << "] ";
    }

    ~t_logger()
    {
        std::cout << ss.str() << std::endl;
    }

    template<typename T>
    t_logger& operator<<(const T& value)
    {
        ss << value;
        return *this;
    }

    std::stringstream ss;
};

#define YLOG_G(severity) t_logger(#severity)
