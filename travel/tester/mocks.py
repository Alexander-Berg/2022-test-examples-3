from StringIO import StringIO

import mock
from django.conf import settings


def set_setting(name, value):
    try:
        getattr(settings, name)
    except AttributeError:
        raise AssertionError(u'No such setting: {}'.format(name))

    return mock.patch.object(settings, name, value)


def patch_urlopen(obj, handler_or_response):
    def process(*args, **kwargs):
        if isinstance(handler_or_response, basestring):
            response = handler_or_response
        else:
            response = handler_or_response(*args, **kwargs)

        stream = StringIO()
        stream.write(response)
        stream.seek(0)
        return stream

    return mock.patch.object(obj, 'urlopen', mock.Mock(side_effect=process))
