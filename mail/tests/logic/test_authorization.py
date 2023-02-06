import aiosmtplib
import pytest

from aiohttp import web
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from unittest.mock import patch


def disable_settings_authorization(conf):
    conf.modules.nwsmtp.configuration.auth_settings.use = False


def enable_settings_authorization(conf):
    conf.modules.nwsmtp.configuration.auth_settings.use = True


@pytest.mark.smtp
async def test_pass_settings_authorization(cluster, users, sender):
    with make_conf(cluster, customize_with=enable_settings_authorization) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            await client._ehlo_or_helo_if_needed()
            await client.auth_plain(sender.email, sender.passwd)
            host, port, _, _ = client.get_transport_info("sockname")
            assert env.stubs.blackbox.requests[0].query["userip"] == host


def make_settings_response():
    return web.json_response(status=200, data={
        "settings": {
            "parameters": {
                "single_settings": {
                    "enable_imap_auth_plain": ""
                }
            },
            "profile": {
                "single_settings": {
                    "enable_pop": "",
                    "enable_imap": "on"
                }
            }
        }
    })


@pytest.mark.smtp
async def test_pass_settings_authorization_for_user_with_app_password_enabled(
        cluster, users, user_with_app_password_enabled):
    with patch("mail.nwsmtp.tests.lib.stubs.settings.handle_get",
               return_value=make_settings_response()) as mock:
        with make_conf(cluster, customize_with=enable_settings_authorization) as conf:
            async with make_env(cluster, users, conf) as env:
                client = await env.nwsmtp.get_client()
                await client._ehlo_or_helo_if_needed()
                await client.auth_plain(user_with_app_password_enabled.email,
                                        user_with_app_password_enabled.passwd)
                assert env.stubs.blackbox.requests[0].query["attributes"] == '107'
        mock.assert_called_once()


@pytest.mark.smtp
async def test_reject_during_settings_authorization(cluster, users, sender):
    with patch("mail.nwsmtp.tests.lib.stubs.settings.handle_get",
               return_value=make_settings_response()) as mock:
        with make_conf(cluster, customize_with=enable_settings_authorization) as conf:
            async with make_env(cluster, users, conf) as env:
                client = await env.nwsmtp.get_client()
                with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as ex:
                    await client._ehlo_or_helo_if_needed()
                    await client.auth_plain(sender.email, sender.passwd)
                    assert ex.value.code == 535
                    assert ex.value.message == "5.7.8 Error: authentication failed:" \
                                               " This user does not have access rights to this service"
        mock.assert_called_once()


@pytest.mark.smtp
async def test_reject_authorization_for_user_with_blocked_email(cluster, users, user_with_blocked_email):
    with make_conf(cluster, customize_with=disable_settings_authorization) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as ex:
                await client.login(user_with_blocked_email.email, user_with_blocked_email.passwd)
                assert ex.value.code == 535
                assert ex.value.message == "5.7.8 Error: authentication failed:" \
                                            " This user does not have access rights to this service"
