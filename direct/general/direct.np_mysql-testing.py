#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
"""

import os
import sys
import argparse
import yaml
from directadmon.directadmin_monitorings import *

### инициализация
(NAMESPACE, HOST) = get_namespace_host(os.path.basename(__file__))

parser = argparse.ArgumentParser()
add_default_args(parser)
args = parser.parse_args()
init_root_logger(args)

checks = DirectAdminChecks(namespace=NAMESPACE, host=HOST, token=args.token)

SHARDS_NUM = 21

aggr_host = HOST + '-mdb'
aggr_kwargs = copy.deepcopy(DEFAULT_CHECK) #Вариант CALLING_CHECK
aggr_kwargs = juggler_merge({'tags': ['direct-%s-health.' % HOST ]}, aggr_kwargs)
aggr_kwargs['tags'] = [t for t in aggr_kwargs['tags'] if t != 'directadmin_TV']  # убираем с основоного tv-дашборда, там будут только общие проверки
aggr_kwargs['ttl'] = SOLOMON_TTL


children = []
for i in range(1, SHARDS_NUM + 1):
    children.append(Child(host='direct.solomon-alert', service='mdb-ppcdata%s-testing-freespace' % i))
children.append(Child(host='direct.solomon-alert', service='mdb-ppcdict-testing-freespace'))
checks.append(children=children, host=aggr_host, service='freespace', **aggr_kwargs)

children = []
deploy_check = copy.deepcopy(LOGIC_AND)
#проверка демона обновляющего db-config в zookeeper
children.append(Child(group_type='DEPLOY', host='direct-np@stage=direct-mmm-testing', service='direct-mysql-master-monitor-testing'))
checks.append(children=children, host=aggr_host, service='direct-mysql-master-monitor-testing', **deploy_check)

### применяем проверки

checks.apply(dry_run=not args.apply, pretty=args.pretty)

