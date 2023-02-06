# -*- coding: utf-8 -*-

import flask
import pytest
import six

from collections import OrderedDict
from dateutil.parser import parse
from six.moves.urllib.parse import urlencode, unquote_plus, quote_plus, quote
from utils import set_config_env_variables
from market.pylibrary.yenv import (
    set_environment_type,
    set_marketindexer_type,
    TESTING,
    PRODUCTION,
    STRATOCASTER,
    PLANESHIFT_STRATOCASTER,
)
from market.idx.api.backend.blueprints.otrace import (
    MBO_ENTITY_URL_TEMPLATE,
    TSUM_BASE_URL,
)
from market.idx.api.backend.config import build_config
from market.idx.api.backend.report import get_documents_from_report
from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.chyt_storage import FeedProperties
from market.idx.api.backend.marketindexer.storage.storage import Storage
from mock import patch

valid_requests = [
    {
        'offer': 'first',
        'feed': 1,
        'date': None,
        'expected_req_id': '3df451ace180922457276c35d6ec64c0',
        'expected_testing_req_id': '44a43d8013f1847e8a5de5b712a6e93b',
        'ware_md5': 'acb128769087602345'
    },
    {
        'offer': 'offer.2',
        'feed': 2,
        'date': '20010101',
        'expected_req_id': '978310800000/1c644c92e173546f98d6e60d7cf1f9ac',
        'expected_testing_req_id': '978310800000/e5c5ff753080ff5b85ae3322793a3410',
        'ware_md5': '12345769407aa'
    },
    {
        'offer': six.ensure_text('Проверочный оффер (.,\\/[]-=.)'),
        'feed': 600,
        'date': '20191123',
        'expected_req_id': '1574470800000/42ee74ce5a646b8f52134f9e9be19d78',
        'expected_testing_req_id': '1574470800000/2a38fd26b380f9fb2fa74a4f116b1a31',
        'ware_md5': '101010101010101010101'
    },
    # Real-world example
    {
        'offer': six.ensure_text('000078.КРЕС0149'),
        'feed': 493303,
        'date': '20190121',
        'expected_req_id': '1548032400000/311cff82f570eef616443d9399e8ef7d',
        'expected_testing_req_id': '1548032400000/625efddf160743d396cb334bba9f8a87',
        'ware_md5': '5454545454545545454545'
    }
]

invalid_requests = [
    {
        'offer': '123',
        'feed': 345,
        'date': 123456789,
        'expected_response': (406, 'Not Acceptable', 'unconverted data remains: 789'),
        'ware_md5': '22222222222222222222222222'
    }
]

DATACAMP_URL_TEMPLATE = "http://{base_url}.vs.market.yandex.net/shops/1/offers?format=json&offer_id={offerid}{warehouse}"
DATACAMP_CASES = OrderedDict([
    # key = (env_type, rgb, ware_id, offerid)
    ((TESTING, "blue", 145, None), DATACAMP_URL_TEMPLATE.format(base_url="datacamp.white.tst", warehouse="&whid=145", offerid=2)),
    ((TESTING, "blue", 145, six.ensure_text("абвгд")), DATACAMP_URL_TEMPLATE.format(
        base_url="datacamp.white.tst",
        warehouse="&whid=145",
        offerid="%D0%B0%D0%B1%D0%B2%D0%B3%D0%B4"
    )),
    ((PRODUCTION, "blue", 145, None), DATACAMP_URL_TEMPLATE.format(base_url="datacamp.white", warehouse="&whid=145", offerid=2)),
    ((TESTING, "blue", None, None), DATACAMP_URL_TEMPLATE.format(base_url="datacamp.white.tst", warehouse="", offerid=2)),
    ((TESTING, "", 145, None), DATACAMP_URL_TEMPLATE.format(base_url="datacamp.white.tst", warehouse="", offerid=2)),
    ((TESTING, "", None, None), DATACAMP_URL_TEMPLATE.format(base_url="datacamp.white.tst", warehouse="", offerid=2)),
])
REPORT_URL_CASES = [
    (env_type, planeshift, rgb)
    for rgb in ('', 'blue')
    for planeshift in (True, False)
    for env_type in (PRODUCTION, TESTING)
]
YCOMBO_URL_CASES = [
    (env_type, rgb)
    for rgb in ('', 'blue')
    for env_type in (PRODUCTION, TESTING)
]
REPORT_BALANCER_VALID_CASES = [
    (PRODUCTION, 'production', 'http://warehouse-report.vs.market.yandex.net:17051'),
    (PRODUCTION, 'production_blue', 'http://warehouse-report.blue.vs.market.yandex.net:17051'),
    (PRODUCTION, 'production_exp', 'http://rw.vs.market.yandex.net:80'),
    (PRODUCTION, 'production_int', 'http://int-report.vs.market.yandex.net:17151'),
    (PRODUCTION, 'production_shadow', 'http://shadow-report.vla.vs.market.yandex.net:17051'),
    (TESTING, 'testing', 'http://report.tst.vs.market.yandex.net:17051'),
    (TESTING, 'testing_blue', 'http://warehouse-report.blue.tst.vs.market.yandex.net:17051'),
]
REPORT_BALANCER_INVALID_CASES_TYPE1 = [
    (PRODUCTION, 'testing'),
    (PRODUCTION, 'abracadabra'),
    (TESTING, 'production_blue'),
    (TESTING, 'avadakedavra'),
]
REPORT_BALANCER_INVALID_CASES_TYPE2 = [
    (PRODUCTION, 'production_t'),
    (PRODUCTION, 'productionverve'),
    (TESTING, 'testing_a'),
    (TESTING, 'testingefr'),
]

