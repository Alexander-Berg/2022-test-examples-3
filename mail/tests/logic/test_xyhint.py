import pytest

from mail.nwsmtp.tests.lib.hint import make_hint, get_hint_values
from mail.nwsmtp.tests.lib.util import make_plain_message, get_session_id


def generate_hint_with_alike_parameters(parameter, value):
    names = [parameter + "_some_suffix", "some_prefix_" + parameter]
    fake_params = {name: value for name in names}
    return make_hint(label="user_label", **fake_params, fid="1234")


@pytest.mark.mxbackcorp
@pytest.mark.mxbackout
@pytest.mark.parametrize("is_hint_targeted", [False, True])
async def test_session_id_when_message_already_contains_one(env, sender, rcpt, is_hint_targeted):
    client = await env.nwsmtp.get_client()

    BLAH_BLAH = "blah-blah"
    hint = {"label": "user_label", "session_id": BLAH_BLAH, "fid": "1234"}
    if is_hint_targeted:
        hint["email"] = sender.email
    hint_with_session_id = make_hint(**hint)
    hint_with_alike_params = generate_hint_with_alike_parameters("session_id", "wrong-choice")
    hints = [("X-Yandex-Hint", value) for value in [hint_with_session_id, hint_with_alike_params]]

    msg_id, msg = make_plain_message(sender, rcpt, headers=hints)
    reply = await client.send_message(msg, sender.email, rcpt.email)
    msg = await env.relays.wait_msg(msg_id)

    expectation = [BLAH_BLAH]
    if is_hint_targeted:  # if none of the non-targeted hints contains a session_id, nwsmtp should add one
        new_session_id = get_session_id(reply)
        assert new_session_id != BLAH_BLAH
        expectation += [new_session_id]

    hints_with_session_id = get_hint_values(msg)["session_id"]
    assert sorted(hints_with_session_id) == sorted(expectation)


@pytest.mark.mxbackcorp
@pytest.mark.mxbackout
@pytest.mark.parametrize("is_hint_targeted", [False, True])
async def test_ipfrom_when_message_already_contains_one(env, sender, rcpt, is_hint_targeted):
    client = await env.nwsmtp.get_client()

    hint = {"label": "user_label", "ipfrom": "::2", "fid": "1234"}
    if is_hint_targeted:
        hint["email"] = sender.email
    hint_with_ipfrom = make_hint(**hint)
    hint_with_alike_params = generate_hint_with_alike_parameters("ipfrom", "::3")
    hints = [("X-Yandex-Hint", value) for value in [hint_with_ipfrom, hint_with_alike_params]]

    msg_id, msg = make_plain_message(sender, rcpt, headers=hints)
    await client.send_message(msg, sender.email, rcpt.email)
    msg = await env.relays.wait_msg(msg_id)

    expectation = ["::2"]
    if is_hint_targeted:  # if none of the non-targeted hints contains an ipfrom, nwsmtp should add one
        expectation += ["::1"]
    hints_with_ipfrom = get_hint_values(msg)["ipfrom"]
    assert sorted(hints_with_ipfrom) == sorted(expectation)
