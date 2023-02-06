# -*- encoding: utf-8 -*-
# django-admin.py process_releaseplans --settings=releaser.settings.direct --pythonpath=../

from __future__ import with_statement

import httplib2
from urllib import urlencode

from django.core.management.base import BaseCommand
from django.db import transaction, models
from django.core.files import locks

from pprint import pprint as p
import tempfile, re, string, os, time, sys, yaml
from datetime import datetime, timedelta
from optparse import make_option
from contextlib import contextmanager
from urllib import urlencode

#import copy

from django.db.models import Q
import json
from django.conf import settings
from releaser.versionica.models import HostProperty, PropertyGroup
from releaser.svnrelease.models import SvnRelease
from releaser.utils import locked_file
from releaser.testupdate.testservers import test_update, testservers_conf
from releaser.yambclient.client import YambClient
from releaser.rights.models import HasRight
from releaser.rights.tools import exclusive_holder


class Command(BaseCommand):
    """
    Если в релиз-тикете записана новая версия, а ТС не обновлена -- yamb-уведомления тестировщикам
    """
    debug = True

    @transaction.commit_on_success
    def do_all_work(self):
        # есть релиз, который тестируется?
        release = SvnRelease.objects.all().filter().order_by('-base_rev')[0]
        if not release.jira_status in ['Testing', 'New']:
            return

        if release.base_rev == release.tip_rev:
            version = "1.%s-1" % (release.base_rev)
        else:
            version = "1.%s.%s-1" % (release.base_rev, release.tip_rev)

        # какие версии установлены сейчас
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

        # текст сообщения
        message = []
        params = {'testserver': []}
        for alias in new_versions:
            if new_versions[alias] != version:
                message += [ "%s: %s" % (alias, new_versions[alias]) ]
                params['testserver'] += [ alias ]

        if len(message) <= 0:
            return

        params['version'] = version

        testupdate_url = "http://%s/testupdate?%s" % (settings.RELEASER_DNS, urlencode(params, True))
        message_text = u"**Мониторинг ТС**\nОбнаружены расхождения в версиях ТС для релизов\n\nАктуальная версия: %s\n\nРасхождения:\n%s\n\nОбновление ТС: %s" % (version, "\n".join(message), testupdate_url)

        yamb = YambClient()
        # кому отправлять сообщение
        to = [ rec.user.login for rec in HasRight.objects.filter(right__right = "receive_ts_update_notifications") ]
        # TODO "True or" убрать
        if True or len(to) == 0:
            to += [exclusive_holder('release_manager').login]

        if len(to) == 0:
            raise Exception("empty 'to' list")

        #sys.stderr.write("to: %s" % to)
        for login in to:
            yamb.send_message(login, message_text)

        return


    def handle(self, **options):
        with locked_file('testupdate_yamb'):
            self.do_all_work()

        return

