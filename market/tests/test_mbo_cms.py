# -*- coding: utf-8 -*-

import gzip
import os
import shutil
import unittest
import json

from getter import core
from getter import logadapter
from getter import util
from getter.service import mbo

from market.pylibrary.pbufsn_utils import read_pbufsn
from market.proto.content.mbo.MboCmsPromo_pb2 import ReportPromo
from market.proto.blender.CmsIncut_pb2 import CmsIncut
from market.library.cms_promo.py_utils.extract_data_from_mbo_cms import CmsPromoExtractException

from util import touch, calc_md5

logadapter.init_logger()

ROOTDIR = os.path.join(os.getcwd(), 'tmp')


def create_featured_msku(msku=None, description=None, picture=None):
    result = {}
    if msku:
        result['msku'] = msku
    if description:
        result['description'] = description
    if picture:
        result['picture'] = picture
    return result


def create_promo(source):
    result = {}
    if 'promo_id' in source:
        result['promo_id'] = source['promo_id']

    if 'featured_mskus' in source:
        result['featured_msku'] = source['featured_mskus']
    if 'forced_mskus' in source:
        result['force_relevance_msku'] = [{
            'msku': msku
        } for msku in source['forced_mskus']]
    if 'available_mskus' in source:
        result['available_mskus'] = source['available_mskus']
    return json.dumps(result)


def get_incuts_mock():
    relations = {
        "414562": [
            {
                "device": "desktop",
                "hid": "10682647",
                "type": "catalog_quiz",
            },
            {
                "device": "desktop",
                "nid": "10682647",
                "type": "catalog_quiz",
            },
            {
                "device": "desktop",
                "hid": "15083319",
                "type": "catalog_quiz",
            },
            {
                "device": "desktop",
                "page_id": "170095",
                "type": "catalog_quiz",
            },
        ],
        "414563": [
            {
                "device": "desktop",
                "hid": "10682647",
                "type": "catalog_quiz",
            },
        ],
        "459930": [
            {
                "device": "desktop",
                "type": "growing_cashback_incut",
            },
        ],
        "459931": [
            {
                "device": "phone",
                "type": "growing_cashback_incut",
            },
        ],

    }
    context = {
        "414562": {
            "entity": "page",
            "id": "170095",
            "name": "name",
            "content": "content",
        },
        "414563": {
            "entity": "page",
            "id": "1700",
            "name": "name",
            "content": "content",
        },
        "459930": {
            "entity": "page",
            "id": "17008",
            "name": "name",
            "content": {"1": "2"},
        },
        "459931": {
            "entity": "page",
            "id": "17108",
            "name": "name",
            "content": {"1": "2"},
        },
    }
    return relations, context


class CmsFiles(object):
    def __init__(self):
        self.__relations = {}
        self.__context = []

    @property
    def relations(self):
        return [
            '{}:{}'.format(meta_str, ','.join(relations)) for meta_str, relations in self.__relations.iteritems()
        ]

    @property
    def context(self):
        return self.__context

    def add_info(self, meta, relation_id, context):
        self.add_relation_only(meta=meta, relation_id=relation_id)
        self.add_context_only(context=context, relation_id=relation_id)

    def add_relation_only(self, relation_id, meta):
        meta_str = '#'.join(['{}={}'.format(key, value) for key, value in meta.iteritems()])
        if meta_str not in self.__relations:
            self.__relations[meta_str] = []
        self.__relations[meta_str] += [str(relation_id)]

    def add_context_only(self, relation_id, context):
        self.__context += ['{}:{}'.format(relation_id, context)]


class FakeMboCmsService(mbo.MboCmsService):
    def __init__(self, test_name, data):
        self.__test_name = test_name
        self.md5sums = {}
        self.prepare_file(mbo.CMS_RELATIONS_NAME, '\n'.join(data.relations))
        self.prepare_file(mbo.CMS_CONTEXT_NAME, '\n'.join(data.context))

        with gzip.open(self.getfilepath(mbo.MD5SUMS_FILENAME), 'w') as fobj:
            for md5 in self.md5sums:
                fobj.write('%s  %s\n' % (md5, self.md5sums[md5]))

        super(FakeMboCmsService, self).__init__()

    def prepare_file(self, filename, content):
        filepath = self.getfilepath(filename)
        touch(filepath, content=content)
        md5 = calc_md5(content)
        self.md5sums[md5] = filename

    def getfilepath(self, filename):
        return os.path.join(ROOTDIR, self.__test_name, filename + '.gz')

    @classmethod
    def _get_recent_sid(cls):
        return cls.GENERATION

    def _calc_url(self, filename):
        filepath = os.path.join(ROOTDIR, self.__test_name, filename)
        return 'file://' + filepath


def create_mbo_cms(generation, data):
    FakeMboCmsService.GENERATION = generation
    service = FakeMboCmsService(test_name=generation, data=data)
    root = core.Root(ROOTDIR, create_lazy=False)
    root.register('mbo_cms', lambda: service)
    return service