DEFAULT_MSKU = 123321
DEFAULT_MODEL_ID = 321123


def get_feed_properties_mock(params=None, shop_name=None, **kwargs):
    feed_props_dict = {
        'shop_id': 111000,
        'feed_id': 222000,
        'warehouse_id': 145,
        'campaign_id': 1,
        'shop_name': 'ShopName',
        'is_enabled': True,
        'is_mock': False,
        'is_tested': False,
        'is_push_partner': True,
        'supplier_type': 3,
        'is_blue': True
    }
    feed_props_dict.update(kwargs)
    return [FeedProperties(**feed_props_dict)]


def get_report_prefix(env_type, planeshift, rgb=None):
    port = 17051
    if planeshift:
        host = 'ps.tst' if env_type == TESTING else 'ps-report'
    else:
        if env_type == PRODUCTION:
            port = 80
        host = 'rw' if env_type == PRODUCTION else 'report.tst'

    return 'http://{}.vs.market.yandex.net:{}/yandsearch?place='.format(host, port)


def url_to_report_prefix(base_url):
    return '{}/yandsearch?place='.format(base_url)


def request_params(request, fields, rgb=None):
    params_dict = {}
    for f in fields:
        if f == 'expected_response':
            continue
        elif f in ('offer', 'shop_sku', 'shop-sku'):
            params_dict[f] = six.ensure_str(request['offer'])
        elif f in ('warehouse', 'supplier'):
            params_dict[f] = 123  # retrieval of feed by supplier+warehouse is always mocked in this test
        elif request.get(f) is not None:
            params_dict[f] = request[f]
    if rgb:
        params_dict['RGB_DEPRECATED_DO_NOT_USE'] = rgb

    return urlencode(params_dict)


@pytest.fixture(scope="module")
def test_app():
    def test_app_executor(env_type=TESTING, is_planeshift=False):
        set_config_env_variables()
        set_environment_type(env_type)
        set_marketindexer_type(PLANESHIFT_STRATOCASTER if is_planeshift else STRATOCASTER)
        return create_flask_app(Storage())
    return test_app_executor


def check_report_url(data, query, place, expected_params, report_prefix=None):
    if report_prefix is None:
        report_prefix = get_report_prefix(TESTING, False)
    url = data['urls']['report'][query]
    assert len(url) > len(report_prefix)
    params = url[len(report_prefix):].split('&')
    assert params[0] == place

    params_dict = {}
    for p in params[1:]:
        k, v = p.split('=')
        params_dict[k] = unquote_plus(v)

    for k, v in list(expected_params.items()):
        assert params_dict[k] == v


def get_supplier_and_warehouse_mock(feed_id, args):
    return 1, 145


