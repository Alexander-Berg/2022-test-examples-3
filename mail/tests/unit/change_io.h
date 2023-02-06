#ifndef DOBERMAN_TESTS_CHANGE_IO_H_
#define DOBERMAN_TESTS_CHANGE_IO_H_

#include <src/logic/change.h>

namespace doberman {
namespace logic {

inline std::ostream& operator << (std::ostream& s, const logic::Change& ch) {
    return s << "{id:" << ch.id() << ", revision:" << ch.revision() << "}";
}

inline std::ostream& operator << (std::ostream& s, const boost::optional<logic::Change>& ch) {
    if (ch) {
        return s << ch;
    }
    return s << "{none}";
}

} // namespace logic
} // namespace doberman


#endif /* DOBERMAN_TESTS_CHANGE_IO_H_ */
