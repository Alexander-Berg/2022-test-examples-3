# coding: utf8
import logging
import os
from copy import deepcopy

from ruamel.yaml import YAML
import yatest

log = logging.getLogger(__name__)


def get_common_dir(dirname):
    return yatest.common.source_path(os.path.join(
        "market/sre/conf/fslb/common",
        dirname
    ))


def get_cert_dir():
    return yatest.common.source_path("market/sre/conf/fslb/tests-common/cert")


def get_template_dir():
    return yatest.common.source_path("market/sre/tools/balancer_regenerate/templates")


def get_temp_dir():
    return yatest.common.output_path()


class Config(object):
    def __init__(self, value_dirs_fn):
        # TODO: убейте меня, а именно: yatest.common.source_path можно вызывать только в тестах, в setUp нельзя.
        self.value_dirs_fn = value_dirs_fn

    def get_configs(self):
        """ Генератор возвращает каждый конфиг из директории values-available в виде дикта """

        for values_dir in self.value_dirs_fn():
            for root, dirs, files in os.walk(values_dir):
                for filename in files:
                    file_path = os.path.join(root, filename)

                    if os.path.splitext(filename)[1] != '.yaml':
                        log.warning("Skipping non-yaml file in config directory: %r", file_path)
                        continue

                    if filename.startswith('00-'):
                        log.warning("Skipping special file: %r", file_path)
                        continue

                    yaml = YAML(typ='safe')
                    with open(file_path) as fd:
                        data = yaml.load(fd)

                    if 'testing' in data['context']:
                        d = deepcopy(data['context'].get('default', {}))
                        d.update(data['context']['testing'])
                        data['context']['testing'] = d

                    yield filename, data
