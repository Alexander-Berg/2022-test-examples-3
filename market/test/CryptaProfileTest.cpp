#include <market/library/recom/src/CryptaProfile.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace Market;

TEST(TCryptaProfile, ParseTest)
{
    TCryptaProfile profile;
    NJson::TJsonValue jsonValue = profile.GetJsonValue();
    EXPECT_TRUE(NJson::JSON_ARRAY == jsonValue.GetType());
    EXPECT_EQ(0, jsonValue.GetArray().size());

    profile.InitFromJson("", TCryptaProfile::EDefaultValuesFillingPolicy::FILL_FROM_DEFAULT_PROFILE);
    jsonValue = profile.GetJsonValue();
    EXPECT_TRUE(NJson::JSON_ARRAY == jsonValue.GetType());
    // Default prifile size = 21
    EXPECT_EQ(21, jsonValue.GetArray().size());

    profile.InitFromJson("{\"data\":[{\"segment\":[{\"id\":\"17", TCryptaProfile::EDefaultValuesFillingPolicy::FILL_FROM_DEFAULT_PROFILE);
    jsonValue = profile.GetJsonValue();
    EXPECT_TRUE(NJson::JSON_ARRAY == jsonValue.GetType());
    EXPECT_EQ(21, jsonValue.GetArray().size());

    profile.InitFromJson("{\"data\":[{\"segment\":[{\"id\":\"174\",\"value\":\"1\",\"weight\":\"100\"}]}]}", TCryptaProfile::EDefaultValuesFillingPolicy::FILL_FROM_DEFAULT_PROFILE);
    jsonValue = profile.GetJsonValue();
    EXPECT_TRUE(NJson::JSON_ARRAY == jsonValue.GetType());
    EXPECT_EQ(21, jsonValue.GetArray().size());
    bool found = false;
    for (const auto& param: jsonValue.GetArray())
    {
        if (param["id"].GetString() == "174" && param["value"].GetString() == "1")
        {
            EXPECT_EQ("100", param["weight"].GetString());
            found = true;
        }
    }
    EXPECT_EQ(true, found);

    profile.InitFromJson("{\"data\":[{\"segment\":[{\"id\":\"174\",\"value\":\"1\",\"weight\":\"100\"},{\"id\":\"238\",\"value\":\"1:10,2:20,3:30\"}]}]}", TCryptaProfile::EDefaultValuesFillingPolicy::FILL_FROM_DEFAULT_PROFILE);
    jsonValue = profile.GetJsonValue();
    EXPECT_TRUE(NJson::JSON_ARRAY == jsonValue.GetType());
    EXPECT_EQ(24, jsonValue.GetArray().size());
    for (const auto& param: jsonValue.GetArray())
    {
        if (param["id"].GetString() == "174" && param["value"].GetString() == "1")
        {
            EXPECT_EQ("100", param["weight"].GetString());
        }
        if (param["id"].GetString() == "238")
        {
            if (param["value"].GetString() == "1")
            {
                EXPECT_EQ("10", param["weight"].GetString());
            }
            if (param["value"].GetString() == "2")
            {
                EXPECT_EQ("20", param["weight"].GetString());
            }
            if (param["value"].GetString() == "3")
            {
                EXPECT_EQ("30", param["weight"].GetString());
            }
        }
    }
}
