import json

import pytest
from freezegun import freeze_time
from jose import jws

from sendr_utils import without_none
from sendr_utils.jwt import get_jwt_signature, get_public_key_copy

from hamcrest import assert_that, equal_to


@pytest.fixture
def jwk():
    return {
        'alg': 'ES256',
        'kty': 'EC',
        'crv': 'P-256',
        'x': 'LEBfQpwTDXJtLFiPcnYvGv-WaFXZGBnFP_yGhLL9MGc',
        'y': 'a1Or3ovkpH12b0o3ruZUtm_z8bg3xQtHXi-uPC7UJT0',
        'd': 'AEjwp7szhRxINz5SF_OKTMmefRbbteONK94nR9CeBEY',
        'kid': 'test-key',
    }


def test_public_key(jwk):
    key = get_public_key_copy(jwk)
    assert_that(
        key,
        equal_to(
            {
                'alg': 'ES256',
                'kty': 'EC',
                'crv': 'P-256',
                'x': 'LEBfQpwTDXJtLFiPcnYvGv-WaFXZGBnFP_yGhLL9MGc',
                'y': 'a1Or3ovkpH12b0o3ruZUtm_z8bg3xQtHXi-uPC7UJT0',
                'kid': 'test-key',
            }
        ),
    )


@pytest.mark.parametrize('ttl', (None, 1200))
@freeze_time('2021-12-31')
def test_sign(jwk, ttl):
    payload = {'data': 1, 'other-data': 'value'}
    key = get_public_key_copy(jwk)

    signature = get_jwt_signature(jwk, payload, ttl_seconds=ttl, extra_headers={'merchant_id': 'some-merchant-id'})

    headers = jws.get_unverified_headers(signature)
    decoded = jws.verify(signature, key, algorithms=['ES256'])

    expected_headers = without_none(
        {
            'alg': 'ES256',
            'iat': 1640908800,
            'exp': 1640910000 if ttl else None,
            'kid': 'test-key',
            'typ': 'JWT',
            'merchant_id': 'some-merchant-id',
        }
    )
    assert_that(headers, equal_to(expected_headers))
    assert_that(json.loads(decoded), equal_to(payload))
