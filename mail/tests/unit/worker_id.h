#pragma once

#include <src/meta/types.h>

namespace doberman {
namespace testing {

struct WorkerId {
    ::macs::WorkerId id_;
    std::function<bool()> valid_;
    WorkerId() = default;
    WorkerId(::macs::WorkerId id, std::function<bool()> valid = []{return true;})
    : id_(id), valid_(valid) {}
    const ::macs::WorkerId& id() const { return id_;}
    operator ::macs::WorkerId () const { return id();}
    bool valid() const { return valid_(); }
    bool operator == (const WorkerId& other) const {
        return id() == other.id();
    }
    bool operator != (const WorkerId& other) const {
        return !(*this == other);
    }
};

} // namespace testing
} // namespace doberman
