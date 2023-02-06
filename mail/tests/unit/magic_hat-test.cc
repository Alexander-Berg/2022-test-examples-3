#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <src/detail/magic_hat.h>
#include <functional>
#include <string>
#include <memory>

namespace {

using namespace ::testing;
using ::doberman::detail::MagicHat;

struct DestructorMock {
    MOCK_METHOD(void, call, (), ());
};

struct Item {
    DestructorMock& mock;
    Item(DestructorMock& mock) : mock(mock) {}
    ~Item() { mock.call(); }
};

using ItemPtr = std::shared_ptr<Item>;

struct CallableMock {
    MOCK_METHOD(ItemPtr, call, (std::string), ());
};
struct MagicHatTest : public Test {
    StrictMock<CallableMock> ctor;
    MagicHat<std::string, std::function<ItemPtr(std::string)>> hat;
    MagicHatTest() : hat([&](std::string v){return ctor.call(v);}) {}
    void addFirstItem(std::string id) {
        EXPECT_CALL(ctor, call(_)).WillOnce(Return(ItemPtr{}));
        hat.get(id);
    }
};

TEST_F(MagicHatTest, get_forIdWithNoItems_callsItemConstructorWithId) {
    EXPECT_CALL(ctor, call("id")).WillOnce(Return(ItemPtr{}));
    hat.get("id");
}

TEST_F(MagicHatTest, get_forIdWithItem_doNotCallsItemConstructorWithId) {
    addFirstItem("id");
    hat.get("id");
}

TEST_F(MagicHatTest, item_whichIsNoMoreUsed_destroyedSeparateFromHat) {
    StrictMock<DestructorMock> dtor;
    StrictMock<DestructorMock> hatDtor;

    InSequence s;
    EXPECT_CALL(dtor, call()).WillOnce(Return());
    EXPECT_CALL(hatDtor, call()).WillOnce(Return());

    {
        auto hat = MagicHat<std::string, std::function<ItemPtr(std::string)>>{
            [&](std::string){return std::make_shared<Item>(dtor);}
        };
        {
            auto item = hat.get("id");
        }
        hatDtor.call();
    }
}

}
