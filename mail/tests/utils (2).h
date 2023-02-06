#ifndef MACS_PG_TESTS_UTILS_H
#define MACS_PG_TESTS_UTILS_H

#include <string>
#include <boost/algorithm/string/join.hpp>
#include <boost/range/adaptor/transformed.hpp>

namespace tests {

using Lids = std::initializer_list<int32_t>;

inline std::string makeLids(const Lids& c) {
    using boost::algorithm::join;
    using boost::adaptors::transformed;
    const std::function<std::string (int)> f = [] (int x) { return std::to_string(x); };
    return "[" + join(c | transformed(f), ", ") + "]";
}

inline std::string quote(const std::string& str) {
    return "\"" + str + "\"";
}

template <class T>
inline std::string keyValue(const std::string& key, const T& value) {
    return quote(key) + ": " + std::to_string(value);
}

template <>
inline std::string keyValue(const std::string& key, const std::string& value) {
    return quote(key) + ": " + quote(value);
}

} // namespace tests

#endif // MACS_PG_TESTS_UTILS_H
