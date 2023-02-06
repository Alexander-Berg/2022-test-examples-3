# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.conf.urls import url
from rest_framework.decorators import api_view, detail_route, list_route
from rest_framework.routers import SimpleRouter
from rest_framework.viewsets import ViewSet

from common.dev_tools.swagger.decorator import swagger_aware_view_set
from common.dev_tools.swagger.endpoint import EndpointEnumerator, Endpoint


def usual_djago_view(request):
    pass


@api_view(['POST', 'GET', 'PUT'])
def django_rest_framework_api_view(request):
    pass


def test_endpoint_enumerator_with_api_view():
    urlpatterns = [
        url(r'^usual_djago_view/$', usual_djago_view),
        url(r'^django_rest_framework_api_view/$', django_rest_framework_api_view),
    ]
    enumerator = EndpointEnumerator(urlpatterns)
    endpoints = enumerator.get_api_endpoints()
    assert endpoints == [
        Endpoint(path=u'/django_rest_framework_api_view/', method=u'GET', callback=django_rest_framework_api_view),
        Endpoint(path=u'/django_rest_framework_api_view/', method=u'POST', callback=django_rest_framework_api_view),
        Endpoint(path=u'/django_rest_framework_api_view/', method=u'PUT', callback=django_rest_framework_api_view),
    ]


@swagger_aware_view_set
class DRFViewSet(ViewSet):
    def list(self):
        pass

    @detail_route()
    def some_detail_ep(self):
        pass

    @list_route()
    def some_list_ep(self):
        pass


def test_endpoint_enumerator_with_view_set():
    router = SimpleRouter()
    router.register('some-set', DRFViewSet, base_name='some-set')
    enumerator = EndpointEnumerator(router.urls)
    endpoints = [(path, method) for path, method, callback in enumerator.get_api_endpoints()]
    assert endpoints == [
        (u'/some-set/', u'GET'),
        (u'/some-set/some_list_ep/', u'GET'),
        (u'/some-set/{pk}/some_detail_ep/', u'GET')
    ]
