# -*- coding: utf-8 -*-

import base64
import os
import shutil
import sys
import time

import yatest.common

from blackbox.blackbox import HttpBlackbox, MetaBlackboxAdapter
from blackbox.testcase import TestCase

from extsearch.geo.kernel.pbreport.intermediate.proto import metadatacollection_pb2
from extsearch.geo.kernel.pymod.runserver import Service

from yandex.maps.proto.common2 import metadata_pb2
from yandex.maps.proto.search import collection_pb2
from yandex.maps.proto.search import collection_response_pb2
from yandex.maps.proto.uri import uri_pb2


import qtree

# Encoding setup
reload(sys)
sys.setdefaultencoding('utf-8')


def memoize(function):
    cache = {}

    def wrapper(*value):
        if value not in cache:
            cache[value] = function(*value)
        return cache[value]

    return wrapper


@memoize
def make_qtree(text):
    args = ['-t', text]
    return qtree.create(*args)


def get_category_names(meta_response):
    return [g.CategoryName for g in meta_response.get_report().Grouping[0].Group]


def parse_metadata_from_property(response, prop_name):
    prop = response.get_property(prop_name)
    if not prop:
        return None
    metadata = metadata_pb2.Metadata()
    metadata.ParseFromString(base64.b64decode(prop.get_value()))
    return metadata


def get_collection_response_metadata(response):
    return parse_metadata_from_property(response, 'binary_response_metadata')


def get_collection_entry_metadata(response, permalink):
    return parse_metadata_from_property(response, 'collection_entry_{}'.format(permalink))


def parse_metadata_from_binary_data(group, extension_type):
    binary_data = group.Document[0].BinaryData.GeosearchDocMetadata
    metadata_collection = metadatacollection_pb2.TMetadataCollection()
    metadata_collection.ParseFromString(binary_data)
    for data in metadata_collection.Data:
        metadata = metadata_pb2.Metadata()
        metadata.ParseFromString(data)
        if metadata.HasExtension(extension_type):
            return metadata.Extensions[extension_type]


def get_collection_metadata(group):
    return parse_metadata_from_binary_data(group, collection_pb2.COLLECTION_METADATA)


def get_uri_metadata(group):
    return parse_metadata_from_binary_data(group, uri_pb2.GEO_OBJECT_METADATA)


def is_debug_enabled():
    return yatest.common.get_param('DEBUG_PORT') is not None


class CollectionsSearchMetaAdapter(MetaBlackboxAdapter):
    pass


