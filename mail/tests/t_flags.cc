#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;

const message_flags FLAG_1 = static_cast<message_flags>(1 << 1);
const message_flags FLAG_2 = static_cast<message_flags>(1 << 2);
const message_flags FLAG_3 = static_cast<message_flags>(1 << 3);
const message_flags FLAG_1_2 = FLAG_1 | FLAG_2;
const message_flags FLAG_1_3 = FLAG_1 | FLAG_3;
const message_flags FLAG_2_3 = FLAG_2 | FLAG_3;
const message_flags FLAG_1_2_3 = FLAG_1 | FLAG_2 | FLAG_3;

TEST_CASE("flags/simple")
{
    message m;
    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));

    m.set_flag(FLAG_1);

    CHECK(m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));

    m.set_flag(FLAG_2);

    CHECK(m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));

    m.unset_flag(FLAG_1);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));

    m.unset_flag(FLAG_2);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));
}

TEST_CASE("flags/composite1")
{
    message m;
    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1_2));
    CHECK(!m.has_flag(FLAG_1_3));
    CHECK(!m.has_flag(FLAG_2_3));
    CHECK(!m.has_flag(FLAG_1_2_3));

    m.set_flag(FLAG_1_2);

    CHECK(m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));
    CHECK(m.has_flag(FLAG_1_2));
    CHECK(!m.has_flag(FLAG_1_3));
    CHECK(!m.has_flag(FLAG_2_3));
    CHECK(!m.has_flag(FLAG_1_2_3));

    m.set_flag(FLAG_2_3);

    CHECK(m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(m.has_flag(FLAG_1_2));
    CHECK(m.has_flag(FLAG_1_3));
    CHECK(m.has_flag(FLAG_2_3));
    CHECK(m.has_flag(FLAG_1_2_3));

    m.unset_flag(FLAG_1_2);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1_2));
    CHECK(!m.has_flag(FLAG_1_3));
    CHECK(!m.has_flag(FLAG_2_3));
    CHECK(!m.has_flag(FLAG_1_2_3));

    m.set_flag(FLAG_2);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1_2));
    CHECK(!m.has_flag(FLAG_1_3));
    CHECK(m.has_flag(FLAG_2_3));
    CHECK(!m.has_flag(FLAG_1_2_3));

    m.set_flag(FLAG_1);

    CHECK(m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(m.has_flag(FLAG_1_2));
    CHECK(m.has_flag(FLAG_1_3));
    CHECK(m.has_flag(FLAG_2_3));
    CHECK(m.has_flag(FLAG_1_2_3));

    m.unset_flag(FLAG_1_2_3);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1_2));
    CHECK(!m.has_flag(FLAG_1_3));
    CHECK(!m.has_flag(FLAG_2_3));
    CHECK(!m.has_flag(FLAG_1_2_3));
}

TEST_CASE("flags/composite2")
{
    message m;
    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2));
    CHECK(!m.has_flag(FLAG_1 | FLAG_3));
    CHECK(!m.has_flag(FLAG_2 | FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2 | FLAG_3));

    m.set_flag(FLAG_1 | FLAG_2);

    CHECK(m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));
    CHECK(m.has_flag(FLAG_1 | FLAG_2));
    CHECK(!m.has_flag(FLAG_1 | FLAG_3));
    CHECK(!m.has_flag(FLAG_2 | FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2 | FLAG_3));

    m.set_flag(FLAG_2 | FLAG_3);

    CHECK(m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(m.has_flag(FLAG_1 | FLAG_2));
    CHECK(m.has_flag(FLAG_1 | FLAG_3));
    CHECK(m.has_flag(FLAG_2 | FLAG_3));
    CHECK(m.has_flag(FLAG_1 | FLAG_2 | FLAG_3));

    m.unset_flag(FLAG_1 | FLAG_2);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2));
    CHECK(!m.has_flag(FLAG_1 | FLAG_3));
    CHECK(!m.has_flag(FLAG_2 | FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2 | FLAG_3));

    m.set_flag(FLAG_2);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2));
    CHECK(!m.has_flag(FLAG_1 | FLAG_3));
    CHECK(m.has_flag(FLAG_2 | FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2 | FLAG_3));

    m.set_flag(FLAG_1);

    CHECK(m.has_flag(FLAG_1));
    CHECK(m.has_flag(FLAG_2));
    CHECK(m.has_flag(FLAG_3));
    CHECK(m.has_flag(FLAG_1 | FLAG_2));
    CHECK(m.has_flag(FLAG_1 | FLAG_3));
    CHECK(m.has_flag(FLAG_2 | FLAG_3));
    CHECK(m.has_flag(FLAG_1 | FLAG_2 | FLAG_3));

    m.unset_flag(FLAG_1 | FLAG_2 | FLAG_3);

    CHECK(!m.has_flag(FLAG_1));
    CHECK(!m.has_flag(FLAG_2));
    CHECK(!m.has_flag(FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2));
    CHECK(!m.has_flag(FLAG_1 | FLAG_3));
    CHECK(!m.has_flag(FLAG_2 | FLAG_3));
    CHECK(!m.has_flag(FLAG_1 | FLAG_2 | FLAG_3));
}
