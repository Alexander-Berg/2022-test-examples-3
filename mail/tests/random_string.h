#include <boost/random/uniform_smallint.hpp>
#include <string>

class random_string
{
public:
    explicit random_string(size_t max_length = 256) : rnd_char_('a', 'z'), max_length_(max_length)
    {
    }

    template <typename Engine>
    std::string operator()(Engine& rnd)
    {
        return operator()(rnd, rnd() % (max_length_ + 1));
    }

    template <typename Engine>
    std::string operator()(Engine& rnd, size_t length)
    {
        std::string str;
        str.resize(length);
        for (std::string::iterator it = str.begin(); it != str.end(); ++it)
            *it = rnd_char_(rnd);
        return str;
    }

private:
    boost::uniform_smallint<char> rnd_char_;
    size_t max_length_;
};
