#include <vector>
#include <iostream>

#if !defined(_STLPORT_VERSION)
#if defined(__GNUC__) && __GNUC__ == 3

template <class T>
std::ostream & std::operator <<(std::ostream & out,
const typename __gnu_cxx::__normal_iterator<T *, std::vector<T , std::allocator<T > > > it) {
    return out << &(*it);
}

#endif
#endif
