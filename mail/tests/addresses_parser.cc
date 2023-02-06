#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/libblackbox2/include/yandex/blackbox/accessors.h>
#include <mail/akita/service/include/blackbox/internal/addresses_parser.h>


using namespace ::testing;

namespace akita::tests {

struct AliasesMock {
    MOCK_METHOD(const std::string&, alias, (), (const));
    MOCK_METHOD(::bb::AliasList::Item::Type, type, (), (const));
};

struct AliasesMockWrapper {
    const std::string& alias() const {
        return mock->alias();
    }

    ::bb::AliasList::Item::Type type() const {
        return mock->type();
    }

    AliasesMock* mock;
};

TEST(ParseAliasesTest, shouldNotReturnMailishIdAndSso) {
    std::shared_ptr<AliasesMock> mailish = std::make_shared<AliasesMock>();
    std::shared_ptr<AliasesMock> regular = std::make_shared<AliasesMock>();
    std::shared_ptr<AliasesMock> sso = std::make_shared<AliasesMock>();

    const std::vector<AliasesMockWrapper> input{{mailish.get()}, {regular.get()}, {sso.get()}};
    std::set<std::string> output;

    const std::string mailishAddress = "mailish_id";
    const std::string regularAddress = "regular@ya.ru";

    EXPECT_CALL(*regular, type()).WillOnce(Return(::bb::AliasList::Item::Type::Mail));
    EXPECT_CALL(*mailish, type()).WillOnce(Return(::bb::AliasList::Item::Type::Mailish));
    EXPECT_CALL(*sso, type()).WillOnce(Return(::bb::AliasList::Item::Type::SSO));
    EXPECT_CALL(*regular, alias()).WillRepeatedly(ReturnRef(regularAddress));


    ::akita::blackbox::parseAliases(input, std::inserter(output, output.end()));

    EXPECT_THAT(output, UnorderedElementsAreArray({regularAddress}));
}

} // namespace akita::tests
