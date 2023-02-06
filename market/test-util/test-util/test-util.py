# -*- coding: utf-8 -*-
from flask import Flask
from flask import make_response
from flask import request
import flask
import os
import datetime
from OpenSSL import SSL
import logging
import requests
import threading
import time
import json
import xml.etree.ElementTree as ET
import functools
import random

logging.basicConfig(
        level=logging.INFO,
        format='[%(asctime)s] %(levelname)s -- %(message)s',
        datefmt='%m/%d/%Y %H:%M:%S')
LOG = logging.getLogger('main')

SHOPS_SVN = 'svn'
SHOPS_QA = 'qa'

FMT_XML = 'XML'
FMT_JSON = 'JSON'
AUTH_URL = 'URL'
AUTH_HEADER = 'HEADER'

R_CART = 'cart'
R_ACCEPT = 'accept'
R_STATUS = 'status'
R_CONFIG = 'config'

FORMATS = {
        'application/xml': FMT_XML,
        'application/json': FMT_JSON}

RESOURCES = {
        SHOPS_SVN: {
            R_CART: 'cart',
            R_ACCEPT: 'order_accept',
            R_STATUS: 'order_status',
            R_CONFIG: 'global'},
        SHOPS_QA: {
            R_CART: 'cart',
            R_ACCEPT: 'order/accept',
            R_STATUS: 'order/status',
            R_CONFIG: 'config'}}

