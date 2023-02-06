# coding: utf8
import logging
from collections import defaultdict
from conftest import config

log = logging.getLogger(__name__)


def test_service_name():
    """ service_name должен быть уникальным """
    service_names = defaultdict(set)

    for cfg in config.get_configs():
        for env in ('default', 'testing'):
            try:
                service_name = cfg['data']['context'][env]['service_name']
            except KeyError:
                pass

            service_names[service_name].add(cfg['filename'])

    duplicate_service_names = {
        service_name: configs
        for service_name, configs in service_names.items()
        if len(configs) > 1
    }

    assert len(duplicate_service_names) == 0, "Found duplicate service names: {!r}".format(duplicate_service_names)
