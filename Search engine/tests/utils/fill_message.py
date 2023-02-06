# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from typing import Any, AnyStr, List

import randomproto

from google.protobuf.message import Message
from google.protobuf.message_factory import MessageFactory


def try_filling_field(message, field, values):
    # type: (Message, AnyStr, List[Any]) -> bool
    for val in values:
        try:
            setattr(message, field, val)
        except Exception:
            continue
        return True
    return False


def fill_message(message, ignore_fields=None):
    # type: (Message) -> None
    """ Fills message to some extend (ignores arrays, maps, messages with maps, ...)"""

    if not ignore_fields:
        ignore_fields = []

    message_factory = MessageFactory()

    for field in message.DESCRIPTOR.fields:
        if field.name in ignore_fields:
            continue

        # try filling as scalar
        if try_filling_field(message, field.name, ['dummy', 123, True]):
            continue

        # fill messages
        try:
            f = getattr(message, field.name)
            f.CopyFrom(randomproto.randproto(message_factory.CreatePrototype(field.message_type)))
        except (AttributeError, TypeError):
            pass
        else:
            continue
