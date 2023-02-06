#include <server/command_parser.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

TEST(TEST_POP3, COMMAND_PARSER)
{
    using Lexems = std::vector<std::string>;

    {
        std::string command = "user yapoptest@yandex.ru\r\n";
        Lexems lexems;
        size_t lenth;

        auto result = ypop::parse_command(command, lexems, lenth);

        EXPECT_EQ(result, true);
        EXPECT_EQ(lexems.size(), 2u);
        EXPECT_EQ(lexems.at(0), "user");
        EXPECT_EQ(lexems.at(1), "yapoptest@yandex.ru");
    }

    {
        std::string firstPart = "user ";
        std::string secondPart = "yapoptest@yandex.ru\r";
        std::string thirdPart = "\n";
        Lexems lexems;
        size_t lenth;

        auto firstResult = ypop::parse_command(firstPart, lexems, lenth);
        auto secondResult = ypop::parse_command(secondPart, lexems, lenth);
        auto thirdResult = ypop::parse_command(thirdPart, lexems, lenth);

        EXPECT_EQ(firstResult, false);
        EXPECT_EQ(secondResult, false);
        EXPECT_EQ(thirdResult, true);

        EXPECT_EQ(lexems.size(), 2u);
        EXPECT_EQ(lexems.at(0), "user");
        EXPECT_EQ(lexems.at(1), "yapoptest@yandex.ru");
    }
}
