# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import six
from httpretty import Response

from travel.rasp.train_api.train_partners.ufs.base import UFS_ENCODING, build_url


def mock_ufs(httpretty, endpoint, **kwargs):
    url = build_url('webservices/Railway/Rest/Railway.svc/{endpoint}'.format(endpoint=endpoint))
    if isinstance(kwargs.get('body'), six.text_type):
        kwargs['body'] = kwargs['body'].encode(UFS_ENCODING)
    httpretty.register_uri(httpretty.GET, url, **kwargs)


def create_ufs_response(body, **kwargs):
    if isinstance(body, six.text_type):
        body = body.encode(UFS_ENCODING)
    return Response(body, **kwargs)