@pytest.mark.parametrize('env_type, is_planeshift, rgb', REPORT_URL_CASES)
def test_valid_requests(test_app, env_type, is_planeshift, rgb):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True) as mock_get_doc_props,\
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True) as mock_get_doctrace,\
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True) as mock_get_feed_props,\
            test_app(env_type, is_planeshift).test_client() as client:
        report_prefix = get_report_prefix(env_type, is_planeshift, rgb)
        for request in valid_requests:
            offer_id = six.ensure_str(request['offer'])
            mock_get_doc_props.return_value = {'feed_id': request['feed'],
                                               'offer_id': offer_id,
                                               'ware_md5': request['ware_md5'],
                                               'sku': DEFAULT_MSKU,
                                               'hyper': DEFAULT_MODEL_ID}
            mock_get_doctrace.return_value = {}
            for fields in (('ware_md5', 'date'),
                           ('feed', 'offer', 'date'),
                           ('feed', 'shop_sku', 'date'),
                           ('feed', 'shop-sku', 'date'),
                           ('warehouse', 'supplier', 'offer', 'date'),
                           ('warehouse', 'supplier', 'shop_sku', 'date'),
                           ('warehouse', 'supplier', 'shop-sku', 'date')):
                if 'feed' in fields:
                    mock_get_feed_props.return_value = None
                else:
                    mock_get_feed_props.return_value = get_feed_properties_mock(feed_id=request['feed'])

                resp = client.get('/v1/otrace?' + request_params(request, fields, rgb=rgb))
                assert resp.status_code == 200
                assert resp.data
                data = flask.json.loads(resp.data)

                # check RequestId
                index_trace = data['urls']['index_trace']
                assert index_trace.startswith(TSUM_BASE_URL)
                feed_path = 'feeds/{}'.format(request['feed'])
                idxapi_links = data['urls']['idxapi']
                assert feed_path in idxapi_links['feed']
                assert ('{}/sessions/published/offers'.format(feed_path)) in idxapi_links['published_offer']
                assert '/v2/smart/offer?' in idxapi_links['smart_offer']
                assert '/v1/check_supplier/get?feed={}'.format(request['feed']) in idxapi_links['check_supplier']

                dukalis_link = idxapi_links['dukalis']
                assert 'v1/admin/dukalis/check_offer_status/offer' in dukalis_link
                assert 'feed_id={}'.format(request['feed']) in dukalis_link
                assert 'offer_id=' + quote_plus(six.ensure_binary(offer_id)) in six.ensure_str(dukalis_link)
                assert 'rgb={}'.format(rgb or 'white') in dukalis_link  # Цвет оффера в url должен быть задан явно
                assert 'env={}'.format(env_type) in dukalis_link

                front_links = data['urls']['front']
                front_pokupka = 'https://desktop-testing-market-preview.market.fslb.yandex.ru' \
                                '/product/description/{}?offerId={}'.format(DEFAULT_MSKU, request['ware_md5'])
                assert front_links['pokupka'] == front_pokupka
                assert front_links['pokupka_without_gps'] == front_pokupka + '&force-report-params=rearr-factors=disable_gps=1'
                assert front_links['market'] == 'https://desktop-testing-market-preview.market.fslb.yandex.ru/offer/{}'.format(request['ware_md5'])

                if rgb != 'blue':
                    saashub_doc_state = six.ensure_str(data['urls']['saashub'])
                    assert ('doc_state/{}/{}'.format(request['feed'], quote_plus(six.ensure_binary(offer_id)))) in saashub_doc_state

                expected_req_id = request['expected_testing_req_id'] if env_type == TESTING else request['expected_req_id']
                request_id = index_trace[len(TSUM_BASE_URL):]
                if request['date'] is not None:
                    assert request_id == expected_req_id
                else:
                    timestamp, req_id = request_id.split('/')
                    assert req_id == expected_req_id

                report_params = {'offerid': request['ware_md5']}
                check_report_url(data, 'print_doc', 'print_doc', report_params, report_prefix)

                report_params.update({'show-urls': 'direct', 'rids': '213', 'pp': '18', 'adult': '1', 'regset': '2'})
                check_report_url(data, 'offer_info', 'offerinfo', report_params, report_prefix)

                report_params['debug'] = '1'
                if rgb == 'blue':
                    delivery_params = report_params.copy()
                    for param in ('offerid', 'feed_shoffer_id', 'show-urls'):
                        delivery_params.pop(param, None)
                    delivery_params['offers-list'] = request['ware_md5'] + ':1'
                    check_report_url(data, 'delivery', 'actual_delivery', delivery_params, report_prefix)

                report_params['rearr-factors'] = 'market_documents_search_trace=' + request['ware_md5']
                check_report_url(data, 'prime', 'prime', report_params, report_prefix)

                mbo_urls = data['urls']['mbo']
                mbo_env_suffix = '-testing' if env_type == TESTING else ''
                msku_url = mbo_urls['msku']
                assert msku_url == MBO_ENTITY_URL_TEMPLATE.format(env_suffix=mbo_env_suffix, entity_id=DEFAULT_MSKU)
                model_url = mbo_urls['model']
                assert model_url == MBO_ENTITY_URL_TEMPLATE.format(env_suffix=mbo_env_suffix, entity_id=DEFAULT_MODEL_ID)

                # check all addresses in output have ports (localhost:xxx)
                assert 'localhost/' not in str(idxapi_links)


