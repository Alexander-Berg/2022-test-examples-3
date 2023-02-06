#ifndef DOBERMAN_TESTS_LABEL_CMP_H_
#define DOBERMAN_TESTS_LABEL_CMP_H_

#include <macs/label.h>

namespace macs {

inline bool operator == (const macs::Label& lhs, const macs::Label& rhs) {
    return lhs.lid() == rhs.lid() && lhs.name() == rhs.name()
            && lhs.symbolicName() == rhs.symbolicName()
            && lhs.type() == rhs.type();
}

}

#endif /* DOBERMAN_TESTS_LABEL_CMP_H_ */
