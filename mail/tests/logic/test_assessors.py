import pytest
import aiosmtplib


@pytest.mark.smtpcorp
async def test_deny_auth_for_assessor(env, assessor):
    client = await env.nwsmtp.get_client()
    with pytest.raises(aiosmtplib.errors.SMTPAuthenticationError) as exc:
        await client.login(assessor.email, assessor.passwd)

    assert exc.value.code == 535
    assert "5.7.8 Error: authentication failed:"\
        " This type of user does not have access rights to this service" in exc.value.message