def check_exception_response(expected_response, response):
    expected_code, expected_msg, expected_reason = expected_response
    assert response.status_code == expected_code
    expected_data = '<p><i>{} {}</i></p>\n'.format(expected_code, expected_msg)\
                    + '<p><b>{}</b></p><br/>\n'.format(expected_reason)\
                    + '<a href="http://localhost:29334/help">Описание IDX-API</a>\n'
    assert response.data == six.ensure_binary(expected_data)


def test_invalid_requests(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True) as mock_get_doc_props,\
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''),\
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            test_app().test_client() as client:
        for request in invalid_requests:
            mock_get_doc_props.return_value = {
                'feed_id': request['feed'],
                'offer_id': six.ensure_str(request['offer'])
            }
            response = client.get('/v1/otrace?' + request_params(request, list(request.keys())))
            check_exception_response(request['expected_response'], response)


def test_invalid_date(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'ware_md5': 'wm5'}),\
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''),\
            test_app().test_client() as client:
        for date in ('123', '-1', '10100', '-12305.3'):
            response = client.get('/v1/otrace?feed=123&offer=321&date=' + date)
            expected_response = (406, 'Not Acceptable', "time data '{}' does not match format '%Y%m%d'".format(date.replace('-', '')))
            check_exception_response(expected_response, response)


def check_assertion(expected_assert, response):
    assert response.status_code == 200
    expected_data = '<p><b>{}</b></p><br/>\n'.format(expected_assert)\
                     + '<a href="http://localhost:29334/help">Описание IDX-API</a>\n'
    assert six.ensure_binary(expected_data) == response.data


def test_missing_params(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, return_value=None),\
            test_app().test_client() as client:
        for params in ('?offer=2', '?warehouse=1', '?shop_sku=3', '?shop-sku=8', '?warehouse=1&offer=2'):
            response = client.get('/v1/otrace' + params)
            check_assertion('Не получилось определить feed id по входным параметрам, пожалуйста, уточните запрос', response)


def test_no_offer_id_in_request(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True) as mock_get_feed_props,\
            test_app().test_client() as client:
        for feed_id in (None, 123456789):
            mock_get_feed_props.return_value = get_feed_properties_mock(feed_id=feed_id)
            params = '?feed=1'
            no_offer_response = client.get('/v1/otrace{}'.format(params))
            check_assertion('Пожалуйста, укажите offer_id/shop_sku', no_offer_response)
            params = '?supplier=33'
            no_offer_response = client.get('/v1/otrace{}'.format(params))
            if feed_id is None:
                check_assertion('Не получилось определить feed id по входным параметрам, пожалуйста, уточните запрос', no_offer_response)
            else:
                check_assertion('Пожалуйста, укажите offer_id/shop_sku', no_offer_response)


def test_parse_request(test_app):
    with test_app().test_request_context('/v1/otrace?feed=123&offer=321&date=01010101'):
        assert flask.request.path == '/v1/otrace'
        assert flask.request.args['feed'] == '123'
        assert flask.request.args['offer'] == '321'
        assert flask.request.args['date'] == '01010101'

    with test_app().test_request_context('/v1/otrace?shop_sku=aaab&warehouse=123&supplier=456'):
        assert flask.request.path == '/v1/otrace'
        assert flask.request.args['shop_sku'] == 'aaab'
        assert flask.request.args['warehouse'] == '123'
        assert flask.request.args['supplier'] == '456'


