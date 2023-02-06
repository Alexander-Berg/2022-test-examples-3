#include "segment_node_differ.h"

#include <crypta/dmp/common/data/segment.h>
#include <crypta/dmp/common/data/segment_fields.h>
#include <crypta/dmp/common/data/segment_statuses.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/maybe.h>

using namespace NCrypta::NDmp;
using namespace NSegmentFields;

Y_UNIT_TEST_SUITE(SegmentNodeDiffer) {
    Y_UNIT_TEST(Diff) {
        const ui64 fromId = 7;
        const auto fromTitle = NYT::TNode()("ru_RU", "xxx");
        const auto fromHierarchy = NYT::TNode()("ru_RU", NYT::TNode().Add("Taxonomy XXX").Add("Auto"));
        const ui64 fromTariff = 1;
        const auto fromDescription = NYT::TNode()("ru_RU", "zzz");
        const auto fromAcl = NYT::TNode().Add("x").Add("z");
        const auto fromStatus = NSegmentStatuses::ENABLED;

        const ui64 toId = 11;
        const auto toTitle = NYT::TNode()("ru_RU", "zzz");
        const auto toHierarchy = NYT::TNode()("ru_RU", NYT::TNode::CreateList());
        const ui64 toTariff = 4;
        const auto toDescription = NYT::TNode()("ru_RU", "xxx");
        const auto toAcl = NYT::TNode().Add("z");
        const auto toStatus = NSegmentStatuses::DISABLED;

        const TSegment from = TSegment(7, fromTitle, fromHierarchy, 1, fromDescription, fromAcl, fromStatus, 1500000000);
        const TSegment to = TSegment(11, toTitle, toHierarchy, 4, toDescription, toAcl, toStatus, 1400000000);

        const auto diff = NYT::TNode()
            (ID, NYT::TNode()("from", fromId)("to", toId))
            (TITLE, NYT::TNode()("from", fromTitle)("to", toTitle))
            (HIERARCHY, NYT::TNode()("from", fromHierarchy)("to", toHierarchy))
            (TARIFF, NYT::TNode()("from", fromTariff)("to", toTariff))
            (DESCRIPTION, NYT::TNode()("from", fromDescription)("to", toDescription))
            (ACL, NYT::TNode()("from", fromAcl)("to", toAcl))
            (STATUS, NYT::TNode()("from", fromStatus)("to", toStatus));

        UNIT_ASSERT_EQUAL(DiffSegments(from, to), diff);
        UNIT_ASSERT(DiffSegments(from, from).Empty());
    }
}
