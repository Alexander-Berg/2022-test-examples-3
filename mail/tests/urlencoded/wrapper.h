#pragma once

using Request = std::multimap<std::string, std::string>;

enum class Tag {
    get,
    header,
    post
};

template<class T, Tag tt>
struct Wrapper {
    using Type = T;
    static constexpr Tag tag = tt;

    T val;

    Wrapper() = default;
    Wrapper(T&& v) : val (std::move(v)) { }
    Wrapper(const T& v) : val (v) { }
    Wrapper(const Wrapper&) = default;

    operator const T&() const {return val;}
    operator T&() {return val;}

    Wrapper& operator=(const T& rhs) { val = rhs; return *this; }

    template<class D>
    bool operator==(const D& rhs) const {return val == rhs;}
};

template<class T, Tag tag>
std::istream& operator>>(std::istream& in, Wrapper<T, tag>& val) {
    in >> val.val;
    return in;
}

template<class T, Tag tag>
std::ostream& operator<<(std::ostream& out, const Wrapper<T, tag>& val) {
    out << val.val;
    return out;
}

template<class T> using Header = Wrapper<T, Tag::header>;
template<class T> using Get = Wrapper<T, Tag::get>;
template<class T> using Post = Wrapper<T, Tag::post>;

template<class T>
struct tagged: std::false_type { };

template<class T, Tag tag>
struct tagged<Wrapper<T, tag>>: std::true_type { };

template<class T>
static constexpr bool tagged_v = tagged<T>::value;
