#include <boost/optional.hpp>
#include <type_traits>

template <typename T, typename = std::void_t<> >
constexpr bool printable = false;

template <typename T>
constexpr bool printable<T, std::void_t<decltype(std::declval<std::ostream&>() << std::declval<T>())>> = true;

namespace boost {

template <typename T >
void PrintTo(const optional<T>& opt, std::ostream* os) {
    if (!opt) {
        *os << "<empty optional>";
        return;
    } 
    if constexpr (printable<T>) {
        *os << *opt;
    } else {
        *os << "<nonprintable>";
    }
}

}