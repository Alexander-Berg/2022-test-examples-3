# -*- coding: utf-8 -*-
from collections import defaultdict
from lxml import etree

from test.base import DiskTestCase

from mpfs.core.operations.stage import get_stage

with open('fixtures/xml/kladun1.xml') as fix_file:
    xml_tree1 = etree.fromstring(fix_file.read())
with open('fixtures/xml/kladun2.xml') as fix_file:
    xml_tree2 = etree.fromstring(fix_file.read())
with open('fixtures/xml/kladun_failed.xml') as fix_file:
    xml_tree_failed = etree.fromstring(fix_file.read())
with open('fixtures/xml/kladun_export_photo.xml') as fix_file:
    xml_tree_export_photo = etree.fromstring(fix_file.read())
with open('fixtures/xml/kladun_export_photo_failed.xml') as fix_file:
    xml_tree_export_photo_failed = etree.fromstring(fix_file.read())


class TestKladunStage(DiskTestCase):
    def setup_method(self, method):
        super(TestKladunStage, self).setup_method(method)
        self.stages_data = defaultdict(dict)
        for i, xml_tree in enumerate((xml_tree1, xml_tree2,
                                      xml_tree_failed, xml_tree_export_photo,
                                      xml_tree_export_photo_failed)):
            kladun_stages = xml_tree.find('stages')
            for stage in kladun_stages.getchildren():
                value = {
                    'result': dict(stage.find('result').items()),
                    'progress': dict(stage.find('progress').items()),
                    'status': stage.get('status'),
                    'details': dict(stage.find('details').items()),
                }
                stage_object = get_stage(stage.tag, value, i)
                self.stages_data[i][stage_object.name] = stage_object

    def test_preview_image(self):
        self.assertTrue('preview_image' not in self.stages_data[0])
        self.assertTrue('preview_image' in self.stages_data[1])

    def test_mulca_digest(self):
        tag_name = 'mulca_digest_upload'
        self.assertTrue(self.stages_data[0].get(tag_name) and self.stages_data[0].get(tag_name).details())
        self.assertTrue(self.stages_data[1].get(tag_name) and self.stages_data[1].get(tag_name).details())
        self.assertTrue(self.stages_data[2].get(tag_name) and self.stages_data[2].get(tag_name).details())

    def test_assert_preview_different(self):
        for k, v in self.stages_data[1].get('preview_image').details()['preview_sizes'].iteritems():
            self.assertTrue(v)
            self.assertNotEqual(v, self.stages_data[2].get('preview_image').details()['preview_sizes'].get(k))

    def test_assert_photo_export(self):
        self.assertEqual(self.stages_data[3].get('export_photo_0').details()['ind'], '0')
        self.assertEqual(self.stages_data[3].get('export_photo_1').details()['ind'], '1')

    def test_assert_photo_export_failed(self):
        self.assertEqual(self.stages_data[4].get('export_photo_0').details()['invalid_token'], '1')
        self.assertEqual(self.stages_data[4].get('export_photo_url_0').details()['invalid_token'], '1')
