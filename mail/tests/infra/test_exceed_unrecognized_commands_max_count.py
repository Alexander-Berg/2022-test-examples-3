import aiosmtplib
import pytest

from mail.nwsmtp.tests.lib.env import make_env, make_conf

COMMADS_MAX_COUNT = 2


def set_unrecognized_commands_max_count(conf):
    conf.modules.nwsmtp.configuration.smtp_connection.unrecognized_commands_max_count = COMMADS_MAX_COUNT


@pytest.mark.mxfront
async def test_exceed_unrecognized_commands_max_count(cluster, users):
    with make_conf(cluster, customize_with=set_unrecognized_commands_max_count) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            with pytest.raises(aiosmtplib.errors.SMTPServerDisconnected):
                for counter in range(COMMADS_MAX_COUNT):
                    code, msg = await client.execute_command("tutututu{counter}".format(counter=counter).encode())
                await client._ehlo_or_helo_if_needed()
    assert code == 502
    assert "Too many unrecognized commands, goodbye" in msg
