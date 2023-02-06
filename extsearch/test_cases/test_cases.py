#!/usr/bin/env python
# -*- coding: utf-8 -*-

import collections
import datetime
import distutils.dir_util
import json
import math
import os
import re
import sys
import time
import urllib
import urlparse

from lxml import etree
import pytz

import yatest.common

# extsearch/geo/kernel/pymod/qtree
import qtree

# extsearch/geo/kernel/pymod/query_attrs
import query_attrs

# extsearch/geo/kernel/pymod/query_embedding
import query_embedding

# extsearch/geo/kernel/pymod/qtree_info
import qtree_info

# extsearch/geo/kernel/pymod/reqbundle
import reqbundle

# extsearch/geo/kernel/pymod/blackbox
from blackbox.blackbox import HttpBlackbox, MetaBlackboxAdapter, BlackboxResponse, Http2StageBlackbox
from blackbox.testcase import TestCase
from extsearch.geo.kernel.pymod.runserver import Service

from kernel.feature_pool.proto.feature_pool_pb2 import TPoolInfo

from extsearch.geo.kernel.pbreport.intermediate.proto import metadatacollection_pb2
from sprav.protos.company_pb2 import Company
from yandex.maps.proto.common2 import metadata_pb2
from yandex.maps.proto.search import business_pb2, hours_pb2, precision_pb2, route_point_pb2, visual_hints_pb2
from yandex.maps.proto.entrance import entrance_pb2
from yandex.maps.proto.search import goods_metadata_pb2 as goods_pb2

# Encoding setup
reload(sys)
sys.setdefaultencoding('utf-8')

# Constants
SCHEMA_REPO_PATH = ''

RUBRIC_DELIMITER = '$'
RUBRIC_DICT = {
    'клиника': ['184106104'],
    'ресторан': ['184106384', '184106394'],
    'бар': ['184106384', '184106394'],
    'кафе': ['184106390'],
    'фастфуд': ['184106386'],
    'прачечная': ['184108251', '184108217'],
    'автошкола': ['184105264'],
    'аптека': ['184105932'],
    'медицинская помощь на дому': ['680383784'],
    'институты': ['184106140'],
    'гостиницы': ['184106414'],
    'книжный магазин': ['184105886'],
    'книжный': ['184105886'],
    'железнодорожный вокзал': ['184108155'],
}
RELEVANCE_FACTORS = ['Exact', 'HostRank', 'MatrixNetScore']
WIZARD_FILTERS = {
    'круглосуточно': ['open_24h:1'],
    'русская': ['type_cuisine:russian_cuisine'],
    'славянская': ['type_cuisine:slavic_cuisine'],
    'наша': ['type_cuisine:russian_cuisine', 'type_cuisine:slavic_cuisine'],
}
QUORUM_ATTRIBUTES = {
    'дети': 'for_kids:1',
    'учебники': 'educational_literature:1',
}
ADDRESS_PARTS = {'3 павловский переулок'}

# from kernel/reqerror/reqerror.h
YX_REQ_EMPTY = 2
YX_INCOMPATIBLE_REQ_PARAMS = 19
YX_UNKNOWN_ERROR = 21


def memoize(func):
    cache = {}

    def wrapper(*value):
        if value not in cache:
            cache[value] = func(*value)
        return cache[value]

    return wrapper


@memoize
def make_qtree(text, rubrics):
    command = ['-t', text]
    for ix, word in enumerate(text.split()):
        for f in WIZARD_FILTERS.get(word, []):
            command.extend(['-a', '{word_index}={filter}'.format(word_index=ix, filter=f)])
        for r in RUBRIC_DICT.get(word, []):
            command.extend(['-a', '{word_index}={rubric}'.format(word_index=ix, rubric='category_id:' + r)])
        if word in QUORUM_ATTRIBUTES:
            command.extend(
                ['-a', '{word_index}=filters:{filter}'.format(word_index=ix, filter=QUORUM_ATTRIBUTES[word])]
            )
    for r in rubrics:
        command.extend(['-m', 'category_id:' + r])
        for ix in range(len(text.split())):
            command.extend(['-a', '{word_index}={rubric}'.format(word_index=ix, rubric='category_id:' + r)])
    return qtree.create(*command)


@memoize
def make_attrs(rubrics):
    args = []
    for r in rubrics:
        args.append('category_id:' + r)
    return query_attrs.query_attrs(*args)


def parse_cgi_raw(query):
    result = {}
    for key_value in query.split('&'):
        if '=' not in key_value:
            result.setdefault(key_value, []).append('')
            continue
        k, v = key_value.split('=', 1)
        result.setdefault(k, []).append(v)
    return result


def combine_cgi_raw(cgi):
    result = []
    for k, v in cgi.items():
        for value in v:
            result.append(k + '=' + value)
    return '&'.join(result)


def remove_geo(text):
    for part in ADDRESS_PARTS:
        if part in text:
            return text.replace(part, '').strip()
    return text


@memoize
def local_wizard(query):
    cgi = parse_cgi_raw(query)
    text = urllib.unquote_plus(cgi.get('text')[0]) if 'text' in cgi else ''
    text_without_geo = remove_geo(text)
    rubrics = tuple()
    qtree = ''
    if text:
        rubrics = tuple(RUBRIC_DICT.get(text_without_geo, tuple()))
        qtree = make_qtree(text_without_geo, rubrics)
        cgi['qbundle'] = [reqbundle.reqbundle(qtree)]
        if 'pron' in cgi:
            cgi['pron'].append('qbundleiter')
        else:
            cgi['pron'] = ['qbundleiter']
        cgi['factors_qtree'] = [make_qtree(text, tuple())]
        if rubrics:
            cgi.setdefault('wiz_rubrics', []).append(RUBRIC_DELIMITER.join(rubrics))
            cgi['rubric_request'] = ['1']
    if 'relev' not in cgi:
        cgi['relev'] = ['']

    if text:
        attrs = ['category_id:' + r for r in rubrics]
        filters = [filter for raw_filter in cgi.get('filter', []) for filter in urllib.unquote(raw_filter).split('~')]
        wizdata = reqbundle.wizdata(qtree, text_without_geo, attrs=attrs, filters=filters, rubrics=list(rubrics))
        cgi['relev'][0] += ';query_embeddings=%s;query_embedding_short=%s;norm=%s;attrs=%s;qtree_info=%s;wizdata=%s' % (
            query_embedding.query_embedding(text),
            query_embedding.query_short_embedding(text),
            urllib.quote_plus(text),
            make_attrs(rubrics),
            qtree_info.qtree_info(qtree, text),
            wizdata,
        )

    return combine_cgi_raw(cgi)


def extract_rating(rating_score_factor):
    # ratings should be sorted by first decimal digit (but may not be sorted otherwise)
    # i.e rating list [9.1, 9.8, 5.5, 5.9, 1.3] is considered sorted by this function
    return int(10 * float(rating_score_factor) + 1.0e-7)


def check_discretized_distance(distance_list):
    def discretize(value, step):
        return math.ceil(float(value) / step) * step

    def get_discrete_distance(d):
        if d < 1000.0:
            return discretize(d, 100.0)
        elif d < 30000.0:
            return discretize(d, 1000.0)
        else:
            return discretize(d, 5000.0)

    distances = [get_discrete_distance(d) for d in distance_list]
    return sorted(distances) == distances


def _relev_has_ts(relev):
    for param in relev.split(';'):
        if param.startswith('ts='):
            return True
    return False


def _query_has_ts(query):
    for param in query.split('&'):
        if '=' not in param:
            continue
        k, v = param.split('=', 1)
        if k == 'relev':
            if _relev_has_ts(v):
                return True
    return False


def set_fixed_timestamp(query, ts=None):
    if _query_has_ts(query):
        return query

    if ts is None:
        ts = int(time.time())
    ts_was_set = False

    params = query.split('&')
    for i, param in enumerate(params):
        if '=' not in param:
            continue
        k, v = param.split('=', 1)
        if k == 'relev':
            params[i] = '{}={}'.format(k, '{};ts={}'.format(v, ts))
            ts_was_set = True
            break

    if not ts_was_set:
        # In case of no relev in original query
        params.append('relev=ts={}'.format(ts))

    return '&'.join(params)


rxSKIP = re.compile(r'\?>\s*<(error|companies)\b')


def has_filter(filters, name):
    for f in filters.ExtFilter:
        if f.Filter.id == name:
            return f.Filter
        if f.ExperimentalFilter.id == name:
            return f.ExperimentalFilter
    return None


def is_debug_enabled():
    return yatest.common.get_param('DEBUG_PORT') is not None


def parse_binary_metadata(metadata_collection):
    EXTENSIONS = {
        "business": business_pb2.GEO_OBJECT_METADATA,
        "entrance": entrance_pb2.ENTRANCE_METADATA,
        "route_point": route_point_pb2.ROUTE_POINT_METADATA,
        "goods": goods_pb2.GOODS_METADATA,
        "visual_hints": visual_hints_pb2.GEO_OBJECT_METADATA,
    }

    def parse_extensions(metadata):
        result = {}
        for item in metadata.Data:
            passage = metadata_pb2.Metadata()
            passage.ParseFromString(item)
            for ext, exttype in EXTENSIONS.iteritems():
                if passage.HasExtension(exttype):
                    result[ext] = passage.Extensions[exttype]
        return result

    return parse_extensions(metadata_collection)


def extract_metadata_collection(response):
    blob = response.get_report().Grouping[0].Group[0].Document[0].BinaryData.GeosearchDocMetadata
    return metadatacollection_pb2.TMetadataCollection.FromString(blob)


def extract_binary_metadata(response):
    return parse_binary_metadata(extract_metadata_collection(response))


def get_goods_ids(response, idx):
    goods = json.loads(response.get_group_property(idx, 'matchedobjects/1.x').get_value())
    return sorted(it["id"] for it in goods)


class BusinessMetaAdapter(MetaBlackboxAdapter):
    def max_span(self):
        lx, ly = map(float, self.get_property('response_lower_corner').get_value().split())
        ux, uy = map(float, self.get_property('response_upper_corner').get_value().split())
        return BlackboxResponse(max(ux - lx, uy - ly), 'method: max_span')


