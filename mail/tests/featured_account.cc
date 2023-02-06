#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/akita/service/include/account/featured_account.h>


using namespace ::testing;

namespace akita::account::tests {

struct UserAccountTest: public ::testing::Test {

    struct CoUserInfoStorage
    {

        std::unique_ptr<FeaturedAccount> createUserInfoStorage() const
        {
            auto impl = std::make_unique<FeaturedAccount>();

            fillInternal( std::inserter(impl->internal, impl->internal.end()) );
            fillValidated( std::inserter(impl->validated, impl->validated.end()) );
            impl->defaultAddress = defaultAddress();
            impl->displayName = {displayName(), socialInfo(), avatar()};

            return impl;
        }

        virtual std::optional<SocialInfo> socialInfo() const = 0;

        static Address defaultAddress()
        {
            return "default@host.com";
        }

        static Address internalAddress()
        {
            return "login.test@yandex.ru";
        }

        static std::string displayName()
        {
            return "John Doe";
        }

        static std::string avatar()
        {
            return "avatar/id";
        }

        template < typename Ins >
        static Ins fillInternal( Ins ins )
        {
            *ins++ = internalAddress();
            *ins++ = "login.test@ya.ru";
            *ins++ = "login.test@narod.ru";
            *ins++ = "login.test@yandex.ua";
            *ins++ = "login.test@yandex.com";
            *ins++ = "login.test@yandex.by";
            *ins++ = "login.test@yandex.kz";
            return ins;
        }

        template < typename Ins >
        static Ins fillValidated( Ins ins )
        {
            *ins++ = defaultAddress();
            *ins++ = "login@validated.ru";
            *ins++ = "login@va.ru";
            *ins++ = "login@validated.ua";
            *ins++ = "login@validated.com";
            *ins++ = "login@validated.by";
            *ins++ = "login@validated.kz";
            return ins;
        }

    }; // struct CoUserInfoStorage

    struct CoUserInfoStorageWithSocialInfo : public CoUserInfoStorage
    {
        std::optional<SocialInfo> socialInfo() const override
        {
            return SocialInfo{ "profile", "provider", "target" };
        }
    }; // CoUserInfoStorageWithSocialInfo

    struct CoUserInfoStorageNoSocialInfo : public CoUserInfoStorage
    {
        std::optional<SocialInfo> socialInfo() const override
        {
            return std::nullopt;
        }
    }; //CoUserInfoStorageNoSocialInfo

    static Address defaultAddress()
    {
        return CoUserInfoStorage::defaultAddress();
    }

    static Address internalAddress()
    {
        return CoUserInfoStorage::internalAddress();
    }

    static std::string displayName()
    {
        return CoUserInfoStorage::displayName();
    }

    static Address badAddress()
    {
        return defaultAddress() + ".bad";
    }

    template < typename Ins >
    static Ins fillInternal( Ins ins )
    {
        return CoUserInfoStorage::fillInternal( ins );
    }

    template < typename Ins >
    static Ins fillValidated( Ins ins )
    {
        return CoUserInfoStorage::fillValidated( ins );
    }

    typedef std::unique_ptr<akita::FeaturedAccount> FeaturedAccountPtr;

    FeaturedAccountPtr getSocialAccount()
    {
        return CoUserInfoStorageWithSocialInfo().createUserInfoStorage();
    }

    FeaturedAccountPtr getNotSocialAccount()
    {
        return CoUserInfoStorageNoSocialInfo().createUserInfoStorage();
    }
};

TEST_F(UserAccountTest, testDefaultAddress) {
    FeaturedAccountPtr account( getSocialAccount() );

    EXPECT_TRUE( account->defaultAddress == defaultAddress() );
}

TEST_F(UserAccountTest, testInternal) {
    FeaturedAccountPtr account( getSocialAccount() );

    std::vector<Address> v;
    std::set<Address> internal;
    fillInternal( std::inserter(internal, internal.end()) );

    boost::copy(account->internal, std::back_inserter(v));

    EXPECT_TRUE( v.size() == internal.size() );
    EXPECT_TRUE( std::equal( v.begin(), v.end(), internal.begin() ) );
}

TEST_F(UserAccountTest, testValidated) {
    FeaturedAccountPtr account( getSocialAccount() );

    std::vector<Address> v;
    std::set<Address> validated;
    fillValidated( std::inserter(validated, validated.end()) );

    boost::copy(account->validated, std::back_inserter(v));

    EXPECT_TRUE( v.size() == validated.size() );
    EXPECT_TRUE( std::equal( v.begin(), v.end(), validated.begin() ) );
}

TEST_F(UserAccountTest, testDisplayNameSocial) {
    FeaturedAccountPtr account( getSocialAccount() );

    const akita::DisplayName & displayName( account->displayName );

    EXPECT_TRUE( displayName.socialInfo );
}

TEST_F(UserAccountTest, testDisplayNameNotSocial) {
    FeaturedAccountPtr account( getNotSocialAccount() );

    const DisplayName & displayName( account->displayName );

    EXPECT_FALSE( displayName.socialInfo );
}

}
