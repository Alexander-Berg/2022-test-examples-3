# coding=utf-8
import json
from mock import MagicMock
import yt.wrapper as yt
import pytest

import judgements
from test_utils import create_serp, create_component
from enrichments import SINSIG_ENRICHMENT, IMAGES_RELEVANCE_ENRICHMENT, RELEVANCE_ENRICHMENT


QUERY = {
    'text': 'test',
    'country': 'RU',
    'device': 'DESKTOP',
    'region': {'id': 1}
}

COMPONENT = create_component(url='http://test')
IMAGES_QUERY = {
    'text': 'test',
    'country': 'RU',
    'device': 'DESKTOP',
    'region': {'id': 1},
    "params": [
        {
            'name': 'query_date',
            'value': 'testdate'
        },
        {
            'name': 'queryfresh',
            'value': 'testfresh'
        }
    ]
}
IMAGES_COMPONENT = {
    'url.imageBigThumbHref': 'http://test',
    'url.mimcaMdsUrl': 'http://mimcaurl',
    'long.MIMCA_CRC64': 1,
    'dimension.MIMCA_IMAGE_DIMENSION': {'w': 1, 'h': 2},
    'componentUrl': {'pageUrl': 'http://pageurl'}
}

JI = {
    'query': 'test',
    'region_id': 1,
    'country': 'RU',
    'device': 'DESKTOP',
    'url': 'http://test'
}
IMAGE_JI = {
    'query': 'test',
    'region_id': 1,
    'country': 'RU',
    'device': 'DESKTOP',
    'image_url': 'test/'
}

IMAGE_HITMAN_JI = {
    'NULLABLE.COMPONENT.url.mimcaMdsUrl': "http://mimcaurl",
    "NULLABLE.COMPONENT.long.MIMCA_CRC64": 1,
    'component_page_url': "http://pageurl",
    "query_text": "test",
    "query_region_id": 1,
    "component_image_url": "http://test",
    "query_country": "RU",
    "NULLABLE.COMPONENT.dimension.IMAGE_DIMENSION": {
        "h": 2,
        "w": 1
    },
    "query_device": "DESKTOP",
    "component_mimca_image_url": "http://test",
    "NULLABLE.SERP.serp_query_param.query_date": "testdate",
    "NULLABLE.SERP.serp_query_param.queryfresh": "testfresh"
}
RELEVANCE_ASSESSMENT = {'relevance': 'RELEVANT_PLUS'}


def check_relevance(component):
    return component[judgements.RELEVANCE_SCALE]['name'] == "RELEVANT_PLUS"


def check_images_relevance(component):
    return component[judgements.IMAGES_RELEVANCE_SCALE]['name'] == "RELEVANT_PLUS"


SINSIG_TAG = "web_world_validate.201810"


SINSIG_QUERY = {
    "text": "кто польжунтся маслом с проблемной кожи",
    "country": "BY",
    "device": "IPHONE",
    "region": {"id": 157},
    "params": [
        {
            "name": "query_group_tag",
            "value": SINSIG_TAG
        }
    ]
}
SINSIG_COMPONENT = create_component(url="https://irecommend.ru/content/eto-voobshche-zakonno-ispolzovat-maslo-dlya-zhirnoi-problemnoi-kozhi-istoriya-o-tom-kak-ya-n")
SINSIG_JI = {
    "qurl_id": "ceb88c18d54c7324565726eafc4edef02fbd8f5efaf2419bdf111b48"
}
SINSIG_ASSESSMENT = {
    "slider_values": [
        -1.0,
        -1.0,
        30.143064852500000228
    ],
    "judgement_values": [
        "RELEVANCE_MINUS_GOOD",
        "RELEVANCE_MINUS_GOOD",
        "SLIDER_GRADE"
    ],
    "spec_scores": [
        2.619474618599999971,
        5.2019770203000001985,
        1.1131916373999999337
    ]
}


def check_sinsig(component):
    v1 = json.loads(component[judgements.JUDGEMENTS_SINSIG_JUDGEMENT_VALUES]["name"]) == SINSIG_ASSESSMENT["judgement_values"]
    v2 = json.loads(component[judgements.JUDGEMENTS_SINSIG_SLIDER_VALUES]["name"]) == SINSIG_ASSESSMENT["slider_values"]
    v3 = json.loads(component[judgements.JUDGEMENTS_SINSIG_SPEC_SCORES]["name"]) == SINSIG_ASSESSMENT["spec_scores"]
    return v1 and v2 and v3

ENRICHMENT = RELEVANCE_ENRICHMENT
IMAGES_ENRICHMENT = IMAGES_RELEVANCE_ENRICHMENT


def test_add_judgements_empty():
    yt.lookup_rows = MagicMock()

    ENRICHMENT.add_judgements([])

    yt.lookup_rows.assert_not_called()


def test_skip_not_valid_components():
    yt.lookup_rows = MagicMock()

    ENRICHMENT.add_judgements([create_serp([{}], query=QUERY)])

    yt.lookup_rows.assert_not_called()


