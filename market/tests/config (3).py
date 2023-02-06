# coding: utf8
import logging
import os
import yatest
import yaml

ENV = u'common'
PKG = u'slb-nginx'

log = logging.getLogger(__name__)


def get_pkg_dir():
    """ Путь к пакету """
    return yatest.common.source_path(u'market/sre/conf/{}'.format(PKG))


def get_environment_dir(environment):
    return os.path.join(get_pkg_dir(), environment)


def get_primer_configs(environment):
    """ Генератор возвращает каждый конфиг из директории values-available в виде дикта """
    for env in (environment, ENV):
        env_dir = get_environment_dir(env)
        values_dir = os.path.join(env_dir, u'etc', u'nginx', u'values-available')
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
                }
