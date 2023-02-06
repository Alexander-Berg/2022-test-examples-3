# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.utils.decorators import classonlymethod

from common.dev_tools.swagger.decorator import swagger_aware_view_set


@swagger_aware_view_set
class _ViewSet(object):
    def callback(self):
        """important docstring"""

    @classonlymethod
    def as_view(cls, actions=None, **kwargs):
        return lambda: None


def test_swagger_aware_view_set():
    assert _ViewSet.__name__ == '_ViewSet'

    fun = _ViewSet.as_view({'get': 'callback'})
    assert fun.__doc__ == 'important docstring'
    assert fun.actions == {'get': 'callback'}
