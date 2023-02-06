# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import requests_mock

from search.martylib.abc.client import AbcClient
from search.martylib.abc.exceptions import ServiceNotFound
from search.martylib.proto.structures import abc_pb2
from search.martylib.test_utils import TestCase


# noinspection SpellCheckingInspection
@requests_mock.Mocker()
class TestProtoInterface(TestCase):
    @classmethod
    def setUpClass(cls):
        super(TestProtoInterface, cls).setUpClass()
        cls.abc_client = AbcClient(oauth_token='TEST')
        cls.abc_empty_response = {
            "count": 0,
            "next": None,
            "previous": None,
            "total_pages": 1,
            "results": []
        }

    def test_correct_exception(self, m):
        service_id = 666
        service_slug = 'wrong_slug'
        m.get(
            'https://abc-back.yandex-team.ru/api/v3/services/{}/'.format(service_id),
            status_code=404
        )
        with self.assertRaises(ServiceNotFound):
            self.__class__.abc_client.get_service(service_id=service_id)
        m.get(
            'https://abc-back.yandex-team.ru/api/v3/services/?slug={}'.format(service_slug),
            json=self.__class__.abc_empty_response
        )
        with self.assertRaises(ServiceNotFound):
            self.__class__.abc_client.get_service_by_slug(slug=service_slug)

    def test_get_services(self, m):
        service_ids = [1143, 26]
        mock_response = {
            "count": 2,
            "next": None,
            "previous": None,
            "total_pages": 1,
            "results": [
                {
                    "activity": {
                        "ru": "",
                        "en": ""
                    },
                    "activity_formatted": {
                        "ru": "",
                        "en": ""
                    },
                    "ancestors": [
                        {
                            "slug": "meta_search",
                            "id": 851,
                            "parent": None,
                            "name": {
                                "ru": "Сервисы Поискового портала",
                                "en": "Search Portal"
                            }
                        },
                        {
                            "slug": "websearch",
                            "id": 945,
                            "parent": 851,
                            "name": {
                                "ru": "WEB-поиск (DUTY)",
                                "en": "WEB-search (DUTY)"
                            }
                        },
                        {
                            "slug": "buki",
                            "id": 26,
                            "parent": 945,
                            "name": {
                                "ru": "Веб-ранжирование",
                                "en": "Веб-ранжирование"
                            }
                        }
                    ],
                    "children_count": 3,
                    "created_at": "2012-03-06T15:39:48Z",
                    "department": None,
                    "descendants_count": 4,
                    "description": {
                        "ru": "",
                        "en": ""
                    },
                    "description_formatted": {
                        "ru": "",
                        "en": ""
                    },
                    "id": 26,
                    "is_exportable": True,
                    "keywords": "",
                    "kpi": {
                        "bugs_count": None,
                        "releases_count": None,
                        "lsr_count": 0
                    },
                    "modified_at": "2018-06-30T05:47:00.460141Z",
                    "name": {
                        "ru": "Веб-ранжирование",
                        "en": "Веб-ранжирование"
                    },
                    "owner": {
                        "id": 759,
                        "login": "styskin",
                        "first_name": {
                            "ru": "Андрей",
                            "en": "Andrey"
                        },
                        "last_name": {
                            "ru": "Стыскин",
                            "en": "Styskin"
                        },
                        "uid": "1120000000000372",
                        "name": {
                            "ru": "Андрей Стыскин",
                            "en": "Andrey Styskin"
                        }
                    },
                    "parent": {
                        "id": 945,
                        "slug": "websearch",
                        "name": {
                            "ru": "WEB-поиск (DUTY)",
                            "en": "WEB-search (DUTY)"
                        },
                        "parent": 851
                    },
                    "path": "/meta_search/websearch/buki/",
                    "readonly_state": None,
                    "slug": "buki",
                    "state": "develop",
                    "state_display": {
                        "ru": "Развивается",
                        "en": "Развивается"
                    },
                    "state_display_i18n": "Развивается",
                    "tags": [],
                    "unique_immediate_members_count": 13
                },
                {
                    "activity": {
                        "ru": "",
                        "en": ""
                    },
                    "activity_formatted": {
                        "ru": "",
                        "en": ""
                    },
                    "ancestors": [
                        {
                            "slug": "meta_search",
                            "id": 851,
                            "parent": None,
                            "name": {
                                "ru": "Сервисы Поискового портала",
                                "en": "Search Portal"
                            }
                        },
                        {
                            "slug": "websearch",
                            "id": 945,
                            "parent": 851,
                            "name": {
                                "ru": "WEB-поиск (DUTY)",
                                "en": "WEB-search (DUTY)"
                            }
                        },
                        {
                            "slug": "runtime",
                            "id": 1605,
                            "parent": 945,
                            "name": {
                                "ru": "Runtime Поиска (DUTY)",
                                "en": "Runtime (DUTY)"
                            }
                        },
                        {
                            "slug": "searchcomponents",
                            "id": 1606,
                            "parent": 1605,
                            "name": {
                                "ru": "Поисковые компоненты (рантайм)",
                                "en": "Search runtime components"
                            }
                        },
                        {
                            "slug": "search-wizard",
                            "id": 1143,
                            "parent": 1606,
                            "name": {
                                "ru": "Колдунщик запросов (begemot, wizard)",
                                "en": "Query Wizard, Begemot"
                            }
                        }
                    ],
                    "children_count": 0,
                    "created_at": "2015-12-02T11:38:42Z",
                    "department": None,
                    "descendants_count": 0,
                    "description": {
                        "ru": "Begemot (Бегемот) - сервис, который преобразует исходный запрос пользователя в формат, понятный поисковому рантайму (разбор запроса, обогащение синонимами, представление в виде qtree), считает запросные факторы, классифицирует запрос. Предыдущая версия сервиса называлась Wizard (Визард), была не шардированной, без статической проверки зависимостей.",
                        "en": ""
                    },
                    "description_formatted": {
                        "ru": "<div class=\"wiki-doc wiki-doc_magiclinks_yes i-bem\" data-bem='{\"wiki-doc\":{\"user\":{\"codeTheme\":\"github\"},\"wiki-attrs\":{\"lang\":\"ru\",\"pos_end\":350,\"code_theme\":\"github\",\"magiclinks_url\":\"https://wf.yandex-team.ru/magiclinks/v1/links\",\"mode\":\"view\",\"env\":\"production\",\"pos_start\":0}}}'><div class=\"wiki-p\">Begemot (Бегемот) - сервис, который преобразует исходный запрос пользователя в формат, понятный поисковому рантайму (разбор запроса, обогащение синонимами, представление в виде qtree), считает запросные факторы, классифицирует запрос. Предыдущая версия сервиса называлась Wizard (Визард), была не шардированной, без статической проверки зависимостей.</div></div>\n<!--wf-ws v6.0-7-->",
                        "en": ""
                    },
                    "id": 1143,
                    "is_exportable": True,
                    "keywords": "",
                    "kpi": {
                        "bugs_count": None,
                        "releases_count": None,
                        "lsr_count": 0
                    },
                    "modified_at": "2018-07-04T08:46:58.297967Z",
                    "name": {
                        "ru": "Колдунщик запросов (begemot, wizard)",
                        "en": "Query Wizard, Begemot"
                    },
                    "owner": {
                        "id": 7439,
                        "login": "dmitryno",
                        "first_name": {
                            "ru": "Дмитрий",
                            "en": "Dmitry"
                        },
                        "last_name": {
                            "ru": "Носов",
                            "en": "Nosov"
                        },
                        "uid": "1120000000012442",
                        "name": {
                            "ru": "Дмитрий Носов",
                            "en": "Dmitry Nosov"
                        }
                    },
                    "parent": {
                        "id": 1606,
                        "slug": "searchcomponents",
                        "name": {
                            "ru": "Поисковые компоненты (рантайм)",
                            "en": "Search runtime components"
                        },
                        "parent": 1605
                    },
                    "path": "/meta_search/websearch/runtime/searchcomponents/search-wizard/",
                    "readonly_state": None,
                    "slug": "search-wizard",
                    "state": "develop",
                    "state_display": {
                        "ru": "Развивается",
                        "en": "Развивается"
                    },
                    "state_display_i18n": "Развивается",
                    "tags": [
                        {
                            "id": 3,
                            "name": {
                                "ru": "MARTY",
                                "en": "MARTY"
                            },
                            "color": "#009900",
                            "slug": "marty"
                        }
                    ],
                    "unique_immediate_members_count": 72
                }
            ]
        }
        m.get(
            'https://abc-back.yandex-team.ru/api/v3/services/?id__in={}'.format(','.join(str(i) for i in service_ids)),
            json=mock_response
        )
        services = self.__class__.abc_client.get_services(id__in=service_ids)  # class ServiceList
        self.assertEqual(len(services.objects), 2)
        service = services.objects[1]
        self.assertEqual(service.slug, 'search-wizard')
        self.assertEqual(service.id, 1143)
        self.assertEqual(service.name.en, mock_response['results'][1]['name']['en'])
        self.assertEqual(service.owner.login, mock_response['results'][1]['owner']['login'])
        self.assertEqual(service.state, abc_pb2.Service.State.DEVELOP)
        self.assertEqual(service.created, 1449056322)

    def test_get_service(self, m):
        mock_response = {
            "count": 1,
            "next": None,
            "previous": None,
            "total_pages": 1,
            "results": [
                {
                    "activity": {
                        "ru": "",
                        "en": ""
                    },
                    "activity_formatted": {
                        "ru": "",
                        "en": ""
                    },
                    "ancestors": [
                        {
                            "slug": "meta_search",
                            "id": 851,
                            "parent": None,
                            "name": {
                                "ru": "Сервисы Поискового портала",
                                "en": "Search Portal"
                            }
                        },
                        {
                            "slug": "websearch",
                            "id": 945,
                            "parent": 851,
                            "name": {
                                "ru": "WEB-поиск (DUTY)",
                                "en": "WEB-search (DUTY)"
                            }
                        },
                        {
                            "slug": "runtime",
                            "id": 1605,
                            "parent": 945,
                            "name": {
                                "ru": "Runtime Поиска (DUTY)",
                                "en": "Runtime (DUTY)"
                            }
                        },
                        {
                            "slug": "searchcomponents",
                            "id": 1606,
                            "parent": 1605,
                            "name": {
                                "ru": "Поисковые компоненты (рантайм)",
                                "en": "Search runtime components"
                            }
                        },
                        {
                            "slug": "search-wizard",
                            "id": 1143,
                            "parent": 1606,
                            "name": {
                                "ru": "Колдунщик запросов (begemot, wizard)",
                                "en": "Query Wizard, Begemot"
                            }
                        }
                    ],
                    "children_count": 0,
                    "created_at": "2015-12-02T11:38:42Z",
                    "department": None,
                    "descendants_count": 0,
                    "description": {
                        "ru": "Begemot (Бегемот) - сервис, который преобразует исходный запрос пользователя в формат, понятный поисковому рантайму (разбор запроса, обогащение синонимами, представление в виде qtree), считает запросные факторы, классифицирует запрос. Предыдущая версия сервиса называлась Wizard (Визард), была не шардированной, без статической проверки зависимостей.",
                        "en": ""
                    },
                    "description_formatted": {
                        "ru": "<div class=\"wiki-doc wiki-doc_magiclinks_yes i-bem\" data-bem='{\"wiki-doc\":{\"user\":{\"codeTheme\":\"github\"},\"wiki-attrs\":{\"lang\":\"ru\",\"pos_end\":350,\"code_theme\":\"github\",\"magiclinks_url\":\"https://wf.yandex-team.ru/magiclinks/v1/links\",\"mode\":\"view\",\"env\":\"production\",\"pos_start\":0}}}'><div class=\"wiki-p\">Begemot (Бегемот) - сервис, который преобразует исходный запрос пользователя в формат, понятный поисковому рантайму (разбор запроса, обогащение синонимами, представление в виде qtree), считает запросные факторы, классифицирует запрос. Предыдущая версия сервиса называлась Wizard (Визард), была не шардированной, без статической проверки зависимостей.</div></div>\n<!--wf-ws v6.0-7-->",
                        "en": ""
                    },
                    "id": 1143,
                    "is_exportable": True,
                    "keywords": "",
                    "kpi": {
                        "bugs_count": None,
                        "releases_count": None,
                        "lsr_count": 0
                    },
                    "modified_at": "2018-07-04T08:46:58.297967Z",
                    "name": {
                        "ru": "Колдунщик запросов (begemot, wizard)",
                        "en": "Query Wizard, Begemot"
                    },
                    "owner": {
                        "id": 7439,
                        "login": "dmitryno",
                        "first_name": {
                            "ru": "Дмитрий",
                            "en": "Dmitry"
                        },
                        "last_name": {
                            "ru": "Носов",
                            "en": "Nosov"
                        },
                        "uid": "1120000000012442",
                        "name": {
                            "ru": "Дмитрий Носов",
                            "en": "Dmitry Nosov"
                        }
                    },
                    "parent": {
                        "id": 1606,
                        "slug": "searchcomponents",
                        "name": {
                            "ru": "Поисковые компоненты (рантайм)",
                            "en": "Search runtime components"
                        },
                        "parent": 1605
                    },
                    "path": "/meta_search/websearch/runtime/searchcomponents/search-wizard/",
                    "readonly_state": None,
                    "slug": "search-wizard",
                    "state": "develop",
                    "state_display": {
                        "ru": "Развивается",
                        "en": "Развивается"
                    },
                    "state_display_i18n": "Развивается",
                    "tags": [
                        {
                            "id": 3,
                            "name": {
                                "ru": "MARTY",
                                "en": "MARTY"
                            },
                            "color": "#009900",
                            "slug": "marty"
                        }
                    ],
                    "unique_immediate_members_count": 72
                }
            ]
        }
        m.get(
            'https://abc-back.yandex-team.ru/api/v3/services/?slug=search-wizard',
            json=mock_response
        )
        service = self.__class__.abc_client.get_service_by_slug(slug='search-wizard')  # class Service

        self.assertEqual(service.slug, 'search-wizard')
        self.assertEqual(service.id, 1143)
        self.assertEqual(service.name.en, mock_response['results'][0]['name']['en'])
        self.assertEqual(service.owner.login, mock_response['results'][0]['owner']['login'])
        self.assertEqual(service.state, abc_pb2.Service.State.DEVELOP)
        self.assertEqual(service.created, 1449056322)

    def test_get_service_members(self, m):
        mock_response = {
            "count": 6,
            "next": None,
            "previous": None,
            "total_pages": 1,
            "results": [
                {
                    "id": 45255,
                    "person": {
                        "id": 21391,
                        "login": "coffeeman",
                        "first_name": {
                            "ru": "Александр",
                            "en": "Aleksandr"
                        },
                        "last_name": {
                            "ru": "Максимов",
                            "en": "Maksimov"
                        },
                        "uid": "1120000000043188",
                        "name": {
                            "ru": "Александр Максимов",
                            "en": "Aleksandr Maksimov"
                        }
                    },
                    "service": {
                        "id": 2030,
                        "slug": "workplace",
                        "name": {
                            "ru": "Workplace. Рабочее место дежурного",
                            "en": "Workplace"
                        },
                        "parent": 1171
                    },
                    "role": {
                        "id": 8,
                        "name": {
                            "ru": "Разработчик",
                            "en": "Developer"
                        },
                        "service": None,
                        "scope": {
                            "slug": "development",
                            "name": {
                                "ru": "Разработка",
                                "en": "Development"
                            }
                        },
                        "code": None
                    },
                    "created_at": "2017-12-19T09:29:39.347457Z",
                    "modified_at": "2018-05-04T10:36:43.061686Z",
                    "state": "approved",
                    "department_member": None
                },
                {
                    "id": 45256,
                    "person": {
                        "id": 27001,
                        "login": "flyik",
                        "first_name": {
                            "ru": "Илья",
                            "en": "Ilya"
                        },
                        "last_name": {
                            "ru": "Квасов",
                            "en": "Kvasov"
                        },
                        "uid": "1120000000052788",
                        "name": {
                            "ru": "Илья Квасов",
                            "en": "Ilya Kvasov"
                        }
                    },
                    "service": {
                        "id": 2030,
                        "slug": "workplace",
                        "name": {
                            "ru": "Workplace. Рабочее место дежурного",
                            "en": "Workplace"
                        },
                        "parent": 1171
                    },
                    "role": {
                        "id": 8,
                        "name": {
                            "ru": "Разработчик",
                            "en": "Developer"
                        },
                        "service": None,
                        "scope": {
                            "slug": "development",
                            "name": {
                                "ru": "Разработка",
                                "en": "Development"
                            }
                        },
                        "code": None
                    },
                    "created_at": "2017-12-19T09:29:40.288915Z",
                    "modified_at": "2018-05-04T10:36:43.255847Z",
                    "state": "approved",
                    "department_member": None
                },
                {
                    "id": 45257,
                    "person": {
                        "id": 17407,
                        "login": "corax",
                        "first_name": {
                            "ru": "Андрей",
                            "en": "Andrey"
                        },
                        "last_name": {
                            "ru": "Воронов",
                            "en": "Voronov"
                        },
                        "uid": "1120000000036217",
                        "name": {
                            "ru": "Андрей Воронов",
                            "en": "Andrey Voronov"
                        }
                    },
                    "service": {
                        "id": 2030,
                        "slug": "workplace",
                        "name": {
                            "ru": "Workplace. Рабочее место дежурного",
                            "en": "Workplace"
                        },
                        "parent": 1171
                    },
                    "role": {
                        "id": 8,
                        "name": {
                            "ru": "Разработчик",
                            "en": "Developer"
                        },
                        "service": None,
                        "scope": {
                            "slug": "development",
                            "name": {
                                "ru": "Разработка",
                                "en": "Development"
                            }
                        },
                        "code": None
                    },
                    "created_at": "2017-12-19T09:29:42.172186Z",
                    "modified_at": "2018-05-04T10:36:45.751533Z",
                    "state": "approved",
                    "department_member": None
                },
                {
                    "id": 58334,
                    "person": {
                        "id": 12509,
                        "login": "roboslone",
                        "first_name": {
                            "ru": "Александр",
                            "en": "Alexander"
                        },
                        "last_name": {
                            "ru": "Христюхин",
                            "en": "Hristyuhin"
                        },
                        "uid": "1120000000020286",
                        "name": {
                            "ru": "Александр Христюхин",
                            "en": "Alexander Hristyuhin"
                        }
                    },
                    "service": {
                        "id": 2030,
                        "slug": "workplace",
                        "name": {
                            "ru": "Workplace. Рабочее место дежурного",
                            "en": "Workplace"
                        },
                        "parent": 1171
                    },
                    "role": {
                        "id": 25,
                        "name": {
                            "ru": "Консультант",
                            "en": "Consultant"
                        },
                        "service": None,
                        "scope": {
                            "slug": "other",
                            "name": {
                                "ru": "Другие роли",
                                "en": "Other roles"
                            }
                        },
                        "code": "consultant"
                    },
                    "created_at": "2018-03-18T05:03:16.920660Z",
                    "modified_at": "2018-05-04T10:42:30.884442Z",
                    "state": "approved",
                    "department_member": None
                },
                {
                    "id": 63630,
                    "person": {
                        "id": 2813,
                        "login": "talion",
                        "first_name": {
                            "ru": "Дмитрий",
                            "en": "Dmitry"
                        },
                        "last_name": {
                            "ru": "Меликов",
                            "en": "Melikov"
                        },
                        "uid": "1120000000001765",
                        "name": {
                            "ru": "Дмитрий Меликов",
                            "en": "Dmitry Melikov"
                        }
                    },
                    "service": {
                        "id": 2030,
                        "slug": "workplace",
                        "name": {
                            "ru": "Workplace. Рабочее место дежурного",
                            "en": "Workplace"
                        },
                        "parent": 1171
                    },
                    "role": {
                        "id": 1,
                        "name": {
                            "ru": "Руководитель сервиса",
                            "en": "Head of product"
                        },
                        "service": None,
                        "scope": {
                            "slug": "services_management",
                            "name": {
                                "ru": "Управление продуктом",
                                "en": "Service management"
                            }
                        },
                        "code": "product_head"
                    },
                    "created_at": "2018-05-15T19:38:05.410601Z",
                    "modified_at": "2018-05-15T19:49:27.719759Z",
                    "state": "approved",
                    "department_member": None
                },
                {
                    "id": 67291,
                    "person": {
                        "id": 18202,
                        "login": "epsilond1",
                        "first_name": {
                            "ru": "Семён",
                            "en": "Semyon"
                        },
                        "last_name": {
                            "ru": "Полторак",
                            "en": "Poltorak"
                        },
                        "uid": "1120000000037738",
                        "name": {
                            "ru": "Семён Полторак",
                            "en": "Semyon Poltorak"
                        }
                    },
                    "service": {
                        "id": 2030,
                        "slug": "workplace",
                        "name": {
                            "ru": "Workplace. Рабочее место дежурного",
                            "en": "Workplace"
                        },
                        "parent": 1171
                    },
                    "role": {
                        "id": 8,
                        "name": {
                            "ru": "Разработчик",
                            "en": "Developer"
                        },
                        "service": None,
                        "scope": {
                            "slug": "development",
                            "name": {
                                "ru": "Разработка",
                                "en": "Development"
                            }
                        },
                        "code": None
                    },
                    "created_at": "2018-06-22T08:01:48.931368Z",
                    "modified_at": "2018-06-22T08:49:03.675445Z",
                    "state": "approved",
                    "department_member": None
                }
            ]
        }
        m.get(
            'https://abc-back.yandex-team.ru/api/v3/services/members/?service=2030',
            json=mock_response
        )
        members = self.__class__.abc_client.get_members(service=2030)
        self.assertEqual(len(members.objects), mock_response['count'])
        test_index = 0
        member = members.objects[test_index]
        self.assertEqual(member.person.login, mock_response['results'][test_index]['person']['login'])
        self.assertEqual(member.service.id, 2030)
        self.assertEqual(member.state, abc_pb2.ServiceMember.State.APPROVED)
        self.assertEqual(member.role.scope.slug, mock_response['results'][test_index]['role']['scope']['slug'])

        member_role = member.role
        role = mock_response['results'][test_index]['role']

        for actual, expected in (
            (member_role.id, role['id']),
            (member_role.name.ru, role['name']['ru']),
            (member_role.name.en, role['name']['en']),
            (member_role.scope.slug, role['scope']['slug']),
            (member_role.scope.name.ru, role['scope']['name']['ru']),
            (member_role.scope.name.en, role['scope']['name']['en']),
            (member_role.code, role['code'] or ''),
        ):
            if isinstance(expected, bytes):
                expected = expected.decode('utf-8')
            if isinstance(actual, bytes):
                actual = actual.decode('utf-8')

            self.assertEqual(actual, expected)

        self.assertIsNotNone(member_role.service)

    def test_get_service_contacts(self, m):
        service_id = 1143
        mock_response = {
            "count": 22,
            "next": None,
            "previous": None,
            "total_pages": 2,
            "results": [
                {
                    "id": 7458,
                    "type": {
                        "id": 4,
                        "code": "tracker_startrek",
                        "validator": "STARTREK",
                        "name": {
                            "ru": "Очередь в Startrek",
                            "en": "Startrek queue"
                        }
                    },
                    "title": {
                        "ru": "REQWIZARD",
                        "en": ""
                    },
                    "content": "REQWIZARD",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:38:34.686911Z",
                    "modified_at": "2018-02-21T23:52:58.645210Z"
                },
                {
                    "id": 7460,
                    "type": {
                        "id": 18,
                        "code": "url_wiki",
                        "validator": "WIKI",
                        "name": {
                            "ru": "Вики",
                            "en": "Wiki"
                        }
                    },
                    "title": {
                        "ru": "Wizard Wiki",
                        "en": ""
                    },
                    "content": "wizard",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:39:57.028934Z",
                    "modified_at": "2018-02-21T23:52:57.899332Z"
                },
                {
                    "id": 7461,
                    "type": {
                        "id": 18,
                        "code": "url_wiki",
                        "validator": "WIKI",
                        "name": {
                            "ru": "Вики",
                            "en": "Wiki"
                        }
                    },
                    "title": {
                        "ru": "Begemot Wiki",
                        "en": ""
                    },
                    "content": "begemot",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:40:18.343627Z",
                    "modified_at": "2018-02-21T23:52:57.976754Z"
                },
                {
                    "id": 7462,
                    "type": {
                        "id": 18,
                        "code": "url_wiki",
                        "validator": "WIKI",
                        "name": {
                            "ru": "Вики",
                            "en": "Wiki"
                        }
                    },
                    "title": {
                        "ru": "Инструкция Wizard web для дежурных",
                        "en": ""
                    },
                    "content": "wizard/Wizard-Marty",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:41:04.853769Z",
                    "modified_at": "2018-02-21T23:52:58.071223Z"
                },
                {
                    "id": 7464,
                    "type": {
                        "id": 23,
                        "code": "url_doc_marty",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Документация для MARTY",
                            "en": "Documentations for MARTY"
                        }
                    },
                    "title": {
                        "ru": "Документация для MARTY",
                        "en": ""
                    },
                    "content": "https://wiki.yandex-team.ru/jandekspoisk/sepe/dezhurnajasmena/Servisy/begemot/",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:41:58.596640Z",
                    "modified_at": "2018-02-21T23:52:58.195890Z"
                },
                {
                    "id": 7465,
                    "type": {
                        "id": 9,
                        "code": "url_doc",
                        "validator": "DOC",
                        "name": {
                            "ru": "Документация",
                            "en": "Documentation"
                        }
                    },
                    "title": {
                        "ru": "Инструкция про сервисный визард",
                        "en": ""
                    },
                    "content": "https://wiki.yandex-team.ru/jandekspoisk/sepe/dezhurnajasmena/servisy/service-wizard/",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:42:28.336217Z",
                    "modified_at": "2018-02-21T23:52:58.152725Z"
                },
                {
                    "id": 7466,
                    "type": {
                        "id": 11,
                        "code": "url_duty_calendar",
                        "validator": "CALENDAR",
                        "name": {
                            "ru": "Календарь дежурства",
                            "en": "Duty calendar"
                        }
                    },
                    "title": {
                        "ru": "Календарь дежурств",
                        "en": ""
                    },
                    "content": "https://calendar.yandex-team.ru/week?embed&layer_ids=44555&tz_id=Europe/Moscow",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:43:33.593905Z",
                    "modified_at": "2018-02-21T23:52:58.237643Z"
                },
                {
                    "id": 7467,
                    "type": {
                        "id": 24,
                        "code": "url_monitoring_panels",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Мониторинг. Панели",
                            "en": "Monitoring Panels"
                        }
                    },
                    "title": {
                        "ru": "Service-wizard Panels",
                        "en": ""
                    },
                    "content": "https://yasm.yandex-team.ru/quentao-panel?axis.cases=service-wizard&axis.groups=rcps&axis.panels=wizards&axis.rotate=off&axis.tier=self&axis.view=charts%20panel",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:43:33.617347Z",
                    "modified_at": "2018-02-21T23:52:58.280264Z"
                },
                {
                    "id": 7468,
                    "type": {
                        "id": 24,
                        "code": "url_monitoring_panels",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Мониторинг. Панели",
                            "en": "Monitoring Panels"
                        }
                    },
                    "title": {
                        "ru": "Search-wizard Panels",
                        "en": ""
                    },
                    "content": "https://yasm.yandex-team.ru/quentao-panel?axis.cases=search-wizard&axis.groups=rcps&axis.panels=wizards&axis.rotate=off&axis.tier=self&axis.view=charts%20panel",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:45:26.129119Z",
                    "modified_at": "2018-02-21T23:52:58.373228Z"
                },
                {
                    "id": 7469,
                    "type": {
                        "id": 25,
                        "code": "url_monitoring_lights",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Мониторинг. Светофоры",
                            "en": "Monitoring Lights"
                        }
                    },
                    "title": {
                        "ru": "Wizard Lights",
                        "en": ""
                    },
                    "content": "https://yasm.yandex-team.ru/menu/quentao/lights_panel/web/wizards/?fullscreen=1",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:45:26.148866Z",
                    "modified_at": "2018-02-21T23:52:58.417731Z"
                },
                {
                    "id": 7471,
                    "type": {
                        "id": 22,
                        "code": "url_monitoring_dt_alerts",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Мониторинг. DT Alerts",
                            "en": "Monitoring Downtime Alerts"
                        }
                    },
                    "title": {
                        "ru": "Downtime Alerts",
                        "en": ""
                    },
                    "content": "https://yasm.yandex-team.ru/panel/begemot-down-alerts",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:45:26.195765Z",
                    "modified_at": "2018-02-21T23:52:58.500110Z"
                },
                {
                    "id": 7472,
                    "type": {
                        "id": 15,
                        "code": "url_telegram",
                        "validator": "TELEGRAM",
                        "name": {
                            "ru": "Телеграм чат",
                            "en": "Telegram"
                        }
                    },
                    "title": {
                        "ru": "Telegram",
                        "en": ""
                    },
                    "content": "https://t.me/joinchat/B5EDfEDnX06iR6GTsfKrEw",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:45:26.212961Z",
                    "modified_at": "2018-02-21T23:52:58.541804Z"
                },
                {
                    "id": 7473,
                    "type": {
                        "id": 1,
                        "code": "email_ml",
                        "validator": "MAILLIST",
                        "name": {
                            "ru": "E-mail рассылка",
                            "en": "Maillist"
                        }
                    },
                    "title": {
                        "ru": "req-wizard",
                        "en": ""
                    },
                    "content": "req-wizard",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:45:26.228245Z",
                    "modified_at": "2018-02-21T23:52:58.583259Z"
                },
                {
                    "id": 7474,
                    "type": {
                        "id": 20,
                        "code": "vcs",
                        "validator": "VCS",
                        "name": {
                            "ru": "Репозиторий",
                            "en": "Source code"
                        }
                    },
                    "title": {
                        "ru": "Wizard Arc",
                        "en": ""
                    },
                    "content": "https://a.yandex-team.ru/arc/trunk/arcadia/search/wizard/",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:46:35.605553Z",
                    "modified_at": "2018-02-21T23:46:35.673701Z"
                },
                {
                    "id": 7475,
                    "type": {
                        "id": 20,
                        "code": "vcs",
                        "validator": "VCS",
                        "name": {
                            "ru": "Репозиторий",
                            "en": "Source code"
                        }
                    },
                    "title": {
                        "ru": "Begemot Arc",
                        "en": ""
                    },
                    "content": "https://a.yandex-team.ru/arc/trunk/arcadia/search/begemot/",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:46:35.731630Z",
                    "modified_at": "2018-02-21T23:46:35.787661Z"
                },
                {
                    "id": 7476,
                    "type": {
                        "id": 26,
                        "code": "url_staff",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Команда на Staff",
                            "en": "Team on the Staff"
                        }
                    },
                    "title": {
                        "ru": "Команда на Staff",
                        "en": ""
                    },
                    "content": "https://staff.yandex-team.ru/departments/yandex_search_tech_quality_component_2978",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:46:35.844175Z",
                    "modified_at": "2018-02-21T23:46:35.900068Z"
                },
                {
                    "id": 7477,
                    "type": {
                        "id": 27,
                        "code": "atr_product",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Аttribute Product",
                            "en": "Аttribute Product"
                        }
                    },
                    "title": {
                        "ru": "PROD-COMMON",
                        "en": ""
                    },
                    "content": "tmp.tmp",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:47:29.369551Z",
                    "modified_at": "2018-02-21T23:47:29.428887Z"
                },
                {
                    "id": 7478,
                    "type": {
                        "id": 28,
                        "code": "atr_service",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Аttribute Service",
                            "en": "Аttribute Service"
                        }
                    },
                    "title": {
                        "ru": "wizard_service",
                        "en": ""
                    },
                    "content": "tmp.tmp",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:48:30.223926Z",
                    "modified_at": "2018-02-21T23:48:30.282432Z"
                },
                {
                    "id": 7479,
                    "type": {
                        "id": 29,
                        "code": "atr_owner",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Аttribute Owner",
                            "en": "Аttribute Owner"
                        }
                    },
                    "title": {
                        "ru": "dmitryno",
                        "en": ""
                    },
                    "content": "tmp.tmp",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:48:30.339346Z",
                    "modified_at": "2018-02-21T23:48:30.396988Z"
                },
                {
                    "id": 7480,
                    "type": {
                        "id": 30,
                        "code": "atr_priority",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Аttribute Priority",
                            "en": "Аttribute Priority"
                        }
                    },
                    "title": {
                        "ru": "0",
                        "en": ""
                    },
                    "content": "tmp.tmp",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:48:30.453447Z",
                    "modified_at": "2018-02-21T23:48:30.509785Z"
                },
                {
                    "id": 7481,
                    "type": {
                        "id": 31,
                        "code": "atr_downtime_impact",
                        "validator": "OTHER",
                        "name": {
                            "ru": "Аttribute Downtime Impact",
                            "en": "Аttribute Downtime Impact"
                        }
                    },
                    "title": {
                        "ru": "0.1",
                        "en": ""
                    },
                    "content": "tmp.tmp",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:48:30.565716Z",
                    "modified_at": "2018-02-21T23:48:30.623556Z"
                },
                {
                    "id": 7482,
                    "type": {
                        "id": 12,
                        "code": "url_monitoring",
                        "validator": "MONITORING",
                        "name": {
                            "ru": "Мониторинг. Основные панели сервиса/алерты",
                            "en": "Monitoring"
                        }
                    },
                    "title": {
                        "ru": "Все графики",
                        "en": ""
                    },
                    "content": "https://wiki.yandex-team.ru/begemot/ops/#testyimonitory",
                    "service": {
                        "id": 1143,
                        "slug": "search-wizard",
                        "name": {
                            "ru": "Колдунщик запросов",
                            "en": "Колдунщик запросов"
                        },
                        "parent": 26
                    },
                    "created_at": "2018-02-21T23:52:18.765247Z",
                    "modified_at": "2018-02-21T23:52:58.458819Z"
                }
            ]
        }

        m.get(
            'https://abc-back.yandex-team.ru/api/v3/services/contacts/?service={}'.format(service_id),
            json=mock_response
        )
        contacts = self.__class__.abc_client.get_contacts(service=service_id)
        self.assertEqual(len(contacts.objects), mock_response['count'])
        contact = contacts.objects[len(contacts.objects)-1]
        self.assertEqual(contact.type.code, 'url_monitoring')