def test_unknown_param(test_app):
    with test_app().test_client() as client:
        response = client.get('/v1/otrace?feed=123&offer=321&date=01010101&warehous_id=145')
        check_assertion('Неизвестные параметры: warehous_id<br>Рекомендуем воспользоваться '
                        + '<a href="http://localhost:29334/v1/otrace" target="_blank" rel="noreferrer noopener">формой</a>',
                        response)


def test_response_headers(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'ware_md5': 'wm5'}),\
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''),\
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:
        resp = client.get('/v1/otrace?feed=123&offer=321')
        assert resp.status_code == 200
        assert resp.headers['Content-type'] == 'application/json; charset=utf-8'
        assert resp.data


def test_response_headers_format_undef(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'ware_md5': 'wm5'}),\
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:
        resp = client.get('/v1/otrace?feed=123&offer=321&format=weird_format')
        check_exception_response((406, 'Not Acceptable', 'request mime type is not implemented: weird_format'), resp)


def test_region_argument(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2'}),\
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:
        for region, region_arg in ((554433, '&region=554433'), (13231, '&rids=13231'), (213, '')):
            resp = client.get('/v1/otrace?waremd5=wm5' + region_arg)
            assert resp.status_code == 200
            assert resp.data
            data = flask.json.loads(resp.data)

            expected_params = {
                'offerid': 'wm5',
                'rids': str(region),
            }
            check_report_url(data, 'offer_info', 'offerinfo', expected_params)
            expected_params.update({'rearr-factors': 'market_documents_search_trace=wm5'})
            check_report_url(data, 'prime', 'prime', expected_params)


def test_rearr_factors(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:
        for r in ('rearr', 'rearr-factors'):
            resp = client.get('/v1/otrace?waremd5=wm5&' + r + '=abcde=1345;fghjk=poiu')
            assert resp.status_code == 200
            assert resp.data
            data = flask.json.loads(resp.data)

            expected_params = {
                'offerid': 'wm5',
                'rearr-factors': 'hide_rules_strategy=use_dynamic;abcde=1345;fghjk=poiu'
            }
            check_report_url(data, 'offer_info', 'offerinfo', expected_params)
            expected_params.update({'rearr-factors': 'abcde=1345;fghjk=poiu;market_documents_search_trace=wm5', 'debug': '1'})
            check_report_url(data, 'prime', 'prime', expected_params)


def test_params(test_app):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:

        resp = client.get('/v1/otrace?waremd5=wm5&params=' + quote('param1=1&param2=2'))
        assert resp.status_code == 200
        assert resp.data
        data = flask.json.loads(resp.data)

        expected_params = {
            'offerid': 'wm5',
            'param1': '1',
            'param2': '2'
        }
        check_report_url(data, 'offer_info', 'offerinfo', expected_params)
        check_report_url(data, 'prime', 'prime', expected_params)


@pytest.mark.parametrize('env_type, is_planeshift, rgb', REPORT_URL_CASES)
def test_report_urls(test_app, env_type, is_planeshift, rgb):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2', 'rgb': rgb}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.datacamp.DataCampHelper.get_base_url', autospec=True, return_value='datacamp.tst.vs.market.yandex.net'), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app(env_type, is_planeshift).test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&RGB_DEPRECATED_DO_NOT_USE=' + rgb)
        data = flask.json.loads(resp.data)
        expected_params = {
            'offerid': 'wm5',
        }
        check_report_url(data, 'offer_info', 'offerinfo', expected_params, get_report_prefix(env_type, is_planeshift, rgb))
        assert(data['urls']["datacamp"] == DATACAMP_URL_TEMPLATE.format(base_url="datacamp.tst", offerid='2',
                                                                        warehouse="&whid=145" if rgb == 'blue' else ''))


@pytest.mark.parametrize('env_type, rgb', YCOMBO_URL_CASES)
def test_ycombo_urls(test_app, env_type, rgb):
    def create_ycombo_link(env_type, handle, params):
        base_url = 'http://ycombo.vs.market.yandex.net/ycombo/' if env_type == PRODUCTION else 'http://ycombo.tst.vs.market.yandex.net/ycombo/'
        fixed_flags = '&courier=on&pickup=on&user_logged_in=on&render_error=on&human=on'

        dimentions = '{}x{}x{}'.format(params['length'], params['width'], params['height'])
        supplier_id, warehouse_id = get_supplier_and_warehouse_mock(1, 2)
        combinator_params = {
            'warehouse' : warehouse_id,
            'region' : params['region'],
            'weight' : params['weight'],
            'dimentions' : dimentions,
            'lat' : params['lat'],
            'lon' : params['lon'],
            'cargo_types' : params['cargo_types'],
            'datetime' : params['timestamp'],
            'shop' : supplier_id,
            'feed' : params['feed_id'],
        }
        return base_url + handle + '?' + urlencode(combinator_params) + fixed_flags

    expected_params = {
        'feed_id': 1,
        'offer_id': '2',
        'rgb': rgb,
        'length' : 10,
        'width' : 15,
        'height' : 20,
        'weight' : 12,
        'cargo_types' : '200%2C201',
        'lat' : 55.4432,
        'lon' : 12.12345,
        'region': 213,
        'timestamp' : '2022-04-13 09:44:41.551254+03:00',
    }
    fake_datetime = parse(expected_params['timestamp'])

    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value=expected_params), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.datacamp.DataCampHelper.get_base_url', autospec=True, return_value='datacamp.tst.vs.market.yandex.net'), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_timestamp', autospec=True, return_value=fake_datetime), \
            test_app(env_type, False).test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&lat={}&lon={}&RGB_DEPRECATED_DO_NOT_USE={}'.format(
            expected_params['lat'],
            expected_params['lon'],
            rgb)
        )
        data = flask.json.loads(resp.data)

        expected_handles = ['debug', 'get_courier_options', 'get_courier_options_return', 'get_pickup_options', 'get_return_route',
                            'scenario_courier', 'scenario_pickup']

        for handle in expected_handles:
            assert(handle in data['urls']['combinator'])
            assert(data['urls']['combinator'][handle] == create_ycombo_link(env_type, handle, expected_params))


