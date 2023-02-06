# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import six
from django.conf import settings
from six.moves import StringIO


def set_setting(name, value):
    try:
        getattr(settings, name)
    except AttributeError:
        raise AssertionError('No such setting: {}'.format(name))

    return mock.patch.object(settings, name, value)


def patch_urlopen(obj, handler_or_response):
    def process(*args, **kwargs):
        if isinstance(handler_or_response, six.string_types):
            response = handler_or_response
        else:
            response = handler_or_response(*args, **kwargs)

        stream = StringIO()
        stream.write(response)
        stream.seek(0)
        return stream

    return mock.patch.object(obj, 'urlopen', mock.Mock(side_effect=process))
