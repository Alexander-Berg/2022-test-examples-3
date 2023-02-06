#ifndef MACS_TESTS_COMPARE_LABELS_H
#define MACS_TESTS_COMPARE_LABELS_H

#include <macs/label.h>

namespace macs {

    inline bool operator ==(const macs::Label& first, const macs::Label& second) {
        return first.lid() == second.lid()
            && first.name() == second.name()
            && first.color() == second.color();
    }

} // namespace macs

#endif // MACS_TESTS_COMPARE_LABELS_H
