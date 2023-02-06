#ifndef DOBERMAN_TESTS_LABELS_H_
#define DOBERMAN_TESTS_LABELS_H_

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/meta/labels.h>

namespace doberman {
namespace testing {

using namespace ::testing;

namespace labels {

using Symbol = ::macs::Label::Symbol;
using Type = ::macs::Label::Type;

using ::macs::Label;
using ::macs::Lid;
using ::macs::LabelSet;
using Lids = std::vector<Lid>;
using Labels = std::vector<Label>;

inline auto label(Lid lid, Symbol symbol) {
    return ::macs::LabelFactory{}
            .lid(std::move(lid))
            .symbol(std::move(symbol))
            .type(std::move(Type::system))
            .product();
}

inline auto label(Lid lid, std::string name = "", Type type = Type::user) {
    return ::macs::LabelFactory{}
            .lid(std::move(lid))
            .name(std::move(name))
            .type(std::move(type))
            .product();
}

inline void makeSetImpl(LabelSet& ) {}

template <typename ... Args>
inline void makeSetImpl(LabelSet& retval, Label l, Args&& ...args) {
    const auto lid = l.lid();
    retval[lid] = std::move(l);
    makeSetImpl(retval, std::forward<Args>(args)...);
}

template <typename ... Args>
inline LabelSet makeSet(Args&& ...args) {
    LabelSet retval;
    makeSetImpl(retval, std::forward<Args>(args)...);
    return retval;
}

inline auto makeLabelsCache(::macs::LabelSet labels) {
    return meta::labels::LabelsCache(labels, [l = std::move(labels)]{ return l; });
}

} // namespace labels
} // namespace testing
} // namespace doberman

#endif /* DOBERMAN_TESTS_LABELS_H_ */
