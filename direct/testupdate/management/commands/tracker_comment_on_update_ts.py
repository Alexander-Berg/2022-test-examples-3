# -*- encoding: utf-8 -*-
# django-admin.py process_releaseplans --settings=releaser.settings.direct --pythonpath=../

from __future__ import with_statement

import httplib2
from urllib import urlencode

from django.core.management.base import BaseCommand
from django.db import transaction, models
from django.core.files import locks
import json

from pprint import pprint as p
import tempfile, re, string, os, time, sys, yaml
from datetime import datetime, timedelta
from optparse import make_option
from contextlib import contextmanager

#import copy

from django.db.models import Q
import json
from django.conf import settings
from releaser.versionica.models import HostProperty, PropertyGroup
from releaser.flagman.models import Flag
from releaser.utils import locked_file
from releaser.testupdate.testservers import test_update, testservers_conf
from releaser.startrek.tools import startrek_status_key_to_str, get_startrek_robot_token, last_releases_query, get_sign_comment
import warnings
with warnings.catch_warnings():
    warnings.filterwarnings("ignore", category=DeprecationWarning)
    from startrek_client import Startrek
import logging
logging.getLogger("startrek_client.collections").addHandler(logging.NullHandler())


STATUSES_TO_COMMENT = ['New', 'Testing']


class Command(BaseCommand):
    """
    Если обновилась ТС -- пишем комментарий в трекер
    """
    debug = True

    @transaction.commit_on_success
    def do_all_work(self, old_versions):
        conf = testservers_conf()
        new_versions = {}
        for alias in conf:
            if 'release' in conf[alias] and conf[alias]['release'] == 1:
                new_versions[conf[alias]['hostname']] = ''


        default_property = PropertyGroup.objects.get(name = "packages").default_property
        for rec in HostProperty.objects.filter( host__name__in = new_versions.keys(), property__name__regex = default_property ):
            new_versions[rec.host.name] = rec.value

        for alias in conf:
            hostname = conf[alias]['hostname']
            if not hostname in new_versions:
                continue
            new_versions[alias] = new_versions[hostname]
            del new_versions[hostname]

        message_diff = []
        message_state = []
        for alias in new_versions:
            old = ''
            if alias in old_versions:
                old = old_versions[alias]
            if old != new_versions[alias]:
                message_diff += [ "%s: %s --> %s" % (alias, old, new_versions[alias]) ]
            message_state += [ "%s: %s" % (alias, new_versions[alias]) ]

        if len(message_diff) > 0:
            versionica_url = "http://%s/versionica/property?group=packages&host_group=test" % settings.RELEASER_DNS
            message = u"**Мониторинг ТС**\n\n__Контейнеры обновлены:__\n%s\n\n__Текущее состояние:__\n%s\n\nВерсионика: %s" % ("\n".join(message_diff), "\n".join(message_state), versionica_url)

            skip_comment = False
            startrek = Startrek(token=get_startrek_robot_token(), useragent=settings.USER_AGENT)
            if settings.PROJECT == 'direct':
                query = last_releases_query(component=settings.STARTREK_DIRECT_RELEASE_COMPONENT)
            else:
                query = last_releases_query()
            release = next(r for r in startrek.issues.find(query, per_page=1))
            key = release.key
            st = startrek_status_key_to_str(release.status.key)
            if st in STATUSES_TO_COMMENT:
                startrek.issues[key].comments.create(text=message + u"\n" + get_sign_comment(os.path.basename(__file__)))
            else:
                skip_comment = True
            if skip_comment:
                sys.stderr.write("skipping comment for status '%s'" % st)

        #new_versions = {'test1': '1.34831-1'}
        return new_versions


    def handle(self, **options):
        with locked_file('log_jira_updates'):
            flag = Flag.objects.get(name='testupdate_last_jira_comment')

            if flag.value == '':
                old_versions = {}
            else:
                old_versions = json.loads(flag.value)


            new_versions = self.do_all_work(old_versions)

            flag.value = json.dumps(new_versions, sort_keys=True)
            flag.save()
        return

