# -*- coding: utf-8 -*-

import json

to_json = json.loads


def post_dataapi_api_batch_get_profile_geopoints_address_id_get_profile_batch_addresses_geopoints():
    """
    Батчевый запрос с 3 запросами внутри:
    * Запросить геопоинт
    * Запросить все адреса и геопоинты (ходит в ручку /profile/batch)
    * Запросить адрес
    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json"
    -d '{"items":[{"body":"{}","headers":{"Content-type":"application/json"},"method":"GET",
    "relative_url":"/profile/geopoints/work=home=0?__uid=503881259"},{"body":"{}",
    "headers":{"Content-type":"application/json"},"method":"GET",
    "relative_url":"/profile/batch?actions=addresses\\,geopoints&__uid=503881259"},
    {"body":"{}","headers":{"Content-type":"application/json"},"method":"GET",
    "relative_url":"/profile/addresses/work?__uid=503881259"}]}'

    """
    return {
        "invocationInfo": {
            "hostname": "dataapi01f.dst.yandex.net",
            "action": "batchAll",
            "app-name": "dataapi",
            "app-version": "100.285.0.3",
            "req-id": "suRN20Uk",
            "exec-duration-millis": "13"
        },
        "result": [
            {
                "code": 200,
                "result": {
                    "title": "transit",
                    "latitude": 55.749776,
                    "longitude": 37.617068,
                    "geopoint_id": "work=home=0",
                    "tags": [
                        "_w-_traffic-1-transit",
                        "_hidden"
                    ]
                }
            },
            {
                "code": 200,
                "result": [
                    {
                        "code": 200,
                        "result": {
                            "items": [
                                {
                                    "title": "Москва",
                                    "latitude": 55.753219,
                                    "longitude": 37.62251,
                                    "address_id": "work",
                                    "address_line": "Россия, Москва",
                                    "address_line_short": "Москва",
                                    "entrance_number": "2",
                                    "custom_metadata": '{"smth": "meta"}',
                                    "created": 1507557844163,
                                    "modified": 1507557844163,
                                    "last_used": 1507557844163,
                                    "tags": [
                                        "work"
                                    ],
                                    "mined_attributes": []
                                }
                            ]
                        }
                    },
                    {
                        "code": 200,
                        "result": {
                            "items": [
                                {
                                    "title": "Moscow",
                                    "latitude": 55.753219,
                                    "longitude": 37.62251,
                                    "geopoint_id": "work",
                                    "tags": [
                                        "work"
                                    ]
                                },
                                {
                                    "title": "transit",
                                    "latitude": 55.749776,
                                    "longitude": 37.617068,
                                    "geopoint_id": "work=home=0",
                                    "tags": [
                                        "_w-_traffic-1-transit",
                                        "_hidden"
                                    ]
                                }
                            ]
                        }
                    }
                ]
            },
            {
                "code": 200,
                "result": {
                    "title": "Москва",
                    "latitude": 55.753219,
                    "longitude": 37.62251,
                    "address_id": "work",
                    "address_line": "Россия, Москва",
                    "address_line_short": "Москва",
                    "entrance_number": "2",
                    "custom_metadata": '{"smth": "meta"}',
                    "created": 1507557844163,
                    "modified": 1507557844163,
                    "last_used": 1507557844163,
                    "tags": ["work"],
                    "mined_attributes": []
                }
            }
        ]
    }


def post_dataapi_api_batch_get_generic_profile():
    """
    Пример:
    curl -v -X POST "http://dataapi07f.disk.yandex.net:21859/api/batch"
    -d '{"items":[{"headers":{"Content-type":"application/json"},"method":"POST",
    "relative_url":"/api/process_generic_data/?http_method=GET&resource_path=morda/usersettings&__uid=547671314
    &cheburek=true"}]}' -H "Content-type: application/json"
    """
    return {
        "invocationInfo":
            {
                "hostname": "dataapi07f.disk.yandex.net",
                "action": "batchAll",
                "app-name": "dataapi",
                "app-version": "100.281.2",
                "req-id": "M6jRjsDN",
                "exec-duration-millis": "6"
            },
        "result": [
            {
                "code": 200,
                "result": {
                    "items": [{"top": "kek"}]
                }
            }
        ]
    }


def post_dataapi_api_batch_get_profile_addresses():
    """
    Получить все адреса пользователя в dataapi.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{}","method":"GET","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses?__uid=503881259"}]}'
    """
    return to_json('''{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"v7B8upuE","exec-duration-millis":"13"},"result":[{"code":200,"result":{"items":[{"title":"Дом","latitude":57.2757987976,"longitude":55.4513816833,"address_id":"home","address_line":"Россия, Пермский край, Оса, улица Пугачёва, 22","address_line_short":"улица Пугачёва, 22","entrance_number":"2","custom_metadata":"{'smth': 'meta'}","created":1508170446555,"modified":1508170446555,"last_used":1508170446555,"tags":[],"mined_attributes":[]}]}}]}''')


