"""
    Tests for gencfg module
"""

import pytest
import requests_mock

from search.mon.wabbajack.libs.modlib.modules import gencfg
from search.mon.wabbajack.libs.modlib.api_wrappers.gencfg import exceptions
from . import CARD_JSON

TEST_GROUP = 'TICKENATOR'
TEST_TAG = 'trunk'

VALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/groups/TICKENATOR/card'
INVALID_URL = 'http://api.gencfg.yandex-team.ru/trunk/groups/TICKENATOR_404/card'


@pytest.mark.parametrize(
    'args', [
        {
            'fkwargs': {
                'group': TEST_GROUP,
            },
            'expected': CARD_JSON
        },
        {
            'fkwargs': {
                'group': TEST_GROUP,
                'tag': TEST_TAG,
            },
            'expected': CARD_JSON,
        },
        {
            'fkwargs': {
                'group': TEST_GROUP,
                'tag': TEST_TAG,
                'items': ['owners'],
            },
            'expected': {
                'group': TEST_GROUP,
                'tag': TEST_TAG,
                'items': {'owners': CARD_JSON['owners']}
            }
        },
        {
            'fkwargs': {
                'group': TEST_GROUP,
                'tag': TEST_TAG,
                'items': ['reqs.instances']
            },
            'expected': {
                'group': TEST_GROUP,
                'tag': TEST_TAG,
                'items': {'instances': CARD_JSON['reqs']['instances']}
            }
        },
        {
            'fkwargs': {
                'group': TEST_GROUP,
                'tag': TEST_TAG,
                'items': ['owners', 'reqs']
            },
            'expected': {
                'group': TEST_GROUP,
                'tag': TEST_TAG,
                'items': {'owners': CARD_JSON['owners'], 'reqs': CARD_JSON['reqs']}
            }
        }
    ]
)
def test_card(args):
    """
        Test gencfg.card function
    """
    with requests_mock.Mocker() as m:
        m.register_uri('GET', VALID_URL, json=CARD_JSON)
        assert gencfg.card(**args['fkwargs']) == args['expected']


def test_card_error():
    with requests_mock.Mocker() as m:
        m.register_uri('GET', INVALID_URL, json={}, status_code=404)
        with pytest.raises(exceptions.EGencfgGroupNotFound):
            gencfg.card(group='TICKENATOR_404')
