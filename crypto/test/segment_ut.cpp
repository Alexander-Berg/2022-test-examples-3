#include "segment.h"
#include "segment_statuses.h"

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NDmp;

Y_UNIT_TEST_SUITE(Segment) {
    Y_UNIT_TEST(Status) {
        TSegment segment(
            1,
            NYT::TNode()("ru_RU", "x"),
            NYT::TNode()("ru_RU", NYT::TNode().Add("Taxonomy XXX")),
            4,
            NYT::TNode::CreateEntity(),
            NYT::TNode::CreateEntity(),
            NSegmentStatuses::DELETED,
            1500000000);

        segment.SetEnabled();
        UNIT_ASSERT(segment.IsEnabled());
        UNIT_ASSERT(!segment.IsDisabled());
        UNIT_ASSERT(!segment.IsDeleted());

        segment.SetDisabled();
        UNIT_ASSERT(!segment.IsEnabled());
        UNIT_ASSERT(segment.IsDisabled());
        UNIT_ASSERT(!segment.IsDeleted());

        segment.SetDeleted();
        UNIT_ASSERT(!segment.IsEnabled());
        UNIT_ASSERT(!segment.IsDisabled());
        UNIT_ASSERT(segment.IsDeleted());
    }
}
