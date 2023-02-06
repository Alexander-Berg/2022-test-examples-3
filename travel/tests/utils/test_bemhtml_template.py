# -*- coding: utf-8 -*-

import mock
from django.http import HttpRequest
from django.template.engine import Engine

from travel.avia.library.python.common.utils.bemhtml.template import Template


def test_creation():
    request = HttpRequest()
    request.META['HTTP_HOST'] = 'rasp.yandex.ru'
    processor_mock = mock.Mock(return_value={})
    with mock.patch.object(Engine, 'template_context_processors',
                           new_callable=mock.PropertyMock, return_value=(processor_mock,)):
        template = Template(request, {'foo': 1})
    context = template.context

    processor_mock.assert_called_once_with(request)
    assert context.request is request
    assert context.absolute_uri
    assert context.foo == 1
