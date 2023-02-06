#pragma once

#include <gmock/gmock.h>

#include <pgg/service/uid_resolver.h>

namespace pgg {

static inline bool operator ==(const UidResolveParams& lhs, const UidResolveParams& rhs) {
    return lhs.userId_ == rhs.userId_ && lhs.endpointType_ == rhs.endpointType_ && lhs.force_ == rhs.force_;
}

} // namespace pgg

namespace collie::tests {

struct UidResolverMock : pgg::UidResolver {
    MOCK_METHOD(void, asyncGetConnInfo, (const pgg::UidResolveParams&, pgg::OnResolve), (const, override));
    MOCK_METHOD(void, asyncGetShardName, (const pgg::UidResolveParams&, pgg::OnShardName), (const, override));
};

struct UidResolver : pgg::UidResolver {
    UidResolverMock* mock_ = nullptr;

    UidResolver(UidResolverMock& mock) : mock_(&mock) {}

    void asyncGetConnInfo(const pgg::UidResolveParams& p, pgg::OnResolve h) const {
        return mock_->asyncGetConnInfo(p, h);
    }

    void asyncGetShardName(const pgg::UidResolveParams& p, pgg::OnShardName h) const {
        return mock_->asyncGetShardName(p, h);
    }
};

} // namespace
