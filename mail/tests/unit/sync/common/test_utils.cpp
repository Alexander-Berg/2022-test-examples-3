#include <gtest/gtest.h>

#include <src/sync/common/utils.hpp>

#include <tests/unit/services/db/events_queue/types/id_map_element.hpp>
#include <tests/unit/logic/interface/types/existing_contact.hpp>
#include <tests/unit/logic/interface/types/vcard.hpp>

namespace {

using collie::sync::common::IdMapElement;
using collie::sync::common::KeyId;
using collie::sync::common::ValueId;
using collie::sync::common::ContactInfo;
using collie::sync::common::ContactsInfo;
using collie::sync::common::ContactsInfoIdMap;
using collie::sync::common::ContactIdSearchMap;

using collie::sync::common::getDbCorrectionData;
using collie::sync::common::getKeyIdsFromIdMapElements;
using collie::sync::common::getValueIdsFromIdMapElements;
using collie::sync::common::makeContactInfoIdMap;
using collie::sync::common::makeContactIdSearchMap;

TEST(TestSyncCommonUtils, get_key_ids_from_id_map_elements) {
    EXPECT_EQ((std::vector<KeyId>{0, 1, 2}), getKeyIdsFromIdMapElements({{0, 3}, {1, 4}, {2, 5}}));
}

TEST(TestSyncCommonUtils, get_value_ids_from_id_map_elements) {
    EXPECT_EQ((std::vector<KeyId>{3, 4, 5}), getValueIdsFromIdMapElements({{0, 3}, {1, 4}, {2, 5}}));
}

TEST(TestSyncCommonUtils, make_contact_info_id_map) {
    ContactsInfo contactsInfo = {std::vector<ContactInfo>{
        {1, 1, 1, {}, {}, {}, {}} ,
        {3, 2, 1, {}, {}, {}, {}} ,
        {2, 3, 1, {}, {}, {}, {}} 
    }};
    ContactsInfoIdMap result = {
        {1, {1, 1, 1, {}, {}, {}, {}}},
        {3, {3, 2, 1, {}, {}, {}, {}}},
        {2, {2, 3, 1, {}, {}, {}, {}}}
    };
    EXPECT_EQ(result, makeContactInfoIdMap(contactsInfo));
}

TEST(TestSyncCommonUtils, make_contact_id_search_map) {
    ContactsInfoIdMap contactsInfo = {
        {4, {4, 1, 1, {}, {}, {}, {}}},
        {5, {5, 2, 1, {}, {}, {}, {}}},
        {3, {3, 3, 1, {}, {}, {}, {}}}
    };
    ContactIdSearchMap result = {
        {0, {3, 3, 1, {}, {}, {}, {}}},
        {1, {4, 1, 1, {}, {}, {}, {}}},
        {2, {5, 2, 1, {}, {}, {}, {}}}
    };
    EXPECT_EQ(result, makeContactIdSearchMap({{0, 3}, {1, 4}, {2, 5}}, contactsInfo));
}

TEST(TestSyncCommonUtils, get_db_correction_data) {
    std::vector<ValueId> contactIdsToDelete;
    std::vector<KeyId> keyIdsToDelete;
    const auto actualIdMapElements{getDbCorrectionData({5, 33, 4, 22}, {{1, 1}, {3, 22}, {2, 33}, {0, 0}},
            contactIdsToDelete, keyIdsToDelete)};
    EXPECT_EQ((std::vector<IdMapElement>{{3, 22}, {2, 33}}), actualIdMapElements);
    EXPECT_EQ((std::vector<ValueId>{4, 5}), contactIdsToDelete);
    EXPECT_EQ((std::vector<KeyId>{0, 1}), keyIdsToDelete);
}

}
