#!/usr/bin/python
# -*- coding: utf-8 -*-

from guruindexer.calc_gurulight_desc import dump_mbo_category
import market.pylibrary.mbostuff.mbomodels as mbo

from market.proto.content.mbo import MboParameters_pb2


def _get_test_mbo_category():
    category = MboParameters_pb2.Category()

    category.hid = 1

    param = category.parameter.add()
    param.id = 2
    param.description = 'description'
    param.xsl_name = 'xsl_name'

    return mbo.MboCategory(category)


def test_xml_generation():
    category = _get_test_mbo_category()

    json = dict()
    xml = dump_mbo_category(category, json)

    expected_xml = '<category id="1"><param id="2">description</param></category>'

    assert xml == expected_xml


def test_json_generation():
    category = _get_test_mbo_category()

    json = dict()
    dump_mbo_category(category, json)

    expected_json = {
        2: 'description'
    }

    assert expected_json == json
