from collections import namedtuple

import pytest

Case = namedtuple("Case", ["email", "expected_code", "expected_response"])


@pytest.mark.mxfront
@pytest.mark.parametrize("case", [
    Case("<plesk>", 504, "5.5.2 Recipient address rejected: need fully-qualified address"),
    Case("<plesk@>", 501, "5.1.3 Bad recipient address syntax"),
    Case("<plesk@ya,>", 501, "5.1.3 Bad recipient address syntax")])
async def test_error_response_on_rcpt(env, case, prod_sender):
    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    mail_from_cmd = "MAIL FROM:<{email}>".format(email=prod_sender.email)
    await client.execute_command(mail_from_cmd.encode())

    rcpt_to_cmd = "RCPT TO:{email}".format(email=case.email)
    code, msg = await client.execute_command(rcpt_to_cmd.encode())

    assert code == case.expected_code
    assert case.expected_response in msg


@pytest.mark.mxfront
@pytest.mark.parametrize("case", [
    Case("<pleskav@ya>", 550, "5.7.1 No such user!")
])
async def test_no_such_user_response_on_incorrect_rcpt_on_mxfronts(env, case, prod_sender):
    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    mail_from_cmd = "MAIL FROM:<{email}>".format(email=prod_sender.email)
    await client.execute_command(mail_from_cmd.encode())

    rcpt_to_cmd = "RCPT TO:{email}".format(email=case.email)
    code, msg = await client.execute_command(rcpt_to_cmd.encode())

    assert code == case.expected_code
    assert case.expected_response in msg


@pytest.mark.mxbackout
@pytest.mark.parametrize("case", [
    Case("<pleskav@ya>", 250, "2.1.5 <pleskav@ya> recipient ok")
])
async def test_ignore_incorrect_rcpt_on_mxbacks(env, case, prod_sender):
    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    mail_from_cmd = "MAIL FROM:<{email}>".format(email=prod_sender.email)
    await client.execute_command(mail_from_cmd.encode())

    rcpt_to_cmd = "RCPT TO:{email}".format(email=case.email)
    code, msg = await client.execute_command(rcpt_to_cmd.encode())

    assert code == case.expected_code
    assert case.expected_response in msg
