#ifndef DOBERMAN_TESTS_ENVELOPE_CMP_H_
#define DOBERMAN_TESTS_ENVELOPE_CMP_H_

#include <macs/envelope.h>
#include <src/meta/types.h>

namespace macs {

inline bool operator == (const ::macs::Envelope& lhs, const ::macs::Envelope& rhs) {
    return lhs.mid() == rhs.mid();
}

inline bool operator == (const macs::MimePart& lhs,
                         const macs::MimePart& rhs) {
    return lhs.hid() == rhs.hid();
}

} // namespace macs

namespace doberman {

inline bool operator == (const doberman::EnvelopeWithMimes& lhs,
                         const doberman::EnvelopeWithMimes& rhs) {
    return std::get<0>(lhs).mid() == std::get<0>(rhs).mid();
}

}


#endif /* DOBERMAN_TESTS_ENVELOPE_CMP_H_ */
