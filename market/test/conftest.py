# coding: utf-8
# import os
# import re
# import six
# import time
# import yatest.common

from six import BytesIO
from library.python.framing.packer import Packer

from market.backbone.offers_store.proto.event_pb2 import (
    EMessageType,
    TEventMessage,
    TOfferUpdateEvent,
    TServiceOfferKey,
)


def pack_messages(msgs):
    output = BytesIO()
    packer = Packer(output)

    for msg in msgs:
        packer.add_proto(msg)

    packer.flush()
    return output.getvalue()


def pack_message(msg):
    return pack_messages([msg])


def offer_update_msg(yabs_id, business_id, content_offer=None, serviceOffers=None, subkeys=None):
    msg = TOfferUpdateEvent()
    msg.YabsId = yabs_id
    msg.BusinessId = business_id

    if content_offer is not None:
        msg.ContentOffer.CopyFrom(content_offer)
    if serviceOffers is not None:
        for keys, offer in zip(subkeys, serviceOffers):
            mapKey = str(keys[0]) + '_' + str(keys[1])
            msg.ServiceOffers[mapKey].CopyFrom(offer)
    return msg.SerializeToString()


def offer_update_event(yabs_id, business_id, content_offer=None, service_offers=None,  subkeys=None):
    event = TEventMessage()
    event.YabsId = yabs_id
    event.BusinessId = business_id
    event.Type = EMessageType.OFFER_UPDATE
    event.Body = offer_update_msg(yabs_id, business_id, content_offer, service_offers, subkeys)
    if subkeys is not None:
        for s in subkeys:
            key = TServiceOfferKey()
            key.ShopId = s[0]
            key.WarehouseId = s[1]
            event.SOfferKeys.append(key)

    return event
