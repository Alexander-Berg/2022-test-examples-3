# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function


class FakeUrlConf(object):
    def __init__(self, urlpatterns):
        self.urlpatterns = urlpatterns