##
# TEST CASE
#
class BusinessTestcase(TestCase):
    @classmethod
    def BuildBusinessIndex(cls):
        pass

    @classmethod
    def setUpClass(cls):
        """Sets the testcase.

        Creates default blackbox.
        """
        # Note that for yatest.common functions we are enforced to use forward slash (/),
        # but for other path operations we must use generic os.path.join function.
        geobasesearch = yatest.common.binary_path('extsearch/geo/base/geobasesearch/geobasesearch')
        indexer_advert_data = yatest.common.binary_path(
            'extsearch/geo/indexer/advert_v2/yandex-geosearch-indexer-advert-v2'
        )
        indexer_advert_menu_tags = yatest.common.binary_path(
            'extsearch/geo/indexer/advert_menu_tags/yandex-geosearch-indexer-advert'
        )

        global SCHEMA_REPO_PATH
        SCHEMA_REPO_PATH = yatest.common.source_path('maps/doc/schemas')

        # launch business indexer
        cls.BuildBusinessIndex()
        # now we have ready index in ./indexer-business/index

        controls_dir = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-business/controls'
        )
        distutils.dir_util.copy_tree(controls_dir, "controls")

        # make empty directory ./indexer-advert/index
        indexadvertdir = os.path.join('indexer-advert', 'index')
        os.makedirs(indexadvertdir)

        # make empty directory ./indexer-features-fast/index
        indexfastdir = os.path.join('indexer-fast-features', 'index')
        os.makedirs(indexfastdir)

        # create advert index at ./indexer-advert/index/
        ads_xml = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-advert/source/ads.xml'
        )
        advert_dir = os.path.join('indexer-advert', 'index')
        yatest.common.execute([indexer_advert_data, '-d', advert_dir, ads_xml])
        yatest.common.execute([indexer_advert_menu_tags, '--output-dir', advert_dir, ads_xml])

        # get search config absolute path
        search_config = yatest.common.source_path('extsearch/geo/base/geobasesearch/tests/business/blackbox.cfg')

        # run geobasesearch!
        port = yatest.common.get_param('DEBUG_PORT', '0')
        argv = [geobasesearch, search_config, '-d', '-p', port]

        cls._service = Service(argv, debug=is_debug_enabled())
        try:
            sockaddr = cls._service.warm_up('', max_warmup_time=200)

            cls._url = sockaddr.get_url('/')
            cls._meta_box = HttpBlackbox(BusinessMetaAdapter, cls._url, 'origin=test&ms=proto')

            cls._meta_box_2stage = Http2StageBlackbox(BusinessMetaAdapter, cls._url, 'origin=test&ms=proto')

        except:
            cls._service.stop()
            raise

    @classmethod
    def tearDownClass(cls):
        cls._service.stop()

    # before each test
    def setUp(self):
        self.set_query('')

    def set_query(self, query):
        self._query = query

    def check_query_factornames(self, query, factor_names):
        checked = False
        report = self.get(query).get_report()
        for group in report.Grouping[0].Group:
            for document in group.Document:
                if document.HasField('ArchiveInfo'):
                    gtas = document.ArchiveInfo.GtaRelatedAttribute
                    self.assertTrue(len(gtas) <= len(factor_names))
                    for gta in gtas:
                        self.assertTrue(gta.Key in factor_names)
                    checked = True
        self.assertTrue(checked)

    def get_query(self, add):
        return local_wizard(self._query + '&binary_metadata=da&' + add)

    def get_unordered(self, query=''):
        query = self.get_query(query)
        query = set_fixed_timestamp(query)
        meta_response = self._meta_box.get(query)
        meta_2stage_response = self._meta_box_2stage.get(query)
        self.assertEqual(meta_response.found(), meta_2stage_response.found())
        return meta_2stage_response

    def get(self, query=''):
        query = self.get_query(query)
        query = set_fixed_timestamp(query)
        meta_response = self._meta_box.get(query)
        meta_2stage_response = self._meta_box_2stage.get(query)
        self.assertEqual(meta_response.found(), meta_2stage_response.found())
        self.assertEqual(meta_response.get_metadata_list(), meta_2stage_response.get_metadata_list())
        return meta_2stage_response

    def get_metadata_property_values(self, key):
        items = self.get().get_business_metadata().get_nested('properties', 'item')
        return [item.value for item in items if item.key == key]

    def get_meta_1st(self, query=''):
        meta_response = self._meta_box.get(self.get_query(query))
        return meta_response

    def should_find(self, query):
        self.assertGreater(self.get(query).found(), 0)

    def should_not_find(self, query):
        self.assertEqual(self.get(query).found(), 0)

    def check_first_doc(self, resp, id, msg=None):
        self.assertPositive(resp.found(), msg)
        self.assertEqual(id, resp.get_group_name(0), msg)

    def check_not_found(self, resp, id, msg=None):
        for i in range(resp.get_group_count()):
            self.assertNotEqual(id, resp.get_group_name(i), msg)

    def testDebug(self):
        # fake test
        if is_debug_enabled():
            while True:
                time.sleep(1)

    def testModeExact(self):  # mode=exact shouldn't fail ##
        coords = '37.582619,55.757779'
        self.set_query('ll=%(coords)s&spn=0.0001,0.0001&sort=distance' % locals())
        response = self.get()
        self.assertEqual(response.found(), 3)
        precisions = response.get_business_nested('geocode_result', 'house_precision')
        self.assertEqual(sorted(precisions), [precision_pb2.EXACT, precision_pb2.EXACT, precision_pb2.NUMBER])

        self.set_query('ll=%(coords)s' % locals())
        response = self.get()
        self.assertEqual(response.found(), 3)
        precisions = response.get_business_nested('geocode_result', 'house_precision')
        self.assertEqual(sorted(precisions), [precision_pb2.EXACT, precision_pb2.EXACT, precision_pb2.NUMBER])

        self.set_query('mode=exact&ll=%(coords)s&rll=%(coords)s' % locals())
        response = self.get()
        self.assertEqual(response.found(), 2)

        distances = response.get_business_nested('distance', 'value')
        self.assertEquals(len(distances), 2)
        self.assertTrue(all(d < 10 for d in distances))

        coords = '37.582619,51.757779'
        self.set_query('mode=exact&ll=%(coords)s&rll=%(coords)s' % locals())
        self.assertZero(self.get().found())

    def testModeExactWithPll(self):
        pll = "37.583284,55.757434~37.581674,55.757689~37.581889,55.758233~37.583520,55.757979"
        self.set_query('mode=exact&pll=%(pll)s' % locals())
        self.assertEqual(self.get().found(), 2)

        pll = "37.0,55.0~37.0,56.0~38.0,56.0~38.0,55.0"
        self.set_query('mode=exact&pll=%(pll)s' % locals())
        self.assertGreater(self.get().found(), 10)

        # mode=exact requests with both ll and pll are supported to maintain consistency
        coords = '37.582619,55.757779'
        self.set_query('mode=exact&pll=%(pll)s&ll=%(coords)s&sort=distance' % locals())
        self.assertEqual(self.get().found(), 2)

    def testBinaryPllFormat(self):
        # binary representation of
        # pll = "37.583284,55.757434~37.581674,55.757689~37.581889,55.758233~37.583520,55.757979"
        pll = "ZCg0I6ObrIxIGkxmuA74ZEg0I9KmWNRIG/gPACPsD"
        self.set_query('mode=exact&pll=%(pll)s' % locals())
        self.assertEqual(self.get().found(), 2)

    def testBinaryRllFormat(self):
        # binary representation of
        # rll = 37.575786,55.688722~37.496821,55.611845
        rll = "ZCgoI1PHqIxID6dEJEgoIpPiNNRIDmbEJ"
        self.set_query('text=кафе&ll=37.55,55.66&spn=0.1,0.1')
        self.assertEqual(self.get().get_property('sort_geometry').get_value(), '')
        self.assertEqual(self.get('rll=%(rll)s' % locals()).get_property('sort_geometry').get_value(), 'route')

    def testModeReversePoint(self):
        coords = '37.582619,55.757779'
        self.set_query('mode=reverse&ll=%(coords)s&spn=0.0001,0.0001' % locals())
        self.assertEqual(self.get().found(), 3)

        self.set_query('mode=reverse&ll=%(coords)s&spn=0.01,0.01' % locals())
        self.assertGreater(self.get().found(), 3)

        coords = '37.582619,51.757779'
        self.set_query('mode=reverse&ll=%(coords)s' % locals())
        self.assertZero(self.get().found())

    def testModeReversePll(self):
        pll = "37.583284,55.757434~37.581674,55.757689~37.581889,55.758233~37.583520,55.757979"
        self.set_query('mode=reverse&pll=%(pll)s' % locals())
        self.assertEqual(self.get().found(), 3)

        pll = "37.583284,55.757434~37.583284,55.757440~37.583290,55.757440~37.583290,55.757434"  # small pll
        self.set_query('mode=reverse&pll=%(pll)s' % locals())
        self.assertZero(self.get().found())

        pll = "37.0,55.0~37.0,56.0~38.0,56.0~38.0,55.0"  # huge pll
        self.set_query('mode=reverse&pll=%(pll)s' % locals())
        self.assertGreater(self.get().found(), 10)

    def testModeReverseRll(self):
        rll = '37.575786,55.688722~37.496821,55.611845'
        self.set_query('mode=reverse&rll=%(rll)s' % locals())
        response = self.get()
        self.assertEqual(response.found(), 3)
        distances = response.get_business_nested('distance', 'value')
        self.assertEquals(len(distances), 3)
        self.assertTrue(all(d < 100 for d in distances))

        rll = '31.575786,51.688722~31.496821,51.611845'
        self.set_query('mode=reverse&rll=%(rll)s' % locals())
        self.assertZero(self.get().found())

    def testCountryFilter(self):
        pll = "37.583284,55.757434~37.581674,55.757689~37.581889,55.758233~37.583520,55.757979"

        self.set_query('text=рестораны&relev=heavy_top_size=25&relev=filter_gwkinds=0.3&pll=%(pll)s' % locals())
        self.assertEqual(self.get().found(), 25)

        self.set_query(
            'text=рестораны&relev=heavy_top_size=25&relev=filter_gwkinds=0.3;gwkind=0.3&pll=%(pll)s' % locals()
        )
        self.assertEqual(self.get().found(), 0)

        self.set_query(
            'text=рестораны&relev=heavy_top_size=25&relev=filter_gwkinds=0.3,0.4;gwkind=0.3&pll=%(pll)s' % locals()
        )
        self.assertEqual(self.get().found(), 0)

        self.set_query(
            'text=рестораны&relev=heavy_top_size=25&relev=filter_gwkinds=0.5,0.4;gwkind=0.3&pll=%(pll)s' % locals()
        )
        self.assertEqual(self.get().found(), 25)

    def testFindAnything(self):  # all these requests should find something ##
        self.set_query('ll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1')
        self.assertPositive(self.get('text=шиномонтаж').found())
        self.assertPositive(self.get('text=&sort=distance').found())
        self.assertPositive(self.get('sort=distance').found())
        self.assertPositive(self.get('text=организация выставок').found())

    def testSpecialSymbols(self):
        self.set_query('ll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1')
        self.assertPositive(self.get('oid=1041298136').found())

    def testUrlSearch(self):
        self.set_query('ll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1')
        self.assertEqual(self.get('url=www.national.ru').found(), 2)

    def testPhoneSearch(self):
        self.set_query('ll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1')
        self.assertEqual(self.get('phone=74952587068').found(), 1)

    def testSearchWords(self):
        self.set_query('ll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1')
        self.assertPositive(self.get('text=пищевой институт').found())

    def testFindNothing(self):  # and these shouldn't
        self.set_query('ll=37.529771,55.606009&spn=1.5,1.5')
        self.assertEqual(self.get('text=勐海茶厂').found(), 0)

    def testSanity1(self):
        self.set_query('ll=37.529771,55.606009&spn=0.5,0.5&autoscale=1')
        self.assertLess(self.get('text=дельфинарий').found(), 10)
        self.assertEqual(self.get('text=зоопарк&maxspn=2,2').found(), 1)

        self.set_query('ll=37.594629,55.735662&spn=0.03037,0.01977&autoscale=1')
        self.assertGreater(self.get('text=шиномонтаж').found(), 3)

    def testMaxSpn(self):
        self.set_query('text=планетарий&ll=44.0,56.3&spn=5.2,2&autoscale=1')
        self.assertEqual(self.get().found(), 2)
        self.assertEqual(self.get('maxspn=5.2,2').found(), 1)

    def testWorldSearch(self):
        self.set_query('text=планетарий&ll=37.0,56.3&spn=5.2,2&autoscale=1')
        self.assertGreater(self.get().found(), 0)
        self.assertEqual(self.get('maxspn=5.2,2').found(), 0)

    def testSearchWithoutText(self):
        self.set_query('ll=37.68,55.84&sort=distance')

        self.assertTrue(self.get('spn=0.001,0.001').found())

        self.set_query('')
        self.assertEqual(self.get().get_error_code(), YX_REQ_EMPTY)

    def testSortDistance(self):  # sorting
        self.set_query('ll=37.68,55.84&spn=0.1,0.1&text=кафе&sort=distance&relev=strict_distance=1')

        response = self.get('results=8')
        self.assertGreater(response.found(), 3)
        distances = response.get_business_nested('distance', 'value')
        self.assertIsSorted(distances)
        self.assertEqual(self.get('results=1').get_business_nested('distance')[0].value, 1000)
        self.assertEqual(self.get('results=1&lang=ru_RU').get_business_nested('distance')[0].text, u'1\u00a0км')
        self.assertEqual(self.get('results=1&lang=en_US').get_business_nested('distance')[0].text, u'0.62\u00a0mi')

        self.set_query('ll=37.68,55.84&spn=0.1,0.1&text=кафе&sort=distance')
        response = self.get('results=8')
        distances = response.get_business_nested('distance', 'value')
        self.assertTrue(check_discretized_distance(distances))
        self.assertEqual(self.get('results=1').get_business_nested('distance')[0].value, 1000)

    def testSortDistanceNoText(self):  # sorting with no text
        self.set_query('rll=37.71,55.77&sort=distance&results=10&lang=ru-RU&oid=1041298136&oid=2119443614&oid=225')
        response = self.get()
        self.assertEqual(response.found(), 3)
        distances = response.get_business_nested('distance', 'value')
        self.assertIsSorted(distances)
        self.assertEqual(distances[0], 1600)
        self.assertEqual(response.get_business_nested('distance')[0].text, u'1,6\u00a0км')

        self.set_query('rll=37.71,55.77&sort=distance&results=10&lang=ru-RU&oid=2119443614&oid=1041298136&oid=225')
        response = self.get()
        self.assertEqual(response.found(), 3)
        distances = response.get_business_nested('distance', 'value')
        self.assertIsSorted(distances)
        self.assertEqual(distances[0], 1600)
        self.assertEqual(response.get_business_nested('distance')[0].text, u'1,6\u00a0км')

    def testUserDistance(self):  # sorting
        self.set_query('ll=37.67,55.85&spn=0.01,0.01&text=кафе&results=1')
        response = self.get()

        self.assertGreater(response.found(), 0)
        self.assertFalse(len(response.get_business_nested('distance')))
        self.assertTrue(len(self.get('ull=37.5,55.5').get_business_nested('distance')))
        self.assertEqual(self.get('ull=37.663,55.846').get_business_nested('distance')[0].value, 20)
        self.assertTrue(len(self.get('ull=59,30').get_business_nested('distance')))

    def testAttributions(self):  # attributions
        self.set_query('ll=39.2,51.66&spn=1.3,0.4&oid=779')
        self.assertTrue(self.get().get_property('attribution'))
        self.assertTrue(self.get().get_property('attribution-vrn'))
        # ensure that there is no <?xml version="1.0" encoding="utf-8"?> header and no extra formatting
        self.assertEqual(
            self.get().get_property('attribution-vrn'),
            '<Source id="vrn">'
            '<author xmlns="http://www.w3.org/2005/Atom">'
            '<name>Гид по Воронежу</name>'
            '<uri>http://www.allvrn.ru</uri>'
            '</author>'
            '</Source>',
        )
        self.assertTrue(self.get('gta=attribution').get_group_property(0, 'attribution'))

        self.set_query('')
        response = self.get('oid=778')
        self.assertTrue(response.get_property('attribution-facebook'))
        self.assertTrue(response.get_property('attribution-oktogo'))
        self.assertTrue(response.get_property('attribution-presscom'))
        self.assertFalse(response.get_property('attribution-linkedin'))

        response = self.get('oid=222')
        self.assertTrue(response.get_property('attribution-mgts'))

        response = self.get('oid=111')
        attribution = response.get_attribution('attribution-yandex').get_value()
        self.assertEqual(attribution.string('//author/name'), 'Yandex')
        self.assertEqual(attribution.string('//author/uri'), 'http://yandex.com')
        response = self.get('oid=111&lang=ru')
        attribution = response.get_attribution('attribution-yandex').get_value()
        self.assertEqual(attribution.string('//author/name'), 'Яндекс')
        self.assertEqual(attribution.string('//author/uri'), 'http://yandex.ru')

    def testReferencesSnippet(self):
        response = self.get('oid=1032821256')
        reference = response.get_metadata_list('references')[0].reference[0]
        self.assertEqual(reference.scope, 'afisha')
        self.assertEqual(reference.id, '5517985d1f7d154a12ddf28f')

    def testParamEnvelope(self):
        self.set_query('ll=37.529771,55.606009&spn=2,2&autoscale=0&text=гостиница космос')
        response = self.get()
        self.assertPositive(response.found())
        self.assertLess(response.max_span(), 0.01)

    def testAdverts(self):
        totalQuery = 'll=37.529771,55.606009&spn=2,2&text=клиника'
        paidQuery = 'll=37.529771,55.606009&spn=2,2&text=клиника&mode=advert'
        total = self.get(totalQuery)
        self.assertPositive(total.found())

        paid = self.get(paidQuery)
        self.assertPositive(paid.found())
        self.assertLess(paid.found(), total.found())

        paidQuery2 = 'll=37.529771,55.606009&spn=2,2&text=клиника&mode=advert&lang=ru_RU'
        paid2 = self.get(paidQuery2)
        self.assertPositive(paid2.found())

        paidQuery2 = 'll=37.529771,55.606009&spn=2,2&text=клиника&mode=advert&lang=de_GE'
        paid2 = self.get(paidQuery2)
        self.assertPositive(paid2.found())

        paidQuery2 = 'll=37.529771,55.606009&spn=2,2&text=клиника&mode=advert&lang=tr_TR'
        paid2 = self.get(paidQuery2)
        self.assertPositive(paid2.found())

        # queries with advert_page_id

        # two companies (from one chain) with different advert data
        paidQueryByPageId = 'll=37.529771,55.606009&spn=2,2&text=клиника&mode=advert&lang=ru_RU&advert_page_id=navi'
        paidByPageId = self.get(paidQueryByPageId)
        self.assertEqual(paidByPageId.found(), 1)

        # only one company (of those two) if spn is small enough
        paidQueryByPageId = 'll=37.63,55.73&spn=0.01,0.1&text=клиника&mode=advert&lang=ru_RU&advert_page_id=navi'
        paidByPageId = self.get(paidQueryByPageId)

        # no results with non-existing page id
        paidQueryByPageId = (
            'll=37.529771,55.606009&spn=2,2&text=клиника&mode=advert&lang=ru_RU&advert_page_id=nonexisting'
        )
        self.assertZero(self.get(paidQueryByPageId).found())

        # no restaurants for page id navi
        paidQueryByPageId = 'll=37.529771,55.606009&spn=2,2&text=ресторан&mode=advert&lang=ru_RU&advert_page_id=navi'
        self.assertZero(self.get(paidQueryByPageId).found())

        # menu advert queries

        paidQueryMenu = (
            'll=37.529771,55.606009&spn=2,2&mode=advert&lang=ru_RU&gta=menu_advert&advert_page_id=navi&advert_mode=menu'
        )
        paidMenu = self.get(paidQueryMenu)
        # both companies in the area have the same menu => only one result
        self.assertEqual(paidMenu.found(), 1)
        paidMenuJson = json.loads(paidMenu.get_group_property(0, 'menu_advert').get_value())
        self.assertEqual(paidMenuJson['style'], 'dental-menu')
        self.assertEqual(paidMenuJson['properties'][0]['key'], 'styleCategory')
        self.assertEqual(paidMenuJson['properties'][0]['value'], 'clinic-category')
        self.assertEqual(paidMenuJson['position'], 2)
        self.assertEqual(paidMenuJson['weight'], 17)
        self.assertEqual(paidMenuJson['search_text'], 'chainId:(321)')

        # dental clinic is not in the requested area
        paidQueryMenu = 'll=37.029771,55.006009&spn=0.8,0.8&mode=advert&lang=ru_RU&advert_page_id=navi&advert_mode=menu'
        self.assertZero(self.get(paidQueryMenu).found())

        # non-existing page id
        paidQueryMenu = (
            'll=37.529771,55.606009&spn=2,2&mode=advert&lang=ru_RU&advert_page_id=nonexisting&advert_mode=menu'
        )
        self.assertZero(self.get(paidQueryMenu).found())

        # menu_ad_id queries

        paidQueryByPageId = (
            'll=37.529771,55.606009&spn=2,2&mode=advert&lang=ru_RU&advert_page_id=navi&menu_ad_id=dental_clinic'
        )
        paidByPageId = self.get(paidQueryByPageId)
        self.assertEqual(paidByPageId.found(), 2)

        # only one company (of those two) if spn is small enough
        paidQueryByPageId = (
            'll=37.63,55.73&spn=0.01,0.1&mode=advert&lang=ru_RU&advert_page_id=navi&menu_ad_id=dental_clinic'
        )
        paidByPageId = self.get(paidQueryByPageId)
        self.assertEqual(paidByPageId.found(), 1)

        # advert_tag_id query

        paidQueryByPageId = (
            'll=37.529771,55.606009&spn=2,2&mode=advert&lang=ru_RU&advert_page_id=navi&advert_tag_id=dental_promo_tag'
        )
        paidByPageId = self.get(paidQueryByPageId)
        self.assertEqual(paidByPageId.found(), 1)

        # request with not existing page_id - still have to find organization but without advert
        paidQueryByPageId = 'll=37.529771,55.606009&spn=2,2&mode=advert&lang=ru_RU&advert_page_id=notexist&advert_tag_id=dental_promo_tag'
        paidByPageId = self.get(paidQueryByPageId)
        self.assertEqual(paidByPageId.found(), 1)

    def testAdvertSearchAlongRoute(self):
        self.set_query('rll=37.579,55.726~37.700,55.800&mode=advert&lang=ru_RU&advert_page_id=navi')
        paidRoute = self.get('relev=advert_thresh_to_rll=100500.0&text=клиника')
        self.assertEqual(paidRoute.found(), 1)
        self.assertEqual(paidRoute.get_title_list().get_value(), [u'Клиника Будь здоров'])

        paidRoute = self.get('relev=advert_thresh_to_rll=200.0&text=клиника')
        self.assertEqual(paidRoute.found(), 1)
        self.assertEqual(paidRoute.get_title_list().get_value(), [u'Клиника Будь здоров'])

        paidRoute = self.get('sort=distance&text=клиника')
        self.assertEqual(paidRoute.found(), 1)
        self.assertEqual(paidRoute.get_title_list().get_value()[0], u'Клиника Будь здоров')

        paidRoute = self.get('relev=advert_thresh_to_rll=200.0&text=клиника+3+павловский+переулок')
        self.assertEqual(paidRoute.found(), 1)
        self.assertEqual(paidRoute.get_title_list().get_value(), [u'Клиника Будь здоров'])

    def testAdvertsPrioritizing(self):
        self.set_query('ll=37.594992,55.753096&spn=0.001,0.001&text=книжный магазин')
        defaultModeResults = self.get()
        self.assertEqual(defaultModeResults.found(), 3)
        advertModeResults = self.get('mode=advert')
        self.assertEqual(advertModeResults.found(), 1)

    def testAdvertsPrioritizingNonRubricRequest(self):
        self.set_query('ll=37.529771,55.606009&spn=2,2&text=клиника+дети&mode=advert&lang=ru_RU')
        results = self.get()
        self.assertEqual(results.found(), 1)

        self.set_query('ll=37.594992,55.753096&spn=0.001,0.001&text=книжный учебники&mode=advert')
        results = self.get()
        self.assertEqual(results.found(), 1)

    def testChainRequest(self):
        self.set_query('ll=37.529771,55.606009&spn=2,2&chain_id=6003441')
        self.assertPositive(self.get().found())

        self.set_query('text=макдоналдс&ll=37.665532,55.749153&spn=0.821228,0.326866&z=11')
        meta = self.get()
        self.assertEqual(meta.get_property("rubric_request"), "0")

    def testRubricRequest(self):
        self.set_query('ll=37.529771,55.606009&spn=2,2&text=клиника&results=1')
        meta = self.get()
        self.assertEqual(meta.get_property("rubric_id"), "184106104")
        self.assertFalse(meta.get_group_property(0, "rubric_id"), "184106104")

    def testRubricSerpData(self):
        self.set_query('ll=37.529771,55.606009&spn=2,2&text=клиника&results=1')
        meta = self.get()
        self.assertFalse(meta.get_property("category_serp"))

        self.set_query('ll=36,54&spn=2,2&text=кафе&results=1')
        meta = self.get()
        self.assertEqual(meta.get_property("category_serp"), "link")

        self.set_query('ll=36,54&spn=2,2&text=фастфуд&results=1')
        meta = self.get()
        self.assertEqual(meta.get_property("category_serp"), "map")

        self.set_query('ll=37.529771,55.606009&spn=2,2&text=ресторан&results=1')  # 2 categories, map+link
        meta = self.get()
        self.assertEqual(meta.get_property("category_serp"), "map")

    def testRubricTags(self):
        self.set_query('ll=37.529771,55.606009&spn=2,2&text=кинотеатры&results=1')
        response = self.get()
        self.assertPositive(response.found())
        metadata = response.get_business_metadata()
        self.assertTrue(metadata.get_nested('category', 'tag'))

        tags = metadata.get_nested('category', 'tag')
        self.assertEqual(tags[1], u'icon:кино')
        self.assertEqual(tags[2], u'id:184105866')

    def testRubricLocalizedTags(self):
        self.set_query('ll=37.529771,55.606009&spn=2,2&text=кинотеатры&results=1')
        response = self.get()
        self.assertPositive(response.found())
        self.assertTrue(response.get_business_metadata().get_nested('category', 'tag'))

        tags = self.get('lang=ru_RU').get_business_metadata().get_nested('category', 'tag')
        self.assertEqual(tags[3], u'plural_name:Кинотеатры')
        self.assertEqual(tags[4], u'short_name:Кинотеатр')

        tags = self.get('lang=uk_UA').get_business_metadata().get_nested('category', 'tag')
        self.assertEqual(tags[3], u'plural_name:Кінотеатри')
        self.assertEqual(tags[4], u'short_name:Кінотеатр')

    def testChainsGrouping(self):
        def test(response, group_limit, has_group_cnt):
            chains = response.get_business_nested('chain', 'id')
            self.assertGreater(len(chains), 0)
            stat = collections.Counter(chains)

            lst = list(filter(lambda x: x[1] > group_limit, stat.items()))
            self.assertFalse(lst, 'Chain exceeding the limit: {}'.format(lst))

            lst = list(filter(lambda x: x[1] >= has_group_cnt, stat.items()))
            self.assertTrue(lst, 'Group greater {} not found: {}'.format(has_group_cnt, stat))

        self.set_query('text=фастфуд&results=1000')
        test(self.get('groupchains=100'), group_limit=100, has_group_cnt=10)
        test(self.get('groupchains=5'), group_limit=5, has_group_cnt=5)
        test(self.get('groupchains=1'), group_limit=1, has_group_cnt=1)

        self.set_query('text=прачечная&results=1000')
        test(self.get('groupchains=5'), group_limit=5, has_group_cnt=3)
        test(self.get('groupchains=2'), group_limit=2, has_group_cnt=2)
        test(self.get('groupchains=1'), group_limit=1, has_group_cnt=1)

    def testChainsGroupingForChainRequest(self):
        self.set_query('chain_id=6003441&results=20')
        self.assertTrue(self.get().found().get_value() > 3)
        self.assertTrue(self.get("groupchains=3").found().get_value() > 3)

        self.set_query('text=макдоналдс&results=20')
        self.assertTrue(self.get().found().get_value() > 3)
        self.assertTrue(self.get("groupchains=3").found().get_value() == 3)

    def testFilterLimiter(self):
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1,1&results=30')

        response = self.get().get_filters().get_value()

        self.assertTrue(has_filter(response, 'type_cuisine'))

        def get_cuisine_types(filters):
            values = []
            for item in has_filter(filters, 'type_cuisine').enum_filter.value:
                values.append(item.value.id)
            return values

        values = get_cuisine_types(response)
        for test in ['russian_cuisine', 'japanese_cuisine', 'home_cuisine', 'italian_cuisine']:
            self.assertIn(test, values)

        self.assertTrue(has_filter(response, 'open_24h'))
        self.assertTrue(has_filter(response, 'food_delivery'))
        self.assertFalse(has_filter(response, 'car_brand'))

        response = self.get('filter=food_delivery:1').get_filters().get_value()
        self.assertTrue(has_filter(response, 'food_delivery'))
        self.assertFalse(has_filter(response, 'car_brand'))

        self.assertTrue(has_filter(response, 'type_cuisine'))
        values = get_cuisine_types(response)
        self.assertIn('russian_cuisine', values)
        self.assertIn('japanese_cuisine', values)
        for item in has_filter(response, 'type_cuisine').enum_filter.value:
            if item.value.id == 'italian_cuisine':
                self.assertTrue(item.disabled)
                break

        response = self.get('filter=open_24h:1').get_filters().get_value()
        self.assertTrue(has_filter(response, 'open_24h'))
        self.assertTrue(has_filter(response, 'food_delivery').disabled)
        self.assertTrue(has_filter(response, 'type_cuisine').disabled)

    def testFilters(self):
        self.set_query(
            'text=ресторан&ll=37.665532,55.749153&spn=0.821228,0.326866&z=11&autoscale=0&relev=coarse_query_geoid=213'
        )

        response = self.get()
        filters = response.get_filters().get_value()

        self.assertEqual(filters.ExtFilter[0].Filter.id, 'open_24h')
        self.assertEqual(filters.ExtFilter[1].Filter.id, 'on_duty')

        self.assertEqual(filters.ExtFilter[9].Filter.id, 'open_now')

        self.assertEqual(filters.ExtFilter[4].Filter.id, 'type_cuisine')
        self.assertFalse(filters.ExtFilter[4].Filter.disabled)

        filters_set_attr = response.get_property('filters_set').get_value()
        self.assertTrue(filters_set_attr)
        filters_set = filters_set_attr.split(',')
        self.assertIn('food_delivery', filters_set)
        self.assertIn('type_cuisine', filters_set)

        self.assertEqual(filters.ExtFilter[2].Filter.id, 'food_delivery')
        self.assertEqual(filters.ExtFilter[2].Filter.name, u'доставка еды')
        self.assertFalse(filters.ExtFilter[2].Filter.disabled)

        self.assertEqual(filters.ExtFilter[3].Filter.id, 'hookah')
        self.assertEqual(filters.ExtFilter[3].Filter.name, u'кальян')

        self.assertEqual(filters.ExtFilter[5].Filter.id, '3514161697')

        self.assertEqual(filters.ExtFilter[6].Filter.id, '3514161698')
        self.assertTrue(filters.ExtFilter[6].Filter.disabled)

        response = self.get('filter=food_delivery:1')
        filters = response.get_filters().get_value()
        self.assertTrue(has_filter(filters, 'food_delivery').boolean_filter.value[0].value)
        response = self.get('filter=hookah:1').get_filters().get_value()
        self.assertTrue(has_filter(response, 'hookah').boolean_filter.value[0].value)
        self.assertTrue(has_filter(response, 'hookah').boolean_filter.value[0].selected)

        response = self.get('filter=open_24h:1')
        self.assertEqual(response.found(), 1)
        self.assertTrue(has_filter(response.get_filters().get_value(), 'open_24h'))

        # test filters
        self.assertEqual(self.get('filter=car_park:1').found(), 0)  # bool filter works
        self.assertEqual(self.get('filter=car_brand:mashinka').found(), 0)  # enum filter works
        self.assertEqual(self.get('filter=cost_delivery:invalid').found(), 0)  # text filter works
        self.assertEqual(self.get('filter=3514161736:1').found(), 0)  # non-restaurant filter still works
        self.assertEqual(self.get("filter=car_park:1").get_property("features"), "car_park:1")
        self.assertEqual(self.get("filter=car_park:1").get_property("user_filters"), "car_park:1")

    def testImportantFiltersSelection(self):
        self.set_query(
            'text=ресторан&ll=37.665532,55.749153&spn=0.821228,0.326866&z=11&autoscale=0&relev=coarse_query_geoid=117067'
        )
        response = self.get()
        filters_set_attr = response.get_property('filters_set').get_value()
        self.assertTrue(filters_set_attr)
        filters_set = filters_set_attr.split(',')
        self.assertNotIn('food_delivery', filters_set)
        self.assertIn('type_cuisine', filters_set)

    def testTimeFiltersOrder(self):
        self.set_query('text=автошкола&ll=32.042879,54.758423&spn=0.821228,0.326866&z=11')

        response = self.get().get_filters().get_value()

        self.assertEqual(response.ExtFilter[0].Filter.id, 'open_24h')
        self.assertEqual(response.ExtFilter[1].Filter.id, 'open_now')
        self.assertEqual(response.ExtFilter[2].Filter.id, '3514161694')

    def testFiltersFromMultipleCategories(self):
        self.set_query('text=прачечная&ll=37.665532,55.749153&spn=0.821228,0.826866')
        response = self.get().get_filters().get_value()

        self.assertTrue(has_filter(response, 'delivery'))  # filter present in both rubrics
        self.assertTrue(has_filter(response, 'home_visit'))  # filter present in both rubrics
        self.assertTrue(has_filter(response, 'self_service'))  # fliter present in second rubric only

    def testBooleanFilters(self):
        self.set_query('text=ресторан&ll=37.665532,55.749153&spn=0.821228,0.326866&z=11&autoscale=0')

        response = self.get('filter=hookah:1')
        self.assertEqual(response.found(), 1)
        self.assertTrue(has_filter(response.get_filters().get_value(), 'hookah').boolean_filter.value[0].value)

        response = self.get('filter=hookah:0')
        self.assertEqual(response.found(), 2)
        self.assertFalse(has_filter(response.get_filters().get_value(), 'hookah').boolean_filter.value[0].value)

        response = self.get('filter=hookah:0,1')
        self.assertEqual(response.found(), 3)
        self.assertFalse(has_filter(response.get_filters().get_value(), 'hookah').boolean_filter.value[0].value)
        self.assertTrue(has_filter(response.get_filters().get_value(), 'hookah').boolean_filter.value[1].value)

        response = self.get('filter=open_24h:1')
        self.assertEqual(response.found(), 1)
        self.assertTrue(has_filter(response.get_filters().get_value(), 'open_24h').boolean_filter.value[0].value)

        response = self.get('filter=open_24h:0')
        self.assertEqual(response.found(), 5)
        self.assertFalse(has_filter(response.get_filters().get_value(), 'open_24h').boolean_filter.value[0].value)

        response = self.get('filter=open_24h:1,0')
        self.assertEqual(response.found(), 6)
        self.assertFalse(has_filter(response.get_filters().get_value(), 'open_24h').boolean_filter.value[0].value)
        self.assertTrue(has_filter(response.get_filters().get_value(), 'open_24h').boolean_filter.value[1].value)

        response = self.get('filter=open_24h:0&filter=hookah:0')
        self.assertEqual(response.found(), 2)

        response = self.get('filter=open_24h:0~hookah:0')
        self.assertEqual(response.found(), 2)

    def testWizardFilters(self):
        self.set_query('text=ресторан круглосуточно&ll=37.6,55.7&spn=0.8,0.3&autoscale=0&wiz_rubrics=184106394')

        self.should_not_find('')
        self.should_not_find('wiz_filters=hookah$open_24h')  # obsolete wizard, shouldn't work without filter values
        self.should_find('wiz_filters=open_24h:1')

        # should not crash
        self.should_find('wiz_filters=open_24h:1')
        self.should_find('wiz_filters=open_24h:1$open_24h:1')
        self.should_find('wiz_filters=open_24h:1&filters=open_24h:1')
        self.should_find('wiz_filters=open_24h:1&filters=open_24h:1&filters=open_24h:1')

        self.assertEqual(self.get('wiz_filters=open_24h:1&filters=hookah:1').found(), 4)

        self.set_query('text=ресторан&ll=37.6,55.7&spn=0.8,0.3')
        self.assertTrue(has_filter(self.get().get_filters().get_value(), 'open_24h'))
        self.assertTrue(has_filter(self.get('wiz_filters=hookah:1').get_filters().get_value(), 'open_24h'))

        self.assertTrue(has_filter(self.get('wiz_filters=open_24h:1').get_filters().get_value(), 'open_24h'))
        self.assertFalse(has_filter(self.get('wiz_filters=open_24h:1').get_filters().get_value(), 'open_24h').disabled)

        self.set_query('text=ресторан круглосуточно&ll=37.665532,55.749153&spn=0.821228,0.326866&z=11&autoscale=0')
        self.assertEqual(self.get('wiz_filters=open_24h:1').found(), 3)

        self.set_query('text=ресторан русская&ll=37.4,55.7&spn=0.8,0.3')
        self.assertEqual(self.get('wiz_filters=type_cuisine:russian_cuisine').found(), 2)

        self.set_query('text=ресторан славянская&ll=37.4,55.7&spn=0.8,0.3')
        self.assertEqual(self.get('wiz_filters=type_cuisine:slavic_cuisine').found(), 1)

        self.set_query('text=ресторан наша&ll=37.4,55.7&spn=0.8,0.3')
        self.assertEqual(self.get('wiz_filters=type_cuisine:slavic_cuisine$type_cuisine:russian_cuisine').found(), 3)

        # Filtering logic is turned off in r3380634
        self.set_query('text=ресторан&ll=37.6,55.7&spn=0.8,0.3')
        self.assertEqual(
            self.get('').found(),
            self.get('wiz_filters=type_cuisine:slavic_cuisine$type_cuisine:russian_cuisine').found(),
        )
        self.assertEqual(
            self.get('wiz_filters=type_cuisine:slavic_cuisine').found(),
            self.get('wiz_filters=type_cuisine:slavic_cuisine$type_cuisine:russian_cuisine').found(),
        )
        self.assertEqual(self.get('').found(), self.get('wiz_filters=home_visit:1').found())
        self.set_query('text=прачечная&ll=37.6,55.7&spn=1,1')
        self.assertEqual(self.get('wiz_filters=home_visit:1').found(), self.get('').found())

    def testUniversalFilters(self):
        self.set_query('text=макдоналдс&ll=37.665532,55.749153&spn=0.821228,0.326866&z=11')
        response = self.get()
        filters = response.get_filters().get_value()
        self.assertTrue(has_filter(filters, 'open_24h'))
        self.assertTrue(has_filter(filters, 'has_photo'))
        self.assertFalse(has_filter(filters, 'chain_id'))

        self.assertGreater(response.found(), self.get('filter=has_photo:1').found())

    def testOpenAtFilter(self):
        def hr_time_to_utc(s):
            dt = datetime.datetime.strptime(s, "%Y-%m-%dT%H:%M")
            result = str(int((dt - datetime.datetime(1970, 1, 1)).total_seconds()))
            return result

        now = hr_time_to_utc('2020-04-13T06:30')
        self.set_query('oid=147&relev=ts={};tz=10800'.format(now))
        self.assertEqual(self.get('').found(), 1)

        cases = [
            # Positive test cases
            {'from': hr_time_to_utc('2020-04-13T06:30'), 'to': '', 'results': 1},
            {'from': '', 'to': hr_time_to_utc('2020-04-13T18:00'), 'results': 1},
            {'from': hr_time_to_utc('2020-04-13T06:30'), 'to': hr_time_to_utc('2020-04-13T18:00'), 'results': 1},
            # Negative test cases
            {'from': hr_time_to_utc('2020-04-13T06:00'), 'to': hr_time_to_utc('2020-04-13T07:00'), 'results': 0},
            {'from': hr_time_to_utc('2020-04-13T18:00'), 'to': '', 'results': 0},
            {'from': '', 'to': hr_time_to_utc('2020-04-13T18:30'), 'results': 0},
            {'from': hr_time_to_utc('2020-04-13T06:30'), 'to': hr_time_to_utc('2020-04-13T18:30'), 'results': 0},
        ]
        for case in cases:
            res = self.get('filter=open_at:{from}-{to}'.format(**case))
            self.assertEqual(res.found(), case['results'])

            filter = has_filter(res.get_filters().get_value(), 'open_at')
            self.assertTrue(filter)

            # Min value: Today
            self.assertEqual(str(int(filter.span_filter.min_value)), hr_time_to_utc('2020-04-13T00:00'))
            # Max value: Today + 7 days
            self.assertEqual(str(int(filter.span_filter.max_value)), hr_time_to_utc('2020-04-20T00:00'))

            # Expected step between values is 5 minutes
            self.assertEqual(int(filter.span_filter.step), 300)
            # Max diff between from and to is 1 day in 5 inutes intervals
            self.assertEqual(filter.span_filter.max_span, 288)

        # Filter values in different days
        now = hr_time_to_utc('2020-04-15T06:30')
        self.set_query('oid=1032821256&relev=ts={};tz=10800'.format(now))
        self.assertEqual(self.get('').found(), 1)

        cases = [
            {'from': hr_time_to_utc('2020-04-16T17:00'), 'to': hr_time_to_utc('2020-04-17T01:00'), 'results': 1},
            {'from': hr_time_to_utc('2020-04-19T17:00'), 'to': hr_time_to_utc('2020-04-20T00:00'), 'results': 1},
            {'from': hr_time_to_utc('2020-04-16T17:00'), 'to': hr_time_to_utc('2020-04-17T03:00'), 'results': 0},
            {'from': hr_time_to_utc('2020-04-19T17:00'), 'to': hr_time_to_utc('2020-04-20T03:00'), 'results': 0},
        ]
        for case in cases:
            res = self.get('filter=open_at:{from}-{to}'.format(**case))
            self.assertEqual(res.found(), case['results'])

        # Custom schedule cases
        self.set_query('oid=1121038211')
        self.assertEqual(self.get('').found(), 1)

        cases = [
            {  # 1. Monday - Custom schedule - break
                'from': hr_time_to_utc('2018-11-05T02:00'),
                'to': hr_time_to_utc('2018-11-05T03:00'),
                'ts': hr_time_to_utc('2018-11-05T06:30'),
                'results': 0,
            },
            {  # 2. Monday - Custom schedule - no break
                'from': hr_time_to_utc('2018-11-05T01:00'),
                'to': hr_time_to_utc('2018-11-05T02:00'),
                'ts': hr_time_to_utc('2018-11-05T06:30'),
                'results': 1,
            },
            {  # 3. Same as 1 but no custom schedule
                'from': hr_time_to_utc('2020-04-13T02:00'),
                'to': hr_time_to_utc('2020-04-13T03:00'),
                'ts': hr_time_to_utc('2020-04-13T06:30'),
                'results': 1,
            },
            {  # 4. Same as 2 but no custom schedule
                'from': hr_time_to_utc('2020-04-13T01:00'),
                'to': hr_time_to_utc('2020-04-13T02:00'),
                'ts': hr_time_to_utc('2020-04-13T06:30'),
                'results': 1,
            },
            {  # 5. Saturday - Custom schedule next week - break
                'from': hr_time_to_utc('2018-11-05T02:00'),
                'to': hr_time_to_utc('2018-11-05T03:00'),
                'ts': hr_time_to_utc('2018-11-03T06:30'),
                'results': 0,
            },
            {  # 6. Saturday - Custom schedule next week - no break
                'from': hr_time_to_utc('2018-11-05T01:00'),
                'to': hr_time_to_utc('2018-11-05T02:00'),
                'ts': hr_time_to_utc('2018-11-03T06:30'),
                'results': 1,
            },
        ]

        for case in cases:
            res = self.get('filter=open_at:{from}-{to}&relev=ts={ts};tz=10800'.format(**case))
            self.assertEqual(res.found(), case['results'])

    def testObjectsPriceFilter(self):
        oid = '1054821695'
        self.set_query('oid={oid}&relev=b_tags=tag_menu:borsch_tag$tag_menu:rolly_tag'.format(oid=oid))
        self.assertEqual(self.get('').found(), 1)
        response = self.get('').get_filters().get_value()
        self.assertTrue(has_filter(response, 'objects_price'))

        self.assertEqual(self.get('filter=objects_price:501-').found(), 0)
        self.assertEqual(self.get('filter=objects_price:500-').found(), 1)
        self.assertEqual(self.get('filter=objects_price:40-').found(), 1)
        self.assertEqual(self.get('filter=objects_price:-90').found(), 1)
        self.assertEqual(self.get('filter=objects_price:-40').found(), 0)
        self.assertEqual(self.get('filter=objects_price:90-110').found(), 1)
        self.assertEqual(self.get('filter=objects_price:90-200').found(), 1)
        self.assertEqual(self.get('filter=objects_price:70-80').found(), 0)

    def testHasBookingLinkFilter(self):
        # Booking link is present
        self.set_query('oid=5555500000')
        self.assertEqual(self.get('').found(), 1)
        self.assertEqual(self.get('filter=has_booking_link:1').found(), 1)

        # org has 3 booking links: with unknown aref, with aref != yclients and with custom href
        # all 3 links are not triggers for filter
        self.set_query('oid=1120353677')
        self.assertEqual(self.get('').found(), 1)
        self.assertEqual(self.get('filter=has_booking_link:1').found(), 0)

        # No booking link
        self.set_query('oid=1015347960')
        self.assertEqual(self.get('').found(), 1)
        self.assertEqual(self.get('filter=has_booking_link:1').found(), 0)

    def testTopListFilter(self):
        # is in top list
        self.set_query('oid=182')
        self.assertEqual(self.get('').found(), 1)
        self.assertEqual(self.get('filter=top_list_id:default').found(), 1)

        # not in top list
        self.set_query('oid=1015347960')
        self.assertEqual(self.get('').found(), 1)
        self.assertEqual(self.get('filter=top_list_id:default').found(), 0)

    def testTopListRanking(self):
        # top list ranking
        res = self.get('filter=top_list_id:default&relev=top_list_request:1')
        self.assertEqual(res.found(), 2)
        self.assertEqual(res.get_report().Grouping[0].Group[0].CategoryName, '1054821695')
        self.assertEqual(res.get_report().Grouping[0].Group[1].CategoryName, '182')

    def testPermalinks(self):
        self.set_query('ol=biz&autoscale=0')

        response = self.get_unordered('oid=225')
        self.assertEqual(response.found(), 1)
        response = self.get_unordered('oid=225&oid=2119443610')
        self.assertEqual(response.found(), 2)
        response = self.get_unordered('oid=225&oid=2119443610&oid=1041298136')
        self.assertEqual(response.found(), 3)
        response = self.get_unordered('oid=225&oid=225&oid=225')
        self.assertEqual(response.found(), 1)
        response = self.get_unordered('oid=111&oid=222')
        self.assertEqual(response.found(), 2)
        response = self.get_unordered('oid=111&oid=222&ll=37.62,55.76&spn=1,1')
        self.assertEqual(response.found(), 0)
        response = self.get_unordered('oid=111&oid=222&ll=40.5,64.5&spn=1,1')
        self.assertEqual(response.found(), 2)
        response = self.get_unordered('oid=111&url=lisichka.ru')
        self.assertEqual(response.found(), 0)
        response = self.get_unordered('oid=1041298136&url=lisichka.ru')
        self.assertEqual(response.found(), 1)

    def testAppleRubricRequest(self):
        self.set_query('text=водолей&ll=40.574506,64.534283&spn=0.188141,0.049366')
        self.assertEqual(self.get().found(), 2)
        self.assertEqual(self.get('acid=medicine').found(), 1)

    def testCategoryClass(self):
        self.set_query('oid=111')
        response = self.get()
        self.assertEqual(response.found(), 1)
        self.assertEqual(response.get_business_metadata().get_nested('category', 'class')[0], 'medicine')

    def testLinks(self):
        self.set_query('oid=778')
        links = self.get().get_business_metadata().get_nested('link')
        self.assertEqual(links[0].type, business_pb2.Link.Type.ATTRIBUTION)
        self.assertEqual(links[0].aref, '#oktogo')
        self.assertEqual(links[0].link.href, 'http://content.oktogo.ru/1st_Arbat_Hostel_h98273.aspx')

        # GEOSEARCH-3713
        self.set_query('oid=1119541190&lang=ru_RU&relev=shorten_address=0')
        links = self.get().get_business_metadata().get_nested('link')
        self.assertEqual(len(links), 4)
        self.assertEqual(links[1].type, business_pb2.Link.Type.ATTRIBUTION)
        self.assertEqual(links[1].aref, '#yandex')
        url = links[1].link.href
        self.assertEqual(
            urllib.unquote_plus(url.encode('ascii')).decode('utf-8'),
            u'https://yandex.ru/search/?faf=adr&text=Шоколадница, Москва, Зубовский бул., 17/1, стр. 1&uri=ymapsbm1://org?oid=1119541190',
        )

        links = self.get('print_mining_links=1').get_business_metadata().get_nested('link')
        self.assertEqual(len(links), 6)
        self.assertEqual(links[1].type, business_pb2.Link.Type.ATTRIBUTION)
        self.assertEqual(links[1].tag, 'attribution')
        self.assertEqual(links[1].aref, '#yandex')
        self.assertEqual(links[2].type, business_pb2.Link.Type.UNKNOWN)
        self.assertEqual(links[2].tag, 'mining')
        self.assertEqual(links[2].aref, '#4sq')
        self.assertEqual(links[2].link.href, 'https://ru.foursquare.com/v/шоколадница/4e86f725722e3a3ed8ee90b1')
        self.assertEqual(links[3].type, business_pb2.Link.Type.UNKNOWN)
        self.assertEqual(links[3].tag, 'mining')
        self.assertEqual(links[3].aref, '#4sq')
        self.assertEqual(links[3].link.href, 'https://en.foursquare.com/v/shokoladnitsa/4e86f725722e3a3ed8ee90b1')

    def testDenoise(self):
        self.set_query(
            'text=Читинский+институт+Байкальского+Государственного+Университета+Экономики+и+Права&ll=112.498624,51.045424&spn=1,1'
        )
        self.assertEqual(self.get().found(), 0)

    def testVariousAttributes(self):
        self.set_query('oid=147')
        response = self.get()
        metadata = response.get_business_metadata()
        attribution = response.get_attribution('attribution').get_value()
        self.assertEqual(metadata.get_nested('address', 'postal_code')[0], '194356')
        self.assertEqual(attribution.string('//Source/@id'), 'presscom')
        self.assertTrue(attribution.has('//Source/author/name'))
        self.assertEqual(attribution.string('//Source/author/uri'), 'http://www.allinform.ru')
        self.assertTrue(metadata.get_nested('open_hours', 'hours'))
        self.assertTrue(metadata.get_nested('open_hours', 'text'))

    def testWorkingTime(self):
        test_data = [
            (
                147,
                [
                    ('ru_RU', u'пн-пт 09:30–21:00; сб,вс 09:30–20:00'),
                    ('uk_UA', u'пн-пт 09:30–21:00; сб,нд 09:30–20:00'),
                    ('en_US', u'Mon-Fri 9:30 AM–9:00 PM; Sat,Sun 9:30 AM–8:00 PM'),
                    ('la_LA', u'Mon-Fri 09:30–21:00; Sat,Sun 09:30–20:00'),
                ],
            ),
            (
                222,
                [
                    (
                        'ru_RU',
                        u'пн-пт 09:00–20:00, перерыв 15:00–16:00; сб 09:00–20:00, перерывы 12:00–13:00, 15:00–16:00',
                    ),
                    (
                        'uk_UA',
                        u'пн-пт 09:00–20:00, перерва 15:00–16:00; сб 09:00–20:00, перерви 12:00–13:00, 15:00–16:00',
                    ),
                    (
                        'en_US',
                        u'Mon-Fri 9:00 AM–8:00 PM, break 3:00 PM–4:00 PM; Sat 9:00 AM–8:00 PM, breaks 12:00 PM–1:00 PM, 3:00 PM–4:00 PM',
                    ),
                    (
                        'la_LA',
                        u'Mon-Fri 09:00–20:00, break 15:00–16:00; Sat 09:00–20:00, breaks 12:00–13:00, 15:00–16:00',
                    ),
                ],
            ),
            (
                1007133620,
                [
                    ('ru_RU', u'ежедневно, 09:00–03:00'),
                    ('uk_UA', u'щодня, 09:00–03:00'),
                    ('en_US', u'daily, 9:00 AM–3:00 AM'),
                    ('la_LA', u'daily, 09:00–03:00'),
                ],
            ),
            (
                779,
                [
                    ('ru_RU', u'ежедневно, круглосуточно'),
                    ('uk_UA', u'щодня, цілодобово'),
                    ('en_US', u'daily, 24 hours'),
                    ('la_LA', u'daily, 24 hours'),
                ],
            ),
        ]

        for oid, data in test_data:
            self.set_query('oid={0}'.format(oid))
            for lang, expected in data:
                response = self.get('lang=' + lang)
                self.assertStringEqualUnified(response.get_business_metadata().open_hours.text, expected)

    def testScheduledTime(self):
        ts_and_tz_to_working_schedule = [
            # today is day of normal week
            (['20181101T092000+03', 10800], 'normal_working_time'),
            # today is monday of holidays week
            (['20181105T092000+03', 10800], 'holidays_working_time'),
            # today is sunday of holidays week
            (['20181111T092000+03', 10800], 'holidays_working_time'),
            # today is sunday of normal week for user, but monday of holidays week for org already
            (['20181104T220000+03', 3600], 'normal_working_time'),
            # today is sunday of normal week for org (20:00) and for user (22:00)
            (['20181104T200000+03', 18000], 'normal_working_time'),
            # today is sunday of normal week for org, but monday of holidays week for user already
            (['20181104T230000+03', 18000], 'holidays_working_time'),
            # today is sunday of holidays week for user, but monday of normal week for org already
            (['20181111T220000+03', 3600], 'holidays_working_time'),
            # today is sunday of holidays week for org (20:00) and for user (22:00)
            (['20181111T200000+03', 18000], 'holidays_working_time'),
            # today is sunday of holidays week for org, but monday of normal week for user already
            (['20181111T230000+03', 18000], 'normal_working_time'),
        ]

        oid_to_schedules = [
            (
                790,
                {
                    'normal_working_time': u'ежедневно, круглосуточно',
                    'holidays_working_time': u'пн круглосуточно, перерывы 05:00–06:00, 10:00–11:00; вт 00:00–10:00; чт,сб,вс круглосуточно',
                },
            ),
            (
                1121038211,
                {
                    'normal_working_time': u'пн,пт круглосуточно',
                    'holidays_working_time': u'пн круглосуточно, перерывы 05:00–06:00, 10:00–11:00; вт 00:00–10:00; вс круглосуточно',
                },
            ),
            (
                1067206831,
                {
                    'normal_working_time': None,
                    'holidays_working_time': u'пн круглосуточно, перерывы 05:00–06:00, 10:00–11:00; вт 00:00–10:00; вс круглосуточно',
                },
            ),
        ]

        for oid, schedules in oid_to_schedules:
            for ts_and_tz, schedule_type in ts_and_tz_to_working_schedule:
                self.set_query('oid={}&relev=ts={}&relev=tz={}'.format(oid, ts_and_tz[0], ts_and_tz[1]))
                got = self.get('lang=ru_RU').get_business_metadata().open_hours.text
                expected = schedules[schedule_type]
                if not expected:
                    self.assertFalse(got)
                else:
                    self.assertStringEqualUnified(got, expected)

        self.set_query('oid=790&relev=ts=20181105T092000+03&relev=tz=10800&apply_scheduled=0')
        got = self.get('lang=ru_RU').get_business_metadata().open_hours.text
        self.assertEqual(got, u'ежедневно, круглосуточно')

    def testScheduledHoursGta(self):
        got = json.loads(self.get('oid=790&gta=scheduled_hours').get_group_property(0, 'scheduled_hours').get_value())
        expected = {
            "20181105": {"from": 660, "to": 1440},
            "20181106": {"from": 1440, "to": 2040},
            "20181107": [],
            "20181109": [],
            "20181111": {"from": 8640, "to": 10080},
            "20181231": [],
            "time_zone": 180,
        }
        self.assertEquals(got, expected)

        got = json.loads(self.get('oid=147&gta=scheduled_hours').get_group_property(0, 'scheduled_hours').get_value())
        expected = dict()
        self.assertEquals(got, expected)

    def testWorkingStatus(self):
        self.set_query('oid=147&relev=ts=20180215T092000+03&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertFalse(response.open_hours.state.is_open_now)
        self.assertStringEqualUnified(response.open_hours.state.text, u'До открытия 10 мин')
        self.assertStringEqualUnified(response.open_hours.state.short_text, u'С 09:30')
        self.assertEqual(list(response.open_hours.state.tag), ['opening_soon'])

        self.set_query('oid=147&relev=ts=20180215T100000+03&lang=en_US')
        response = self.get().get_business_metadata()
        self.assertTrue(response.open_hours.state.is_open_now)
        self.assertStringEqualUnified(response.open_hours.state.text, u'Open until 9:00 PM')
        self.assertStringEqualUnified(response.open_hours.state.short_text, u'Until 9:00 PM')
        self.assertEqual(list(response.open_hours.state.tag), [])

        self.set_query('oid=1034177171&relev=ts=20180215T092000+03&lang=ru_RU&show_closed=1')
        response = self.get().get_business_metadata()
        self.assertEqual(
            response.open_hours.text, u'пн-пт 07:00–00:00; сб,вс 09:00–22:00'
        )  # even though the company is unreliable
        self.assertFalse(response.open_hours.state.is_open_now)
        self.assertEqual(response.open_hours.state.text, u'Возможно, не работает')
        self.assertEqual(response.open_hours.state.short_text, u'Закрыто')
        self.assertEqual(list(response.open_hours.state.tag), [])

        # Today is monday, org opens on next monday (no holidays here, just strange schedule)
        self.set_query('oid=1132400859&relev=ts=20200706T190000+03&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertFalse(response.open_hours.state.is_open_now)
        self.assertEqual(response.open_hours.state.text, u'Закрыто до 13 июля')
        self.assertEqual(response.open_hours.state.short_text, u'Закрыто до 13 июл.')

        # Today is sunday of normal week, tomorrow org will be closed by holiday
        self.set_query('oid=1045755033&relev=ts=20200705T190000+03&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertFalse(response.open_hours.state.is_open_now)
        self.assertEqual(response.open_hours.state.text, u'Закрыто до вторника')
        self.assertEqual(response.open_hours.state.short_text, u'Закрыто до вт.')

        # Today is sunday of holiday week - org was closed on monday, tomorrow org will open by normal schedule
        self.set_query('oid=1045755033&relev=ts=20200712T190000+03&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertFalse(response.open_hours.state.is_open_now)
        self.assertEqual(response.open_hours.state.text, u'Закрыто до завтра')
        self.assertEqual(response.open_hours.state.short_text, u'Закрыто до завтра')

        # Today is sunday of normal week, org will be closed whole next week by holidays
        self.set_query('oid=1045755033&relev=ts=20200719T190000+03&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertFalse(response.open_hours.state.is_open_now)
        self.assertEqual(response.open_hours.state.text, u'Закрыто до 27 июля')
        self.assertEqual(response.open_hours.state.short_text, u'Закрыто до 27 июл.')

        # Today is sunday 23:30, org will closed on monday 1:00
        self.set_query('oid=27971862924&relev=ts=20210321T233000+03&lang=ru_RU&show_online_orgs=both')
        response = self.get().get_business_metadata()
        self.assertTrue(response.open_hours.state.is_open_now)
        self.assertEqual(response.open_hours.state.text, u'Открыто до 01:00')
        self.assertEqual(response.open_hours.state.short_text, u'До 01:00')

    def testGTAAttributes(self):
        self.set_query('oid=1054821695')
        self.assertEqual(self.get('gta=geoid').get_group_property(0, 'geoid'), '120540')
        self.assertEqual(self.get('gta=best_locale').get_group_property(0, 'best_locale'), 'ru')

        self.assertEqual(self.get('gta=has_visual_rubric').get_group_property(0, 'has_visual_rubric'), '1')

        self.assertEqual(
            self.get('gta=similar_orgs').get_group_property(0, 'similar_orgs'), '1120353677,1120046349,1764093792'
        )
        self.assertEqual(
            self.get('gta=similar_orgs&relev=experiment=exp1').get_group_property(0, 'similar_orgs'),
            '1120353677,1764093792',
        )
        self.assertEqual(
            self.get('gta=similar_orgs&relev=experiment=dummy_exp').get_group_property(0, 'similar_orgs'),
            '1120353677,1120046349,1764093792',
        )

        self.assertEqual(
            self.get('gta=similar_orgs&advert_page_id=maps').get_group_property(0, 'similar_orgs'),
            '1120353677,1120046349,1120046349,1114039551,1764093792',
        )
        self.assertEqual(
            self.get('gta=similar_orgs&advert_page_id=maps&relev=experiment=exp1').get_group_property(
                0, 'similar_orgs'
            ),
            '1120353677,1764093792',
        )

        self.assertEqual(self.get('gta=platinum').get_group_property(0, 'platinum'), '0')

        self.assertEqual(self.get('gta=shard_id').get_group_property(0, 'shard_id'), '2')

        self.set_query('oid=1041298136')
        self.assertEqual(self.get('gta=rubric_id_lvl2').get_group_property(0, 'rubric_id_lvl2'), '184108205')
        self.assertEqual(self.get('gta=rubric_id_lvl3').get_group_property(0, 'rubric_id_lvl3'), '184108203')

        self.set_query('oid=1054821695')
        self.assertFloatEqual(self.get().get_factor(0, 'RawMachineRating'), 9.1)
        self.assertFloatEqual(self.get().get_factor(0, 'DocPhotoCount'), 0.13)

        self.set_query('oid=1032821256')
        self.assertEqual(self.get('gta=platinum').get_group_property(0, 'platinum'), '1')

        self.set_query('oid=1046632891')
        self.assertEqual(
            self.get('gta=emails').get_group_property(0, 'emails'), 'arenda@orel-tsum.ru,info@orel-tsum.ru'
        )

    def testShortAddress(self):
        self.set_query('oid=1054821695&lang=ru_RU')
        self.assertEqual(
            self.get('gta=description&relev=shorten_address=0').get_group_property(0, 'description'),
            'Москва, ул. Моховая, 15/1, стр. 1',
        )
        self.assertEqual(
            self.get('gta=description').get_group_property(0, 'description'), 'Моховая ул., 15, Москва г., Россия'
        )

        self.set_query('oid=141037609806&relev=user_country=225&lang=ru_RU')
        self.assertEqual(
            self.get('gta=description').get_group_property(0, 'description'),
            u'Россия, Москва, поселение Вороновское, квартал № 16, 1, стр. 2',
        )

        self.set_query('oid=151037609806&relev=user_country=225&lang=ru_RU')
        self.assertEqual(
            self.get('gta=description').get_group_property(0, 'description'),
            u'квартал № 16, 1, стр. 2, пос. Вороновское',
        )

        self.set_query('oid=1053527740&lang=ru_RU&hide_address_components_for_ldnr=0')
        self.assertEqual(
            self.get('gta=description').get_group_property(0, 'description'), u'ул. Артема, 80Е, Донецк, Украина'
        )

        self.set_query('oid=1053527740&lang=ru_RU')
        self.assertEqual(self.get('gta=description').get_group_property(0, 'description'), u'ул. Артема, 80Е, Донецк')

    def testFilterUrl(self):
        self.set_query('text=лисичка&geo_id=213')
        self.assertEqual(self.get().found(), 3)
        self.assertEqual(self.get('url=lisichka.ru').found(), 1)

    def testSynonyms(self):
        self.set_query('oid=111')
        self.assertEqual(self.get().get_title_list().get_value()[0], u'Водолей + ООО')

    def testFeatureLangValue(self):
        self.set_query('oid=222')
        self.assertEqual(
            self.get('lang=ru_RU').get_business_metadata().get_nested('feature', 'value', 'enum_value', 'name')[0],
            u'автомобиль',
        )
        self.assertEqual(
            self.get('lang=uk_UA').get_business_metadata().get_nested('feature', 'value', 'enum_value', 'name')[0],
            u'автомобiль',
        )
        self.assertEqual(
            self.get('lang=en_US').get_business_metadata().get_nested('feature', 'value', 'enum_value', 'name')[0],
            u'автомобиль',
        )

    def testSpecialCharacters(self):
        self.set_query('oid=777')
        response = self.get().get_business_metadata()
        self.assertEqual(response.feature[0].name, '''Special characters: <&''"">''')
        self.assertEqual(response.feature[0].value.enum_value[0].name, '<')

    def testOwners(self):
        response = self.get('owner_id=12345000')
        self.assertEqual(response.found(), 3)
        ids = response.get_ids()
        self.assertEqual(sorted(ids), ['1041298136', '148', '149'])

        response = self.get('owner_id=12345001')
        self.assertEqual(response.found(), 1)
        self.assertEqual(response.get_ids()[0], '1041298136')

        response = self.get('direct_owner_id=12345001')
        self.assertEqual(response.found(), 1)
        self.assertEqual(response.get_ids()[0], '1041298136')

    def testOnDutyFilter(self):
        self.set_query('text=ресторан&ll=37.657673,55.837191&spn=0.1,0.1&autoscale=0')
        self.assertGreater(self.get().found(), 0)

        # 8:00 and 9:00 in Moscow
        self.assertEqual(self.get('filter=on_duty:1&relev=ts=20130406T08:00+03').found(), 1)
        self.assertEqual(self.get('filter=on_duty:1&relev=ts=20130406T09:00+03').found(), 0)

        self.assertEqual(self.get('filter=on_duty:1&relev=ts=1365224400').found(), 1)
        self.assertEqual(self.get('filter=on_duty:1&relev=ts=1365228000').found(), 0)

    def testFeatureName(self):
        response = self.get('oid=147')
        self.assertEqual(response.found(), 1)
        self.assertFalse(response.get_business_metadata().get_nested('feature', 'name'))

        request_tmpl = 'oid=147&vrb={0}'
        for v in ['0', 'no', 'false']:
            response = self.get(request_tmpl.format(v))
            self.assertEqual(response.found(), 1)
            self.assertFalse(response.get_business_metadata().get_nested('feature', 'name'))

        for v in ['1', 'yes', 'true']:
            response = self.get(request_tmpl.format(v))
            self.assertEqual(response.found(), 1)
            self.assertTrue(response.get_business_metadata().get_nested('feature', 'name'))

    def testFeaturesOrder(self):
        response = self.get('oid=1054821695').get_business_metadata()
        self.assertEqual(
            response.get_nested('feature', 'id'),
            [
                'karaoke',
                'food_delivery',
                'hookah',
                'around_the_clock_work',
                'type_public_catering',
                'type_cuisine',
                'average_bill',
            ],
        )

    def testFeatureGroups(self):
        self.set_query('oid=1219528050')
        self.assertEqual(len(self.get().get_business_metadata().get_nested('feature_group')), 2)
        self.assertEqual(self.get().get_business_metadata().get_nested('feature_group')[0].name, u'Food')
        self.assertEqual(len(self.get().get_business_metadata().get_nested('feature_group')[0].feature_id), 2)
        self.assertEqual(self.get('lang=ru').get_business_metadata().get_nested('feature_group')[0].name, u'Еда')
        self.assertEqual(
            self.get('lang=ru').get_business_metadata().get_nested('feature_group')[1].name, u'Доступность'
        )
        self.set_query('oid=1045755033')
        self.assertEqual(len(self.get().get_business_metadata().get_nested('feature_group')), 1)
        self.assertEqual(self.get().get_business_metadata().get_nested('feature_group')[0].name, u'Food')
        self.set_query('oid=1015347960')
        self.assertEqual(len(self.get().get_business_metadata().get_nested('feature_group')), 2)
        self.assertEqual(self.get().get_business_metadata().get_nested('feature_group')[0].name, u'Accessibility')
        self.assertEqual(self.get().get_business_metadata().get_nested('feature_group')[1].name, u'Food')

    def testLocalization(self):
        self.set_query('oid=1035492987')

        response = self.get('lang=ru_RU').get_business_metadata()
        self.assertEqual(response.name, u'Клиника Будь здоров')
        self.assertEqual(response.get_nested('category')[0].name, u'Клиники')

        response = self.get('lang=uk_UA').get_business_metadata()
        self.assertEqual(response.name, u'Клиника Будь здоров')
        self.assertEqual(response.get_nested('category')[0].name, u'Клініки')

        response = self.get('lang=en_US').get_business_metadata()
        self.assertEqual(response.name, 'Bud\' Zdorov Clinic')
        self.assertEqual(response.get_nested('category')[0].name, 'Clinics')

        response = self.get('lang=tr_TR').get_business_metadata()
        self.assertEqual(response.name, 'Bud\' Zdorov Clinic')
        self.assertEqual(response.get_nested('category')[0].name, 'Clinics')

        self.set_query('oid=123456')
        response = self.get('lang=ru_RU').get_business_metadata()
        self.assertEqual(response.name, u'Русский')
        self.assertEqual(response.address.formatted_address, u'Русский')
        response = self.get('lang=uk_UA').get_business_metadata()
        self.assertEqual(response.name, u'Русский')
        self.assertEqual(response.address.formatted_address, u'Русский')
        response = self.get('lang=en_US').get_business_metadata()
        self.assertEqual(response.name, 'English')
        self.assertEqual(response.address.formatted_address, 'English')
        response = self.get('lang=tr_TR').get_business_metadata()
        self.assertEqual(response.name, 'English')
        self.assertEqual(response.address.formatted_address, 'English')

    def testDistanceLocalization(self):
        self.set_query('oid=152&ull=37.61,55.61')
        for cgi, expected in [
            ('lang=ru', u'км'),
            ('lang=ru&i18n_prefs=metric', u'км'),
            ('lang=ru&i18n_prefs=imperial', u'мил'),
            ('lang=en_US', u'mi'),
            ('lang=en_US&i18n_prefs=metric', u'km'),
            ('lang=en_US&i18n_prefs=imperial', u'mi'),
            ('lang=uk', u'км'),
            ('lang=uk&i18n_prefs=metric', u'км'),
            ('lang=uk&i18n_prefs=imperial', u'мил'),
            ('lang=tr', u'km'),
            ('lang=tr&i18n_prefs=metric', u'km'),
            ('lang=tr&i18n_prefs=imperial', u'mi'),
        ]:
            self.assertSubstring(expected, self.get(cgi).get_business_metadata().distance.text)

    def testWorkingHoursLocalization(self):
        self.set_query('oid=152&results=1')
        for cgi, expected in [
            ('lang=ru', '21:00'),
            ('lang=ru&i18n_prefs=12h', '9:00 AM'),
            ('lang=ru&i18n_prefs=24h', '21:00'),
            ('lang=en', '9:00 PM'),
            ('lang=en&i18n_prefs=12h', '9:00 PM'),
            ('lang=en&i18n_prefs=24h', '21:00'),
            ('lang=uk', '21:00'),
            ('lang=uk&i18n_prefs=12h', '9:00 дп'),
            ('lang=uk&i18n_prefs=24h', '21:00'),
            ('lang=tr', '21:00'),
            ('lang=tr&i18n_prefs=12h', 'ÖÖ 9:00'),
            ('lang=tr&i18n_prefs=24h', '21:00'),
        ]:
            self.assertSubstringUnified(expected, self.get(cgi).get_business_metadata().open_hours.text)

    def testPrintAllFactors(self):
        self.set_query('oid=1035492987&ll=37.6,55.7&spn=1,1')
        gta_string = 'gta=' + '&gta='.join(RELEVANCE_FACTORS)

        factors = self.get(gta_string).get_all_properties_list().get_value()
        all_factors = self.get('gta=_RankingFactors').get_all_properties_list().get_value()
        all_factors_only = self.get('gta=_RankingFactorsOnly').get_all_properties_list().get_value()

        for factors_for_doc, all_factors_for_doc, all_factors_only_for_doc in zip(
            factors, all_factors, all_factors_only
        ):
            for k, v in factors_for_doc.items():
                self.assertTrue(k in all_factors_for_doc)
                self.assertEqual(v, all_factors_for_doc[k])
                self.assertTrue(k in all_factors_only_for_doc)
                self.assertEqual(v, all_factors_only_for_doc[k])

    def testClosedFactors(self):
        # permanently closed
        self.set_query('oid=87192713965&show_closed=1')
        self.assertEqual(self.get().get_factor(0, 'PersonalDocClosedPermanently'), 1.0)
        self.assertEqual(self.get("set_closed_factor_for_moved=1").get_factor(0, 'PersonalDocClosedPermanently'), 1.0)

        # opened
        self.set_query('oid=222&show_closed=1')
        self.assertEqual(self.get().get_factor(0, 'PersonalDocClosedPermanently'), 0.0)
        self.assertEqual(self.get("set_closed_factor_for_moved=1").get_factor(0, 'PersonalDocClosedPermanently'), 0.0)

        # moved
        self.set_query('oid=1035492987&show_closed=1')
        self.assertEqual(self.get().get_factor(0, 'PersonalDocClosedPermanently'), 0.0)
        self.assertEqual(self.get("set_closed_factor_for_moved=1").get_factor(0, 'PersonalDocClosedPermanently'), 1.0)

    def testFeatureAref(self):
        self.assertEqual(self.get('oid=222').get_business_metadata().get_nested('feature')[0].aref, '#mgts')
        self.assertTrue(self.get('oid=777').get_business_metadata().get_nested('feature'))
        self.assertFalse(self.get('oid=777').get_business_metadata().get_nested('feature', 'aref'))

    def testSearchAlongRoute(self):
        self.set_query('text=кафе&ll=37.55,55.66&spn=0.1,0.1')
        route = 'rll=37.575786,55.688722~37.496821,55.611845'

        self.assertGreater(self.get().found(), 2)
        self.assertGreater(self.get(route).found(), 2)

        self.assertEqual(self.get().get_property('sort_geometry').get_value(), '')
        self.assertEqual(self.get('sort=distance').get_property('sort_geometry').get_value(), 'point')
        self.assertEqual(self.get('rll=37.55,55.66').get_property('sort_geometry').get_value(), 'point')
        self.assertEqual(self.get(route).get_property('sort_geometry').get_value(), 'route')

    def testSimilarOrgsSnippet(self):
        self.set_query('similar_orgs_request=1&fsgta=tags')

        tags = etree.fromstring(self.get('oid=1035492987').get_group_property(0, 'tags').get_value())
        advert_tag_present, icon_present = False, False
        for element in tags.findall('./Tags/tag'):
            if element.text == 'Advert':
                advert_tag_present = True
            elif element.text.startswith('icon:'):
                self.assertEqual(element.text, 'icon:medicine')
                icon_present = True
        self.assertTrue(advert_tag_present and icon_present)

        self.assertEqual(self.get('oid=1041298136').get_group_property(0, 'tags').get_value(), None)

        self.set_query('similar_orgs_request=1&fsgta=seoname')
        self.assertEqual(self.get('oid=1018831613').get_group_property(0, 'seoname'), 'medsi')

    def testClosedSimilarOrgs(self):
        self.set_query('similar_orgs_request=1&oid=234198147115')
        response = self.get('show_closed=1')
        self.assertEqual(response.found(), 1)

        self.set_query('similar_orgs_request=1&show_closed=0')
        response = self.get('oid=234198147115').get_report()
        self.assertEqual(response.ErrorInfo.Text, 'Cannot find these words')

    def testSnippet(self):
        self.set_query('oid=1041298136')
        self.assertTrue(self.get().get_business_metadata().get_nested('snippet'))

    def testAddressLine(self):
        self.assertEqual(
            self.get('oid=111').get_business_metadata().address.formatted_address, u'Архангельск, ул. Тимме, 2, корп.1'
        )
        self.assertEqual(
            self.get('oid=222').get_business_metadata().address.formatted_address,
            u'Архангельская область, Архангельск, Тимме улица, 2',
        )

        self.assertEqual(
            self.get('oid=1032821256').get_business_metadata().address.formatted_address,
            u'Москва, Семёновская пл., 1, ТРЦ Семеновский, эт. 2',
        )
        self.assertEqual(
            self.get('oid=1032821256').get_business_metadata().address.additional_info, u'ТРЦ Семеновский, эт. 2'
        )

    def testRubricRanking(self):
        # Non-rubric query

        self.set_query('text=cafe&ll=37.5,55.7&spn=1.0,1.0&gta=ShowedMachineRating')
        # self.assertIsNotSorted(self.get().get_property_list('ShowedMachineRating'), key=extract_rating, reverse=True)

        # sort=rank
        self.assertIsSorted(
            self.get('sort=rank').get_property_list('ShowedMachineRating'), key=extract_rating, reverse=True
        )

        # Rubric query with sort=rank
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1.0,1.0&gta=ShowedMachineRating&sort=rank')
        meta = self.get()
        self.assertIsSorted(meta.get_property_list('ShowedMachineRating'), key=extract_rating, reverse=True)
        self.assertEqual(meta.get_property("sort"), "rank")
        self.assertEqual(meta.get_property("sort_origin"), "")

        # Rubric query without sort=rank
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1.0,1.0&gta=ShowedMachineRating')
        meta = self.get()
        self.assertEqual(meta.get_property("sort"), "")
        self.assertEqual(meta.get_property("sort_origin"), "")

        # Rubric query with route point
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1.0,1.0&rll=37.5,55.7&gta=ShowedMachineRating')
        meta = self.get()
        self.assertEqual(meta.get_property("sort"), "")
        self.assertEqual(meta.get_property("sort_origin"), "request")

        # Rubric query with sort point
        meta = self.get('sort=distance')
        self.assertEqual(meta.get_property("sort"), "distance")
        self.assertEqual(meta.get_property("sort_origin"), "request")

        # relev=rank_sort_rating_rubrics
        self.set_query(
            'text=ресторан&ll=37.5,55.7&spn=1.0,1.0&gta=ShowedMachineRating&relev=rank_sort_rating_rubrics=1'
        )
        meta = self.get()
        self.assertEqual(meta.get_property("sort"), "rank")

    def testDebugInfo(self):
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1,1&results=1')

        prop_names = ['extended_tree']

        meta = self.get()
        for prop in prop_names:
            self.assertTrue(
                meta.get_property(prop).get_value() is None, message='property "{0}" found without d=1'.format(prop)
            )

        meta = self.get('d=1')
        for prop in prop_names:
            self.assertTrue(
                meta.get_property(prop).get_value() is not None,
                message='property "{0}" not found with d=1'.format(prop),
            )

    def testURI(self):
        self.set_query('ll=37.6,55.788&spn=0.02,0.02')
        zooURI = 'ymapsbm1://org?oid=140'
        for query in ['text=зоопарк', 'oid=140']:
            self.assertEqual(self.get(query).get_metadata_list('uri')[0].uri[0].uri, zooURI)

    def testFilterRate(self):
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1.0,1.0')
        self.assertEqual(self.get('index_filter_rate=0').found(), self.get().found())
        self.assertLess(self.get('index_filter_rate=50').found(), self.get().found())
        self.assertEqual(self.get('index_filter_rate=100').found(), 0)

    def testQLoss(self):
        self.set_query('ll=37.68,55.84&spn=1,1&text=кафе')
        self.assertEquals(self.get().get_property('qloss').get_value(), None)

        qloss = (
            self.get('qloss_id=1108827929&qloss_id=182&qloss_id=123&qloss_id=140&qloss_id=111')
            .get_property('qloss')
            .get_value()
        )
        self.assertEquals(
            json.loads(qloss),
            {
                "123": "Index",
                "111": "SearchWindow",
                "140": "TextFiltration",
                "182": "OkNotFirst",
                "1108827929": "OkNotFirst",
            },
        )

        self.set_query(
            'll=75.7054158104476,56.16817157743144&spn=130.16601562500003,46.00738874397413&text=ремонт&query_geopart=Россия'
        )
        original_found = self.get().found()
        qloss_found = self.get('qloss_id=1229302818&qloss_id=180').found()
        self.assertPositive(qloss_found)
        self.assertEqual(original_found, qloss_found)

        # test OkNotFirst vs Ranking
        self.set_query(
            'll=27.557600,53.878824&&spn=0.180588,0.071813&text=зоопарк&qloss_id=1084272096&qloss_id=140295611177'
        )
        qloss = self.get('results=1').get_property('qloss').get_value()
        self.assertEquals(
            json.loads(qloss),
            {
                "1084272096": "Ok",
                "140295611177": "Ranking",
            },
        )
        qloss = self.get('results=100').get_property('qloss').get_value()
        self.assertEquals(
            json.loads(qloss),
            {
                "1084272096": "Ok",
                "140295611177": "OkNotFirst",
            },
        )

    def testTimezone(self):
        today = datetime.datetime(2015, 10, 23)
        data = [(147, 'Europe/Moscow'), (180, 'Asia/Kamchatka'), (225, 'Asia/Chita'), (181, 'Europe/Amsterdam')]

        for oid, zone_name in data:
            expected = pytz.timezone(zone_name).utcoffset(today).seconds
            self.set_query('oid={0}'.format(oid))
            self.assertEqual(self.get().get_business_metadata().open_hours.tz_offset, expected)

    def testRubricFilter(self):
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1,1')
        self.assertLess(self.get('wiz_rubrics=184106384:1$184106394:1').found(), self.get().found())
        self.assertGreater(self.get('wiz_rubrics=184106384:1$184106394:1').found(), 0)
        # Use rubric class to surely filter all restaurants
        self.assertEqual(self.get('wiz_rubrics=184106382:1').found(), 1)
        self.assertEqual(self.get('wiz_rubrics=184106382:5').found(), 5)

    def testChains(self):
        self.set_query('text=лисичка')
        self.assertEqual(set(self.get().get_business_nested('chain', 'id')), {'6002333'})

    def testClosedOrgs(self):
        self.set_query('text=fight club&ll=37.5,55.7&spn=1,1')
        self.should_not_find('')
        self.should_find('show_closed=1')
        self.should_find('show_closed=1&rubric_request=1')

    def testNumberOfResultsFound(self):
        self.set_query('ll=37.68,55.84&spn=0.1,0.1&text=кафе&autoscale=0')
        total = 4

        meta = self.get('results=2').get_report()
        requested = 2

        self.assertEqual(meta.TotalDocCount[0], total)
        self.assertEqual(meta.Grouping[0].NumGroups[0], total)
        self.assertEqual(meta.Grouping[0].NumDocs[0], total)
        self.assertEqual(len(meta.Grouping[0].Group), requested)

        meta = self.get('results=100').get_report()
        requested = 100

        self.assertEqual(meta.TotalDocCount[0], total)
        self.assertEqual(meta.Grouping[0].NumGroups[0], total)
        self.assertEqual(meta.Grouping[0].NumDocs[0], total)
        self.assertEqual(len(meta.Grouping[0].Group), total)

    def testNumberOfResultsFoundWithPruning(self):
        self.set_query('ll=37.68,55.84&spn=0.1,0.1&text=кафе&autoscale=0&shards=1')
        meta = self.get('results=100&pron=pruncount1').get_report()
        self.assertEqual(meta.TotalDocCount[0], 4)
        self.assertEqual(meta.Grouping[0].NumGroups[0], 4)
        self.assertEqual(meta.Grouping[0].NumDocs[0], 4)
        self.assertEqual(len(meta.Grouping[0].Group), 4)

    def testQlossPruning(self):
        self.set_query(
            'll=37.68,55.84&spn=0.1,0.1&text=кафе&autoscale=0&qloss_id=160&pron=pruncount1&relev=panther_lite_pruning=1;'
        )
        qloss = self.get().get_property('qloss')
        qloss_dict = json.loads(qloss.get_value())
        self.assertIn('160', BlackboxResponse(qloss_dict, qloss.get_message()))
        self.assertIn(BlackboxResponse(qloss_dict['160'], qloss.get_message()), ['Pruning', 'TextFiltration'])

    def testSeoUrls(self):
        self.set_query('oid=111')
        self.assertEqual(
            self.get('lang=ru_RU').get_business_metadata().get_nested('COMPANY_INFO', 'seoname')[0], 'vodoley_ooo'
        )
        self.assertEqual(
            self.get('lang=ru_UA').get_business_metadata().get_nested('COMPANY_INFO', 'seoname')[0], 'vodolii_ooo'
        )
        self.assertEqual(
            self.get('lang=uk_UA').get_business_metadata().get_nested('COMPANY_INFO', 'seoname')[0], 'vodolii_ooo'
        )

        self.set_query('oid=1120046349')
        self.assertEqual(self.get().get_business_metadata().get_nested('COMPANY_INFO', 'seoname')[0], 'cafe_mumu')
        self.assertEqual(
            self.get('lang=tr_TR').get_business_metadata().get_nested('COMPANY_INFO', 'seoname')[0], 'cafe_mumu'
        )
        self.assertEqual(
            self.get('lang=ru_RU').get_business_metadata().get_nested('COMPANY_INFO', 'seoname')[0], 'mu_mu'
        )
        self.assertEqual(
            self.get('lang=uk_UA').get_business_metadata().get_nested('COMPANY_INFO', 'seoname')[0], 'mu_mu'
        )

    def testEmails(self):
        self.set_query('oid=1046632891')
        self.assertEqual(
            self.get().get_business_metadata().get_nested('COMPANY_INFO', 'email'),
            ['arenda@orel-tsum.ru', 'info@orel-tsum.ru'],
        )

    def testSeoCategories(self):
        self.set_query('oid=1120353677')
        self.assertEqual(
            set(self.get().get_business_metadata().get_nested('category', 'CATEGORY_INFO', 'seoname')),
            set(['cafe_and_cafeterias', 'sushi_bars', 'restorany']),
        )

    def testAutoaccept(self):
        self.set_query('text=кафе&ll=37.613781,55.756584&spn=1,1&autoscale=1&relev=acceptonly=1120046349;nofilter=1')
        response = self.get()
        self.assertEqual(response.found(), 1)
        self.assertEqual(response.get_ids()[0], '1120046349')
        self.set_query(
            'text=автошкола&ll=17.613781,15.756584&spn=0.0001,0.0001&autoscale=0&relev=acceptonly=1120046349;nofilter=1'
        )
        response = self.get()
        self.assertEqual(response.found(), 1)
        self.assertEqual(response.get_ids()[0], '1120046349')
        self.set_query('text=кафе&ll=37.613781,55.756584&spn=1,1&autoscale=1&relev=nofilter=1')
        self.assertGreater(self.get().found(), 0)
        self.set_query(
            'text=кафе&ll=37.613781,55.756584&spn=1,1&autoscale=1&relev=acceptonly=no_such_permalink;nofilter=1'
        )
        self.assertEqual(self.get().found(), 0)

    def testTASSHandle(self):
        def tass(name, default=None):
            tass = urlparse.urljoin(self._url, 'tass')
            response = urllib.urlopen(tass).read()
            data = json.loads(response)
            # response is list, not dict
            for k, v in data:
                if k == name:
                    return v
            return default

        errors_before = tass('request_error_dmmm', 0)

        self.set_query('oid=1&results=-10')
        self.assertEqual(self.get().get_error_code(), YX_INCOMPATIBLE_REQ_PARAMS)

        errors_after = tass('request_error_dmmm', 0)

        self.assertGreater(errors_after, errors_before)

    def testAllFactors(self):
        self.set_query('oid=111')

        report = self.get('allfctrs=da').get_report()
        factors = report.Grouping[0].Group[0].Document[0].DocRankingFactors
        self.assertGreater(len(factors), 0)

    def testHidingExpFeatures(self):
        self.set_query('oid=777')
        response = self.get().get_business_metadata()
        self.assertEqual(response.get_nested('feature', 'id'), ['special_char'])

        response = self.get('show_exp_features=exp1').get_business_metadata()
        self.assertEqual(response.get_nested('feature', 'id'), ['special_char', 'secret_feature'])

        response = self.get('show_exp_features=exp2').get_business_metadata()
        self.assertEqual(response.get_nested('feature', 'id'), ['special_char'])

        self.set_query(
            'text=быстрое питание&ll=37.675478,55.852758&spn=0.5,0.5&wiz_rubrics=184106386'
        )  # window around oid=777
        response = self.get()
        filters = response.get_filters().get_value()
        self.assertFalse(has_filter(filters, 'secret_feature'))

        response = self.get('show_exp_features=exp1')
        filters = response.get_filters().get_value()
        self.assertTrue(has_filter(filters, 'secret_feature'))

    def testPharmacyOnDuty(self):
        # pharmacy on duty
        self.set_query('ll=34,62&text=аптека&results=1')
        response = self.get('filters=pharmacy_on_duty:1')
        self.assertPositive(response.found())
        metadata = response.get_business_metadata()
        self.assertTrue(hours_pb2.DayOfWeek.EVERYDAY in metadata.get_nested('open_hours', 'hours', 'day'))
        self.assertTrue(any(metadata.get_nested('open_hours', 'hours', 'time_range', 'all_day')))

    def testTurkish24HoursPharmacy(self):
        # ordinary pharmacy
        self.set_query('oid=1015507590')

        filters = self.get('').get_filters().get_value()
        self.assertTrue(has_filter(filters, 'open_24h'))
        self.assertFalse(has_filter(filters, 'on_duty'))

        # turkish pharmacy
        self.set_query('oid=1015507591')

        filters = self.get('').get_filters().get_value()
        self.assertFalse(has_filter(filters, 'open_24h'))
        self.assertTrue(has_filter(filters, 'on_duty'))

    def testWll(self):
        novinsky_plaza = "37.583284,55.757434~37.581674,55.757689~37.581889,55.758233~37.583520,55.757979"
        self.set_query('wll=%(novinsky_plaza)s' % locals())
        self.assertEqual(self.get().found(), 3)

        yakimanka_and_zamoskvorechie = "37.610437,55.747134~37.602197,55.734158~37.645112,55.736385~37.635671,55.748393"
        self.set_query('wll=%(yakimanka_and_zamoskvorechie)s' % locals())
        self.assertEqual(self.get('autoscale=0').found(), 0)
        self.assertEqual(self.get('autoscale=1').found(), 11)

    def testWllIsEqualToLlSpn(self):
        def compareWllAndLLResponse(query=''):
            wll = 'wll=37.610000,55.750000~37.610000,55.730000~37.570000,55.730000~37.570000,55.750000'
            ll_spn = 'll=37.590000,55.740000&spn=0.04,0.02'

            self.set_query(query)
            wll_response = self.get(wll)
            ll_spn_response = self.get(ll_spn)
            self.assertEqual(wll_response.found(), ll_spn_response.found())
            self.assertEqual(wll_response.get_business_nested('name'), ll_spn_response.get_business_nested('name'))

        compareWllAndLLResponse()
        compareWllAndLLResponse('autoscale=0')

        pll = '37.610000,55.750000~37.610000,55.730000~37.590000,55.730000~37.590000,55.750000'
        compareWllAndLLResponse('pll=%(pll)s' % locals())
        compareWllAndLLResponse('autoscale=0&pll=%(pll)s' % locals())
        compareWllAndLLResponse('mode=exact&pll=%(pll)s' % locals())

    def testFactorNamesInfoRequest(self):
        url = self._url + '?info=factornames'
        response = urllib.urlopen(url).read()
        factor_names = []

        for i, line in enumerate(response.splitlines()):
            index, name = line.split('\t')
            self.assertEqual(int(index), i)
            factor_names.append(name)

        query_geosearch_rubric = 'text=кафе&results=3&gta=_RankingFactorsOnly'
        query_geosearch_org1 = 'text=пищевой институт&results=3&gta=_RankingFactorsOnly'

        self.check_query_factornames(query_geosearch_rubric, factor_names)
        self.check_query_factornames(query_geosearch_org1, factor_names)

    def testHeavyRank(self):
        self.set_query(
            'll=37.529771,55.606009&spn=0.1,0.1&autoscale=1&relev=heavy_top_size=10;calc_dssm=1;calc_web_ann=1;&d=1'
        )
        self.assertEqual(self.get('text=одна+организация').get_property('heavy_top_size'), '10')
        self.assertEqual(self.get('text=аптека').get_property('heavy_top_size'), '10')

        # Smoke
        self.assertPositive(self.get('text=&sort=distance').found())
        self.assertPositive(self.get('sort=distance').found())
        self.assertPositive(self.get('text=бар').found())
        self.assertPositive(self.get('text=шиномонтаж').found())
        self.assertPositive(self.get('text=организация выставок').found())

    def testDSSM(self):
        # heavy top size = 10
        self.set_query(
            'll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1&relev=heavy_top_size=10;calc_dssm=1;calc_web_ann=1;panther_save_l1=1'
        )
        self.assertPositive(self.get('text=пищевой институт').found())
        self.assertGreater(self.get('text=пищевой институт').get_factor(0, 'DSSM0'), 1e-5)
        self.assertFloatEqual(self.get('text=пищевой институт').get_factor(0, 'L1Dssm'), 0.7715555, abs_tol=1e-4)

        # heavy top size = 0
        self.set_query('ll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1&relev=heavy_top_size=0;calc_dssm=1')
        self.assertPositive(self.get('text=пищевой институт').found())
        self.assertLess(self.get('text=пищевой институт').get_factor(0, 'DSSM0'), 1e-5)

    def testWebAnn(self):
        self.set_query(
            'll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1&relev=heavy_top_size=10;calc_dssm=1;calc_web_ann=1;&text=прачечная лисичка 1'
        )
        response = self.get()
        self.assertEqual(response.found(), 1)
        self.assertGreater(response.get_factor(0, 'LongClickBocmPlain'), 1e-5)
        self.assertGreater(response.get_factor(0, 'LongClickAnnotationMatchPredictionWeighted'), 1e-5)

        self.set_query(
            'll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1&relev=heavy_top_size=10;calc_dssm=1;calc_web_ann=1;&text=химчистка lisichka'
        )
        response = self.get()
        self.assertEqual(response.found(), 1)
        self.assertGreater(response.get_factor(0, 'DoubleFrcValueWcmAvg'), 1e-5)
        self.assertGreater(response.get_factor(0, 'LongClickBocmPlain'), 1e-5)

    def testMainRubric(self):
        self.set_query('text=ресторан&ll=37.5,55.7&spn=1,1&oid=1015347960')
        response = self.get().get_business_metadata()
        self.assertEqual(response.get_nested('category')[0].name, u'Быстрое питание')
        self.assertEqual(response.get_nested('category')[1].name, u'Рестораны')

        self.set_query('oid=1052729494&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertEqual(response.get_nested('category')[0].name, u'Медицинские центры')  # manual main
        self.assertEqual(response.get_nested('category')[1].name, u'Клиники')  # most probable
        self.assertEqual(response.get_nested('category')[2].name, u'Медицинская помощь на дому')

        self.set_query('text=медицинская помощь на дому&oid=1052729494&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertEqual(response.get_nested('category')[0].name, u'Медицинские центры')  # manual main
        self.assertEqual(response.get_nested('category')[1].name, u'Медицинская помощь на дому')  # higlighted
        self.assertEqual(response.get_nested('category')[2].name, u'Клиники')

    def testUniversalFiltersNonrubricRequest(self):
        self.set_query('text=Парковка Торговый центр Мега&ll=37.491043,55.603573&spn=0.02,0.01&autoscale=0')
        self.assertEqual(self.get().found(), 1)

        # wizard filters
        self.assertEqual(self.get('wiz_filters=car_park:1').found(), 1)
        self.assertEqual(self.get('wiz_filters=car_park:0').found(), 1)  # document does not get filtered

        # manual user filters
        self.assertEqual(self.get('filter=car_park:1').found(), 0)

    def testInTitleOnly(self):
        self.set_query('text=burger&ll=37.5,55.7&spn=1,1&results=1')
        self.assertLess(self.get('relev=in_title_only=1').found(), self.get().found())
        self.set_query('text=burger king&ll=37.5,55.7&spn=1,1&results=1')
        self.assertEqual(self.get('relev=in_title_only=1').found(), self.get().found())

    def testBestMatchName(self):
        self.set_query('text=Ёлки-Палки&ll=37.5,55.7&spn=1,1&results=1&lang=ru')
        self.assertEqual(
            unicode(self.get('relev=best_matched_name=0').get_business_metadata().name), u'Трактир кафе Ёлки-Палки'
        )
        self.assertEqual(unicode(self.get('relev=best_matched_name=1').get_business_metadata().name), u'Ёлки-Палки')

        self.set_query('text=Трактир Рога и копыта&ll=37.5,55.7&spn=1,1&results=1&lang=ru')
        self.assertEqual(
            unicode(self.get('relev=best_matched_name=0').get_business_metadata().name), u'Трактир кафе Ёлки-Палки'
        )
        self.assertEqual(
            unicode(self.get('relev=best_matched_name=1').get_business_metadata().name), u'ООО "Рога и копыта"'
        )

    def testShortname(self):
        self.set_query('oid=1051431193&lang=ru_RU')
        response = self.get().get_business_metadata()
        self.assertEqual(unicode(response.short_name), u'Ёлки-Палки')

        self.set_query('oid=1051431193&lang=en_US')
        response = self.get().get_business_metadata()
        self.assertFalse(response.get_nested('short_name'))

    def get_exp_snippet_item_value(self, snippet, key):
        ns = {'e': 'http://maps.yandex.ru/snippets/experimental/1.x'}
        root = etree.XML(snippet).find('e:ExperimentalStorage', ns)
        for el in root.findall('e:Item', ns):
            if el.find('e:key', ns).text == key:
                return el.find('e:value', ns).text

    def testSubtitleRubricId(self):
        self.set_query('text=ресторан&lang=ru_RU&ll=37.5,55.7&spn=1,1&results=1&gta=subtitle_rubric_id')
        self.assertEqual(self.get().get_group_property(0, "subtitle_rubric_id").get_value(), '184106394')

        self.set_query('text=связной&lang=ru_RU&ll=37.5,55.7&spn=1,1&results=1&gta=rubric_subtitle')
        self.assertEqual(self.get().get_group_property(0, "rubric_subtitle").get_value(), None)

    def testPhoneMatch(self):
        self.assertEqual(self.get('text=774685').get_factor(0, 'PhoneMatch'), 1.0)
        self.assertEqual(self.get('text=8142774685').get_factor(0, 'PhoneMatch'), 1.0)
        self.assertEqual(self.get('text=78142774685').get_factor(0, 'PhoneMatch'), 1.0)
        self.assertEqual(self.get('text=1').get_factor(0, 'PhoneMatch'), 0.0)

    def testFormattedPhones(self):
        self.set_query('oid=1045755033&results=1')
        self.assertTrue(self.get().get_business_metadata().get_nested('phone'))

        ext_translates = {'ru_RU': 'доб.', 'uk_UA': 'дод.', 'en_US': 'ext.', 'tr_TR': 'dah.'}

        for lang, ext in ext_translates.items():
            phones = (
                self.get('lang={}&relev=add_extension_number_to_formatted_phone=1'.format(lang))
                .get_business_metadata()
                .get_nested('phone', 'formatted')
            )
            self.assertEqual(phones[0], '+7 (495) 989-45-60 ({} 717)'.format(ext))
            self.assertEqual(phones[1], '+7 (495) 989-45-61')

        phones = (
            self.get('lang=ru_RU&relev=add_extension_number_to_formatted_phone=0')
            .get_business_metadata()
            .get_nested('phone', 'formatted')
        )
        self.assertEqual(phones[0], '+7 (495) 989-45-60')
        self.assertEqual(phones[1], '+7 (495) 989-45-61')

    def testHiddenPhones(self):
        self.set_query('lang=ru_RU&relev=add_extension_number_to_formatted_phone=0&gta=hidden_phone_mask')

        resp = self.get('oid=1045755033&&print_hidden_phones=1')
        metadata = resp.get_business_metadata()
        self.assertTrue(metadata.get_nested('phone'))
        phones = metadata.get_nested('phone', 'formatted')
        self.assertEqual(len(phones), 3)
        self.assertEqual(phones[0], '+7 (495) 989-45-60')
        self.assertEqual(phones[1], '+7 (495) 989-45-61')
        self.assertEqual(phones[2], '+7 (495) 989-45-62')
        self.assertEqual(resp.get_group_property(0, 'hidden_phone_mask'), '001')

        metadata = self.get('oid=1119541190&print_hidden_phones=1').get_business_metadata()
        self.assertTrue(metadata.get_nested('phone'))
        phones = metadata.get_nested('phone', 'formatted')
        self.assertEqual(phones[0], '+7 (495) 916-86-99')

        metadata = self.get('oid=1119541190&print_hidden_phones=0').get_business_metadata()
        self.assertFalse(metadata.get_nested('phone'))

    def testTags(self):
        self.set_query('oid=1015347960&results=1')
        self.assertTrue(self.get().get_business_metadata().get_nested('properties', 'item'))

    def testAdvertAttribute(self):
        self.set_query('oid=5555500002&results=1')
        self.assertEqual(self.get('gta=advert').get_group_property(0, 'advert'), '1')

        self.set_query('oid=5555500003&results=1')
        self.assertEqual(self.get('gta=advert').get_group_property(0, 'advert'), '0')

    def testReferences(self):
        self.set_query('reference=nyak:1542018415')
        self.assertGreater(self.get().found(), 0)

        self.set_query('reference=nyak:1542598826')
        self.assertGreater(self.get().found(), 0)

        self.set_query('oid=164')
        self.assertEqual(self.get('gta=ref_nyak').get_group_property_list(0, 'ref_nyak'), ['1542018415', '1542598826'])

        self.set_query('oid=164')
        self.assertEqual(
            self.get('gta=yandex_travel_refs').get_group_property_list(0, 'yandex_travel_refs'),
            ['ytravel_booking.some_booking_id~ytravel_trivago.trivago_id'],
        )

    def testStaticFactorsCalcer(self):
        self.set_query('text=Парковка Торговый центр Мега')

        # general case
        self.assertFloatEqual(self.get().get_factor(0, 'SameHostCountMaxi'), 0.22374)

        # experimental factor
        self.assertFloatEqual(self.get().get_factor(0, 'TripadvisorRating'), 0.95)

    def testReviewFeatures(self):
        response = self.get('oid=1045755033')
        self.assertFloatEqual(response.get_factor(0, 'ReviewCount'), 0.928571)
        self.assertFloatEqual(response.get_factor(0, 'RatingScore'), 0.32)
        self.assertFloatEqual(response.get_factor(0, 'ShowedMachineRating'), 0.32)

    def testHiddenLinks(self):
        self.set_query('oid=1764093792&text=Black Burger')
        response = self.get()
        self.assertEqual(response.found(), 1)
        self.assertFalse(response.get_business_metadata().get_nested('link'))
        self.assertGreater(response.get_factor(0, 'LongClickBocmPlain'), 1e-5)

    def testMordaBan(self):
        self.assertLess(
            self.get(
                'll=37.613781,55.756584&spn=0.0001,0.0001&lang=ru&relev=filter_morda_forbidden_rubrics=1&autoscale=0'
            ).found(),
            self.get(
                'll=37.613781,55.756584&spn=0.0001,0.0001&lang=ru&relev=filter_morda_forbidden_rubrics=0&autoscale=0'
            ).found(),
        )

    def testHeavyRankingFormulaParam(self):
        self.set_query('text=пищевой институт&ll=37.529771,55.606009&spn=0.0018667,0.00127648&autoscale=1')

        # good formula name
        self.assertEqual(self.get('relev=heavy_top_size=10;ranking_heavy_formula=common_heavy').get_error_code(), None)

        # no heavy rank - no care
        self.assertEqual(self.get('relev=heavy_top_size=0;ranking_heavy_formula=bla-bla-bla').get_error_code(), None)

        # heavy rank on meta - no care
        self.assertEqual(self.get('relev=heavy_top_size=10;ranking_heavy_formula=bla-bla-bla').get_error_code(), None)

        # point cloud - no care
        self.set_query('text=пищевой институт&tx=154&ty=80&z=8&results=1')
        self.assertEqual(self.get('relev=heavy_top_size=10;ranking_heavy_formula=bla-bla-bla').get_error_code(), None)

    def testRegularCoordsFactors(self):
        regular_coords = [[37.689689, 55.740586], [37.727106, 55.782831]]
        self.set_query(
            'oid=1032821256&ll=37.67,55.75&sspn=0.72,0.23&relev=regular_coords=%s' % json.dumps(regular_coords)
        )
        self.assertEqual(self.get().get_factor(0, 'UserToRegularCoordsMinDistance'), 0.00324138)
        self.assertEqual(self.get().get_factor(0, 'OrgToRegularCoordsMinDistance'), 0.000788728)
        self.assertEqual(
            self.get('ull=37.716261,55.781878').get_factor(0, 'UserToRegularCoordsMinDistance'), 0.00137595
        )
        self.assertEqual(self.get('ull=30.315868,59.939095').get_factor(0, 'UserToRegularCoordsMinDistance'), 1.0)

        self.set_query('oid=1032821256&ll=37.67,55.75&sspn=0.72,0.23')
        self.assertEqual(self.get().get_factor(0, 'UserToRegularCoordsMinDistance'), 2.0)
        self.assertEqual(self.get().get_factor(0, 'OrgToRegularCoordsMinDistance'), 2.0)

    def testFeatureQuorumFactor(self):
        self.set_query('ll=37.594992,55.753096&spn=0.001,0.001&text=учебники&oid=5555500003')
        self.assertEqual(self.get().get_factor(0, 'FeaturesQuorum'), 1.0)

        self.set_query('ll=37.594992,55.753096&spn=0.001,0.001&text=книжный учебники&oid=5555500003')
        self.assertLess(self.get().get_factor(0, 'FeaturesQuorum'), 1.0)

    def testSearchGroups(self):
        self.set_query('oid=98021051311&gta=search_group')
        self.assertEqual(self.get().get_group_property(0, 'search_group'), "similarity_0.45:98021051311")

    def testGeoAdvertFactor(self):
        self.assertFloatEqual(self.get('oid=1035492987').get_factor(0, 'HasGeoAdvert'), 1.0)
        self.assertFloatEqual(self.get('oid=98021051311').get_factor(0, 'HasGeoAdvert'), 0.0)

    # HOTELS-4090 Фильтрация с учётом полной стоимости пребывания
    # test data in geobasesearch/tests/business/indexer-fast-features/source/hotel-prices.json
    def testHotelPriceFilter(self):

        hotel_id = '790'

        self.set_query('text=гостиницы&ll=37.657673,55.837191&spn=1.0,1.0&relev=calc_hotels_slice=1')

        resp = self.get()
        self.check_first_doc(resp, hotel_id, "hotel found without filters")

        resp = self.get("filter=hotel_total_cost_for_dates:20190607-20190610_4000-5000")
        self.check_first_doc(resp, hotel_id, "found with price in the range")

        resp = self.get("filter=hotel_total_cost_for_dates:20190607-20190610_3000-4000")
        self.check_first_doc(resp, hotel_id, "found by lower bound intersection")

        resp = self.get(
            "filter=hotel_total_cost_for_dates:20190607-20190610_3000-4000&relev=hotel_price_filter_flags=i"
        )
        self.check_not_found(resp, hotel_id, "found by lower bound intersection - inverted filter")

        resp = self.get("filter=hotel_total_cost_for_dates:20190607-20190610_5000-6000")
        self.check_first_doc(resp, hotel_id, "found by upper bound intersection")

        resp = self.get("filter=hotel_total_cost_for_dates:20190607-20190610_1000-2000")
        self.check_not_found(resp, hotel_id, "not found with price too low")

        resp = self.get("filter=hotel_total_cost_for_dates:20190610-20190612_2000-4000")
        self.check_not_found(resp, hotel_id, "not found with price too low for high price period")

        resp = self.get("filter=hotel_total_cost_for_dates:20190610-20190612_4000-5000")
        self.check_first_doc(resp, hotel_id, 'found with high price filter for high price period')

        resp = self.get("filter=hotel_total_cost_for_dates:20190611-20190612_8000-9000")
        self.check_not_found(resp, hotel_id, "single-day filter")

        resp = self.get("filter=hotel_total_cost_for_dates:20190611-20190611_1-2")
        self.check_first_doc(resp, hotel_id, "found with empty date range (any price)")

        resp = self.get("filter=hotel_total_cost_for_dates:20190706-20190710-5000-25000")
        self.check_first_doc(resp, hotel_id, "found with filter price range wider than hotel price range")

        resp = self.get("filter=hotel_total_cost_for_dates:20190710-20190715-8000-20000")
        self.check_not_found(resp, hotel_id, "sold out for certain dates in filter range")

        resp = self.get("filter=hotel_total_cost_for_dates:20190908-20190910_100-500")
        self.check_first_doc(resp, hotel_id, "found with any price and non-matching period")

        resp = self.get("filter=hotel_total_cost_for_dates:20190607-20190610")
        self.check_first_doc(resp, hotel_id, "found with dates-only filter and no sold-out days")

        resp = self.get("filter=hotel_total_cost_for_dates:20190710-20190715")
        self.check_not_found(resp, hotel_id, "sold out for certain dates in dates-only filter")

        resp = self.get("filter=hotel_total_cost_for_dates:20190710-20190715&relev=hotel_price_filter_flags=i")
        self.check_first_doc(resp, hotel_id, "sold out for certain dates in dates-only filter - inverted filter")

        resp = self.get("filter=hotel_total_cost_for_dates:20190908-20190910")
        self.check_first_doc(resp, hotel_id, "found with non-matching dates-only filter")

        resp = self.get("filter=hotel_total_cost_for_dates:ggg")
        self.check_first_doc(resp, hotel_id, "found with invalid filter")

        resp = self.get("filter=hotel_total_cost_for_dates:20200505-20190505_3000-4000")
        self.check_first_doc(resp, hotel_id, "found with invalid date range")

        filters = self.get('relev=print_range_filters=1').get_filters().get_value()
        self.assertTrue(has_filter(filters, 'hotel_total_cost_for_dates'))

        filters = (
            self.get('relev=print_range_filters=1&filter=hotel_total_cost_for_dates:20190607-20190610')
            .get_filters()
            .get_value()
        )
        self.assertEqual(has_filter(filters, 'hotel_total_cost_for_dates').range_filter.to, 5400)

        filters = self.get().get_filters().get_value()
        self.assertFalse(has_filter(filters, 'hotel_total_cost_for_dates'))

        resp = self.get('relev=print_range_filters=1')
        filters_set_attr = resp.get_property('filters_set').get_value()
        self.assertTrue(filters_set_attr)
        filters_set = filters_set_attr.split(',')
        self.assertIn('hotel_date_range', filters_set)
        self.assertIn('hotel_price_range', filters_set)

    # HOTELS-4306: Фильтр по провязкам на базовом
    def testHotelProviderFilter(self):
        self.set_query('oid=164')
        self.assertEqual(self.get().get_group_count(), 1)

        self.set_query('oid=164')
        self.assertEqual(self.get('filter=hotel_provider:ytravel_booking').get_group_count(), 1)

        self.set_query('oid=164')
        self.assertEqual(
            self.get('filter=hotel_provider:something_strange,ytravel_trivago,something_else').get_group_count(), 1
        )

        self.set_query('oid=164')
        self.assertEqual(self.get('filter=hotel_provider:something_strange').get_group_count(), 0)

    # HOTELS-4366: Фильтр по отелям с офферами с бесплатной отменой
    # HOTELS-4367: Фильтр по отелям по наличию офферов с завтраками
    def testHotelTraits(self):
        hotel_id = '790'
        self.set_query('text=гостиницы&ll=37.657673,55.837191&spn=1.0,1.0&relev=calc_hotels_slice=1')

        resp = self.get('filter=hotel_free_cancellation:1')
        self.check_first_doc(resp, hotel_id, "found with explicitly matching feature")

        resp = self.get('filter=hotel_free_cancellation:0')
        self.check_not_found(resp, hotel_id, "not found with explicitly non-matching feature")

        resp = self.get('filter=hotel_breakfast_included:1')
        self.check_first_doc(resp, hotel_id, "found with positive filter on undefined feature")

        resp = self.get('filter=hotel_breakfast_included:0')
        self.check_first_doc(resp, hotel_id, "found with negative filter on undefined feature")

        self.set_query('oid=181')
        self.assertEqual(
            self.get("filter=hotel_breakfast_included:0").get_group_count(),
            1,
            "found by oid with explicitly matching feature",
        )

        self.set_query('oid=181')
        self.assertEqual(
            self.get("filter=hotel_breakfast_included:1").get_group_count(),
            0,
            "not found by oid with explicitly non-matching feature",
        )

    # TRAVELBACK-1336: Фильтр по отелям по наличию определённого типа питания
    def testHotelPansions(self):
        hotel_id = '790'
        self.set_query('text=гостиницы&ll=37.657673,55.837191&spn=1.0,1.0&relev=calc_hotels_slice=1')

        resp = self.get('filter=hotel_pansion_with_offerdata:hotel_pansion_breakfast_included')
        self.check_first_doc(resp, hotel_id, "found with matching feature")

        resp = self.get('filter=hotel_pansion_with_offerdata:hotel_pansion_all_inclusive')
        self.check_not_found(resp, hotel_id, "not found with non-matching feature")

        resp = self.get(
            'filter=hotel_pansion_with_offerdata:hotel_pansion_breakfast_included,hotel_pansion_breakfast_dinner_included'
        )
        self.check_first_doc(resp, hotel_id, "found with matching or-feature")

        resp = self.get(
            'filter=hotel_pansion_with_offerdata:hotel_pansion_breakfast_included,hotel_pansion_all_inclusive'
        )
        self.check_first_doc(resp, hotel_id, "found with matching or-feature")

    def testTravelRequest(self):
        boosted = '1051431193'
        self.set_query('ll=37,55&spn=2,2&text=ресторан&results=1&relev=calc_hotels_slice=1')

        self.assertNotIn(boosted, self.get().get_ids())
        self.assertIn(boosted, self.get('relev=travel_request=1').get_ids())

        self.set_query('ll=37,55&spn=2,2&text=ресторан&results=2&relev=calc_hotels_slice=1')
        self.assertNotIn('164', self.get().get_ids())
        self.assertIn('164', self.get('relev=travel_request=1').get_ids())

        self.set_query('text=гостиницы&ll=37.657673,55.837191&spn=1.0,1.0&relev=calc_hotels_slice=1')
        self.assertEqual('790', self.get().get_group_name(0))
        self.assertEqual(boosted, self.get('relev=hotel_boost=mean_conversion_12w;travel_request=1').get_group_name(0))
        self.assertEqual(boosted, self.get('relev=hotel_boost=conversion_mean_12w;travel_request=1').get_group_name(0))
        self.assertEqual(boosted, self.get('relev=hotel_boost=booking_bookings_12w;travel_request=1').get_group_name(0))
        self.assertEqual(boosted, self.get('relev=hotel_boost=booking_hits_12w;travel_request=1').get_group_name(0))
        self.assertEqual(boosted, self.get('relev=hotel_boost=predicted_clicks_12w;travel_request=1').get_group_name(0))
        self.assertEqual(
            boosted, self.get('relev=hotel_boost=predicted_bookings_12w;travel_request=1').get_group_name(0)
        )
        self.assertEqual(boosted, self.get('relev=hotel_boost=hotel_can_be_sold;travel_request=1').get_group_name(0))

    def testRealtyRequest(self):
        self.set_query('text=гостиницы&ll=37.657673,55.837191&spn=1.0,1.0')
        self.assertEqual('790', self.get().get_ids()[0])
        self.assertEqual('1051431193', self.get('relev=realty_request=1').get_ids()[0])

    def testFactorsInfo(self):
        url = self._url + '?info=factors_info'
        response = urllib.urlopen(url).read()
        pool_info = TPoolInfo()
        pool_info.ParseFromString(response)
        geo_prod_features = 0
        for feature in pool_info.FeatureInfo:
            if feature.Slice == 'geo_production':
                geo_prod_features += 1
        self.assertGreater(geo_prod_features, 300)

    def testHotelStaticYtFactorsOid(self):
        query = 'text=гостиницы&ll=37.657673,55.837191&spn=1.0,1.0&relev=calc_hotels_slice=1'

        factors = {
            "HotelBookings_1W": 1.0,
            "TravelPartnerClicks_12W": 12.0,
            "TravelPartnerClicks_4W": 4.0,
            "AvgNightProfit_4W": 10.0,
            "AvgNightCost_4W": 100.0,
            "HotelBookings_4W": 4.0,
            "TravelRating": 5.1,
            "HotelBookings_12W": 12.0,
            "TravelPartnerClicks_1W": 1.0,
        }
        meta = self.get(query)
        for factorName, factorValue in factors.items():
            self.assertEqual(meta.get_factor(0, factorName), factorValue)

    def testSocialLinks(self):
        self.set_query('oid=778')
        self.assertEqual(
            self.get('gta=social_links').get_group_property(0, 'social_links'),
            'http://facebook.com/virus.info\thttps://vk.com/akveduk',
        )

    def testStaticRubricFactors(self):
        self.assertEqual(self.get('oid=1132400859').get_factor(0, 'OrgRubricIsUniversity'), 1.0)
        self.assertEqual(self.get('oid=181').get_factor(0, 'OrgRubricIsBooking'), 1.0)
        self.assertEqual(self.get('oid=1046632891').get_factor(0, 'OrgRubricIsDepartmentStore'), 1.0)
        self.assertEqual(
            self.get('oid=64694304270').get_factor(0, 'OrgRubricIsIndustrialBodiesAndManufacturingPlants'), 1.0
        )
        self.assertEqual(self.get('oid=1703889352').get_factor(0, 'OrgRubricIsWaterTransport'), 1.0)
        self.assertEqual(self.get('oid=152361075435').get_factor(0, 'OrgRubricIsTrainStation'), 1.0)

    def testChainFilter(self):
        # Minsk, non-rubric request
        self.set_query('ll=27.635601,53.849944&spn=0.0001,0.0001&autoscale=0&lang=ru')
        self.assertPositive(self.get().found())

        # chain 1075473676 is present in index
        response = self.get('filter=chain_id:1075473676')
        self.assertZero(response.found())
        filters = response.get_filters().get_value()
        chain_filter = has_filter(filters, 'chain_id')
        self.assertTrue(chain_filter)
        self.assertEqual(chain_filter.enum_filter.value[0].value.id, '1075473676')
        self.assertEqual(chain_filter.enum_filter.value[0].value.name, 'Москоммерцбанк')

        # chain 123456 is fake
        response = self.get('filter=chain_id:123456')
        self.assertZero(response.found())
        chain_filter = has_filter(response.get_filters().get_value(), 'chain_id')
        self.assertTrue(chain_filter)
        self.assertEqual(len(chain_filter.enum_filter.value), 0)

        # Moscow, rubric request
        self.set_query('text=клиника&ll=37.5,55.6&spn=1,1&autoscale=0')
        self.assertPositive(self.get().found())
        # chain 1075473676 is present in index
        response = self.get('filter=chain_id:1075473676')
        self.assertZero(response.found())
        chain_filter = has_filter(response.get_filters().get_value(), 'chain_id')
        self.assertTrue(chain_filter)
        self.assertEqual(chain_filter.enum_filter.value[0].value.id, '1075473676')

    def testRubricChainFilter(self):
        def get_chains(filters):
            chains = []
            for item in has_filter(filters, 'chain_id').enum_filter.value:
                chains.append(item.value.id)
            return chains

        self.set_query('text=ресторан&ll=37.5,55.6&spn=1,1&autoscale=0')
        response = self.get()
        self.assertPositive(response.found())
        self.assertTrue(has_filter(response.get_filters().get_value(), 'chain_id'))

        # Chain is present in index, but has no restaurants
        response = self.get('filter=chain_id:1075473676')
        self.assertZero(response.found())
        filters = response.get_filters().get_value()
        self.assertTrue(has_filter(filters, 'chain_id'))
        self.assertIn('1075473676', get_chains(filters))

        # Chain is present in index, but has no restaurants
        # Add Moscow query region
        response = self.get('filter=chain_id:1075473676&relev=coarse_query_geoid=213')
        self.assertZero(response.found())
        metadata = response.get_filters().get_value()
        self.assertTrue(has_filter(metadata, 'chain_id'))
        self.assertIn('1075473676', get_chains(metadata))

        # Chain is fake
        response = self.get('filter=chain_id:123456')
        self.assertZero(response.found())
        metadata = response.get_filters().get_value()
        self.assertTrue(has_filter(metadata, 'chain_id'))
        self.assertNotIn('123456', get_chains(metadata))

    def testEnumFilterMissingValue(self):
        self.set_query('text=ресторан&ll=37.5,55.6&spn=1,1&autoscale=0')

        def find_cuisine(filters, name):
            for item in has_filter(filters, 'type_cuisine').enum_filter.value:
                if item.value.id == name:
                    return item.value.name
            return None

        response = self.get('filter=type_cuisine:russian_cuisine')
        self.assertPositive(response.found())
        self.assertEqual(find_cuisine(response.get_filters().get_value(), 'russian_cuisine'), u'русская')
        self.assertEqual(find_cuisine(response.get_filters().get_value(), 'blahblah_cuisine'), None)

        response = self.get('filter=type_cuisine:blahblah_cuisine')
        self.assertZero(response.found())
        filters = response.get_filters().get_value()
        self.assertTrue(find_cuisine(filters, 'blahblah_cuisine') is not None)
        self.assertEqual(find_cuisine(response.get_filters().get_value(), 'russian_cuisine'), u'русская')
        self.assertEqual(find_cuisine(response.get_filters().get_value(), 'blahblah_cuisine'), u'blahblah_cuisine')

    def testBestRatingFilter(self):
        self.set_query('text=ресторан&ll=37.5,55.6&spn=1,1&autoscale=0&lang=ru')
        filter_off = self.get()

        filter_on = self.get('filter=best_rating:1')
        self.assertPositive(filter_on.found())
        self.assertEqual(has_filter(filter_on.get_filters().get_value(), 'best_rating').name, u'Высокий рейтинг')

        self.assertGreater(filter_off.found(), filter_on.found())

    def testRatingThresholdFilter(self):
        self.set_query('text=ресторан')
        filter_off = self.get()

        filter_on = self.get('filter=rating_threshold:gt3.0&lang=ru')
        self.assertPositive(filter_on.found())
        rating_threshold = has_filter(filter_on.get_filters().get_value(), 'rating_threshold')
        self.assertTrue(rating_threshold)
        self.assertEqual(rating_threshold.name, u'рейтинг')

        self.assertTrue(len(rating_threshold.enum_filter.value) > 0)
        self.assertTrue(all(v.value.name.startswith(u'не ниже ★') for v in rating_threshold.enum_filter.value))
        self.assertTrue(rating_threshold.enum_filter.single_select)

        self.assertGreater(filter_off.found(), filter_on.found())

    def testSearchReasonGta(self):
        self.set_query(
            'text=ресторан&ll=37.5,55.6&spn=1,1&autoscale=0&gta=search_reason&wiz_filters=type_cuisine:japanese_cuisine&filter=type_cuisine:japanese_cuisine'
        )
        self.assertEqual(
            self.get('gta=search_reason').get_group_property(0, 'search_reason'),
            '[{"features":[{"id":"type_cuisine","type":"enum","name":"кухня","value_id":"japanese_cuisine","value":"японская"}]}]',
        )

        self.set_query(
            'text=ресторан&ll=37.5,55.6&spn=1,1&autoscale=0&gta=search_reason&filter=wi_fi:1&wiz_filters=wi_fi:1'
        )
        self.assertEqual(
            self.get('gta=search_reason').get_group_property(0, 'search_reason'),
            '[{"features":[{"id":"wi_fi","type":"bool","name":"wi-fi","value":"1"}]}]',
        )

        self.set_query('text=ресторан&ll=37.5,55.6&spn=1,1&autoscale=0&gta=search_reason')
        self.assertEqual(self.get('gta=search_reason').get_group_property(0, 'search_reason'), '[]')

    def testJsonStateResponse(self):
        url = self._url + '?ms=json'
        response = json.loads(urllib.urlopen(url).read())
        for key in ['fast_features', 'advert']:
            md5 = response['index'][key]['md5']
            self.assertEqual(len(md5), 32)

    def testBinaryMetadataEntrances(self):
        def check_entrance(entrance, expected):
            self.assertEqual(len(entrance), len(expected))
            for i, e in enumerate(entrance):
                self.assertAlmostEqual(e.point.lon, expected[i][0], delta=1e-6)
                self.assertAlmostEqual(e.point.lat, expected[i][1], delta=1e-6)
                self.assertAlmostEqual(e.direction.azimuth, expected[i][2], delta=1e-6)
                self.assertAlmostEqual(e.direction.tilt, expected[i][3], delta=1e-6)

        expected = [(37.603200, 55.764700, 310.555914, 0.000000), (37.603100, 55.764800, -154.220000, 0.000000)]

        self.set_query('oid=1015347960&binary_metadata=da')

        response = self.get()
        entrance_metadata = extract_binary_metadata(response)['entrance']
        check_entrance(entrance_metadata.entrance, expected)

        response = self.get('fill_route_point_metadata=da')
        route_point_metadata = extract_binary_metadata(response)['route_point']
        self.assertEqual(route_point_metadata.route_point_context, "")
        check_entrance(route_point_metadata.entrance, expected)

    def testRequestedSlices(self):
        boosted = '1051431193'

        self.set_query('text=гостиницы&ll=37.657673,55.837191&spn=1.0,1.0&relev=calc_hotels_slice=1')
        self.assertEqual('790', self.get().get_group_name(0))
        self.assertEqual(boosted, self.get('relev=hotel_boost=booking_bookings_12w;travel_request=1').get_group_name(0))
        self.assertNotEqual(
            boosted,
            self.get('relev=hotel_boost=booking_bookings_12w;travel_request=1;requested_slices=hotel').get_group_name(
                0
            ),
        )

    def testRubricNameAttr(self):
        self.set_query('oid=780&gta=rubric_name')
        self.assertEqual(
            self.get().get_group_property(0, 'rubric_name').get_value(),
            '''[\"Cafe and cafeterias\",\"Доставка еды и обедов\",\"Банкетные залы\",\"Кейтеринг\"]''',
        )
        self.assertEqual(
            self.get('lang=ru').get_group_property(0, 'rubric_name').get_value(),
            '''[\"Кафе и кофейни\",\"Доставка еды и обедов\",\"Банкетные залы\",\"Кейтеринг\"]''',
        )

    def testRubricsAttr(self):
        self.set_query('oid=780&gta=rubrics')
        got = json.loads(self.get().get_group_property(0, 'rubrics').get_value())
        expected = [
            {"name": "Cafe and cafeterias"},
            {"name": "Доставка еды и обедов"},
            {"name": "Банкетные залы"},
            {"name": "Кейтеринг"},
        ]
        self.assertEqual(got, expected)
        got = json.loads(self.get('lang=ru').get_group_property(0, 'rubrics').get_value())
        expected = [
            {"name": "Кафе и кофейни", "plural_name": "кофейни"},
            {"name": "Доставка еды и обедов", "plural_name": "доставка еды и обедов"},
            {"name": "Банкетные залы"},
            {"name": "Кейтеринг"},
        ]
        self.assertEqual(got, expected)

    def testEnableSublineGoods(self):
        self.set_query('text=кафе&results=1&enable_subline_closed_everywhere=0')
        self.assertEqual(self.get_metadata_property_values('snippet_show_subline'), [])
        self.set_query('text=кафе&results=1&enable_subline_goods_everywhere=1&&enable_subline_closed_everywhere=0')
        self.assertEqual(self.get_metadata_property_values('snippet_show_subline'), ["goods"])

        self.set_query('oid=778&enable_subline_closed_everywhere=0')
        self.assertEqual(self.get_metadata_property_values('snippet_show_subline'), ["menu", "average_bill2"])
        self.set_query('oid=778&enable_subline_goods_everywhere=1&enable_subline_closed_everywhere=0')
        self.assertEqual(self.get_metadata_property_values('snippet_show_subline'), ["goods"])

    def testMxNetAttrs(self):
        self.set_query('oid=780')
        self.assertFalse(self.get().get_group_property(0, 'MxNetClass'))
        self.assertEqual(self.get('fsgta=MxNetClass').get_group_property(0, 'MxNetClass'), '0.09469125601')
        self.assertFalse(self.get().get_group_property(0, 'MxNetMSE'))
        self.assertEqual(self.get('fsgta=MxNetMSE').get_group_property(0, 'MxNetMSE'), '-0.002155043808')
        response = self.get('fsgta=MxNetClass;ll&fsgta=spn;MxNetMSE')
        self.assertEqual(response.get_group_property(0, 'MxNetClass'), '0.09469125601')
        self.assertEqual(response.get_group_property(0, 'MxNetMSE'), '-0.002155043808')

    def testShowOnlineOrgs(self):
        self.set_query('text=Доставка еды')
        response = self.get()
        self.assertEqual(response.get_group_count(), 2)
        self.assertNotIn('27971862924', response.get_ids())
        response = self.get('show_online_orgs=both')
        self.assertEqual(response.get_group_count(), 3)
        self.assertIn('27971862924', response.get_ids())
        response = self.get('show_online_orgs=only')
        self.assertEqual(response.get_group_count(), 1)
        self.assertIn('27971862924', response.get_ids())

        self.set_query('oid=27971862924&similar_orgs_request=1')
        self.assertEqual(self.get().get_group_count(), 0)
        self.assertEqual(self.get('show_online_orgs=both').get_group_count(), 1)
        self.assertEqual(self.get('show_online_orgs=only').get_group_count(), 1)

        self.set_query('oid=1051431193&similar_orgs_request=1')
        self.assertEqual(self.get().get_group_count(), 1)
        self.assertEqual(self.get('show_online_orgs=both').get_group_count(), 1)
        self.assertEqual(self.get('show_online_orgs=only').get_group_count(), 0)

    def testOnlineOrgAttr(self):
        self.set_query('gta=online_org&show_online_orgs=both')
        self.assertEqual(self.get('oid=27971862924').get_group_property(0, 'online_org'), '1')
        self.assertEqual(self.get('oid=181').get_group_property(0, 'online_org'), '0')

    def testHiddenFeatures(self):
        response = self.get('oid=1054821695&wiz_filters=tag_menu:borsch_tag')
        features = response.get_business_metadata().get_nested('snippet', 'feature_ref')
        self.assertFalse(features)

    def testFastReferences(self):
        response = self.get('oid=1764093792&gta=ref_ytravel_booking')

        reference = response.get_metadata_list('references')[0].reference[0]
        self.assertEqual(reference.scope, 'ytravel_booking')
        self.assertEqual(reference.id, 'very_fast_original_id')

        self.assertEqual(response.get_group_property(0, 'ref_ytravel_booking'), 'very_fast_original_id')

    def testDuplicatesUrls(self):
        self.set_query('url=www.vk.com')
        self.assertGreater(self.get().found(), 0)

    def testRangeFilter(self):
        self.set_query('text=железнодорожный вокзал&ll=126.786933,55.130840&spn=0.1,0.1')
        self.assertEqual(self.get().found(), 1)

        self.assertEqual(self.get('filter=room_number:200-').found(), 0)
        self.assertEqual(self.get('filter=room_number:100-').found(), 1)
        self.assertEqual(self.get('filter=room_number:90-').found(), 1)
        self.assertEqual(self.get('filter=room_number:-90').found(), 0)
        self.assertEqual(self.get('filter=room_number:90-110').found(), 1)
        self.assertEqual(self.get('filter=room_number:90-100').found(), 1)

        self.assertEqual(self.get('filter=3514161735:200-').found(), 1)
        self.assertEqual(self.get('filter=3514161735:-200').found(), 0)
        self.assertEqual(self.get('filter=3514161735:100000-200000').found(), 1)
        self.assertEqual(self.get('filter=3514161735:150000-200000').found(), 1)
        self.assertEqual(self.get('filter=3514161735:200-100000').found(), 1)
        self.assertEqual(self.get('filter=3514161735:100000-3000000').found(), 1)
        self.assertEqual(self.get('filter=3514161735:30000000-').found(), 0)

        response = self.get('relev=print_range_filters=1')
        filters = response.get_filters().get_value()
        self.assertTrue(has_filter(filters, 'room_number'))
        self.assertTrue(has_filter(filters, '3514161735'))

        self.assertEqual(has_filter(filters, '3514161735').range_filter.to, 2000000)
        self.assertEqual(has_filter(filters, 'room_number').range_filter.to, 100)

    def testLocatedAtAttr(self):
        main_mall_id = '1030209739'
        mall_ids = [main_mall_id, '1801431216', '1072052143']
        located_comp_id = '1051431193'
        department_comp_id = '1229302818'

        self.setUp()

        for mall_id in mall_ids:
            resp = self.get('located_at=' + mall_id)
            self.assertEqual(resp.found(), 1)
            self.assertEqual(resp.get_ids(), [located_comp_id])

        for comp_id in [located_comp_id, department_comp_id]:
            resp = self.get('located_at=' + comp_id)
            self.assertZero(resp.found())

    def testQuarantineChain(self):
        # Chain 6002347
        self.set_query('oid=164')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), [])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["1"])

        # Chain 6003441, GeoId 213
        self.set_query('oid=777')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["1"])

        # Chain 6003441, GeoId 193
        self.set_query('oid=779')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), [])

    def testQuarantineRubric(self):
        # Rubric 184105514
        self.set_query('oid=151')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["1"])
        self.assertEqual(
            self.get('gta=quarantine_temporarily_closed_gta').get_group_property(
                0, 'quarantine_temporarily_closed_gta'
            ),
            '1',
        )
        self.assertEqual(self.get('gta=closure_type').get_group_property(0, 'closure_type'), 'temporary')
        self.assertEqual(self.get().get_business_metadata().closed, business_pb2.Closed.TEMPORARY)

        # Rubric 184106414
        self.set_query('oid=790')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), [])

        # Rubric 184106414, GeoId Amsterdam
        self.set_query('oid=181')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), [])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), [])

        # Main rubric 184106108
        self.set_query('oid=1052729494')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), [])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["1"])

    def testQuarantineMultiple(self):
        self.set_query('oid=1007133620')
        # Tag value
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["0"])
        # Chain 6003441, GeoId 213
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), [])

        # Both tags exists
        self.set_query('oid=111')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["0"])

        # Both tags exists
        self.set_query('oid=161')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["0"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["0"])

        self.set_query('oid=1108827929')
        # Rubric 184106390
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        # Tag value
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["1"])

        # Chain 6003441, Rubric 184106386 ignored
        self.set_query('oid=1067206831')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["1"])

        #  Rubric 184106386
        self.set_query('oid=160')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), [])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["1"])

    def testQuarantineForClosed(self):
        # Rubric 184105698
        self.set_query('oid=151117500836')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), [])
        self.assertEqual(
            self.get('gta=quarantine_temporarily_closed_gta').get_group_property(
                0, 'quarantine_temporarily_closed_gta'
            ),
            None,
        )

        # Rubric 184105698, closed Temporary
        self.set_query('oid=234198147115&show_closed=1')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), [])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), [])

        # Rubric 184105698, closed Permanent
        self.set_query('oid=87192713965&show_closed=1')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), [])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), [])

        #  Rubric 184105698, closed Permanent, Tags exists
        self.set_query('oid=36678434934&show_closed=1')
        self.assertEqual(self.get_metadata_property_values("closed_for_visitors"), ["1"])
        self.assertEqual(self.get_metadata_property_values("closed_for_quarantine"), ["0"])

    def testVisualHints_WhenSnippetIsNotRequested_WontAddMetadata(self):
        self.set_query('oid=778')
        response = self.get('binary_metadata=da')

        metadata = extract_binary_metadata(response)

        self.assertTrue("visual_hints" not in metadata)

    def testVisualHints_WhenSnippetIsRequestedForOrgWithNoTags_WontAddMetadata(self):
        self.set_query('oid=1015507590&gta=visual_hints/1.x')
        response = self.get('binary_metadata=da')

        metadata = extract_binary_metadata(response)

        self.assertTrue("visual_hints" not in metadata)

    def testVisualHints_WhenSnippetIsRequestedForOrgWithSomeTags_AddsMetadataWithDefaultValues(self):
        self.set_query('oid=778&gta=visual_hints/1.x')

        visual_hints = extract_binary_metadata(self.get('binary_metadata=da'))["visual_hints"]
        card_hints = visual_hints.card_hints
        serp_hints = visual_hints.serp_hints

        SerpHints = visual_hints_pb2.SerpHints
        self.assertEqual(serp_hints.show_title, SerpHints.SHORT_TITLE)
        self.assertEqual(serp_hints.show_address, SerpHints.NO_ADDRESS)
        self.assertEqual(serp_hints.show_category, SerpHints.ALL_CATEGORIES)
        self.assertEqual(serp_hints.show_rating, SerpHints.FIVE_STAR_RATING)
        self.assertEqual(serp_hints.show_photo, SerpHints.SINGLE_PHOTO)
        self.assertEqual(serp_hints.action_button, [])

        self.assertTrue(serp_hints.show_work_hours)
        self.assertFalse(serp_hints.show_verified)
        self.assertFalse(serp_hints.show_distance_from_transit)
        self.assertFalse(serp_hints.show_bookmark)
        self.assertFalse(serp_hints.show_eta)
        self.assertFalse(serp_hints.show_geoproduct_offer)

        self.assertFalse(card_hints.show_claim_organization)
        self.assertTrue(card_hints.show_taxi_button)
        self.assertTrue(card_hints.show_feedback_button)
        self.assertTrue(card_hints.show_reviews)
        self.assertTrue(card_hints.show_add_photo_button)

    def testVisualHints_WhenSnippetIsRequestedForOrgWithAllTags_AddsExpectedMetadata(self):
        self.set_query('oid=779&gta=visual_hints/1.x')

        visual_hints = extract_binary_metadata(self.get('binary_metadata=da'))["visual_hints"]
        card_hints = visual_hints.card_hints
        serp_hints = visual_hints.serp_hints

        SerpHints = visual_hints_pb2.SerpHints
        self.assertEqual(serp_hints.show_title, SerpHints.LONG_TITLE)
        self.assertEqual(serp_hints.show_address, SerpHints.SHORT_ADDRESS)
        self.assertEqual(serp_hints.show_category, SerpHints.ALL_CATEGORIES)
        self.assertEqual(serp_hints.show_rating, SerpHints.NUMERIC_RATING)
        self.assertEqual(serp_hints.show_photo, SerpHints.GALLERY)
        self.assertEqual(serp_hints.action_button, [SerpHints.OPEN_PRIMARY_URL])

        self.assertTrue(serp_hints.show_work_hours)
        self.assertTrue(serp_hints.show_verified)
        self.assertTrue(serp_hints.show_distance_from_transit)
        self.assertTrue(serp_hints.show_bookmark)
        self.assertTrue(serp_hints.show_eta)
        self.assertTrue(serp_hints.show_geoproduct_offer)

        self.assertTrue(card_hints.show_claim_organization)
        self.assertFalse(card_hints.show_taxi_button)
        self.assertTrue(card_hints.show_feedback_button)
        self.assertFalse(card_hints.show_reviews)
        self.assertFalse(card_hints.show_add_photo_button)

    def testSerpSubtitleProperty_WhenNotRequested_ReturnsNothing(self):
        result = self.get('oid=778').get_group_property(0, 'serp_subtitle_type').get_value()

        self.assertIsNone(result)

    def testSerpSubtitleProperty_WhenRequested_ReturnsShowSublineProperty(self):
        result = self.get('oid=778&gta=serp_subtitle_type&enable_subline_closed_everywhere=0').get_group_property(
            0, 'serp_subtitle_type'
        )

        self.assertEqual(result, 'menu,average_bill2')

    def testProviderFilter(self):
        self.set_query('text=макдоналдс&results=20')
        self.assertTrue(self.get().found().get_value() > 3)
        response = self.get('provider=yandex_district')
        self.assertEqual(response.found().get_value(), 1)
        self.assertEqual(response.get_business_metadata().id, "1015347960")

    def testAdvertsPriorityStrength(self):
        self.set_query(
            'll=37.594992,55.753096&spn=0.001,0.001&text=книжный магазин&mode=advert&fsgta=priority_strength'
        )
        res = self.get()
        self.assertEqual(res.found(), 1)
        self.assertEqual(res.get_group_property(0, "priority_strength"), '0.67')

        self.set_query(
            'll=37.594992,55.753096&spn=0.001,0.001&text=книжный магазин&mode=advert&relev=advert_priority_filter_seed=1'
        )
        res = self.get()
        self.assertEqual(res.found(), 1)

        self.set_query(
            'll=37.594992,55.753096&spn=0.001,0.001&text=книжный магазин&mode=advert&relev=advert_priority_filter_seed=9000'
        )
        res = self.get()
        self.assertEqual(res.found(), 0)

    def testAdvertsFakePriorityStrength(self):
        self.set_query(
            'll=37.594992,55.753096&spn=0.001,0.001&text=книжный магазин&mode=advert&relev=advert_generate_priority_strength=1&fsgta=fake_priority_strength'
        )
        self.assertEqual(self.get().get_group_property(0, 'fake_priority_strength'), '0.3')

        self.set_query(
            'll=37.594992,55.753096&spn=0.001,0.001&text=книжный магазин&mode=advert&relev=advert_generate_priority_strength=1;advert_priority_filter_seed=1'
        )
        res = self.get()
        self.assertEqual(res.found(), 1)

        self.set_query(
            'll=37.594992,55.753096&spn=0.001,0.001&text=книжный магазин&mode=advert&relev=advert_generate_priority_strength=1;advert_priority_filter_seed=9000'
        )
        res = self.get()
        self.assertEqual(res.found(), 0)

    def testProfileData(self):
        self.set_query('oid=161')
        response = self.get().get_business_metadata()
        self.assertEqual(response.profile.description, u'Какой-то текст про эту замечательную организацию')

        self.set_query('oid=1051431193&lang=en_US')
        response = self.get().get_business_metadata()
        self.assertFalse(response.profile.description)

    def testSearchText(self):
        self.set_query('oid=148')
        self.assertEqual(
            self.get('relev=search_text_exp_name=v1&gta=search_text').get_group_property(0, 'search_text'),
            u'Лисичка 2, Москва',
        )

    def testNoAddressFragments(self):
        self.set_query('oid=1252793103')
        response = self.get('lang=ru_RU').get_business_metadata()
        self.assertEqual(unicode(response.name), u'Courtyard by Marriott Roseville')
        self.assertEqual(unicode(response.address.formatted_address), u'США, Розвилль, 1920 Taylor Rd, Roseville, Us')
        self.set_query('oid=1252793103&relev=user_country=225')
        self.assertEqual(
            self.get('gta=description').get_group_property(0, 'description'),
            u'США, Розвилль, 1920 Taylor Rd, Roseville, Us',
        )

    def testSourceProto(self):
        self.set_query('oid=1084272096&binary_metadata=source_proto')
        response = self.get()
        collection = extract_metadata_collection(response)

        company = Company.FromString(collection.SourceProto)
        self.assertEqual(company.export_id, 1084272096)

    def testFactorRequest(self):
        self.set_query('text=Будь+Здоров&allfctrs=da')
        response = self.get()
        report = response.get_report()
        docid = report.Grouping[0].Group[0].Document[0].DocId
        # factors strictly between 0 and 1
        factors = ['DSSM0', 'OriginalRequestChainNamesBocm', 'WhatOnlyRequestMaxNameCoverageSynonym']
        factors_original_request = [response.get_factor(0, factor_name).get_value() for factor_name in factors]
        meta_response = self.get_meta_1st('dh={0}:'.format(docid))
        factors_factor_request = [meta_response.get_factor(0, factor_name).get_value() for factor_name in factors]
        for factor_original_request, factor_factor_request in zip(factors_original_request, factors_factor_request):
            self.assertFloatEqual(factor_original_request, factor_factor_request, abs_tol=1e-4)

    def testMovedTo(self):
        # 100500 -> 100501 -> 100502
        self.set_query('oid=100500&show_closed=1')
        self.assertEqual(self.get_metadata_property_values("moved"), ["1"])
        self.assertEqual(self.get_metadata_property_values("moved_to"), ["100502"])

        # 100503 <-> 100504
        self.set_query('oid=100503&show_closed=1')
        self.assertEqual(self.get_metadata_property_values("moved"), ["1"])
        self.assertEqual(self.get_metadata_property_values("moved_to"), [])