class TestUtil:
    def __init__(self):
        print 'Running TestUtil.__init__'
        print 'flask.__version__: ' + flask.__version__
        self.app = Flask('test-util')
        self.shops_data = {}
        self.last_reload = datetime.datetime.now()
        self.last_requests = []
        self.outlets = {}
        self._parse_outlets()
        self._init_routes()

    def _parse_outlets(self):
        print 'Running TestUtil._parse_outlets'
        if os.path.exists('shopsOutlet.xml'):
            tree = ET.parse('shopsOutlet.xml')
        else:
            tree = ET.parse('/var/lib/yandex/market-checkout-test-util/shopsOutlet.xml')

        outlet_info = tree.getroot()

        for shop in outlet_info.findall('Shop'):
            shop_id = shop.attrib['id']
            shop_outlets = {}
            for outlet in shop.findall('outlet'):
                if outlet.find('ShopPointId') is None:
                    continue
                if outlet.find('RegionId') is None:
                    continue
                point_id = outlet.find('ShopPointId').text
                region_id = outlet.find('RegionId').text

                shop_outlets.setdefault(region_id, [])
                shop_outlets[region_id].append(point_id)
            self.outlets[shop_id] = shop_outlets
        LOG.info('loaded outlets for %s shops' % len(self.outlets))

    def __make_simple_config(self, shop_id):
        return {'token': 'token',
                'data-type': FMT_XML,
                'auth-type': AUTH_URL,
                'generate-data': 'on',
                'shop-id': shop_id,
                'random-order-id': 'on',
                'price':250,
                'timeout': 0}

    def _init_routes(self):
        @self.app.route(
                '/auto-shop/<shop_id>/cart',
                methods=['GET', 'HEAD', 'POST'])
        def auto_cart(shop_id):
            return self.__generate_response(
                    self.__make_simple_config(shop_id),
                    shop_id, R_CART)

        @self.app.route(
                '/auto-shop/<shop_id>/order/accept',
                methods=['GET', 'HEAD', 'POST'])
        def auto_order_accept(shop_id):
            return self.__generate_response(
                    self.__make_simple_config(shop_id),
                    shop_id, R_ACCEPT)

        @self.app.route(
                '/auto-shop/<shop_id>/order/status',
                methods=['GET', 'HEAD', 'POST'])
        def auto_order_status(shop_id):
            return self.__generate_response(
                    self.__make_simple_config(shop_id),
                    shop_id, R_STATUS)

        @self.app.route(
                '/shop/<shop_id>/cart',
                methods=['GET', 'HEAD', 'POST'])
        def cart(shop_id):
            return self.__make_response(shop_id, R_CART)

        @self.app.route(
                '/shop/<shop_id>/order/accept',
                methods=['GET', 'HEAD', 'POST'])
        def order_accept(shop_id):
            return self.__make_response(shop_id, R_ACCEPT)

        @self.app.route(
                '/shop/<shop_id>/order/status',
                methods=['GET', 'HEAD', 'POST'])
        def order_status(shop_id):
            return self.__make_response(shop_id, R_STATUS)

        @self.app.route('/info')
        def info():
            html = u'последнее обновление %s' % self.last_reload
            for last_request in self.last_requests:
                html += '<hr />'
                html += u'<pre>%s</pre>' % last_request
            return html

    def run(self, **kwargs):
        self.app.run(**kwargs)

    def read_file(self, contents):
        reading_body = False
        result = {}
        for l in contents.split('\n'):
            if reading_body:
                result['body'] += l
            elif len(l.strip()) > 0:
                l = l.strip()
                data = l.split('=', 2)
                if len(data) < 2:
                    return {'error': u'строка \'%s\' не похожа на параметр' % l}
                param, value = data
                param = param.strip()
                if param == 'body':
                    result[param] = value
                    reading_body = True
                else:
                    result[param] = value

        return {'data': result}


    def validate_fields(self, data, fields):
        for param in fields:
            if param not in data:
                return {'error': (u'не указан параметр \'%s\' в глобальных' + \
                        u' настройках магазина') % param}

    def num(self, s):
        try:
            return int(s)
        except ValueError:
            return float(s)

    def read_global(self, contents):
        data = self.read_file(contents)
        if 'error' in data:
            return data
        data = data['data']
        if 'generate-data' not in data:
            data['generate-data'] = False
        else:
            data['generate-data'] = True

        if 'random-order-id' not in data:
            data['random-order-id'] = False
        else:
            data['random-order-id'] = True

        if 'timeout' not in data:
            data['timeout'] = 0
        try:
            timeout = float(data['timeout'])
            if timeout < 0 or timeout > 60:
                raise ValueError()
            data['timeout'] = timeout
        except ValueError:
            return {'error': u'timeout должен быть числом 0-60, ' + \
                    u'не обязательно целым: %s' % data['timeout']}

        result = self.validate_fields(data, ['token', 'data-type', 'auth-type'])
        if result is not None:
            return result
        if data['data-type'] not in [FMT_XML, FMT_JSON]:
            return {'error': u'Неизвестный формат данных "%s". Должен быть' + \
                    u' XML или JSON.' % data['data-type']}
        if data['auth-type'] not in [AUTH_URL, AUTH_HEADER]:
            return {'error': u'Неизвестный способ авторизации "%s".' + \
                    u' Должен быть URL или HEADER.' % data['auth-type']}

        if 'price' in data:
            data['price'] = self.num(data['price'])
        return data


    def read_resource(self, contents):
        data = self.read_file(contents)
        if 'error' in data:
            return data
        data = data['data']
        result = self.validate_fields(data, ['status', 'body'])
        if result is not None:
            return result
        if len(data['status'].split(' ', 2)) < 2:
            return {'error': u'status должен быть указан' + \
                    u' полностью: код и сообщение. Например,' + \
                    u' "400 BAD REQUEST"'}
        return data

    def __request(self, shop_id, resource, type_):
        resource = RESOURCES[type_][resource]
        auth = None
        if type_ == SHOPS_SVN:
            url = 'https://svn.yandex.ru/market/market/trunk/checkout/' + \
                    'push-api/test-util/shops/%(shop_id)s/%(resource)s.txt'
            #auth = ('marketdatabuild', 'kleschevina')
            auth = ('marketdatabuild', 'FjqG*Ya9')
        elif type_ == SHOPS_QA:
            url = 'http://aqua.yandex-team.ru/storage/get/ru/yandex/' + \
                    'autotests/market/push/api/shop/%(shop_id)s/%(resource)s'
        return requests.get(url % {'shop_id': shop_id, 'resource': resource},
                auth=auth, verify=False, timeout=1)

    def __build_request(self):
        last_request = '%s %s\n' % (request.method, request.url)
        for header in request.headers.keys():
            if header == 'Cookie':
                continue
            last_request += '%s: %s\n' % (header, request.headers[header])
        last_request += '\n'
        last_request += request.data.decode('utf-8')

        return last_request

    def __make_response(self, shop_id, resource):
        type_ = SHOPS_SVN
        res = self.__request(shop_id, R_CONFIG, type_)
        if res.status_code == 404:
            type_ = SHOPS_QA
            res = self.__request(shop_id, R_CONFIG, type_)
        if res.status_code == 404:
            return make_response(
                    u'<h2>магазин "%s" не найден</h2>' % shop_id,
                    '404 not found')

        config = self.read_global(res.text)
        if 'error' in config:
            return make_response(
                    u'Проблемы с конфигурацией магазина:' + \
                            config['error'], '404 not found')

        return self.__generate_response(config, shop_id, resource, type_)

    def __generate_response(self, config, shop_id, resource, type_=None):
        if config['generate-data']:
            if 'shop-id' in config:
                real_shop_id = config['shop-id']
            else:
                real_shop_id = shop_id
            response_body = self.__parse_request(
                    config, request.data, real_shop_id, resource)
            response = make_response(response_body)
        else:
            res = self.__request(shop_id, resource, type_)
            if res.status_code == 404:
                return make_response(
                    u'не могу получить ресурс "%s" магазина "%s", т.к. "%s" отдает 404' % (
                        resource, shop_id, res.url))
            if res.encoding is not None:
                encoding = res.encoding
            else:
                encoding = 'utf-8'

            try:
                text = res.text.encode(encoding)
            except UnicodeEncodeError:
                text = res.text

            resource_data = self.read_resource(text)
            if 'error' in resource_data:
                return make_response(
                        u'Проблемы с конфигурацией магазина:<br />' + \
                                resource_data['error'], '404 not found')

            last_request = self.__build_request()
            LOG.info(last_request)

            self.last_requests.append(last_request)
            if len(self.last_requests) > 40:
                self.last_requests.pop(0)

            response = make_response(resource_data['body'], resource_data['status'])

        if config['data-type'] == FMT_XML:
            response.headers['Content-Type'] = 'application/xml; charset=utf-8'
        else:
            response.headers['Content-Type'] = 'application/json; charset=utf-8'

        if config['auth-type'] == AUTH_URL:
            if 'auth-token' not in request.args:
                return make_response(u'токен не найден в урле', '403 forbidden')
            token = request.args['auth-token']
        else:
            if 'Authorization' not in request.headers:
                return make_response(
                        u'токен не найден в заголовках', '403 forbidden')
            token = request.headers['Authorization']

        if config['token'] != 'token' and token != config['token']:
            return make_response(u'токен не совпадает', '403 forbidden')

        time.sleep(config['timeout'])
        return response

    def __parse_request(self, config, request_body, shop_id, resource):
        fmt = config['data-type']
        random_order_id = config['random-order-id']
        def get_outlets_by_region(regions):
            if shop_id not in self.outlets:
                return [1]
            else:
                outlets = []
                for region in regions:
                    region = str(region)
                    if region in self.outlets[shop_id]:
                        outlets += self.outlets[shop_id][region]
                if len(outlets) == 0:
                    outlets.append(9)
                return outlets

        if fmt == FMT_XML:
            typedConverter = XmlRequestConverter(config)
        else:
            typedConverter = JsonRequestConverter(config)
        return Converter().convert(typedConverter, resource, request_body,
                get_outlets_by_region, random_order_id)

