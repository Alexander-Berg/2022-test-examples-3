# -*- coding: utf-8 -*-

import logging
import pytest
import os

from lxml import etree

from report.base import BaseReportTest
from report.const import *


class TestImagesXML(BaseReportTest):

    @pytest.mark.parametrize(
        ('text', 'per_page', 'g'), [
            ('природа', 20, None),
            ('обои', 64, '1.ii.64.1.-1'),
        ])
    def test_images_text(self, query, class_static_data_dir, text, per_page, g):
        query.set_url(SEARCH_XML)
        query.set_internal()
        params = {'type': 'pictures', 'text': text}

        if g is not None:
            params['g'] = g

        query.set_params(params)

        resp = self.request(query)

        schema = etree.XMLSchema(file=os.path.join(class_static_data_dir, 'images.xsd'))
        parser = etree.XMLParser(schema=schema)
        root = etree.fromstring(resp.content, parser)

        groups_count = int(root.xpath('count(//response/results//grouping/group)'))
        assert groups_count == per_page

    @pytest.mark.parametrize(
        ('s_type'), [
            'cbir',
            'cbirlike'
        ])
    def test_images_cbir_url(self, query, class_static_data_dir, s_type):
        query.set_url(SEARCH_XML)
        query.set_internal()
        query.set_params({
            'url': 'http://avatars.mds.yandex.net/get-images-cbir/163435/Iq28XI4PSSPMLKGid86i3A/orig',
            'type': s_type,
        })

        resp = self.request(query)
        logging.info("test_images_cbir_url: content=\n%s\n\n========================", resp.content)

        schema = etree.XMLSchema(file=os.path.join(class_static_data_dir, 'images.xsd'))
        parser = etree.XMLParser(schema=schema)

        etree.fromstring(resp.content, parser)

    @pytest.mark.parametrize(
        ('s_type'), [
            'cbir',
            'cbirlike'
        ])
    def test_images_cbir_upload(self, query, class_static_data_dir, s_type):
        query.set_method('POST')
        query.set_url(SEARCH_XML)
        query.set_internal()
        query.set_content_type('multipart/form-data, image/jpeg')

        with open(os.path.join(class_static_data_dir, 'kotik.jpg')) as f:
            pic = f.read()

        query.set_post_body_multipart_form_data([('type', s_type)], [('upfile', 'pic.jpg', 'image/jpeg', pic)])

        resp = self.request(query)

        schema = etree.XMLSchema(file=os.path.join(class_static_data_dir, 'images.xsd'))
        parser = etree.XMLParser(schema=schema)

        etree.fromstring(resp.content, parser)