@pytest.mark.parametrize('env_type, rgb', YCOMBO_URL_CASES)
def test_ycombo_urls_are_empty_if_no_doc_properties(test_app, env_type, rgb):
    fake_datetime = parse('2022-04-13 09:44:41.551254+03:00')

    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value=None), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.datacamp.DataCampHelper.get_base_url', autospec=True, return_value='datacamp.tst.vs.market.yandex.net'), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_timestamp', autospec=True, return_value=fake_datetime), \
            test_app(env_type, False).test_client() as client:
        resp = client.get('/v1/otrace?feed=123&offer=321&lat={}&lon={}&RGB_DEPRECATED_DO_NOT_USE={}'.format(
            55.4432,
            12.12345,
            rgb)
        )
        data = flask.json.loads(resp.data)

        assert(data['urls']['combinator'] == {})


@pytest.mark.parametrize('env_type, rgb', YCOMBO_URL_CASES)
def test_ycombo_urls_some_properties_are_missing(test_app, env_type, rgb):
    def create_ycombo_link(env_type, handle, params):
        base_url = 'http://ycombo.vs.market.yandex.net/ycombo/' if env_type == PRODUCTION else 'http://ycombo.tst.vs.market.yandex.net/ycombo/'
        fixed_flags = '&courier=on&pickup=on&user_logged_in=on&render_error=on&human=on'

        supplier_id, warehouse_id = get_supplier_and_warehouse_mock(1, 2)
        combinator_params = {
            'warehouse' : warehouse_id,
            'region' : params['region'],
            'weight' : None,
            'dimentions' : None,
            'lat' : params['lat'],
            'lon' : params['lon'],
            'cargo_types' : None,
            'datetime' : params['timestamp'],
            'shop' : supplier_id,
            'feed' : params['feed_id'],
        }
        return base_url + handle + '?' + urlencode(combinator_params) + fixed_flags

    expected_params = {
        'feed_id': 1,
        'offer_id': '2',
        'rgb': rgb,
        'lat' : 55.4432,
        'lon' : 12.12345,
        'region': 213,
        'timestamp' : '2022-04-13 09:44:41.551254+03:00',
    }
    fake_datetime = parse(expected_params['timestamp'])

    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value=expected_params), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.datacamp.DataCampHelper.get_base_url', autospec=True, return_value='datacamp.tst.vs.market.yandex.net'), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_timestamp', autospec=True, return_value=fake_datetime), \
            test_app(env_type, False).test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&lat={}&lon={}&RGB_DEPRECATED_DO_NOT_USE={}'.format(
            55.4432,
            12.12345,
            rgb)
        )
        resp = client.get('/v1/otrace?feed=1&offer=321&lat={}&lon={}&RGB_DEPRECATED_DO_NOT_USE={}'.format(
            55.4432,
            12.12345,
            rgb)
        )
        data = flask.json.loads(resp.data)

        expected_handles = ['debug', 'get_courier_options', 'get_courier_options_return', 'get_pickup_options', 'get_return_route',
                            'scenario_courier', 'scenario_pickup']

        for handle in expected_handles:
            assert(handle in data['urls']['combinator'])
            assert(data['urls']['combinator'][handle] == create_ycombo_link(env_type, handle, expected_params))


