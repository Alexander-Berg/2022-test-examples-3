#include <market/library/shiny/server/gen/lib/tests/app/handler/hello.h>
#include <market/library/shiny/server/handler.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket::NShiny;
using namespace NMarket::NApp;

struct TTestEnv {};

Y_UNIT_TEST_SUITE(THelloTestSuite) {
    Y_UNIT_TEST(TestReturnsGreetingsOnGivenName) {
        TTestEnv env;
        THandler<THello> sut(env, "hello", EHttpMethod::GET);
        const auto response = sut.Invoke("name=Morty");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        UNIT_ASSERT_EQUAL(
            NSc::TValue::FromJson(response.Text)["greetings"],
            "Hello, Morty!"
        );
    }

    Y_UNIT_TEST(TestReturnsGreetingsInTextFormat) {
        TTestEnv env;
        THandler<THello> sut(env, "hello", EHttpMethod::GET);
        const auto response = sut.Invoke("name=Rick&format=text");
        UNIT_ASSERT_EQUAL(response.Code, 200);
        UNIT_ASSERT_EQUAL(response.Text, "Hello, Rick!");
    }

    Y_UNIT_TEST(TestReturnsErrorOnNameAbsence) {
        TTestEnv env;
        THandler<THello> sut(env, "hello", EHttpMethod::GET);
        UNIT_ASSERT_EQUAL(sut.Invoke("").Code, 400);
    }
}
