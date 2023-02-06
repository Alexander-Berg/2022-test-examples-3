#include "i18n/i18n.h"
#include <catch.hpp>

namespace botserver::i18n {

TEST_CASE("send_code_from_email/markdown_escaping")
{
    REQUIRE(
        (string)send_code_from_email(language::ru, "my-mail_1@email.ru") ==
        "Проверьте почту my\\-mail\\_1@email\\.ru — на нее отправлен код "
        "подтверждения\\. Чтобы отправить его мне, "
        "введите команду `/code`, а затем через пробел код подтверждения\\.");
}

}
