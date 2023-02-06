#include <mail/notsolitesrv/src/user/storage.h>
#include <util/generic/algorithm.h>
#include <util/generic/yexception.h>
#include <gtest/gtest.h>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NUser;

struct TUserStorageTest: public Test {
    void GenerateUsers(size_t count, bool isFromRcpt, bool needDelivery) {
        do {
            Storage.AddUser("email" + std::to_string(count), isFromRcpt, needDelivery);
        } while (--count != 0);
    }

    using TExpected = std::vector<std::string>;
    template <typename TCont>
    TExpected UsersToEmails(const TCont& users) const {
        TExpected ret;
        std::transform(users.cbegin(), users.cend(), std::back_inserter(ret),
            [](const auto& userPair) { return userPair.first; });
        return ret;
    }

    TStorage Storage;
};

TEST_F(TUserStorageTest, Empty) {
    EXPECT_EQ(Storage.size(), 0ul);
    EXPECT_EQ(Storage.RecipientsCount(), 0ul);
    EXPECT_FALSE(Storage.IsMailish());
}

TEST_F(TUserStorageTest, AddUser) {
    Storage.AddUser("email", false, false);
    EXPECT_EQ(Storage.size(), 1ul);
    EXPECT_EQ(Storage.RecipientsCount(), 0ul);
}

TEST_F(TUserStorageTest, AddDuplicateUserDoesNotChangeStorage) {
    Storage.AddUser("email", false, false);
    EXPECT_EQ(Storage.size(), 1ul);
    EXPECT_EQ(Storage.RecipientsCount(), 0ul);

    Storage.AddUser("email", false, false);
    EXPECT_EQ(Storage.size(), 1ul);
    EXPECT_EQ(Storage.RecipientsCount(), 0ul);
}

TEST_F(TUserStorageTest, AddDuplicateUserWithDifferentFlagsUpdateFlags) {
    Storage.AddUser("email", false, false);
    EXPECT_EQ(Storage.size(), 1ul);
    EXPECT_EQ(Storage.RecipientsCount(), 0ul);
    const auto& user = Storage.GetUserByEmail("email");
    EXPECT_TRUE(user);
    EXPECT_FALSE(user->DeliveryParams.IsFromRcptTo);
    EXPECT_FALSE(user->DeliveryParams.NeedDelivery);

    Storage.AddUser("email", true, false);
    EXPECT_EQ(Storage.size(), 1ul);
    EXPECT_EQ(Storage.RecipientsCount(), 1ul);
    EXPECT_TRUE(user);
    EXPECT_TRUE(user->DeliveryParams.IsFromRcptTo);
    EXPECT_FALSE(user->DeliveryParams.NeedDelivery);

    Storage.AddUser("email", false, true);
    EXPECT_EQ(Storage.size(), 1ul);
    EXPECT_EQ(Storage.RecipientsCount(), 1ul);
    EXPECT_TRUE(user);
    EXPECT_TRUE(user->DeliveryParams.IsFromRcptTo);
    EXPECT_TRUE(user->DeliveryParams.NeedDelivery);
}

TEST_F(TUserStorageTest, AddUserIncorrectArgs) {
    EXPECT_THROW(Storage.AddUser("", true, true), yexception);
    EXPECT_EQ(Storage.size(), 0ul);
    EXPECT_EQ(Storage.RecipientsCount(), 0ul);
}

TEST_F(TUserStorageTest, AddUserRcptOrNeedDeliveryToMailishStorageThrows) {
    Storage.AddUser("email", true, true);
    Storage.SetMailish("123");
    ASSERT_TRUE(Storage.IsMailish());

    EXPECT_THROW(Storage.AddUser("email2", true, false), yexception);
    EXPECT_THROW(Storage.AddUser("email2", false, true), yexception);
}

TEST_F(TUserStorageTest, AddUserDummyToMailishStorage) {
    Storage.AddUser("email", true, true);
    Storage.SetMailish("123");
    ASSERT_TRUE(Storage.IsMailish());

    ASSERT_NO_THROW(Storage.AddUser("email2", false, false));
    EXPECT_EQ(Storage.size(), 2ul);
    EXPECT_EQ(Storage.RecipientsCount(), 1ul);
}

