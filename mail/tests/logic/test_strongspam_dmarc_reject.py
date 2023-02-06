import pytest

from unittest.mock import patch

from mail.nwsmtp.tests.lib.stubs import so
from mail.nwsmtp.tests.lib.util import make_plain_message
from aiosmtplib.errors import SMTPDataError


@pytest.mark.mxfront
async def test_reject_by_dmarc_has_higher_priority_than_reject_by_strongspam(env, sender, rcpt):
    _, msg = make_plain_message("info@paypal.com", rcpt)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_REJECT"
        client = await env.nwsmtp.get_client()
        with pytest.raises(SMTPDataError) as exc:
            await client.send_message(msg, sender.email)
        assert patched.called_once

    assert "5.7.1 Email rejected per DMARC policy" in str(exc)
