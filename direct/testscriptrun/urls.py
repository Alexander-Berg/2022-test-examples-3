# -*- encoding: utf-8 -*-
from django.conf.urls import patterns, url, include

urlpatterns = patterns('releaser.testscriptrun.views',
    (r'^/?$', 'index'),
)
