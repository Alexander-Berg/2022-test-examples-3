# -*- encoding: utf-8 -*-
from logging import getLogger

from requests import RequestException

log = getLogger(__name__)


class Behavior(object):
    URL = 0
    BAD_REQUEST = 1
    ERROR_VIEW = 2


def book(order_data):
    log.info('Order data: %s', order_data)

    if order_data['behavior'] == Behavior.URL:
        return 'http://example.com'
    elif order_data['behavior'] == Behavior.BAD_REQUEST:
        raise RequestException
    elif order_data['behavior'] == Behavior.ERROR_VIEW:
        raise Exception

    return order_data