class Test(unittest.TestCase):
    def setUp(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        util.makedirs(ROOTDIR)

    def find_source(self, src_list, promo_id):
        for promo in src_list:
            if promo['promo_id'] == promo_id:
                return promo
        self.fail('Promo {} is unexpected'.format(promo_id))

    def check_promo(self, sample, promo):
        self.assertEquals(sample.get('forced_mskus', []), promo.force_relevance_mskus)
        self.assertEquals(sample.get('available_mskus', []), promo.available_mskus)
        self.assertEquals(sample.get('featured_mskus', []), [create_featured_msku(rec.msku, rec.description, rec.picture) for rec in promo.featured_mskus])

    def test_valid_report_promo(self):
        source_full = {
            'promo_id': 'BlackFriday',
            'featured_mskus': [
                create_featured_msku(2000000011, description='Descr11', picture='pic11'),
                create_featured_msku(14, description='Описане на русском №;%:?*#', picture='pic14')
            ],
            'forced_mskus': [11, 12, 13],
            'available_mskus': [11, 12, 13, 14, 15]
        }
        source_featured_only = {
            'promo_id': 'Featured',
            'featured_mskus': [
                create_featured_msku(12, description='Descr12'),
                create_featured_msku(13, picture='pic13'),
            ],
        }
        source_forced_only = {
            'promo_id': 'Forced',
            'forced_mskus': [21, 22, 23],
        }
        source_available_only = {
            'promo_id': 'Available',
            'available_mskus': [31, 32, 33, 34, 35]
        }
        source_not_report = {
            'promo_id': 'NotReport',
            'featured_mskus': [
                create_featured_msku(11, description='Descr11', picture='pic11'),
                create_featured_msku(14, description='Descr14', picture='pic14')
            ],
            'forced_mskus': [11, 12, 13],
            'available_mskus': [11, 12, 13, 14, 15]
        }
        sources = [
            source_full,
            source_featured_only,
            source_forced_only,
            source_available_only
            # source_not_report Не будет добавлен в результат, т.к. не помечен типом report
        ]

        data = CmsFiles()
        data.add_info(
            meta=dict(type='report_actions'),
            relation_id=1,
            context=create_promo(source_full)
        )
        data.add_info(
            meta=dict(format='json', type='report_actions'),
            relation_id=2,
            context=create_promo(source_featured_only)
        )
        data.add_info(
            meta=dict(format='json', type='report_actions'),
            relation_id=3,
            context=create_promo(source_forced_only)
        )
        data.add_info(
            meta=dict(format='json', type='report_actions'),
            relation_id=4,
            context=create_promo(source_available_only)
        )
        data.add_info(
            meta=dict(format='json', type='not_report'),
            relation_id=5,
            context=create_promo(source_not_report)
        )
        data.add_info(
            meta=dict(format='text', type='ignore'),
            relation_id=6,
            context="Любой произвольный текст"
        )
        service = create_mbo_cms('valid_report_promo', data)
        service.update_service(names=[mbo.CMS_RELATIONS_NAME, mbo.CMS_CONTEXT_NAME, mbo.CMS_REPORT_PROMO_NAME], lazy=False)

        result_path = os.path.join(ROOTDIR, 'mbo_cms', 'recent', 'cms_report_promo.pbsn')
        total = 0

        for promo_result in read_pbufsn(result_path, ReportPromo, 'CMPR'):
            sample = self.find_source(sources, promo_result.promo_id)
            self.check_promo(sample, promo_result)
            total += 1

        self.assertEqual(total, len(sources))

    def test_blender_incuts(self):
        """
        проверяем содержимое файла cms_blender_incuts.pbsn
        """
        data = CmsFiles()
        incut_relations, incut_context = get_incuts_mock()
        for relation_id, relations in incut_relations.iteritems():
            for meta in relations:
                data.add_info(meta=meta, relation_id=relation_id, context=json.dumps(incut_context[relation_id]))
        service = create_mbo_cms('blender_incuts', data)
        service.update_service(
            names=[mbo.CMS_RELATIONS_NAME, mbo.CMS_CONTEXT_NAME, mbo.CMS_BLENDER_INCUTS_NAME], lazy=False
        )

        incut_result_path = os.path.join(ROOTDIR, 'mbo_cms', 'recent', 'cms_blender_incuts.pbsn')
        for incut in read_pbufsn(incut_result_path, CmsIncut, 'BCMS'):
            if incut.resource_type == "growing_cashback_incut":
                for resource in incut.resources:
                    if resource.page_id == 17008:
                        self.assertEqual(resource.name, "name")
                        self.assertEqual(resource.device, "desktop")
                        self.assertEqual(resource.content, json.dumps({"1": "2"}))
                    elif resource.page_id == 17108:
                        self.assertEqual(resource.name, "name")
                        self.assertEqual(resource.device, "phone")
                        self.assertEqual(resource.content, json.dumps({"1": "2"}))
            elif incut.resource_type == "catalog_quiz":
                for resource in incut.resources:
                    if resource.page_id == 170095:
                        self.assertEqual(set([10682647, 15083319]), set([hid for hid in resource.hids]))
                        self.assertEqual(set([10682647]), set([nid for nid in resource.nids]))
                        self.assertEqual(resource.content, json.dumps("content"))
                        self.assertEqual(resource.name, "name")

    def test_cut_unused_widgets(self):
        """
        Проверяется, что после получения от МБО коллекции cms-context-pages.txt,
        будет создаваться новая, коллекция cms-context-pages-cutted.txt,
        в которой будут отфильтрованы неиспользуемые виджеты
        """
        source_available_only = {
            'promo_id': 'Available',
            'available_mskus': [31, 32, 33, 34, 35]
        }

        data = CmsFiles()
        data.add_info(
            meta=dict(format='json', type='report_actions'),
            relation_id=1,
            context=create_promo(source_available_only)
        )
        data.add_info(
            meta=dict(format='text', type='not_report'),
            relation_id=2,
            context="Используемый текст, который должен присутствовать"
        )
        data.add_context_only(
            relation_id=3,
            context="Неиспользуемый текст, который должен быть исключен"
        )

        service = create_mbo_cms('valid_report_promo', data)
        service.update_service(names=[mbo.CMS_RELATIONS_NAME, mbo.CMS_CONTEXT_NAME, mbo.CMS_CONTEXT_NAME_CUTTED], lazy=False)

        context_path = os.path.join(ROOTDIR, 'mbo_cms', 'recent', mbo.CMS_CONTEXT_NAME)
        result_path = os.path.join(ROOTDIR, 'mbo_cms', 'recent', mbo.CMS_CONTEXT_NAME_CUTTED)

        # В изначальном файле от МБО может содержаться неиспользуемая информация
        with open(context_path, 'r') as context_file:
            context = context_file.read()
            self.assertNotEqual(context.find("1:{}".format(create_promo(source_available_only))), -1)
            self.assertNotEqual(context.find("2:Используемый текст, который должен присутствовать"), -1)
            self.assertNotEqual(context.find("3:Неиспользуемый текст, который должен быть исключен"), -1)

        # Неиспользуемая информация не должна уходить дальше геттера, чтобы не нагружать бекенды
        with open(result_path, 'r') as context_file:
            context = context_file.read()
            self.assertNotEqual(context.find("1:{}".format(create_promo(source_available_only))), -1)
            self.assertNotEqual(context.find("2:Используемый текст, который должен присутствовать"), -1)
            self.assertEqual(context.find("3:Неиспользуемый текст, который должен быть исключен"), -1)

    def check_invalid_promo(self, source, err_code):
        data = CmsFiles()
        data.add_info(
            meta=dict(format='json', type='report_actions'),
            relation_id=1,
            context=source
        )
        service = create_mbo_cms('invalid', data)

        with self.assertRaises(CmsPromoExtractException) as cm:
            service.update_service(names=[mbo.CMS_RELATIONS_NAME, mbo.CMS_CONTEXT_NAME, mbo.CMS_REPORT_PROMO_NAME], lazy=False)

        self.assertEqual(str(cm.exception), err_code + ". Action 1: {}".format(source))

    def test_invalid_promo_id(self):
        self.check_invalid_promo(create_promo({
            'available_mskus': [1]
        }), "Missed reqired field promo_id")

    def test_invalid_featured_msku_missed(self):
        self.check_invalid_promo(create_promo({
            'promo_id': 'good',
            'featured_mskus': [
                create_featured_msku(description='Descr12'),
            ],
        }), "Missed reqired field msku")

    def test_invalid_featured_msku_type(self):
        self.check_invalid_promo(create_promo({
            'promo_id': 'good',
            'featured_mskus': [
                create_featured_msku(msku='wrong type', description='Descr12'),
            ],
        }), "Field msku (value: wrong type) should be integer")

    def test_invalid_featured_picture(self):
        self.check_invalid_promo(create_promo({
            'promo_id': 'good',
            'featured_mskus': [
                create_featured_msku(msku=7, picture=1),
            ],
        }), 'Field picture (value: 1) should be string')

    def test_invalid_featured_description(self):
        self.check_invalid_promo(create_promo({
            'promo_id': 'good',
            'featured_mskus': [
                create_featured_msku(msku=7, description=7),
            ],
        }), 'Field description (value: 7) should be string')

    def test_invalid_forced_type(self):
        self.check_invalid_promo(create_promo({
            'promo_id': 'good',
            'forced_mskus': ['wrong_type']
        }), "Field msku (value: wrong_type) should be integer")

    def test_invalid_available_type(self):
        self.check_invalid_promo(create_promo({
            'promo_id': 'good',
            'available_mskus': ['wrong_type']
        }), "Field available_mskus (value: wrong_type) should be integer")

    def test_invalid_json_format(self):
        self.check_invalid_promo("NotJson", "Report action should be json")

if __name__ == '__main__':
    unittest.main()