class Converter:
    def __create_dates(self):
        now = datetime.datetime.now()
        next_week = now + datetime.timedelta(days=7)
        def format_date(date):
            return date.strftime('%d-%m-%Y')

        return {'from-date': format_date(now),
                'to-date': format_date(next_week)}

    def convert(self, converter, resource, request_body,
            get_outlets_by_region, random_order_id):
        if resource == R_CART:
            items, regions = converter.parse(request_body)
            print 'got regions %s' % regions
            dates = self.__create_dates()
            outlets = get_outlets_by_region(regions)
            print 'got outlets %s' % outlets
            return converter.build_cart(items, dates, outlets)
        elif resource == R_ACCEPT:
            order_id = random.randint(1, 1000000000) if random_order_id \
                    else 1234
            return converter.build_accept(order_id)
        elif resource == R_STATUS:
            return converter.build_status()

class JsonRequestConverter():
    def __init__(self, config):
        self.config = config

    def get_regions(self, jsn, regions=None):
        if regions is None:
            regions = []
        regions.append(jsn['id'])
        if 'parent' in jsn:
            return self.get_regions(jsn['parent'], regions)
        else:
            return regions

    def get_items(self, jsn):
        return [{
            'feedId': item['feedId'],
            'offerId': item['offerId'],
            'offerName': item['offerName'],
            'count': item['count'],
            'price': self.config['price'] if 'price' in self.config else 250,
            'delivery': True
            } for item in jsn ]

    def parse(self, body):
        jsn = json.loads(body)
        items = self.get_items(jsn['cart']['items'])
        regions = self.get_regions(jsn['cart']['delivery']['region'])
        return (items, regions)

    def build_cart(self, items, dates, outlets):
        items_json = [
                '            %s' % json.dumps(item)
                for item in items ]
        outlets_json = [
                '{"id": %s}' % outlet_id for outlet_id in outlets
                ]
        return '''
{
    "cart": {
        "items": [
%(items)s
        ],
        "deliveryOptions": [
            {
                "type": "PICKUP",
                "serviceName": "Почта России PICKUP",
                "price": 0,
                "dates": {"fromDate":"%(from_date)s", "toDate":"%(to_date)s"},
                "outlets": [%(outlets)s]
            },
            {
                "type": "DELIVERY",
                "serviceName": "Почта России DELIVERY",
                "price": 350,
                "dates": {"fromDate":"%(from_date)s", "toDate":"%(to_date)s"}
            },
            {
                "type": "POST",
                "serviceName": "Почта России POST",
                "price": 250,
                "dates": {"fromDate":"%(from_date)s", "toDate":"%(to_date)s"}
            }
        ],
        "paymentMethods": [
            "CASH_ON_DELIVERY",
            "CARD_ON_DELIVERY",
            "SHOP_PREPAID",
            "YANDEX"   
        ]
    }
}
        ''' % {'items': ',\n'.join(items_json),
                'from_date': dates['from-date'],
                'to_date': dates['to-date'],
                'outlets': ','.join(outlets_json)}
    def build_accept(self, order_id):
        return '''
{
    "order": {
        "id": "%(order_id)s",
        "accepted": true
    }
}
        ''' % {'order_id': order_id}

    def build_status(self):
        return ''

