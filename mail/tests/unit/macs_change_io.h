#ifndef DOBERMAN_TESTS_MACS_CHANGE_IO_H_
#define DOBERMAN_TESTS_MACS_CHANGE_IO_H_

#include <macs_pg/changelog/change.h>

namespace macs {

inline std::ostream& operator << (std::ostream& s, const Change& c) {
    return s << "{ changeId: " << c.changeId() << " type: " << c.type() << "}";
}

inline std::ostream& operator << (std::ostream& s, const boost::optional<macs::Change>& c) {
    if(!c) {
        return s << "{none}";
    }
    return s << *c;
}

} // namespace macs



#endif /* DOBERMAN_TESTS_MACS_CHANGE_IO_H_ */
