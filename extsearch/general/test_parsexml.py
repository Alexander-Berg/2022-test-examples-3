from __future__ import print_function
import json
import os
import xml.etree.ElementTree as ETree

import extsearch.ymusic.scripts.reindex.parsexml as px

import extsearch.ymusic.scripts.reindex.ut.utils as test_common
import yatest.common as yc


def test__parse_localization_info():
    element = ETree.fromstring("""
    <root>
        <localized-info>
            <titles>
                <ru>ru-title</ru>
            </titles>
            <versions>
                <ru>ru-version</ru>
                <en>en-version</en>
            </versions>
        </localized-info>
    </root>
    """)
    localization_info = px.BaseEntityParser().parse_localization_info(element)
    assert localization_info.titles == {"ru": "ru-title"}
    assert localization_info.versions == {"ru": "ru-version", "en": "en-version"}


def test__parse_localization_info__empty():
    element = ETree.fromstring("""
    <root>
        <localized-info>
            <titles></titles>
            <versions></versions>
        </localized-info>
    </root>
    """)
    localization_info = px.BaseEntityParser().parse_localization_info(element)
    assert len(localization_info.titles) == 0
    assert len(localization_info.versions) == 0


def test__parse_track():
    out_file = os.path.join(yc.output_path(), 'out-track')
    return _parse_single_entity_from_file_and_save_canonical('track.xml', out_file)


def test__parse_album():
    out_file = os.path.join(yc.output_path(), 'out-album')
    return _parse_single_entity_from_file_and_save_canonical('album.xml', out_file)


def test__parse_artist():
    out_file = os.path.join(yc.output_path(), 'out-artist')
    return _parse_single_entity_from_file_and_save_canonical('artist.xml', out_file)


def _parse_single_entity_from_file_and_save_canonical(filename, canonical_file):
    entity = _parse_xml_from_file(filename)
    assert len(entity) == 1
    with open(canonical_file, 'w') as f:
        json.dump(entity[0], f, indent=4, sort_keys=True)
    return yc.canonical_file(canonical_file, local=True)


def _parse_xml_from_file(filename):
    data = test_common.read_test_data(filename)
    parsers = px.build_parsers()
    return px.parse_xml(data, parsers)
