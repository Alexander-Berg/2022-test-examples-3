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

with open('/etc/yandex-direct/direct-apps.conf.yaml') as f:
    apps_conf = yaml.load(f)

top_level_children = []
for app, app_conf in apps_conf['apps'].items():
    if 'health-checks' in app_conf.get('ignore-features', []):
        continue

    child_checks = app_conf.get('health_checks_np', [])
    children = []
    for child_host, child_svc in [x.split(':') for x in child_checks]:
        children.append(Child(host=child_host, service=child_svc))

    meta = {}
    url_num = 0
    for url in app_conf.get('info_urls_np', []):
        if not "urls" in meta:
            meta["urls"] = []

        url_num += 1
        if isinstance(url, str):
            url = {"url": url}
        if isinstance(url, dict):
            meta["urls"].append({
                "title": url.get("title", "url" + str(url_num)),
                "type": "yasm_alert",
                "url": url.get("url", "https://wiki.yandex-team.ru/jeri/apps-health/")
            })

    checks.append(children=children, host=HOST, service=app, meta=meta, ttl=SOLOMON_TTL, tags=['direct-apps-health-np', 'direct-health-np']) # не добавляем тут directadmin_TV
    top_level_children.append(Child(host=HOST, service=app))

checks.append(children=top_level_children, host=HOST, service='apps-health', meta=meta, tags=['direct-birds-eye-np'])

### применяем проверки

checks.apply(dry_run=not args.apply, pretty=args.pretty)