@pytest.mark.parametrize('env_type, rgb', YCOMBO_URL_CASES)
def test_ycombo_urls_are_empty_if_no_config_url(test_app, env_type, rgb):
    expected_params = {
        'feed_id': 1,
        'offer_id': '2',
        'rgb': rgb,
        'length' : 10,
        'width' : 15,
        'height' : 20,
        'weight' : 12,
        'cargo_types' : '200%2C201',
        'lat' : 55.4432,
        'lon' : 12.12345,
        'region': 213,
        'timestamp' : '2022-04-13 09:44:41.551254+03:00',
    }
    fake_datetime = parse(expected_params['timestamp'])
    fake_config = build_config()
    del fake_config["ycombo.production_url"]
    del fake_config["ycombo.testing_url"]

    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value=expected_params), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, side_effect=get_supplier_and_warehouse_mock),\
            patch('market.idx.api.backend.datacamp.DataCampHelper.get_base_url', autospec=True, return_value='datacamp.tst.vs.market.yandex.net'), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            patch('market.idx.api.backend.blueprints.otrace.get_timestamp', autospec=True, return_value=fake_datetime), \
            patch('market.idx.api.backend.blueprints.otrace.build_config', autospec=True, return_value=fake_config), \
            test_app(env_type, False).test_client() as client:
        resp = client.get('/v1/otrace?feed=123&offer=321&lat={}&lon={}&RGB_DEPRECATED_DO_NOT_USE={}'.format(
            55.4432,
            12.12345,
            rgb)
        )
        data = flask.json.loads(resp.data)

        assert(data['urls']['combinator'] == {})


@pytest.mark.parametrize('input, expected', list(DATACAMP_CASES.items()))
def test_datacamp(test_app, input, expected):
    env_type, rgb, ware_id, offerid = input
    if offerid is None:
        offerid = '2'
    datacamp_bases = {
        (TESTING, 'blue'): 'datacamp.white.tst.vs.market.yandex.net',
        (PRODUCTION, 'blue'): 'datacamp.white.vs.market.yandex.net',
        (TESTING, ''): 'datacamp.white.tst.vs.market.yandex.net',
        (PRODUCTION, ''): 'datacamp.white.vs.market.yandex.net',
    }
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': offerid, 'rgb': rgb}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, return_value=(1, ware_id)), \
            patch('market.idx.api.backend.datacamp.DataCampHelper.get_base_url', autospec=True, return_value=datacamp_bases[(env_type, rgb)]), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app(env_type).test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&RGB_DEPRECATED_DO_NOT_USE={}'.format(rgb))
        data = flask.json.loads(resp.data)
        assert(data['urls']['datacamp'] == expected)


@pytest.mark.parametrize('rgb, is_available', [('blue', True), ('blue', False), ('', True), ('', False)])
def test_is_available(test_app, rgb, is_available):

    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2', 'rgb': rgb}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, return_value=(1, 145)), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True, return_value=is_available), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5')
        data = flask.json.loads(resp.data)
        assert data['offer'][six.ensure_text('Содержится в последнем поколении индекса')] == six.ensure_text('Да') if is_available else six.ensure_text('Нет')