def post_dataapi_api_batch_post_profile_addresses():
    """
    Создать в dataapi адрес.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{\"address_id\":\"home\",\"title\":\"Дом\",\"longitude\":55.4513816833,\"address_line_short\":\"улица Пугачёва, 22\",\"latitude\":57.2757987976,\"address_line\":\"Россия, Пермский край, Оса, улица Пугачёва, 22\"}","method":"POST","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses?__uid=503881259"}]}'
    """
    return to_json('''{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"K0j60rry","exec-duration-millis":"37"},"result":[{"code":200,"result":{"title":"Дом","latitude":57.2757987976,"longitude":55.4513816833,"address_id":"home","address_line":"Россия, Пермский край, Оса, улица Пугачёва, 22","address_line_short":"улица Пугачёва, 22","entrance_number":"2","custom_metadata":"{'smth': 'meta'}","created":1508169969793,"modified":1508169969793,"last_used":1508169969793,"tags":[],"mined_attributes":[]}}]}''')


def post_dataapi_api_batch_get_profile_addresses_address_id():
    """
    Получить инфу по конкретному адресу.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{}","method":"GET","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses/home?__uid=503881259"}]}'
    """
    return to_json('''{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"UHY8phJm","exec-duration-millis":"7"},"result":[{"code":200,"result":{"title":"Дом","latitude":57.2757987976,"longitude":55.4513816833,"address_id":"home","address_line":"Россия, Пермский край, Оса, улица Пугачёва, 22","address_line_short":"улица Пугачёва, 22","entrance_number":"2","custom_metadata":"{'smth': 'meta'}","created":1508170446555,"modified":1508170446555,"last_used":1508170446555,"tags":[],"mined_attributes":[]}}]}''')


def post_dataapi_api_batch_patch_profile_adresses_address_id():
    """
    Изменить конкретный адрес частично.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{\"address_line\":\"Россия, Пермский край, Оса, улица Пугачёва, 23\"}","method":"PATCH","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses/home?__uid=503881259"}]}'
    """
    return to_json('{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"9LIOQpUU","exec-duration-millis":"26"},"result":[{"code":200,"result":{}}]}')


def post_dataapi_api_batch_delete_profile_adresses_address_id():
    """
    Удалить конкретный адрес.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{}","method":"DELETE","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses/home?__uid=503881259"}]}'
    """
    return to_json('{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"8bC4vJY6","exec-duration-millis":"27"},"result":[{"code":200,"result":{}}]}')


def post_dataapi_batch_put_profile_addresses_address_id():
    """
    Изменить конкретный адрес не частично.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{\"title\":\"Дом2\",\"longitude\":55.4513816833,\"address_line_short\":\"улица Пугачёва, 23\",\"latitude\":57.2757987976,\"address_line\":\"Россия, Пермский край, Оса, улица Пугачёва, 23\"}","method":"PUT","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses/home?__uid=503881259"}]}'
    """
    return to_json('''{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"jDYdBJ7J","exec-duration-millis":"44"},"result":[{"code":200,"result":{"title":"Дом2","latitude":57.2757987976,"longitude":55.4513816833,"address_id":"home","address_line":"Россия, Пермский край, Оса, улица Пугачёва, 23","address_line_short":"улица Пугачёва, 23","entrance_number":"2","custom_metadata":"{'smth': 'meta'}","created":1508170446555,"modified":1508173778756,"last_used":1508170446555,"tags":[],"mined_attributes":[]}}]}''')


def post_dataapi_batch_put_profile_addresses_address_id_touch():
    """
    Обновить дату последнего использования адреса.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{}","method":"PUT","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses/home/touch?__uid=503881259"}]}'
    """
    return to_json('{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"MJVJlLhu","exec-duration-millis":"31"},"result":[{"code":200,"result":{}}]}')


def post_dataapi_batch_put_profile_addresses_address_id_tag():
    """
    Добавить теги к адресу.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{}","method":"PUT","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses/home/tag?__uid=503881259&tag=lol,kek,cheburek"}]}'
    """
    return to_json('{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"iTjl4j3f","exec-duration-millis":"44"},"result":[{"code":200,"result":{}}]}')


def post_dataapi_batch_put_profile_addresses_address_id_untag():
    """
    Удалить теги из адреса.

    Пример:
    curl -v -X POST "http://dataapi01f.dst.yandex.net:21859/api/batch" -H "Content-type: application/json" -d '{"items":[{"body":"{}","method":"PUT","headers":{"Content-type": "application/json"},"relative_url":"/profile/addresses/home/untag?__uid=503881259&tag=lol,cheburek"}]}'
    """
    return to_json('{"invocationInfo":{"hostname":"dataapi01f.dst.yandex.net","action":"batchAll","app-name":"dataapi","app-version":"100.285.0.3","req-id":"kTQMVpif","exec-duration-millis":"52"},"result":[{"code":200,"result":{}}]}')
