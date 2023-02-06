#ifndef DOBERMAN_TESTS_SUBSCRIPTION_IO_H_
#define DOBERMAN_TESTS_SUBSCRIPTION_IO_H_

#include <src/logic/subscription.h>

namespace doberman {
namespace logic {

inline std::ostream& operator << (std::ostream& s, const boost::optional<SubscriptionData>& d) {
    if(!d) {
        return s << "{none}";
    }
    return s << "{ (" << d->id.uid << "," << d->id.id << "), " << d->folder.owner.uid << ", " << d->folder.fid << ", " << d->subscriber.uid << "}";
}

} // namespace logic
} // namespace doberman



#endif /* DOBERMAN_TESTS_SUBSCRIPTION_IO_H_ */
