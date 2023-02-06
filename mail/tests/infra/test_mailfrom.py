from collections import namedtuple

import pytest
import random
import string

Rnd = random.Random(42)


def generate_random_string(size):
    alphabet = string.ascii_letters + string.digits
    return ''.join(Rnd.choice(alphabet) for n in range(size))


ACCEPTED_MAIL_FROM = generate_random_string(246)
NOT_ACCEPTED_MAIL_FROM = generate_random_string(247)

Case = namedtuple("Case", ["email", "expected_code", "expected_response"])


@pytest.mark.mxfront
@pytest.mark.parametrize("case", [
    Case("<plesk", 555, "5.5.2 Syntax error"),
    Case("<plesk>", 501, "5.1.7 Bad address mailbox syntax"),
    Case("<plesk@>", 501, "5.1.7 Bad address mailbox syntax"),
    Case("<plesk@ya.ru>", 250, "2.1.0 <plesk@ya.ru> ok"),
    Case("<" + NOT_ACCEPTED_MAIL_FROM + "@yandex.ru>", 501, "5.1.7 Path too long."),
    Case("<" + ACCEPTED_MAIL_FROM + "@yandex.ru>", 250, "2.1.0 <" + ACCEPTED_MAIL_FROM + "@yandex.ru> ok")])
async def test_response_on_mailfrom(env, case):
    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    mail_from_cmd = "mail from:{email}".format(email=case.email)
    code, msg = await client.execute_command(mail_from_cmd.encode())

    assert code == case.expected_code
    assert case.expected_response in msg