TEST_F(TUserStorageTest, GetUserByEmail) {
    Storage.AddUser("email", false, false);
    EXPECT_TRUE(Storage.GetUserByEmail("email"));
    EXPECT_FALSE(Storage.GetUserByEmail("email2"));
}

TEST_F(TUserStorageTest, EmptyStorageIgnoreSetMailish) {
    EXPECT_FALSE(Storage.SetMailish("1234"));
    EXPECT_FALSE(Storage.IsMailish());
}

TEST_F(TUserStorageTest, IncorrectSetMailishArg) {
    Storage.AddUser("email", true, true);
    EXPECT_FALSE(Storage.SetMailish(""));
    EXPECT_FALSE(Storage.SetMailish("123 aaa 123"));
    EXPECT_FALSE(Storage.IsMailish());
}

TEST_F(TUserStorageTest, SetMailish) {
    std::string uid{"123"};
    Storage.AddUser("email", true, true);
    EXPECT_TRUE(Storage.SetMailish(uid));
    EXPECT_TRUE(Storage.IsMailish());
    EXPECT_EQ(Storage.GetUserByEmail("email")->Uid, uid);
}

TEST_F(TUserStorageTest, CloneAsCopyToInboxUser) {
    GenerateUsers(3, true, true);
    EXPECT_EQ(Storage.size(), 3ul);
    EXPECT_EQ(Storage.RecipientsCount(), 3ul);

    auto& user = *Storage.GetUserByEmail("email2");
    EXPECT_TRUE(user.DeliveryParams.DeliveryId.empty());

    ASSERT_NO_THROW(Storage.CloneAsCopyToInboxUser("email2", user, "2"));
    EXPECT_EQ(Storage.size(), 4ul);
    EXPECT_EQ(Storage.RecipientsCount(), 3ul);
    auto copyToInboxUsersCount = CountIf(Storage.GetUsers(), [](const auto& u) { return u.second.DeliveryParams.CopyToInbox; });
    EXPECT_EQ(copyToInboxUsersCount, 1);
}

TEST_F(TUserStorageTest, CloneAsCopyToInboxUserWithinMailishStorageThrows) {
    Storage.AddUser("email", true, true);
    Storage.SetMailish("123");
    ASSERT_TRUE(Storage.IsMailish());

    auto& user = *Storage.GetUserByEmail("email");
    EXPECT_THROW(Storage.CloneAsCopyToInboxUser("email", user, "2"), yexception);
}

TEST_F(TUserStorageTest, CloneAsCopyToInboxUserInvalidArgs) {
    Storage.AddUser("email", true, true);

    auto& user = *Storage.GetUserByEmail("email");
    EXPECT_THROW(Storage.CloneAsCopyToInboxUser("", user, "2"), yexception);
    EXPECT_THROW(Storage.CloneAsCopyToInboxUser("email", user, ""), yexception);
    EXPECT_THROW(Storage.CloneAsCopyToInboxUser("email", user, "12 aaa 21"), yexception);

    user.DeliveryParams.NeedDelivery = false;
    EXPECT_THROW(Storage.CloneAsCopyToInboxUser("email", user, "12"), yexception);
}

TEST_F(TUserStorageTest, GetFoundUsers) {
    GenerateUsers(10, false, false);
    int i = 0;
    for (auto& [email, user]: Storage.GetUsers()) {
        user.Status = static_cast<ELoadStatus>(i++ % 3);
    }

    TExpected expected{"email2", "email5", "email8"};
    EXPECT_EQ(UsersToEmails(Storage.GetFilteredUsers(NUser::Found)), expected);
    EXPECT_TRUE(AllOf(Storage.GetFilteredUsers(NUser::Found), [](const auto& u) { return u.second.Status == ELoadStatus::Found; }));
}

