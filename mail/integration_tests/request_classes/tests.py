# -*- coding: utf-8 -*-

from mail.swat.integration_tests.lib.http_request import HttpRequest, HttpRequestContext
from mail.swat.integration_tests.lib import environments
from time import sleep


class Service(HttpRequest):

    def __init__(self, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'Service'
        self.method = 'post'
        self.path = f'service'
        self.status_codes = [200, 409]
        self.data = {'token': self.token, "entity_id": "integration_testing 16", "description": "Integration testing tool"}

    def fill_ctx(self, response, context):
        if response['code'] == 200:
            context.service_merchant_id = response['data']['service_merchant_id']
        elif response['code'] == 409:
            context.service_merchant_id = response['data']['params']['service_merchant_id']

class Draft(HttpRequest):

    def __init__(self, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'Draft'
        self.method = 'post'
        self.path = f'merchant/{self.uid}/draft'
        self.req_type = 'internal'
        self.status_codes = [200, 409]
        self.data = environments.MERCHANT_DRAFT_DATA
        self.data.update({'token': self.token})

    def fill_ctx(self, response, context):
        if response['code'] == 200:
            context.service_merchant_id = response['data']['service_merchant_id']
        elif response['code'] == 409:
            context.service_merchant_id = response['data']['params']['service_merchant_id']

class Suggest(HttpRequest):

    def __init__(self, inn, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'Suggest'
        self.method = 'get'
        self.path = f'merchant/suggest'
        self.req_type = 'client'
        self.params = {'query': inn}


class Preregister(HttpRequest):

    def __init__(self, uid, spark_id, categories: list, inn: str, services: list, require_online=True, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'Preregister'
        self.method = 'post'
        self.path = f'merchant/{uid}/preregister'
        self.data = {'spark_id': spark_id, 'categories': categories, 'require_online': require_online, 'inn': str(inn), 'services': services}


class CreateOrder(HttpRequest):

    def __init__(self,  service_merchant_id, caption, items: list, description=None, autoclear=True, mode='prod', **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'CreateOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}'
        self.data = {'caption': caption, 'autoclear': autoclear, 'items': items, 'mode': mode}
        self.mode = mode

    def fill_ctx(self, response, context):
        if self.mode == 'prod':
            context.order_id = response['data']['order_id']
        else:
            context.test_order_id = response['data']['order_id']


class GetOrders(HttpRequest):

    def __init__(self, service_merchant_id, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'GetOrders'
        self.path = f'order/{service_merchant_id}'


class GetOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'GetOrder'
        self.path = f'order/{service_merchant_id}/{order_id}'

class ChangeOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, caption, items: list, description=None, autoclear=True, mode='prod', **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'ChangeOrder'
        self.method = 'put'
        self.path = f'order/{service_merchant_id}/{order_id}'
        self.data = {'caption': caption, 'autoclear': autoclear, 'items': items, 'mode': mode, 'autoclear': False}

class StartOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, email, return_url='https://ya.ru/', **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'StartOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/start'
        self.data = {'email': email, 'return_url': return_url}

    def fill_ctx(self, response, context):
        context.pay_link = response['data']['trust_url']

class ClearOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, items=None, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'ClearOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/clear'
        if items:
            self.params = {'items': items}


class UnholdOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'UnholdOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/unhold'


class CancelOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'CancelOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/cancel'


class RefundOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, items=None, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'RefundOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/refund'
        if items:
            self.params = {'items': items}


class PayOffline(HttpRequest):

    def __init__(self, service_merchant_id, order_id, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'PayOffline'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/pay_offline'


class ActivateOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'ActivateOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/activate'


class DeactivateOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'DeactivateOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/deactivate'


class CloseOrder(HttpRequest):

    def __init__(self, service_merchant_id, order_id, items, **kwargs):
        super().__init__()
        self.__dict__.update(kwargs)
        self.name = 'CloseOrder'
        self.method = 'post'
        self.path = f'order/{service_merchant_id}/{order_id}/receipt/close'
        self.params = {'items': items}

    def fill_ctx(self, response, ctx):
        pass


def run_tests(params):
    ctx = HttpRequestContext()
    Service(**params).run(ctx)    

    # Draft(**params).run(ctx) а мы будем оставлять драфты?

    input('Please activate a new service in settings and proceed')
    # Suggest(inn=510105936358, reference='suggest.json', **params).run(ctx)
    GetOrders(service_merchant_id=ctx.service_merchant_id, **params).run(ctx)
    CreateOrder(service_merchant_id=ctx.service_merchant_id, caption='Integration Test Order (Test order)', items=[{'name': "11", 'nds': "nds_20", 'currency': "RUB", 'amount': 11, 'price': 1, 'total_price': 11}], mode='test', description='Integration Test Order Description in Testing', **params).run(ctx)
    CreateOrder(service_merchant_id=ctx.service_merchant_id, caption='Integration Test Order', items=[{'name': "11", 'nds': "nds_20", 'currency': "RUB", 'amount': 11, 'price': 1, 'total_price': 11}], description='Integration Test Order Description', **params).run(ctx)
    GetOrder(service_merchant_id=ctx.service_merchant_id, order_id=ctx.order_id, **params).run(ctx) #my_feature=ctx.my_feature, 
    ChangeOrder(service_merchant_id=ctx.service_merchant_id, order_id=ctx.order_id, caption='Integration Test Order (edited)', items=[{'name': "1", 'nds': "nds_20", 'currency': "RUB", 'amount': 1, 'price': 1, 'total_price': 1}], **params).run(ctx)
    GetOrders(service_merchant_id=ctx.service_merchant_id, **params).run(ctx)#reference='orders.json'
    StartOrder(service_merchant_id=ctx.service_merchant_id, order_id=ctx.order_id, email='pay-testuser@yandex.ru', **params).run(ctx)
    input(f'Open link and pay for order (1RUB) {ctx.pay_link}')
    ClearOrder(service_merchant_id=ctx.service_merchant_id, order_id=ctx.order_id, **params).run(ctx)
