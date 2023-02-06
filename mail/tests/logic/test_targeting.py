import pytest

from yatest.common.network import PortManager

from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env

from asyncio.exceptions import TimeoutError


def make_config_customizer(relay, **kwargs):
    def customize_config_func(conf):
        targeting = conf.nwsmtp.delivery.relays.__getattr__(relay).targeting
        targeting.use = True
        targeting.fallback = kwargs.get('fallback', False)
        targeting.timeouts.d.update(kwargs.get('timeouts', {}))
        if 'bypass_port' in kwargs:
            targeting.bypass_port = kwargs['bypass_port']

    return customize_config_func


@pytest.mark.mxfront
@pytest.mark.mxcorp(relay='fallback')
@pytest.mark.mxbackout
@pytest.mark.mxbackcorp(relay='fallback')
@pytest.mark.yaback
async def test_targeting_send_by_primary_relay(request, cluster, users, sender, rcpt):
    params = next(x.kwargs for x in request.node.own_markers if x.name == cluster.replace("-", ""))
    relay = params.get('relay', 'local')

    config_customizer = make_config_customizer(relay, bypass_port=PortManager().get_port())

    with make_conf(cluster, customize_with=config_customizer) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            if env.nwsmtp.is_auth_required():
                await client.login(sender.email, sender.passwd)

            msg_id, msg = make_plain_message(sender, rcpt)
            await client.send_message(msg)
            assert await env.relays.__dict__[relay].wait_msg(msg_id)


@pytest.mark.mxfront
@pytest.mark.mxcorp(relay='fallback')
@pytest.mark.mxbackout
@pytest.mark.mxbackcorp(relay='fallback')
@pytest.mark.yaback
async def test_targeting_fallback_send_by_primary_relay(request, cluster, users, sender, rcpt):
    params = next(x.kwargs for x in request.node.own_markers if x.name == cluster.replace("-", ""))
    relay = params.get('relay', 'local')

    config_customizer = make_config_customizer(
        relay,
        fallback=True,
        timeouts=dict(connect='0ms', connectAttempt='0ms', command='0ms')
    )

    with make_conf(cluster, customize_with=config_customizer) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            if env.nwsmtp.is_auth_required():
                await client.login(sender.email, sender.passwd)

            msg_id, msg = make_plain_message(sender, rcpt)
            await client.send_message(msg)
            assert await env.relays.__dict__[relay].wait_msg(msg_id)


@pytest.mark.mxfront
@pytest.mark.mxbackout
@pytest.mark.mxbackcorp
@pytest.mark.yaback
async def test_targeting_send_by_fallback_relay(cluster, users, sender, rcpt):
    config_customizer = make_config_customizer(
        'local',
        timeouts=dict(connect='0ms', connectAttempt='0ms', command='0ms')
    )

    with make_conf(cluster, customize_with=config_customizer) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            if env.nwsmtp.is_auth_required():
                await client.login(sender.email, sender.passwd)

            msg_id, msg = make_plain_message(sender, rcpt)
            await client.send_message(msg)

            with pytest.raises(TimeoutError):
                await env.relays.local.wait_msg(msg_id, timeout=0.5)

            assert await env.relays.fallback.wait_msg(msg_id)