@pytest.mark.parametrize('market_rgb, offer_rgb', [('blue', 'blue'), ('blue', 'green'), ('green', 'blue'), ('green', 'green')])
def test_market_and_offer_color(test_app, market_rgb, offer_rgb):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2', 'rgb': offer_rgb}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value={'not_empty': 'not_empty'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, return_value=(1, 145)), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True, return_value=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&RGB_DEPRECATED_DO_NOT_USE={}'.format(market_rgb))
        data = flask.json.loads(resp.data)
        assert data['offer']['color_info']['requested_market_color'] == market_rgb
        assert data['offer']['color_info']['offer_color'] == offer_rgb
        datacamp_prefix_url = 'datacamp.white.tst.vs.market.yandex.net'
        assert datacamp_prefix_url in data['urls']['datacamp']


@pytest.mark.parametrize('supplier_id', [1, 2, 3])
def test_supplier_id(test_app, supplier_id):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value={'not_empty': 'not_empty'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, return_value=(supplier_id, 145)), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True, return_value=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app().test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5')
        data = flask.json.loads(resp.data)
        assert data['offer']['supplier_id'] == supplier_id


def test_skip_fake_offer(test_app):
    """ Берем из print_doc только реальные офферы """
    print_doc_result = {
        'documents': [
            {
                'id': '1',
                'doc_type': 'market_sku',
                'properties': {
                    'ware_md5': 'fake_wm5',
                }
            },
            {
                'id': '2',
                'doc_type': 'offer',
                'properties': {
                    'ware_md5': 'wm5',
                }
            }
        ]
    }
    with patch('market.idx.api.backend.report.get_print_doc', autospec=True, return_value=print_doc_result):
        docs = get_documents_from_report(None, None, None, None, None)
        assert len(docs) == 1
        assert docs[0]['properties']['ware_md5'] == 'wm5'


@pytest.mark.parametrize('env_type, balancer, url', REPORT_BALANCER_VALID_CASES)
def test_balancer(test_app, env_type, balancer, url):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, return_value=(1, 145)), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True, return_value=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app(env_type).test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&balancer={}'.format(balancer))
        assert resp.status_code == 200

        data = flask.json.loads(resp.data)
        report_prefix = url_to_report_prefix(url)
        report_params = {
            'offerid': 'wm5',
        }

        check_report_url(data, 'offer_info', 'offerinfo', report_params, report_prefix)
        check_report_url(data, 'prime', 'prime', report_params, report_prefix)
        check_report_url(data, 'print_doc', 'print_doc', report_params, report_prefix)


@pytest.mark.parametrize('env_type, balancer', REPORT_BALANCER_INVALID_CASES_TYPE1)
def test_invalid_balancer_type1(test_app, env_type, balancer):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, return_value=(1, 145)), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True, return_value=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app(env_type).test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&balancer={}'.format(balancer))
        assertion_message = 'Название балансера "{0}" не соответствует ENV_TYPE={1}, оно должно начинаться с {1} либо быть равным {1}'.format(
            balancer,
            'production' if env_type==PRODUCTION else 'testing',
        )
        check_assertion(assertion_message, resp)


@pytest.mark.parametrize('env_type, balancer', REPORT_BALANCER_INVALID_CASES_TYPE2)
def test_invalid_balancer_type2(test_app, env_type, balancer):
    with patch('market.idx.api.backend.blueprints.otrace.get_doc_properties_from_report', autospec=True, return_value={'feed_id': '1', 'offer_id': '2'}), \
            patch('market.idx.api.backend.blueprints.otrace.get_search_trace', autospec=True, return_value=''), \
            patch('market.idx.api.backend.blueprints.otrace.get_supplier_and_warehouse', autospec=True, return_value=(1, 145)), \
            patch('market.idx.api.backend.blueprints.otrace.is_available_in_print_doc', autospec=True, return_value=True), \
            patch('market.idx.api.backend.blueprints.otrace.get_feed_properties', autospec=True, side_effect=get_feed_properties_mock),\
            test_app(env_type).test_client() as client:
        resp = client.get('/v1/otrace?waremd5=wm5&balancer={}'.format(balancer))
        assertion_message = 'Балансер "{0}" отсутствует в конфиге'.format(balancer)
        check_assertion(assertion_message, resp)
