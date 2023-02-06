#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys
import argparse
from directadmon.directadmin_monitorings import *

### инициализация
(NAMESPACE, HOST) = get_namespace_host(os.path.basename(__file__))

# суффиксы для агрегации, для удобства совпадают с кондукторной группой и названием инстанса базы
CHILDREN = [
#    {'suffix': 'tabula', 'children': [{'host': 'direct_testnets_tabula_mysql', 'group_type': 'CGROUP'}]}
]
CHILDREN = cgroups_to_children(CHILDREN)

def cc(suffixes):
    assert all([isinstance(x, str) for x in suffixes])
    children_subset = [x for x in CHILDREN if x['suffix'] in suffixes]
    return cgroups_to_children(children_subset)

parser = argparse.ArgumentParser()
add_default_args(parser)
args = parser.parse_args()
init_root_logger(args)

### tv проверки
checks = DirectAdminChecks(namespace=NAMESPACE, host=HOST, children=CHILDREN, token=args.token)
checks.append_common()

checks.append_mysql(xtradb=True, cluster_settings_children=[], skip_grants=1, spec_children=[], zkguard_children=[])

### применяем проверки
checks.apply(dry_run=not args.apply, pretty=args.pretty)
