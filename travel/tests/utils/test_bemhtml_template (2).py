# -*- coding: utf-8 -*-

import mock
from django.http import HttpRequest
from django.template.engine import Engine
from django.test.utils import override_settings

from common.tests.utils import has_hemi


@has_hemi
def test_creation():
    from common.utils.bemhtml.template import Template

    request = HttpRequest()
    request.META['HTTP_HOST'] = 'rasp.yandex.ru'
    processor_mock = mock.Mock(return_value={})
    with override_settings(ALLOWED_HOSTS=['rasp.yandex.ru']), \
            mock.patch.object(Engine, 'template_context_processors',
                              new_callable=mock.PropertyMock, return_value=(processor_mock,)):
        template = Template(request, {'foo': 1})
    context = template.context

    processor_mock.assert_called_once_with(request)
    assert context.request is request
    assert context.absolute_uri
    assert context.foo == 1
