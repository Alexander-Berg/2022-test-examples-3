# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest
from datetime import date

from travel.buses.connectors.tests.yabus.common.library.test_utils import matching_provider_patch

from yabus.etraffic.segments_provider import segments_provider

from yabus.etraffic.entities.book import Book


class TestClient(object):

    def test_search_without_segments(self, etraffic_client, etraffic_converter):
        test_date = date(2000, 1, 1)
        with matching_provider_patch({}):
            etraffic_client.search("c1", "c2", test_date)

        etraffic_client.call.assert_has_calls(
            (
                mock.call("getRaces", ["map(c1, use_relations=True)", "map(c2, use_relations=True)", test_date]),
                mock.call("getRaces", ["map(c1, use_relations=True)", "map_to_children(c2)", test_date]),
                mock.call("getRaces", ["map_to_children(c1)", "map(c2, use_relations=True)", test_date]),
                mock.call("getRaces", ["map_to_children(c1)", "map_to_children(c2)", test_date]),
            ),
            any_order=True,
        )

    def test_search_with_segments(self, etraffic_client, etraffic_converter):
        test_date = date(2000, 1, 1)

        with mock.patch.object(segments_provider, 'get_segments', return_value={
            ("map_to_children(c1)", "map(c2, use_relations=True)"),
            ("map_to_children(c1)", "map_to_children(c2)"),
        }), matching_provider_patch({}):
            etraffic_client.search("c1", "c2", test_date)

            etraffic_client.call.assert_has_calls(
                (
                    mock.call("getRaces", ["map_to_children(c1)", "map(c2, use_relations=True)", test_date]),
                    mock.call("getRaces", ["map_to_children(c1)", "map_to_children(c2)", test_date]),
                ),
                any_order=True,
            )

    @pytest.mark.parametrize('doc_data, expected_data', (
        (
            {'docNumber': '6513738292', 'docTypeCode': 'Недействительный код', 'docTypeId': '1'},
            {'docNum': '6513738292', 'docSeries': ''}
        ),
        (
            {'docNumber': '6513738292', 'docTypeCode': '0'},
            {'docNum': '6513738292', 'docSeries': ''}
        ),
        (
            {'docNumber': '738292', 'docTypeCode': '0', 'docSeries': '6513', 'docTypeId': '1'},
            {'docNum': '738292', 'docSeries': '6513'}
        ),
        (
            {'docNumber': 'IVМЮ123456', 'docTypeCode': '4', 'docTypeId': 2},
            {'docNum': '123456', 'docSeries': 'IV-МЮ'}
        ),
        (
            {'docNumber': 'III-МЮ123456', 'docTypeCode': '4', 'docTypeId': 2},
            {'docNum': '123456', 'docSeries': 'III-МЮ'}
        ),
        (
            {'docNumber': 'I-МЮ123456', 'docTypeCode': '14', 'docTypeId': 2},
            {'docNum': '123456', 'docSeries': 'I-МЮ'}
        ),
        (
            {'docNumber': 'IV-МЮ123456', 'docTypeCode': '2', 'docTypeId': 2},
            {'docNum': '123456', 'docSeries': 'IV-МЮ'}
        ),
        (
            {'docNumber': '123456', 'docTypeCode': '4', 'docSeries': 'IV-МЮ', 'docTypeId': 2},
            {'docNum': '123456', 'docSeries': 'IV-МЮ'}
        ),
        (
            {'docNumber': '123456', 'docTypeCode': '4', 'docSeries': 'IVМЮ', 'docTypeId': 2},
            {'docNum': '123456', 'docSeries': 'IV-МЮ'}
        )
    ))
    def test_book_document_transform(self, doc_data, expected_data):
        result = Book.init(dict({
            'firstName': 'Ivan',
            'lastName': 'Ivanov',
            'middleName': 'Ivanovich',
            'birthDate': '2000-01-01',
            'genderCode': 1,
            'citizenshipCode': 'RU',
            'seatCode': '1',
            'docSeries': ''
        }, **doc_data))
        assert result['docNum'] == expected_data['docNum']