class CollectionsSearchTestcase(TestCase):
    @classmethod
    def setUpClass(cls):
        """Sets the testcase.

        Creates default blackbox.
        """
        # Note that for yatest.common functions we are enforced to use forward slash (/),
        # but for other path operations we must use generic os.path.join function.
        geobasesearch = yatest.common.binary_path('extsearch/geo/base/geobasesearch/geobasesearch')
        indexer = yatest.common.binary_path('extsearch/geo/indexer/collections/yandex-geosearch-indexer-collections')

        # . is current temp directory (initially empty).
        # It will be removed after the test.

        # put small Collections export into ./source directory
        shutil.copytree(
            yatest.common.source_path('extsearch/geo/base/geobasesearch/tests/collections/indexer-collections/source'),
            'source',
        )

        # make empty directory ./indexer-collections/index
        indexdir = os.path.join('indexer-collections', 'index')
        os.makedirs(indexdir)

        # paths to collections and static factors
        collections_path_2 = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/collections/indexer-collections/source/collections2.json'
        )
        static_factors_path = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/collections/indexer-collections/source/static_factors.mms'
        )

        # launch collections indexer
        yatest.common.execute([indexer, '-c', collections_path_2, '-o', indexdir])

        # copy resources to index, collections.json will appear in the indexdir automatically
        shutil.copy('geodata4.bin', indexdir)
        shutil.copy(static_factors_path, indexdir)

        # copy index to output (may be useful for investigating problems, index will be available in TestEnv)
        shutil.copytree(indexdir, yatest.common.output_path('indexer-collections/index'))

        # get search config absolute path
        search_config = yatest.common.source_path('extsearch/geo/base/geobasesearch/tests/collections/blackbox.cfg')

        # run geobasesearch!
        port = yatest.common.get_param('DEBUG_PORT', '0')
        argv = [geobasesearch, search_config, '-d', '-p', port]

        cls._service = Service(argv, debug=is_debug_enabled())
        try:
            sockaddr = cls._service.warm_up('')

            cls._url = sockaddr.get_url('/')
            cls._meta_box = HttpBlackbox(CollectionsSearchMetaAdapter, cls._url, 'ms=proto')

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
        self._meta_box.set_query(query)

    def get(self, query=''):
        return self._meta_box.get(query)

    def testDebug(self):
        # fake test
        if is_debug_enabled():
            while True:
                time.sleep(1)

    def testSearch_WithoutInputParameters_ReturnsNothing(self):
        self.assertZero(self.get('ll=37.5,55.6&spn=0.1,0.1').found())

    def testSearch_WithoutRequestWindow_ReturnsNothing(self):
        self.assertZero(self.get('source=expert').found())

    def testSearch_BySource_FiltersResultsByRequestWindow(self):
        self.set_query('source=expert&results=30&ll=37.5,55.6&spn=0.5,0.5')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 18)

        self.set_query('source=expert&results=30&ll=37.5,55.6&spn=0.1,0.1')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 16)

    def testStaticFactors(self):
        response = self.get('source=expert&gta=_RankingFactors&ll=37.5,55.6&spn=0.1,0.1')
        factorName2NonZeroCount = {
            'CollectionCompaniesMaxRating': 0,
            'CollectionCompaniesMeanRating': 0,
            'CollectionCompaniesHaveRating': 0,
        }
        for factorName in factorName2NonZeroCount.keys():
            for docNum in xrange(len(response.get_report().Grouping[0].Group)):
                if response.get_factor(docNum, factorName) > 0.0:
                    factorName2NonZeroCount[factorName] += 1

        for fv in factorName2NonZeroCount.values():
            self.assertPositive(fv)

    def testSearchCollections_Always_ReturnsCollectionMetadata(self):
        response = self.get('source=expert&ll=37.5,55.6&spn=0.1,0.1')
        self.assertPositive(response.found())
        for group in response.get_report().Grouping[0].Group:
            collection = get_collection_metadata(group).collection
            self.assertTrue(collection.uri)
            self.assertTrue(collection.title)

    def testSearchCollections_Always_ReturnsUriMetadata(self):
        response = self.get('source=expert&ll=37.5,55.6&spn=0.1,0.1')
        self.assertPositive(response.found())
        for group in response.get_report().Grouping[0].Group:
            metadata = get_uri_metadata(group)
            self.assertTrue(metadata.uri[0].uri)

    def testRandomRanking(self):
        resp1 = get_category_names(self.get('source=expert&ranking_model=collections_random&ll=37.5,55.6&spn=0.1,0.1'))
        resp2 = get_category_names(self.get('source=expert&ranking_model=collections_random&ll=37.5,55.6&spn=0.1,0.1'))
        resp3 = get_category_names(self.get('source=expert&ranking_model=collections_random&ll=37.5,55.6&spn=0.1,0.1'))
        self.assertGreater((resp1 != resp2) + (resp2 != resp3) + (resp3 != resp1), 1)

    def testTimePopularityFactor(self):
        factorName = 'CollectionRubricTimePopularity'
        response = self.get('source=expert&gta=%s&ll=37.5,55.6&spn=0.1,0.1' % factorName)
        for docNum in xrange(len(response.get_report().Grouping[0].Group)):
            self.assertPositive(response.get_factor(docNum, factorName))

    def testSearchByRelevantRubricId(self):
        self.set_query('wiz_rubrics=184106394&results=30&ll=37.5,55.6&spn=0.1,0.1')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 12)

        self.set_query('wiz_rubrics=31370163603&results=30&ll=37.5,55.6&spn=0.1,0.1')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 2)

        self.set_query('wiz_rubrics=31370163603$184106394&results=30&ll=37.5,55.6&spn=0.1,0.1')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 14)

    def testSearchByRubricWithWeightBelowThreshold(self):
        self.set_query('wiz_rubrics=35193114937&results=30&ll=37.5,55.6&spn=0.1,0.1')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping), 0)

    def testSearchPriority_GivenRubricsAndQtree_ReturnsResultByRubrics(self):
        query = 'wiz_rubrics={rubrics}&qtree={qtree}&results=30&ll=37.5,55.6&spn=0.1,0.1'.format(
            rubrics='184106394', qtree=make_qtree('завтрак')
        )
        self.set_query(query)
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 12)

    def testSearchByQtree(self):
        self.set_query('qtree={}&results=30&ll=37.5,55.6&spn=0.1,0.1'.format(make_qtree('завтрак')))
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 3)

    def testSearchByKeyword(self):
        self.set_query('qtree={}&results=30&ll=37.5,55.6&spn=0.1,0.1'.format(make_qtree('креветка')))
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 2)

    def testSearchByRelevantRubricId_ForCollectionWithEmptyRub2Weight_ReturnsResultByRubric(self):
        self.set_query('wiz_rubrics=87654321&results=30&ll=37.6,55.8&spn=0.1,0.1')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 2)

    def testSearchByCollectionId_GivenValidId_ReturnsCollection(self):
        self.set_query('collection_id=gde-est-pelmeni-v-ekaterinburge')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping[0].Group), 1)

    def testSearchByCollectionId_GivenInvalidId_DoesntReturnCollection(self):
        self.set_query('collection_id=invalid-collection-id')
        report = self.get().get_report()
        self.assertEqual(len(report.Grouping), 0)

    def testSearchByCollectionId_GivenValidId_ReturnsValidPermalinks(self):
        self.set_query('collection_id=gde-est-pelmeni-v-ekaterinburge&gta=permalinks')
        permalinks = self.get().get_property_list('permalinks').get_value()
        self.assertEqual(permalinks, ['1234', '5678'])

    def testSearchByCollectionId_GivenValidId_ReturnsCollectionResponseMetadata(self):
        self.set_query('collection_id=gde-est-pelmeni-v-ekaterinburge&gta=permalinks')
        metadata = get_collection_response_metadata(self.get())
        self.assertIsNotNone(metadata)
        self.assertTrue(metadata.HasExtension(collection_response_pb2.COLLECTION_RESPONSE_METADATA))
        extension = metadata.Extensions[collection_response_pb2.COLLECTION_RESPONSE_METADATA]
        self.assertEqual(extension.collection.uri, 'ymapsbm1://collection?id=gde-est-pelmeni-v-ekaterinburge')

    def testSearchByCollectionId_GivenValidId_ReturnsCollectionEntryMetadatas(self):
        self.set_query('collection_id=gde-est-pelmeni-v-ekaterinburge&gta=permalinks')
        response = self.get()
        permalinks = response.get_property_list('permalinks').get_value()
        self.assertTrue(permalinks)
        permalink2title = {'1234': 'Пельменная', '5678': 'Столовая'}
        for permalink in permalinks:
            metadata = get_collection_entry_metadata(response, permalink)
            self.assertIsNotNone(metadata)
            self.assertTrue(metadata.HasExtension(collection_response_pb2.COLLECTION_ENTRY_METADATA))
            extension = metadata.Extensions[collection_response_pb2.COLLECTION_ENTRY_METADATA]
            self.assertEqual(extension.title, permalink2title[permalink])

    def testSearchBySource_WhenAskedForGeoidGta_ReturnsIt(self):
        self.set_query('source=expert&results=30&gta=geoid&ll=37.6,55.8&spn=0.1,0.1')
        response = self.get()
        self.assertPositive(response.found())
        for i in range(response.get_group_count()):
            self.assertEqual(response.get_group_property(i, "geoid"), '213')

    def testSearch_WhenAcceptingByGeoId_ReturnsExpectedCollections(self):
        response = self.get('accept_by_geoid=1&geoid=1&gta=geoid')

        self.assertPositive(response.found())
        for i in range(response.get_group_count()):
            self.assertEqual(response.get_group_property(i, 'geoid'), '1')

    def testSearch_WhenOnlyOneCollectionIsRelevant_ReturnsEmptyForRubricRequest(self):
        self.set_query('results=30&ll=39.9,43.5&spn=0.1,0.1')

        response = self.get('wiz_rubrics=184106384')
        self.assertEqual(response.found(), 0)

        response = self.get('qtree={}'.format(make_qtree('спорт')))
        self.assertEqual(response.found(), 1)

    def testSearch_GivenCarousel_ReturnsIt(self):
        self.set_query('results=30&ll=39.9,43.5&spn=0.1,0.1&qtree={}'.format(make_qtree('спорт')))
        report = self.get().get_report()

        self.assertEqual(len(report.Grouping), 1)

        collection = get_collection_metadata(report.Grouping[0].Group[0]).collection
        self.assertEqual(len(collection.carousel), 2)
        self.assertEqual(collection.carousel[0].url_template, 'img1')
        self.assertEqual(collection.carousel[1].url_template, 'img2')
