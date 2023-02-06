import pytest
from collections import namedtuple

from mail.nwsmtp.tests.lib import CLUSTERS

Case = namedtuple("Case", ["ehlo_command", "expected_code", "expected_response"])

EXPECTED_SMTP_EHLO_RESPONSE = "\n".join(["8BITMIME", "PIPELINING", "SIZE 53477376", "STARTTLS",
                                         "AUTH LOGIN PLAIN XOAUTH2", "DSN", "ENHANCEDSTATUSCODES"])
EXPECTED_MXFRONT_EHLO_RESPONSE = "\n".join(["8BITMIME", "PIPELINING", "SIZE 53477376", "STARTTLS", "DSN", "ENHANCEDSTATUSCODES"])
EXPECTED_MXBACK_OUT_EHLO_RESPONSE = "\n".join(["8BITMIME", "PIPELINING", "SIZE 53477376", "DSN", "ENHANCEDSTATUSCODES"])
EXPECTED_YABACK_EHLO_RESPONSE = "\n".join(["8BITMIME", "PIPELINING", "SIZE 53477376", "STARTTLS", "ENHANCEDSTATUSCODES"])
EXPECTED_MXCORP_EHLO_RESPONSE = "\n".join(["8BITMIME", "PIPELINING", "SIZE 134217728", "STARTTLS", "DSN", "ENHANCEDSTATUSCODES"])
EXPECTED_MXBACKCORP_EHLO_RESPONSE = "\n".join(["8BITMIME", "PIPELINING", "SIZE 134217728", "DSN", "ENHANCEDSTATUSCODES"])
EXPECTED_SMTPCORP_EHLO_RESPONSE = "\n".join(["8BITMIME", "PIPELINING", "SIZE 134217728", "STARTTLS",
                                             "AUTH LOGIN PLAIN XOAUTH2", "DSN", "ENHANCEDSTATUSCODES"])

CLUSTER_TO_EXPECTED_ANSWER = {
    "smtp": EXPECTED_SMTP_EHLO_RESPONSE,
    "mxfront": EXPECTED_MXFRONT_EHLO_RESPONSE,
    "mxbackout": EXPECTED_MXBACK_OUT_EHLO_RESPONSE,
    "yaback": EXPECTED_YABACK_EHLO_RESPONSE,
    "mxcorp": EXPECTED_MXCORP_EHLO_RESPONSE,
    "mxbackcorp": EXPECTED_MXBACKCORP_EHLO_RESPONSE,
    "smtpcorp": EXPECTED_SMTPCORP_EHLO_RESPONSE,
}


@pytest.mark.mxfront
@pytest.mark.parametrize("case", [
    Case("eh sdfsdff", 502, "5.5.1 Unrecognized command"),
    Case("ehlo", 502, "5.5.1 Unrecognized command"),
    Case("lhlo sfsdd", 502, "5.5.1 Unrecognized command"),
    Case("mail from:<>", 503, "5.5.4 Bad sequence of commands"),
    Case("data", 503, "5.5.4 Bad sequence of commands"),
    Case("rcpt_to:<blabla@ya.ru>", 502, "5.5.1 Unrecognized command")])
async def test_error_response_on_incorrect_ehlo_command(env, case):
    client = await env.nwsmtp.get_client()
    errcode, errmsg = await client.execute_command(case.ehlo_command.encode())
    assert errcode == case.expected_code
    assert case.expected_response in errmsg


@pytest.mark.cluster(CLUSTERS)
async def test_response_on_correct_ehlo_command(env, cluster):
    client = await env.nwsmtp.get_client(use_ssl=False)
    code, msg = await client.execute_command("ehlo test.host".encode())
    assert code == 250
    assert CLUSTER_TO_EXPECTED_ANSWER[cluster] in msg


@pytest.mark.cluster(CLUSTERS)
async def test_response_on_correct_helo_command(env, cluster):
    client = await env.nwsmtp.get_client(use_ssl=False)
    code, msg = await client.execute_command("helo test.host".encode())
    assert code == 250
