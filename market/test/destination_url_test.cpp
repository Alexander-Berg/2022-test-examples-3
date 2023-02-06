#include <market/report/src/money/destination_url.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport::NMoney;

TEST(AddReservedToQueryTest, Empty)
{
    // both empty
    EXPECT_EQ("", AddReservedToQuery("", TCgiParameters()));

    // empty original
    {
        TCgiParameters reserved = {std::make_pair("a", "A"), std::make_pair("b", "B")};
        EXPECT_EQ("a=A&b=B", AddReservedToQuery("", reserved));
    }

    // empty reserved
    EXPECT_EQ("a=A&b=B", AddReservedToQuery("a=A&b=B", TCgiParameters()));

    // extra '&' in original
    EXPECT_EQ("&a=A", AddReservedToQuery("&a=A", TCgiParameters()));
    EXPECT_EQ("a=A&", AddReservedToQuery("a=A&", TCgiParameters()));
    EXPECT_EQ("&a=A&", AddReservedToQuery("&a=A&", TCgiParameters()));
}

TEST(AddReservedToQueryTest, NoIntersection)
{
    {
        TCgiParameters reserved = {std::make_pair("c", "C"), std::make_pair("d", "D")};
        EXPECT_EQ("a=A&b=B&c=C&d=D", AddReservedToQuery("a=A&b=B", reserved));
    }
    {   // with extra '&', original query left as is
        TCgiParameters reserved = {std::make_pair("b", "B")};
        EXPECT_EQ("a=A&b=B", AddReservedToQuery("a=A&", reserved));
        EXPECT_EQ("&a=A&b=B", AddReservedToQuery("&a=A", reserved));
        EXPECT_EQ("&a=A&b=B", AddReservedToQuery("&a=A&", reserved));
    }
}

TEST(AddReservedToQueryTest, TotalReplacement)
{
    {
        TCgiParameters reserved = {std::make_pair("a", "A2"), std::make_pair("b", "B2")};
        EXPECT_EQ("a=A2&b=B2", AddReservedToQuery("b=B&a=A", reserved));
    }
    {   // with extra '&'
        TCgiParameters reserved = {std::make_pair("a", "A2")};
        EXPECT_EQ("a=A2", AddReservedToQuery("&a=A&", reserved));
    }
}

TEST(AddReservedToQueryTest, PartialReplacement)
{
    {   // replace first
        TCgiParameters reserved = {std::make_pair("a", "A2"), std::make_pair("c", "C2")};
        EXPECT_EQ("b=B&a=A2&c=C2", AddReservedToQuery("a=A&b=B", reserved));
    }
    {   // replace last
        TCgiParameters reserved = {std::make_pair("b", "B2"), std::make_pair("c", "C2")};
        EXPECT_EQ("a=A&b=B2&c=C2", AddReservedToQuery("a=A&b=B", reserved));
    }
    {   // replace middle
        TCgiParameters reserved = {std::make_pair("b", "B2")};
        EXPECT_EQ("a=A&c=C&b=B2", AddReservedToQuery("a=A&b=B&c=C", reserved));
    }
    {   // replace more, original parameters will be rearranged
        TString original = "&g=G&f=F&e=E&d=D&c=C&b=B&a=A&";
        TCgiParameters reserved = {
            std::make_pair("b", "B2"),
            std::make_pair("d", "D2"),
            std::make_pair("f", "F2")
        };
        EXPECT_EQ(
            "a=A&c=C&e=E&g=G&b=B2&d=D2&f=F2",
            AddReservedToQuery(original, reserved)
        );
    }
}

TEST(AddReservedToQueryTest, SlashesInReservedParameters)
{
    // @see MARKETOUT-18366
    const TString originalQuery =
            "name1=value-without-slashes"
            "&reserved1=something"
            "&name2=value/with/slashes";
    const TCgiParameters reservedParameters = {
        std::make_pair("reserved1", "value/with/slashes"),
        std::make_pair("reserved2", "value_without_slashes")
    };

    const TString res = AddReservedToQuery(originalQuery, reservedParameters);

    EXPECT_EQ(
        "name1=value-without-slashes"
        "&name2=value/with/slashes"
        "&reserved1=value%2Fwith%2Fslashes"
        "&reserved2=value_without_slashes", res);
}

