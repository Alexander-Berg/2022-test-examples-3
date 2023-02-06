from .utils import (
    assert_status,
    contains_link,
    contains_otp,
    contains_request,
    contains_requests,
    otp,
)
from hamcrest import assert_that, contains, has_entry, starts_with
import pytest

welcome_prompt = "Здравствуйте.\nЯ телеграм-бот Яндекс.Почты 360. Перешлите мне важное сообщение, которое вы не успеваете сейчас прочитать — и я отправлю его в ваш ящик."
welcome_no_link_prompt = "Чтобы начать, привяжите адрес почты — введите команду `/email`, а затем через пробел ваш адрес\\."
bind_email_prompt = "Вы не привязали адрес почты\\. Чтобы привязать, введите команду `/email`, а затем через пробел ваш адрес\\."
missing_email_prompt = (
    "Чтобы привязать почту, введите команду `/email`, а затем через пробел ваш адрес\\."
)
otp_prompt = (
    "Чтобы отправить мне код, введите команду `/code`, а затем через пробел код подтверждения\\."
)
otp_invalid = "Это неправильный код\\. Привяжите адрес заново — введите команду `/email`\\."
email_binded_response = "Почта успешно привязана. Теперь вы можете переслать мне сообщение, и я отправлю его в ваш ящик."
error_occured = "При обработке сообщения произошла ошибка"


def test_sends_message_to_telegram_with_token(fake_telegram, botpeer):
    resp = fake_telegram.send_update_text(int(botpeer["chat_id"]), "doesn't matter")
    assert_status(resp, 200)
    assert_that(
        fake_telegram.send_message_requests, contains(has_entry("api_token", "fake_api_token"))
    )


def test_sends_message_to_telegram_chat(fake_telegram, botpeer):
    resp = fake_telegram.send_update_text(int(botpeer["chat_id"]), "doesn't matter")
    assert_status(resp, 200)
    assert_that(
        fake_telegram.send_message_requests,
        contains(has_entry("chat_id", botpeer["chat_id"])),
    )


def test_forward_no_link(fake_telegram):
    resp = fake_telegram.send_update_text(888888, "эээ")
    assert_status(resp, 200)
    assert_that(fake_telegram, contains_request(bind_email_prompt))


def test_start_no_link(fake_telegram):
    resp = fake_telegram.send_update_text(888888, "/start")
    assert_status(resp, 200)
    assert_that(fake_telegram, contains_requests(welcome_prompt, welcome_no_link_prompt))


def test_start_with_link(fake_telegram, botpeer, mail_account, botpeer_mail_account_link):
    resp = fake_telegram.send_update_text(int(botpeer["chat_id"]), "/start")
    assert_status(resp, 200)
    assert_that(fake_telegram, contains_request(welcome_prompt))


def test_code_no_arg(fake_telegram):
    resp = fake_telegram.send_update_text(888888, "/code")
    assert_status(resp, 200)
    assert_that(fake_telegram, contains_request(otp_prompt))


def test_code_invalid_otp(fake_telegram):
    resp = fake_telegram.send_update_text(888888, "/code 12345")
    assert_status(resp, 200)
    assert_that(fake_telegram, contains_request(otp_invalid))


def test_code_sends_message_to_telegram(
    fake_telegram, botpeer, mail_account, botpeer_mail_account_otp
):
    resp = fake_telegram.send_update_text(
        int(botpeer["chat_id"]), "/code %s" % otp(botpeer, mail_account)["otp_value"]
    )
    assert_status(resp, 200)
    assert_that(fake_telegram, contains_request(email_binded_response))


def test_code_stores_link(fake_telegram, botpeer, mail_account, botpeer_mail_account_otp, db):
    resp = fake_telegram.send_update_text(
        int(botpeer["chat_id"]), "/code %s" % otp(botpeer, mail_account)["otp_value"]
    )
    assert_status(resp, 200)
    assert_that(db, contains_link(botpeer, mail_account))


def test_email_no_arg(fake_telegram):
    resp = fake_telegram.send_update_text(888888, "/email")
    assert_status(resp, 200)
    assert_that(fake_telegram, contains_request(missing_email_prompt))


def test_email_nonexistent(fake_telegram, mail_account_with_empty_bb_response):
    resp = fake_telegram.send_update_text(
        888888, "/email %s" % mail_account_with_empty_bb_response["email"]
    )
    assert_status(resp, 200)


def test_email_blackbox_error(fake_telegram, mail_account_with_bb_error):
    resp = fake_telegram.send_update_text(888888, "/email %s" % mail_account_with_bb_error["email"])
    assert_status(resp, 500)


def test_telegram_retries_limit(fake_telegram, mail_account_with_bb_error):
    retries_limit = 10
    expected_codes = [500] * (retries_limit) + [200]
    responses = []
    for i in range(0, retries_limit + 1):
        responses.append(
            fake_telegram.send_update_text(
                888888, "/email %s" % mail_account_with_bb_error["email"], update_id=333
            ).status_code
        )
    assert_that(responses, contains(*expected_codes))
    assert_that(fake_telegram, contains_request(starts_with(error_occured)))


@pytest.mark.xfail
def test_email_sends_message_to_telegram(fake_telegram, botpeer, mail_account):
    resp = fake_telegram.send_update_text(
        int(botpeer["chat_id"]), "/email %s" % mail_account["email"]
    )
    assert_status(resp, 200)  # 500 (smtp error: Bad recipient)
    assert_that(fake_telegram, contains_request(otp_prompt))


@pytest.mark.xfail
def test_email_stores_otp(fake_telegram, botpeer, mail_account, db):
    resp = fake_telegram.send_update_text(
        int(botpeer["chat_id"]), "/email %s" % mail_account["email"]
    )
    assert_status(resp, 200)  # 500 (smtp error: Bad recipient)
    assert_that(db, contains_otp(botpeer, mail_account))
