# -*- coding: utf-8 -*-

import os.path

import yaml
import xml.etree.ElementTree as ET

import yatest.common


def test_ua_conf_consistency():
    """
    Проверка на то, что все логгеры включены в конфиг Unified Agent
    """
    log4j2_config = ET.parse(os.path.join(yatest.common.source_path(), 'direct', 'libs-internal', 'config', 'src', 'main', 'resources', 'log4j2-common-loggers.xml'))
    with open(os.path.join(yatest.common.source_path(), 'direct', 'packages', 'deploy', 'etc', 'yandex', 'unified_agent', 'conf.d', 'direct.production.yml')) as f:
        ua_config = yaml.safe_load(f)
    loggers_in_log4j_config = []
    for logger_item in [child for child in log4j2_config.getroot() if child.tag == 'Logger']:
        logger_name = logger_item.attrib['name']
        for appender_ref_item in [child for child in logger_item if child.tag == 'AppenderRef']:
            # в MESSAGES логи попадают по умолчанию
            if appender_ref_item.attrib.get('ref', '') != 'MESSAGES':
                loggers_in_log4j_config.append(logger_name)
    loggers_in_unified_agent_config = []
    for route in ua_config.get('routes', []):
        cases = route.get('channel', {}).get('case', [])
        logger_names = [(case.get('when', None) or {}).get('message', {}).get('logger_name', "") for case in cases]
        loggers_in_unified_agent_config += [name for name in logger_names if name]
    assert set(loggers_in_log4j_config) == set(loggers_in_unified_agent_config)
