#include <crypta/cm/services/api/lib/logic/upload/request/upload_request.h>
#include <crypta/cm/services/api/lib/logic/upload/request/upload_request_validator.h>
#include <crypta/cm/services/common/data/id.h>
#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/data/matched_id.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NCm;
using namespace NCrypta::NCm::NApi;
using namespace NUploadRequestValidator;

Y_UNIT_TEST_SUITE(NUploadRequestValidator) {
    const TId EXT_ID("fpc", "1");
    const TId INT_ID("yandexuid", "1");

    Y_UNIT_TEST(Valid) {
        TMatch match(EXT_ID);
        match.AddId(TMatchedId(INT_ID, TInstant::Zero(), 0, {{SYNT_ATTRIBUTE, SYNT_TRUE_STR}}));

        UNIT_ASSERT_NO_EXCEPTION(Validate({.Match=match}));
    }

    Y_UNIT_TEST(ValidMultipleAttributes) {
        TMatch match(EXT_ID);
        match.AddId(TMatchedId(INT_ID, TInstant::Zero(), 0, {{SYNT_ATTRIBUTE, SYNT_TRUE_STR}, {REALTIME_ATTRIBUTE, REALTIME_FALSE_STR}}));

        UNIT_ASSERT_NO_EXCEPTION(Validate({.Match=match}));
    }

    Y_UNIT_TEST(InvalidSynt) {
        TMatch match(EXT_ID);
        match.AddId(TMatchedId(INT_ID, TInstant::Zero(), 0, {{SYNT_ATTRIBUTE, "-1"}}));

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            Validate({.Match=match}),
            yexception,
            "Invalid value '-1' for attribute 'synt'"
        );
    }

    Y_UNIT_TEST(InvalidMultipleAttributies) {
        {
            TMatch match(EXT_ID);
            match.AddId(TMatchedId(INT_ID, TInstant::Zero(), 0, {{SYNT_ATTRIBUTE, SYNT_FALSE_STR}, {REALTIME_ATTRIBUTE, "xyz"}}));

            UNIT_ASSERT_EXCEPTION_CONTAINS(
                Validate({.Match=match}),
                yexception,
                "Invalid value 'xyz' for attribute 'rt'"
            );
        }
    }

    Y_UNIT_TEST(EmptyExtId) {
        const auto& emptyIds = TVector<TId>({
            TId("", ""),
            TId("foo", ""),
            TId("", "bar"),
        });
        for (const auto& id : emptyIds) {
            TMatch match(id);
            match.AddId(TMatchedId(INT_ID, TInstant::Zero(), 0, {{SYNT_ATTRIBUTE, "-1"}}));

            UNIT_ASSERT_EXCEPTION_CONTAINS(Validate({.Match=match}), yexception, "Ext id must not be empty");
        }
    }

    Y_UNIT_TEST(ValidateAttributes) {
        ValidateAttributes({{SYNT_ATTRIBUTE, SYNT_TRUE_STR}});
        ValidateAttributes({{SYNT_ATTRIBUTE, SYNT_FALSE_STR}});
        ValidateAttributes({{REALTIME_ATTRIBUTE, REALTIME_TRUE_STR}});
        ValidateAttributes({{REALTIME_ATTRIBUTE, REALTIME_FALSE_STR}});
        ValidateAttributes({{REALTIME_ATTRIBUTE, REALTIME_FALSE_STR}, {SYNT_ATTRIBUTE, SYNT_TRUE_STR}});
        ValidateAttributes({{SYNT_ATTRIBUTE, SYNT_TRUE_STR}, {REALTIME_ATTRIBUTE, REALTIME_TRUE_STR}});
        ValidateAttributes({});
        UNIT_ASSERT_EXCEPTION_CONTAINS(ValidateAttributes({{SYNT_ATTRIBUTE, "xyz"}}), yexception, "Invalid value 'xyz' for attribute 'synt'");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ValidateAttributes({{REALTIME_ATTRIBUTE, "xyz"}}), yexception, "Invalid value 'xyz' for attribute 'rt'");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ValidateAttributes({{"xyz", SYNT_TRUE_STR}}), yexception, "Invalid attribute 'xyz'");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ValidateAttributes({{SYNT_ATTRIBUTE, SYNT_TRUE_STR}, {"xyz", SYNT_TRUE_STR}}), yexception, "Invalid attribute 'xyz'");
        UNIT_ASSERT_EXCEPTION_CONTAINS(ValidateAttributes({{REALTIME_ATTRIBUTE, REALTIME_TRUE_STR}, {"xyz", SYNT_TRUE_STR}}), yexception, "Invalid attribute 'xyz'");
    }
}