def test_add_missing_judgements():
    hash = ENRICHMENT.get_hash(JI, lambda x: x)
    judgement = {'hash': hash, 'assessment_result': {}}
    yt.lookup_rows = MagicMock(return_value=[judgement])
    serp = create_serp(query=QUERY, components=[COMPONENT.copy()])

    ENRICHMENT.add_judgements([serp])

    assert judgements.RELEVANCE_SCALE not in serp.components[0]


def test_looks_like_judgement():
    assert judgements.looks_like_judgement({judgements.HIT_TYPE_FIELD: 'test_hit_type'})


def test_dont_looks_like_judgement():
    assert not judgements.looks_like_judgement({'k': 'v'})


def test_get_ji():
    assert ENRICHMENT.get_ji(QUERY, COMPONENT) == JI


def test_get_ji_no_url():
    assert ENRICHMENT.get_ji(QUERY, {}) is None


def test_get_ji_no_query():
    assert ENRICHMENT.get_ji({}, COMPONENT) is None


def test_get_hitman_ji():
    assert ENRICHMENT.get_hitman_ji(QUERY, COMPONENT) ==\
        {
            'query_text': 'test',
            'query_region_id': 1,
            'query_country': 'RU',
            'query_device': 'DESKTOP',
            'component_page_url_or_site_link_url': 'http://test',
            'NULLABLE.SERP.query_param.navmx_tmp': None
        }


def test_get_hitman_ji_navmx():
    query = QUERY.copy()
    query['params'] = [{'name': 'navmx_tmp', 'value': '0.5'}]
    assert ENRICHMENT.get_hitman_ji(query, COMPONENT) ==\
        {
            'query_text': 'test',
            'query_region_id': 1,
            'query_country': 'RU',
            'query_device': 'DESKTOP',
            'component_page_url_or_site_link_url': 'http://test',
            'NULLABLE.SERP.query_param.navmx_tmp': 0.5
        }


def test_get_hitman_ji_no_url():
    assert ENRICHMENT.get_hitman_ji(QUERY, {}) is None


def test_get_hitman_ji_no_query():
    assert ENRICHMENT.get_hitman_ji({}, COMPONENT) is None


def test_get_hitman_ji_sinsig():
    assert SINSIG_ENRICHMENT.get_hitman_ji(SINSIG_QUERY, SINSIG_COMPONENT) == {
        'query_text': SINSIG_QUERY['text'],
        'query_region_id': SINSIG_QUERY['region']['id'],
        'query_country': SINSIG_QUERY['country'],
        'query_device': SINSIG_QUERY['device'],
        'component_page_url': SINSIG_COMPONENT['componentUrl']['pageUrl'],
        'SERP.query_param.query_group_tag': SINSIG_TAG
    }


def test_get_hitman_ji_images():
    assert IMAGES_ENRICHMENT.get_hitman_ji(IMAGES_QUERY, IMAGES_COMPONENT) == IMAGE_HITMAN_JI


def test_get_image_url_empty():
    assert judgements.get_image_url({}) is None


def test_get_image_url_big_thumb():
    assert judgements.get_image_url(IMAGES_COMPONENT) == 'http://test'


def test_get_image_url_candidates():
    assert judgements.get_image_url({'imageadd': {'candidates': ['url']}}) == 'url'


def test_images_get_ji():
    assert IMAGES_ENRICHMENT.get_ji(QUERY, IMAGES_COMPONENT) == IMAGE_JI


@pytest.mark.parametrize('enrichment, ji, component, check', [
    (ENRICHMENT, JI, COMPONENT, check_relevance),
    (IMAGES_ENRICHMENT, IMAGE_JI, IMAGES_COMPONENT, check_images_relevance)
])
def test_add_judgements(enrichment, ji, component, check):
    hash = enrichment.get_hash(ji, lambda x: x)
    judgement = {'hash': hash, 'assessment_result': RELEVANCE_ASSESSMENT}
    yt.lookup_rows = MagicMock(return_value=[judgement])
    serp = create_serp(query=QUERY, components=[component.copy()])

    enrichment.add_judgements([serp])

    assert check(serp.components[0])


@pytest.mark.parametrize('enrichment, ji, assessment, query, component, check', [
    (ENRICHMENT, JI, RELEVANCE_ASSESSMENT, QUERY, COMPONENT, check_relevance),
    (IMAGES_ENRICHMENT, IMAGE_JI, RELEVANCE_ASSESSMENT, QUERY, IMAGES_COMPONENT, check_images_relevance),
    (SINSIG_ENRICHMENT, SINSIG_JI, SINSIG_ASSESSMENT, SINSIG_QUERY, SINSIG_COMPONENT, check_sinsig)
])
def test_add_judgements_for_two_same_serps(enrichment, ji, assessment, query, component, check):
    hash = enrichment.get_hash(ji, lambda x: x)
    judgement = {'hash': hash, 'assessment_result': assessment}
    yt.lookup_rows = MagicMock(return_value=[judgement])
    serp = create_serp(query=query, components=[component.copy()])
    serp_copy = create_serp(query=query, components=[component.copy()])

    enrichment.add_judgements([serp, serp_copy])

    assert check(serp.components[0])
    assert check(serp_copy.components[0])
