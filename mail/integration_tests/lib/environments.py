# -*- coding: utf-8 -*-

URL = {
    'testing': {
        'internal': {
            'scheme': 'https',
            'host': 'payments-test.mail.yandex.net',
            'path': 'v1/internal'
        },
        'client': {
            'scheme': 'https',
            'host': 'payments-test.mail.yandex.net',
            'path': 'v1'
        }
    },
    'production': {
        'internal': {
            'scheme': 'https',
            'host': 'payments.mail.yandex.net',
            'path': 'v1/internal',
        },
        'client': {
            'scheme': 'https',
            'host': 'payments.mail.yandex.net',
            'path': 'v1',
        } 
    }
}

TVM = {
    'testing': {
        'self_client_id': 1,
        'api_client_id': 2002162
    },
    'production': {
        'self_client_id': 1,
        'api_client_id': 1
    }
}

MERCHANT_DRAFT_DATA = {
  "description": "Integration Tests Merchant Creation",
  "entity_id": "Integration Tests Merchant Creation 1",
  "organization": {
    "type": "ooo",
    "inn": "5043041353",
    "kpp": "504301001",
    "ogrn": "1234567890123",
    "siteUrl": "pay.yandex.ru",
    "name": "Yandex",
    "fullName": "Hoofs & Horns",
    "englishName": "HH",
    "scheduleText": "с 9 до 6"
  },
  "bank": {
    "account": "40702810700190000201",
    "bik": "044583503",
    "correspondentAccount": "12345678901234567890",
    "name": "Tinkoff"
  },
  "name": "Test merchant",
  "addresses": {
    "legal": {
      "home": "16",
      "zip": "119021",
      "street": "Льва Толстого",
      "country": "RUS",
      "city": "Москва"
    },
    "post": {
      "home": "16",
      "zip": "119021",
      "street": "Льва Толстого",
      "country": "RUS",
      "city": "Москва"
    }
  },
  "username": "pay-testuser",
  "persons": {
    "ceo": {
      "phone": "+71111111111",
      "patronymic": "Patronymic",
      "birthDate": "1900-01-02",
      "email": "pay-testuser@yandex.ru",
      "name": "Name",
      "surname": "Surname"
    },
    "contact": {
      "phone": "+711_phone",
      "patronymic": "Patronymic",
      "birthDate": "1900-01-02",
      "email": "pay-testuser@yandex.ru",
      "name": "Name",
      "surname": "Surname"
    },
    "signer": {
      "phone": "+711_phone",
      "patronymic": "Patronymic",
      "birthDate": "1900-01-02",
      "email": "pay-testuser@yandex.ru",
      "name": "Name",
      "surname": "Surname"
    }
  }
}
