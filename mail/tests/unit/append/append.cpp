#include <backend/append/append.h>
#include <backend/backend_types.h>
#include <backend/yreflection_types.h>

#include <yplatform.hpp>

#include <yamail/data/serialization/json_writer.h>
#include <yamail/data/deserialization/json_reader.h>
#include <yamail/data/reflection/details/adt.h>

#include <sstream>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

TEST(TEST_APPEND, CREATE_URL)
{
    std::string email = "yapoptest@yandex.ru";
    std::string date = "1454436917";
    std::vector<std::string> systemFlags = { "Junk", "Seen" };
    std::vector<std::string> userFlags = { "Foo", "Bar" };
    yimap::DBFolderId folder = { "inbox", "1" };

    {
        yimap::backend::AppendRequest request{
            email, date, systemFlags, userFlags, "hello", folder
        };
        auto url = yimap::backend::AppendUrl::create(request);
        EXPECT_EQ(
            url,
            "append?src_email=yapoptest@yandex.ru&fid=1&user_flags=Foo&user_flags=Bar&system_flags="
            "Junk&system_flags=Seen&date=1454436917");
    }

    {
        userFlags.clear();
        yimap::backend::AppendRequest request{
            email, date, systemFlags, userFlags, "hello", folder
        };
        auto url = yimap::backend::AppendUrl::create(request);
        EXPECT_EQ(
            url,
            "append?src_email=yapoptest@yandex.ru&fid=1&system_flags=Junk&system_flags=Seen&date="
            "1454436917");
    }

    {
        systemFlags.clear();
        yimap::backend::AppendRequest request{
            email, date, systemFlags, userFlags, "hello", folder
        };
        auto url = yimap::backend::AppendUrl::create(request);
        EXPECT_EQ(url, "append?src_email=yapoptest@yandex.ru&fid=1&date=1454436917");
    }
}

TEST(TEST_APPEND, APPEND_RESULT_FROM_JSON)
{
    std::string uid = "10";
    std::string mid = "1283712371923";

    {
        std::stringstream message;
        message << "{\"mid\":\"" << mid << "\",\"imap_id\":\"" << uid << "\"}\n";
        auto result =
            yamail::data::deserialization::fromJson<yimap::backend::AppendResult>(message.str());
        EXPECT_EQ(result.uid, uid);
        EXPECT_EQ(result.mid, mid);
    }
}
