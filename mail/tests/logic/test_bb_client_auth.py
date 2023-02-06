import aiosmtplib
import pytest

from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.util import make_plain_message


def set_deny_auth_for_assessors(conf):
    conf.modules.nwsmtp.configuration.blackbox.deny_auth_for_assessors=True


def disable_deny_auth_for_assessors(conf):
    conf.modules.nwsmtp.configuration.blackbox.deny_auth_for_assessors=False


def get_email(login, country):
    return login + "@yandex." + country


@pytest.mark.smtp
async def test_rejection_for_empty_auth_data_and_empty_token(env, passwordless_sender):
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        if env.nwsmtp.is_auth_required():
            await client.login(passwordless_sender.email, passwordless_sender.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: Invalid format" in exc.value.message


@pytest.mark.smtp
async def test_rejection_for_blocked_user(env, user_with_blocked_email):
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        if env.nwsmtp.is_auth_required():
            await client.login(user_with_blocked_email.email, user_with_blocked_email.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: Invalid user or password" in exc.value.message


@pytest.mark.smtp
async def test_rejection_for_assessor(cluster, users, assessor):
    with make_conf(cluster, customize_with=set_deny_auth_for_assessors) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
                if env.nwsmtp.is_auth_required():
                    await client.login(assessor.email, assessor.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: This type of user does not have access" in exc.value.message


@pytest.mark.smtp
async def test_acceptance_for_assessor(cluster, users, assessor):
    with make_conf(cluster, customize_with=disable_deny_auth_for_assessors) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            if env.nwsmtp.is_auth_required():
                code, reply = await client.login(assessor.email, assessor.passwd)

    assert code == 235
    assert "2.7.0 Authentication successful." in reply


@pytest.mark.smtp
async def test_rejection_for_mdbreg(env, mdbreg_user):
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        if env.nwsmtp.is_auth_required():
            await client.login(mdbreg_user.email, mdbreg_user.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: Invalid user or password!" in exc.value.message


@pytest.mark.smtp
async def test_rejection_for_zero_suid(env, zero_suid_user):
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        if env.nwsmtp.is_auth_required():
            await client.login(zero_suid_user.email, zero_suid_user.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: Invalid user or password!" in exc.value.message


@pytest.mark.smtp
async def test_rejection_for_non_accepted_eula(env, hosted_noeula_user):
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        if env.nwsmtp.is_auth_required():
            await client.login(hosted_noeula_user.email, hosted_noeula_user.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: Please accept EULA first." in exc.value.message
    assert hosted_noeula_user.country in exc.value.message


@pytest.mark.smtp
async def test_rejection_for_bad_karma(env, bad_karma_user):
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        if env.nwsmtp.is_auth_required():
            await client.login(bad_karma_user.email, bad_karma_user.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: Your message looks like spam." in exc.value.message


@pytest.mark.smtp
@pytest.mark.parametrize("country, link", [
    ("by", "link_by"),
    ("kz", "link_kz"),
    ("ru", "link_ru"),
    ("tr", "link_tr"),
    ("com", "link_com")
], ids=["by", "kz", "ru", "tr", "com"])
async def test_rejection_for_threshold_karma(env, threshold_karma_user, country, link):
    env.stubs.blackbox.set_glue_different_zones_emails(True)
    env.stubs.blackbox.set_country_from_email(True)
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        if env.nwsmtp.is_auth_required():
            await client.login(get_email(threshold_karma_user.login, country), threshold_karma_user.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed: Your message looks like spam." in exc.value.message
    assert f"You need to use web for sending or prove you are not a robot using the following link {link}" in exc.value.message


@pytest.mark.smtp
async def test_acceptance_for_bad_karma_hosted_pdd(env, bad_karma_hosted_user):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        code, reply = await client.login(bad_karma_hosted_user.email, bad_karma_hosted_user.passwd)

    assert code == 235
    assert "2.7.0 Authentication successful." in reply


@pytest.mark.smtp
async def test_acceptance_for_bad_karma_phone_confirmed(env, bad_karma_phone_comfirmed_user):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        code, reply = await client.login(bad_karma_phone_comfirmed_user.email, bad_karma_phone_comfirmed_user.passwd)

    assert code == 235
    assert "2.7.0 Authentication successful." in reply


@pytest.mark.mxfront
@pytest.mark.mxbackout
@pytest.mark.yaback
async def test_no_auth_acceptance_for_bad_karma(env, bad_karma_user, rcpt):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(bad_karma_user.email, bad_karma_user.passwd)

    _, msg = make_plain_message(bad_karma_user, rcpt)

    code, reply = await client.send_message(msg)

    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxfront
@pytest.mark.mxbackout
@pytest.mark.yaback
async def test_no_auth_acceptance_for_threshold_karma(env, threshold_karma_user, rcpt):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(threshold_karma_user.email, threshold_karma_user.passwd)

    _, msg = make_plain_message(threshold_karma_user, rcpt)

    code, reply = await client.send_message(msg)

    assert not code
    assert "2.0.0 Ok: queued on " in reply
