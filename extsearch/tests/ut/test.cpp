#include <library/cpp/testing/unittest/registar.h>

#include <extsearch/geo/kernel/broto/tests/proto/example.pb.h>
#include <extsearch/geo/kernel/broto/tests/proto/example.pbro.h>

#include <google/protobuf/arena.h>

#include <util/memory/pool.h>

Y_UNIT_TEST_SUITE(TProtobROTest) {
    Y_UNIT_TEST(TestDefaultValues) {
        using namespace NOuterNS::NInnerNS::NRO;
        TPerson person;
        UNIT_ASSERT_VALUES_EQUAL(person.Name, "");
        UNIT_ASSERT_VALUES_EQUAL(person.Id, 0);
        UNIT_ASSERT_VALUES_EQUAL(person.Phone.size(), 0);

        TPerson::TPhoneNumber number;
        UNIT_ASSERT_EQUAL(number.Type, TPerson::EPhoneType::HOME);

        TExtendedPhoneNumber exNumber;
        Y_UNUSED(exNumber);
    }

    Y_UNIT_TEST(TestHasBits) {
        NOuterNS::NInnerNS::NRO::TPerson person;
        UNIT_ASSERT(!person.HasEmail());

        person.SetHasEmail();
        UNIT_ASSERT(person.HasEmail());

        person.ClearHasEmail();
        UNIT_ASSERT(!person.HasEmail());

        person.AssignHasEmail(true);
        UNIT_ASSERT(person.HasEmail());

        person.AssignHasEmail(false);
        UNIT_ASSERT(!person.HasEmail());
    }

    Y_UNIT_TEST(TestCopy) {
        using namespace NOuterNS::NInnerNS;

        TPerson person;
        person.SetName("John");
        person.SetId(123);
        person.SetEmail("smith@example.com");
        *person.MutableReferenceId()->Add() = 123;
        *person.MutableReferenceId()->Add() = 4567;
        auto* phone = person.MutablePhone() -> Add();
        phone->SetNumber("+375 (29) 012-34-56");
        phone->SetType(TPerson::MOBILE);

        TMemoryPool pool(100);
        NRO::TPerson personRO;
        NBroto::Copy(person, personRO, pool);

        UNIT_ASSERT_STRINGS_EQUAL(personRO.Name, "John");
        UNIT_ASSERT_VALUES_EQUAL(personRO.Id, 123);
        UNIT_ASSERT_STRINGS_EQUAL(personRO.Email, person.GetEmail());
        UNIT_ASSERT(personRO.HasEmail());
        UNIT_ASSERT_VALUES_EQUAL(personRO.ReferenceId.size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(personRO.ReferenceId[0], 123);
        UNIT_ASSERT_VALUES_EQUAL(personRO.ReferenceId[1], 4567);
        UNIT_ASSERT_VALUES_EQUAL(personRO.Phone.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(personRO.Phone[0].Number, "+375 (29) 012-34-56");
        UNIT_ASSERT_EQUAL(personRO.Phone[0].Type, NRO::TPerson::EPhoneType::MOBILE);

        UNIT_ASSERT(!personRO.HasExternal());
        UNIT_ASSERT_EQUAL(personRO.External, nullptr);
    }

    Y_UNIT_TEST(TestEnumSerialization) {
        using namespace NOuterNS::NInnerNS::NRO;
        UNIT_ASSERT_EQUAL(ToString(EPhoneOrFax::PhoneFax), "phone_or_fax");
        UNIT_ASSERT_EQUAL(FromString<EPhoneOrFax>("phone"), EPhoneOrFax::Phone);
    }

    Y_UNIT_TEST(TestNativeProtoMessagesOnArena) {
        NOuterNS::NInnerNS::TPerson person;
        person.SetName("Vasya");
        person.MutableExternal()->SetFoo("hello!");
        person.MutableExternal()->SetBar(42);

        TMemoryPool pool(100);
        google::protobuf::Arena arena;

        NOuterNS::NInnerNS::NRO::TPerson personRO;
        NBroto::Copy(person, personRO, pool, &arena);

        UNIT_ASSERT_STRINGS_EQUAL(personRO.Name, "Vasya");
        UNIT_ASSERT(personRO.External);
        UNIT_ASSERT(personRO.HasExternal());
        UNIT_ASSERT_VALUES_EQUAL(personRO.External->GetFoo(), "hello!");
        UNIT_ASSERT_VALUES_EQUAL(personRO.External->GetBar(), 42);
        UNIT_ASSERT(personRO.Externals.empty());
    }

    Y_UNIT_TEST(TestNativeProtoRepeatedMessages) {
        NOuterNS::NInnerNS::TPerson person;
        for (int i = 0; i < 42; ++i) {
            auto* msg = person.AddExternals();
            msg->SetFoo("foo_" + ToString(i));
            msg->SetBar(i * i);
        }

        TMemoryPool pool(100);
        google::protobuf::Arena arena;
        NOuterNS::NInnerNS::NRO::TPerson personRO;
        NBroto::Copy(person, personRO, pool, &arena);

        UNIT_ASSERT(!personRO.Externals.empty());
        UNIT_ASSERT_VALUES_EQUAL(person.ExternalsSize(), personRO.Externals.size());
        for (size_t i = 0; i < personRO.Externals.size(); ++i) {
            UNIT_ASSERT_VALUES_EQUAL(person.GetExternals(i).GetFoo(), personRO.Externals[i]->GetFoo());
            UNIT_ASSERT_VALUES_EQUAL(person.GetExternals(i).GetBar(), personRO.Externals[i]->GetBar());
        }
    }

    Y_UNIT_TEST(TestForgotArena) {
        NOuterNS::NInnerNS::TPerson person;
        NOuterNS::NInnerNS::NRO::TPerson personRO;

        TMemoryPool pool(100);
        UNIT_ASSERT_NO_EXCEPTION(NBroto::Copy(person, personRO, pool));
        person.MutableExternal();
        UNIT_ASSERT_EXCEPTION_CONTAINS(NBroto::Copy(person, personRO, pool), yexception, "arena pointer must be passed");
    }
}