class XmlRequestConverter():
    def __init__(self, config):
        self.config = config

    def get_items(self, cart_tag):
        items = []
        i = 0
        for item_tag in cart_tag.find('items').findall('item'):
            i += 1
            item = {'feed-id': item_tag.attrib['feed-id'],
                    'offer-id': item_tag.attrib['offer-id'],
                    'feed-category-id': item_tag.attrib['feed-category-id'],
                    'offer-name': item_tag.attrib['offer-name'],
                    'price': self.config['price'] if 'price' in self.config else 250,
                    'count': item_tag.attrib['count']}
            items.append(item)
        return items

    def get_regions(self, region_tag, regions=None):
        if regions is None:
            regions = []

        regions.append(region_tag.attrib['id'])

        parent_tag = region_tag.find('parent')
        if parent_tag is not None:
            return self.get_regions(parent_tag, regions)
        else:
            return regions

    def parse(self, body):
        cart_tag = ET.fromstring(body)
        items = self.get_items(cart_tag)
        regions = self.get_regions(cart_tag.find('delivery').find('region'))
        return items, regions

    def build_cart(self, items, dates, outlets):
        items_xml = [
                '        <item feed-id="%s" offer-id="%s" price="%s" count="%s" delivery="true" />' % (
                    item['feed-id'], item['offer-id'], item['price'], item['count'])
                for item in items
                ]
        outlets_xml = [
                '                <outlet id="%s"/>' % outlet_id
                for outlet_id in outlets
                ]
        return '''
<cart>
    <items>
{items}
    </items>
    <delivery-options>
        <delivery service-name="Почта России PICKUP" type="PICKUP" price="0">
            <dates from-date="{from_date}" to-date="{to_date}" />
            <outlets>
{outlets}
            </outlets>
        </delivery>
        <delivery service-name="Почта России DELIVERY" type="DELIVERY" price="350">
            <dates from-date="{from_date}" to-date="{to_date}" />
        </delivery>
        <delivery service-name="Почта России POST" type="POST" price="250">
            <dates from-date="{from_date}" to-date="{to_date}" />
        </delivery>
    </delivery-options>
    <payment-methods>
        <payment-method>SHOP_PREPAID</payment-method>
        <payment-method>CASH_ON_DELIVERY</payment-method>
        <payment-method>CARD_ON_DELIVERY</payment-method>
        <payment-method>YANDEX</payment-method>
    </payment-methods>
</cart>
        '''.format(
                items='\n'.join(items_xml),
                from_date=dates['from-date'],
                to_date=dates['to-date'],
                outlets='\n'.join(outlets_xml))

    def build_accept(self, order_id):
        return '<order id="%(order_id)s" accepted="true" />' % {
                'order_id': order_id}

    def build_status(self):
        return ''

def main():
    def start_server(port, use_ssl):
        print 'Running start_server'
        test_util = TestUtil()

        if use_ssl:
            context = SSL.Context(SSL.SSLv23_METHOD)
            context.use_privatekey_file('cert/server.key')
            context.use_certificate_file('cert/server.crt')
        else:
            context = None

        test_util.run(host='0.0.0.0', port=port, debug=True, use_reloader=False, ssl_context=context)

    threads = [
        threading.Thread(target=lambda: start_server(39003, False)),
        threading.Thread(target=lambda: start_server(39005, True))
    ]

    for thread in threads:
        thread.daemon = True
        thread.start()

    while True:
        time.sleep(10000)


if __name__ == '__main__':
    print 'Running main'
    main()

