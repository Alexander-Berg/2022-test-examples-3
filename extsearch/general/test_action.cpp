#include "extsearch/ymusic/indexer/lib/action.h"

#include <library/cpp/testing/unittest/registar.h>


Y_UNIT_TEST_SUITE(TRemapActionTypeTest) {
    Y_UNIT_TEST(TestDelete) {
        UNIT_ASSERT_EQUAL(NSaas::TAction::atDelete, RemapActionType("delete"));
    }

    Y_UNIT_TEST(TestModify) {
        UNIT_ASSERT_EQUAL(NSaas::TAction::atModify, RemapActionType("modify"));
        UNIT_ASSERT_EQUAL(NSaas::TAction::atModify, RemapActionType("new"));
    }

    Y_UNIT_TEST(TestUnknown) {
        UNIT_ASSERT_EXCEPTION(RemapActionType("something"), yexception);
    }

}