TEST_F(TUserStorageTest, GetRcptUsers) {
    GenerateUsers(10, false, false);
    bool isRcpt = false;
    for (auto& [email, user]: Storage.GetUsers()) {
        user.DeliveryParams.IsFromRcptTo = isRcpt;
        isRcpt = !isRcpt;
    }

    TExpected expected{"email10", "email3", "email5", "email7", "email9"};
    EXPECT_EQ(UsersToEmails(Storage.GetFilteredUsers(NUser::FromRcptTo)), expected);
    EXPECT_TRUE(AllOf(Storage.GetFilteredUsers(NUser::FromRcptTo), [](const auto& u) { return u.second.DeliveryParams.IsFromRcptTo; }));
}

TEST_F(TUserStorageTest, GetFilteredUsers) {
    GenerateUsers(10, false, false);
    int i = 0;
    for (auto& [email, user]: Storage.GetUsers()) {
        user.Status = static_cast<ELoadStatus>(i++ % 3);
    }

    const auto& users = Storage.GetFilteredUsers([](const auto& u) { return EqualToOneOf(u.Status, ELoadStatus::Unknown, ELoadStatus::Found); });
    EXPECT_TRUE(!AnyOf(users, [](const auto& u) { return u.second.Status == ELoadStatus::Loaded; }));
}

TEST_F(TUserStorageTest, GetFilteredFilteredUsers) {
    GenerateUsers(9, false, false);
    int i = 0;
    for (auto& [email, user]: Storage.GetUsers()) {
        user.Status = static_cast<ELoadStatus>(i++ % 3);
        user.Uid = std::to_string(i);
    }

    const auto& users = Storage.GetFilteredUsers([](const auto& u) { return u.Status == ELoadStatus::Loaded; });
    const auto& evenUsers = users | [](const auto& u) { return std::stoi(u.Uid) % 2 == 0; };
    TExpected expected{"email2", "email8"};
    EXPECT_EQ(UsersToEmails(evenUsers), expected);
}

TEST_F(TUserStorageTest, GetFilteredEmptyUsers) {
    GenerateUsers(9, false, false);
    int i = 0;
    for (auto& [email, user]: Storage.GetUsers()) {
        user.Status = static_cast<ELoadStatus>(i++ % 3);
        user.Uid = std::to_string(i);
    }

    const auto& users = Storage.GetFilteredUsers([](const auto& u) { return u.Uid == "17"; });
    const auto& evenUsers = users | [](const auto& u) { return std::stoi(u.Uid) % 2 == 0; };
    TExpected expected{};
    EXPECT_EQ(UsersToEmails(evenUsers), expected);
}

TEST_F(TUserStorageTest, GetFilteredUsersLongPipeline) {
    GenerateUsers(9, false, false);
    int i = 0;
    for (auto& [email, user]: Storage.GetUsers()) {
        user.Status = static_cast<ELoadStatus>(i++ % 3);
        user.Uid = std::to_string(i);
        user.Suid = std::to_string(i / 2);
    }

    const auto& users = Storage.GetFilteredUsers([](const auto& u) { return u.Status == ELoadStatus::Loaded; });
    EXPECT_EQ(UsersToEmails(users), (TExpected{"email2", "email5", "email8"}));
    const auto& evenUsers = users | [](const auto& u) { return std::stoi(u.Uid) % 2 == 0; };
    EXPECT_EQ(UsersToEmails(evenUsers), (TExpected{"email2", "email8"}));
    const auto& smallSuid = evenUsers | [](const auto& u) { return std::stoi(u.Suid) < 3; };
    EXPECT_EQ(UsersToEmails(smallSuid), (TExpected{"email2"}));
}

TEST_F(TUserStorageTest, FilteredIterator) {
    auto foundUsers = Storage.GetFilteredUsers(NUser::Found);
    auto beg = foundUsers.begin();
    auto end = foundUsers.end();
    EXPECT_EQ(beg, end);

    Storage.AddUser("email", true, true);
    Storage.AddUser("not-found", true, true);
    Storage.GetUserByEmail("email")->Status = ELoadStatus::Found;
    EXPECT_NE(foundUsers.begin(), end);
    EXPECT_EQ(++foundUsers.begin(), end);
    EXPECT_EQ(std::distance(Storage.begin(), Storage.end()), 2);
    EXPECT_EQ(std::distance(foundUsers.begin(), end), 1);
}
