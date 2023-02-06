#!/usr/bin/env python
# -*- coding: utf-8 -*-

import mock

from base64 import b64decode

from search.geo.tools.collections.lib.discovery_snippet import (
    discovery_2x_generator,
    make_discovery_2x_snippet,
)

import yandex.maps.proto.common2.metadata_pb2 as pb_metadata
import yandex.maps.proto.search.discovery_2x_pb2 as pb_discovery_2x


def test_make_discovery_2x_snippet__given_valid_colllection__returns_snippet():
    collection = mock.Mock()
    collection.id = 'worldwide-cuisine'
    collection.title = 'From Italy to China'
    collection.author = {
        'name': 'Chips Journal',
        'favicon': {'urlTemplate': 'chips-favicon'}
    }

    snippet = b64decode(make_discovery_2x_snippet([collection]))

    metadata = pb_metadata.Metadata()
    metadata.ParseFromString(snippet)
    extension = metadata.Extensions[pb_discovery_2x.GEO_OBJECT_METADATA]

    assert extension is not None
    assert len(extension.collection) == 1

    result = extension.collection[0]
    assert result.uri == 'ymapsbm1://collection?id={}'.format(collection.id)
    assert result.seoname == collection.id
    assert result.title == collection.title
    assert result.author.name == collection.author['name']
    assert result.author.favicon.url_template == \
        collection.author['favicon']['urlTemplate']


@mock.patch('search.geo.tools.collections.lib.discovery_snippet.make_discovery_2x_snippet')
def test_discovery_2x_generator__given_valid_collections__yields_snippet_for_every_org(
    make_discovery_2x_snippet
):

    def make_collection(_id, permalinks):
        collection = mock.Mock()
        collection.id = _id
        collection.organizations = [{'oid': it} for it in permalinks]
        return collection

    make_discovery_2x_snippet.side_effect = \
        lambda collections: ','.join(it.id for it in collections)

    collections = [
        make_collection('first', ['1', '2']),
        make_collection('second', ['2', '3'])
    ]

    result = list(discovery_2x_generator(collections))

    assert len(result) == 3
    assert {'permalink': '1', 'value': 'first'} in result
    assert {'permalink': '2', 'value': 'first,second'} in result
    assert {'permalink': '3', 'value': 'second'} in result


def test_discovery_2x_generator__given_empty_collections__yields_nothing():
    result = list(make_discovery_2x_snippet([]))

    assert not result


def test_discovery_2x_generator__given_empty_orgs__yields_nothing():
    collection = mock.Mock()
    collection.organizations = []

    result = list(discovery_2x_generator([collection]))

    assert not result
