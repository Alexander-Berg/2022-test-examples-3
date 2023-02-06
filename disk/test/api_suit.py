# -*- coding: utf-8 -*-
import time
import traceback

from copy import deepcopy
from contextlib import contextmanager

import mpfs.engine.process
import mpfs.common.util.mailer
import mpfs.core.services.search_service

from mpfs.config import settings
from mpfs.frontend.api.disk import Default
from mpfs.common.util import from_json

log = mpfs.engine.process.get_default_log()

with open('fixtures/json/api.json') as fix_file:
    fixtures = from_json(fix_file.read())


original_search_service = deepcopy(mpfs.core.services.search_service.SearchService)
original_mail_send = deepcopy(mpfs.common.util.mailer.send)

USE_DYNAMIC_PREVIEWS = settings.feature_toggles['dynamicpreviews']

Default.user_ip = ''

share_user = mpfs.engine.process.share_user()


def set_up_mailbox(real_send=True):
    mailbox = []

    def fake_mail_send(*args, **kwargs):
        mailbox.append({'args': args, 'kwargs': kwargs})
        result = {}
        try:
            if real_send:
                result = original_mail_send(*args, **kwargs)
        except Exception:
            log.error(traceback.format_exc())
        return result or {}

    mpfs.common.util.mailer.send = fake_mail_send
    return mailbox


def tear_down_mailbox():
    mpfs.common.util.mailer.send = original_mail_send


@contextmanager
def patch_mailbox(real_send=True):
    """
    Патчим отправку писем
    """
    try:
        yield set_up_mailbox(real_send=real_send)
    finally:
        tear_down_mailbox()


def unique_int():
    return int(str('%f' % time.time()).replace('.', ''))
