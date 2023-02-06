# coding: utf8

import pytest

good_requests = [
    {
        "backend_proto": "https",
        "service_name": "partner",
        "fallback_pool": "partner-front--mp-10469-prestable-fix-popular-assortment-x5",
        "vhost": "partner-front--mp-10469-prestable-fix-popular-assortment",
        "web_sockets": False,
        "host": "2a02:6b8:c0d:3c07:10c:db40:0:262a",
        "port": 9770
    },
    {
        "vhost": "red-front--redmarketfront-1839",
        "host": "2a02:6b8:c0d:348a:10c:f597:0:4821",
        "port": 18465,
        "web_sockets": True,
        "backend_proto": "https",
        "service_name": "market"
    },
]


@pytest.mark.parametrize("request_json", good_requests)
def test_add_with_valid_inputs(client, request_json):
    """ Smoke test. Валидный запрос должен проходить валидацию. """
    response = client.post(
        '/add',
        json=request_json,
    )
    assert response.status_code == 200


bad_fields = (
    ('backend_proto', ''),
    ('backend_proto', 'ftp'),
    ('service_name', ''),
    ('service_name', '123'),
    ('fallback_pool', "${envName}"),
    ('vhost', ''),
    ('port', 'abc'),
)


@pytest.mark.parametrize("bad_field", bad_fields)
def test_add_with_invalid_requests(client, bad_field):
    """ CSADMIN-30480 Значение fallback_pool может содержать только буквы и подчеркивания,
    чтобы получился валидный lua конфиг
    """
    request_json = {
        "backend_proto": "https",
        "service_name": "market",
        "fallback_pool": "mbo--mbo06",
        "vhost": "mbo--mbo06",
        "host": "2a02:6b8:c1c:187:10b:11e5:242:0",
        "port": 80,
    }
    bad_key, bad_value = bad_field
    request_json[bad_key] = bad_value

    response = client.post('/add', json=request_json)
    assert response.status_code == 400, "Request with %r=%r didn't fail" % bad_field
    assert bad_value in response.data


def test_delete_existing_balancer(client):
    response = client.post('/add', json={
        "backend_proto": "https",
        "service_name": "market",
        "fallback_pool": "mbo--mbo06",
        "web_sockets": True,
        "vhost": "mbo--mbo06",
        "host": "2a02:6b8:c1c:187:10b:11e5:242:0",
        "port": 80,
    })
    assert response.status_code == 200

    response = client.post('/delete', json={
        "vhost": "mbo--mbo06",
    })
    assert response.status_code == 200


def test_delete_nonexistent_balancer(client):
    response = client.post('/delete', json={
        "vhost": "missing-vhost",
    })
    assert response.status_code == 404
