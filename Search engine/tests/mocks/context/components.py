from contextlib import contextmanager
from typing import List

from bot.aiowarden import Component, Chat, OnDuty, Functionality, ProtocolSettings


def fast_component(
    name: str,
    parent_name: str = '',
    human: str = '',
    owners: List[str] = None,
    spi_chat: str = None,
    onduty: List[str] = None,
    flow: List[str] = None,
    pr: List[str] = None,
    support: List[str] = None,
    functionalities: List[Functionality] = None,
    curators: List[str] = None,
    proto_settings: ProtocolSettings = None,
    abc_slug: str = None
) -> Component:
    chat = None
    if spi_chat:
        chat = Chat(chat_link=spi_chat)
    if not proto_settings:
        proto_settings = ProtocolSettings()

    owners = owners or []

    onduty = [OnDuty('dutyrole', l) if isinstance(l, str) else l for l in onduty or []]
    flow = flow or []
    pr = pr or []
    support = support or []
    functionalities = functionalities or []
    curators = curators or []

    return Component(
        name=name,
        human_readable_name=human,
        parent_component_name=parent_name,
        owners=owners,
        spi_chat=chat,
        onduty=onduty,
        flow=flow,
        pr=pr,
        support=support,
        functionality_list=functionalities,
        curators=curators,
        protocol_settings=proto_settings,
        abc_service_slug=abc_slug,
    )


@contextmanager
def temp_components(context, *components: Component):
    keys = set()

    for c in components:
        context.warden._components[(c.parent_component_name, c.name)] = c
        keys.add((c.parent_component_name, c.name))

    try:
        yield None
    finally:
        for k in keys:
            context.warden._components.pop(k, None)
