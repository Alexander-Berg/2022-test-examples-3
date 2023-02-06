# -*- coding: utf-8 -*-

from __future__ import absolute_import, print_function, unicode_literals
import re
import logging
import os

from salt.ext import six
import salt.utils.stringutils

YAV_RE = re.compile(r'^(?P<uuid>(?:sec|ver)-[0-9a-z]{26,})(?:\[(?P<keys>.+?)\])?$', re.I)

LOG = logging.getLogger(__name__)


def __virtual__():
    """
    We always return True here (we are always available)
    """
    return True

def get(data, *args, **kwargs):
    LOG.warning('mock secret with id: "%s"' % data)
    data = salt.utils.stringutils.to_bytes(data)
    matches = YAV_RE.match(data.strip())
    secret_uuid = matches.group('uuid')
    keys = [s.strip() for s in matches.group('keys').split(',')] if matches.group('keys') else None

    # default string
    mock = "test"

    if secret_uuid == 'sec-01ct3gzva7mygpsczsekv44axt':
        mock = {
            'pubring.gpg': '',
            'secring.gpg': '',
            'trustdb.gpg': '',
        }
        for k in mock.keys():
            path = os.path.join(os.path.dirname(__file__), '%s.b64' % k)
            with open(path, 'r') as f:
                mock[k] = f.read()
        return mock
    elif secret_uuid == 'sec-01crx3h1fsq6w1kbd8hdyp30y5':
        mock = {
            'pem': '',
            'key': '',
        }
        for k in mock.keys():
            path = os.path.join(os.path.dirname(__file__), k)
            with open(path, 'r') as f:
                mock[k] = f.read()
        if keys and len(keys) == 1:
            return mock.get(keys[0], 'test')
        return mock
    return mock
