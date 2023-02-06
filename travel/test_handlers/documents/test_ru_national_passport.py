# coding=utf-8
import json
import pytest

from travel.avia.travelers.tests.conftest import USER_UID


@pytest.mark.gen_test
def test_documents_get(http_client, base_url, faker, header, data_sync_client):
    path = '/travelers/{}/passengers/{}/documents'.format(USER_UID, faker.uuid4())
    response = yield http_client.fetch(base_url + path, headers=header)

    assert response.code == 200, response.body


@pytest.mark.gen_test
def test_documents_post(http_client, base_url, faker, header, geobase):
    citizenship = faker.pyint()
    geobase.save(id=citizenship)
    document_params = _create_document_params(citizenship)

    path = '/travelers/{}/passengers/{}/documents'.format(USER_UID, faker.uuid4())
    response = yield http_client.fetch(
        base_url + path,
        method='POST',
        headers=header,
        raise_error=False,
        body=json.dumps(document_params),
    )

    assert response.code == 200, response.body


def _create_document_params(citizenship=None):
    return dict(
        title='Мой паспорт РФ',
        type='ru_national_passport',
        first_name='Иван',
        middle_name='Иванович',
        last_name='Иванов',
        number='0123456789',
        citizenship=citizenship,
    )


@pytest.mark.gen_test
def test_documents_post_should_validation_error(http_client, base_url, faker, header, geobase):
    document_params = _create_document_params()
    document_params['number'] = '123'

    path = '/travelers/{}/passengers/{}/documents'.format(USER_UID, faker.uuid4())
    response = yield http_client.fetch(
        base_url + path,
        method='POST',
        headers=header,
        raise_error=False,
        body=json.dumps(document_params),
    )

    assert response.code == 400, response.body
    assert json.loads(response.body.decode('utf8')) == {'citizenship': ['Required field'], 'number': ['Wrong field']}
