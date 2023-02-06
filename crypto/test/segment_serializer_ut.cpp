#include "segment_serializer.h"

#include <crypta/dmp/common/data/segment_fields.h>
#include <crypta/dmp/common/data/segment_statuses.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/maybe.h>

using namespace NCrypta::NDmp;

Y_UNIT_TEST_SUITE(SegmentSerializer) {
    Y_UNIT_TEST(Optional) {
        const NYT::TNode Title = NYT::TNode()("en_US", "Soul");
        const NYT::TNode Hierarchy = NYT::TNode()("en_US", NYT::TNode().Add("Taxonomy XXX").Add("Auto").Add("Kia"));
        const NYT::TNode Description = NYT::TNode()("en_US", "Kia Soul");
        const NYT::TNode Acl = NYT::TNode().Add("xxx").Add("zzz");
        const TSegment segment(1, Title, Hierarchy, 1, Description, Acl, NSegmentStatuses::DELETED, 1500000000);
        const NYT::TNode node = NYT::TNode()
            (NSegmentFields::ID, 1u)
            (NSegmentFields::TITLE, Title)
            (NSegmentFields::HIERARCHY, Hierarchy)
            (NSegmentFields::TARIFF, 1u)
            (NSegmentFields::DESCRIPTION, Description)
            (NSegmentFields::ACL, Acl)
            (NSegmentFields::STATUS, NSegmentStatuses::DELETED)
            (NSegmentFields::TIMESTAMP, 1500000000u);
        UNIT_ASSERT_EQUAL(NSegmentSerializer::Serialize(segment), node);
        UNIT_ASSERT_EQUAL(NSegmentSerializer::Deserialize(node), segment);
    }

    Y_UNIT_TEST(Required) {
        const NYT::TNode Title = NYT::TNode()("en_US", "Soul");
        const NYT::TNode Hierarchy = NYT::TNode()("en_US", NYT::TNode().Add("Taxonomy XXX").Add("Auto").Add("Kia"));
        const NYT::TNode Description = NYT::TNode::CreateEntity();
        const NYT::TNode Acl = NYT::TNode::CreateEntity();
        const TSegment segment(1, Title, Hierarchy, 1, Description, Acl, NSegmentStatuses::DISABLED, 1500000000);
        const NYT::TNode node = NYT::TNode()
            (NSegmentFields::ID, 1u)
            (NSegmentFields::TITLE, Title)
            (NSegmentFields::HIERARCHY, Hierarchy)
            (NSegmentFields::TARIFF, 1u)
            (NSegmentFields::DESCRIPTION, Description)
            (NSegmentFields::ACL, Acl)
            (NSegmentFields::STATUS, NSegmentStatuses::DISABLED)
            (NSegmentFields::TIMESTAMP, 1500000000u);
       UNIT_ASSERT_EQUAL(NSegmentSerializer::Serialize(segment), node);
       UNIT_ASSERT_EQUAL(NSegmentSerializer::Deserialize(node), segment);
    }
}
