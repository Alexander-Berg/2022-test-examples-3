#include <yplatform/time_traits.h>

using namespace yplatform::time_traits;

std::ostream& operator<<(std::ostream& os, const float_seconds& s)
{
    return os << s.count() << "s";
}

std::ostream& operator<<(std::ostream& os, const seconds& s)
{
    return os << s.count() << "s";
}

std::ostream& operator<<(std::ostream& os, const milliseconds& s)
{
    return os << s.count() << "ms";
}

std::ostream& operator<<(std::ostream& os, const microseconds& s)
{
    return os << s.count() << "us";
}

std::ostream& operator<<(std::ostream& os, const nanoseconds& s)
{
    return os << s.count() << "ns";
}
