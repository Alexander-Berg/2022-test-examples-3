# coding: utf8


import logging
import os
import yatest
import yaml


ENV = u'common'
PKG = u'slb-haproxy'


log = logging.getLogger(__name__)


# CSADMIN-23607
# Имена файлов, которые мы игнорируем при проверке на уникальность номера и кондукторной группы
excludes = [
    'some-example.yaml',
]


def get_pkg_dir():
    """ Путь к пакету """
    return yatest.common.source_path(u'market/sre/conf/{}'.format(PKG))


def get_environment_dir(environment):
    return os.path.join(get_pkg_dir(), environment)


def get_primer_configs(environment):
    """ Генератор возвращает каждый конфиг из директории values-available в виде дикта """
    for env in (environment, ENV):
        env_dir = get_environment_dir(env)
        values_dir = os.path.join(env_dir, u'etc', u'haproxy', u'values-available')
        if not os.path.isdir(values_dir):
            continue

        for f in os.listdir(values_dir):
            filename = os.path.join(values_dir, f)
            if not filename.endswith(u'.yaml') or not os.path.isfile(filename):
                continue

            with open(filename) as fd:
                yield {
                    'data': yaml.safe_load(fd),
                    'filename': filename,
                }, utils()


def utils():
    """Фикстура с классом для проверки ряда полей в yaml конфиге."""
    return Utils


class Utils:

    @staticmethod
    def use_yp_services(dicti):
        """Проверяем, есть ли в списке сервисов с реалами реалы из yp.

        Для этого раскрываем словарь и ищем среди элементов список, а
        в списке ищем наличие yp сервиса.
        """

        stack = dicti.items()

        while stack:
            _, value = stack.pop()
            if isinstance(value, dict):
                stack.extend(value.iteritems())
            if isinstance(value, list):
                return Utils.__is_yp_service(value)

        return False

    @staticmethod
    def resolve_enabled(dicti):
        """Метод проверяет, включено ли разрешение dns-имен реалов в yaml-конфиге
        при генерации конфига для revers-proxy (haproxy, nginx, yandex-balancer).

        Вернет True, если у нас конфигурация выглядит так:

        params:
          default:
            ...
            resolve: yes
            ...
        """

        stack = dicti.items()

        while stack:
            key, value = stack.pop()
            if isinstance(value, dict):
                stack.extend(value.iteritems())
            else:
                if key == 'resolve' and value is True:
                    return True
        return False

    @staticmethod
    def resolve_realyp_enabled(dicti):
        """Метод получает на вход секцию servers и проверяет, что в ней:
        1. Есть YP сервисы
        2. Для них включен резолвинг для вставки ip адреса в конфиг haproxy.
        """

        stack = dicti.items()
        yp_services = []

        while stack:
            _, value = stack.pop()
            if isinstance(value, dict):
                stack.extend(value.iteritems())
            if isinstance(value, list) and Utils.__is_yp_service(value):
                yp_services.extend(value)

        # Проверяем на наличие включенного резолвинга только yp сервисы
        return Utils.__ensure_enable_resolve(yp_services)

    @staticmethod
    def dns_resolvers_setuped(dicti):
        """Проверяем, установлен ли для YP сервиса dns-resolver."""
        stack = list(dicti.items())
        yp_services = []

        while stack:
            _, value = stack.pop()
            if isinstance(value, dict):
                stack.extend(list(value.items()))
            if isinstance(value, list) and Utils.__is_yp_service(value):
                yp_services.extend(value)

        # Проверяем на наличие включенного резолвинга только yp сервисы
        return Utils.__ensure_setup_dns_resolvers(yp_services)

    @staticmethod
    def __is_yp_service(lst):
        """Проверяем, является ли сервис с реалами сервисом в yp.

        lst представляет собой нечто вроде:
        [
          {
            'dns_resolvers': 'yandex-ns',
            'name': 'YP@testing_market_cs-dashboard.sas@sas'
          }, {
            'sort_order': 'shuffled',
            'dns_resolvers': 'yandex-ns',
            'name': 'YP@testing_market_cs-dashboard.vla@vla'
          }
        ]

        """
        for el in lst:
            if 'name' in el:
                if 'YP@' in el['name'].upper():
                    log.error('name %s', el)
                    return True
        return False

    @staticmethod
    def __ensure_enable_resolve(yp_services):
        """Метод проверяет, что конкретный yp сервис помечен флажком разрешения dns имен для
        последующей вставки ip-адреса в конфиг haproxy. Не путать опцию resolve с опцией
        dns_resolvers, которая указывает haproxy, обновлять ip-адреса списка реалов из
        в райнтайме.

        Список сервисов выглядит как-то так:
        [
          {
            'dns_resolvers': 'yandex-ns',
            'name': 'YP@testing_market_cs-dashboard.sas@sas',
            'resolve': True
          }, {
            'dns_resolvers': 'yandex-ns',
            'name': 'YP@testing_market_cs-dashboard.vla@vla',
            'resolve': True
          }
        ]
        """
        for service in yp_services:
            if 'YP@' not in service['name']:
                continue
            if 'resolve' not in service:
                return True
            if service['resolve'] in ('yes', 'True', 'true', True):
                return True
        return False

    @staticmethod
    def __ensure_setup_dns_resolvers(yp_services):
        """Метод проверяет, что для конкретного yp сервис установлена опция dns_resolvers.

        Список сервисов выглядит как-то так:
        [
          {
            'dns_resolvers': 'yandex-ns',
            'name': 'YP@testing_market_cs-dashboard.sas@sas',
            'resolve': True
          }, {
            'dns_resolvers': 'yandex-ns',
            'name': 'YP@testing_market_cs-dashboard.vla@vla',
            'resolve': True
          }
        ]
        """
        for service in yp_services:
            if 'YP@' not in service['name']:
                continue
            if 'dns_resolvers' not in service:
                return False
        return True
