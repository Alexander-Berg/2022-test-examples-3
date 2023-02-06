# -* encoding: utf-8 -*-
from django.http import HttpResponse, Http404, HttpResponseNotFound
from django.shortcuts import render_to_response, redirect
from django.template import RequestContext
from django.contrib.auth.decorators import login_required
from django.db.models import Q
import math, sys, yaml
from subprocess import call
from datetime import datetime, timedelta
from urllib import urlencode


from django.conf import settings
from releaser.testupdate.models import *
from releaser.versionica.models import *
from releaser.rights.tools import allowed, has_right
from releaser.testupdate.testservers import test_update, testservers_conf
from releaser.logcmd.models import LogCmd

@login_required
def index(r):
    checks_and_defaults()
    testservers_to_update = r.GET.getlist('testserver')
    version_to_install = r.GET.get('version', '').strip()

    conf = testservers_conf()

    hosts = [ conf[h]['hostname'] for h in conf ]

    ready_versions = [v.version for v in ReadyVersion.objects.all().filter(testserver=settings.TESTUPDATE_TESTSERVER_ID).order_by('-version')[:70]]

    default_property = PropertyGroup.objects.get(name = "packages").default_property

    installed = {}
    for rec in HostProperty.objects.filter( host__name__in = hosts, property__name__regex = default_property ):
        installed[rec.host.name] = rec.value

    testservers = []
    for alias in conf:
        hostname = conf[alias]["hostname"]
        try:
            version = installed[hostname]
        except Exception, e:
            version = '-'
        testservers += [{
            "alias": alias,
            "hostname": hostname,
            "version": version,
            "description": conf[alias]["name"]
            }]
    testservers = sorted(testservers,  key=lambda p: p['alias'])

    return render_to_response('testupdate/index.html', {
        'ready_versions': ready_versions,
        'testservers': testservers,
        'version_to_install': version_to_install,
        'testservers_to_update': testservers_to_update,
        'may_register_versions': has_right(r.user.username, ['developer', 'test_engineer']),
        },
        context_instance=RequestContext(r))


@login_required
@allowed(['developer', 'test_engineer'])
def update(r):
    # перестаем поддерживать обновление через веб-интерфейс.
    # Вложений требует, преимуществ по сравнению с direct-release нет, и есть неудобья с рефрешем страницы и обновлением на неожиданные версии
    return HttpResponseNotFound("not supported")

    testservers = r.GET.getlist('testserver')
    version = r.GET.get('version', '').strip()
    force = r.GET.get('force', '').strip()

    conf = testservers_conf()

    if version == '':
        return HttpResponseNotFound("empty version")
    if len(testservers) <= 0:
        return HttpResponseNotFound("empty list of testservers to update")

    for testserver in testservers:
        if not testserver in conf:
            return HttpResponseNotFound(("testserver '%s' not found" % testserver))
        if conf[testserver]['allow_any_version'] != 1:
            return HttpResponseNotFound("allow_any_version should be 1 for %s" % testserver)
        # не даем дергать обновление слишком часто
        try:
            last_update = UpdateRequest.objects.filter(testserver=testserver).order_by('-logtime')[0]
        except IndexError:
            last_update = None
        if last_update != None and abs(last_update.logtime - datetime.now()).seconds < 120 and force != '1':
            return HttpResponseNotFound("previous update took place less than 2 min ago (%s)" % (testserver))

    ok = True
    for testserver in testservers:
        # может, вообще избавиться от этой таблицы? Есть ведь logcmd
        update = UpdateRequest(
                testserver = testserver,
                version = version,
                user = r.user.username,
                )
        update.save()
        status = test_update(testserver, version, reqid = r.reqid)
        ok = ok and status == 0
        if not ok:
            break

    # TODO для скриптов отвечать простым текстом
    params = r.GET.copy()
    params['reqid'] = r.reqid
    params['ok'] = 1 if ok else 0
    return redirect('/testupdate/done?%s' % params.urlencode())


@login_required
def done(r):
    testservers = r.GET.getlist('testserver')
    version = r.GET.get('version', '').strip()
    ok = r.GET.get('ok', 0).strip()
    reqid = r.GET.get('reqid', '').strip()

    conf = testservers_conf()

    hostname_regexp = "|".join([conf[a]['hostname'] for a in testservers])
    hostname_regexp = u"^(%s)$" % hostname_regexp
    versionica_link = '/versionica?%s' % urlencode({'host': hostname_regexp})

    return render_to_response('testupdate/done.html', {
        'testservers': [ conf[alias] for alias in testservers],
        'version': version,
        'ok': ok,
        'reqid': reqid,
        'update_reqid': reqid,
        'versionica_link': versionica_link,
        },
        context_instance=RequestContext(r))


def ready(r):
    checks_and_defaults()

    ip = r.META['REMOTE_ADDR']

    testserver = r.GET.get('testserver', '').strip()
    version = r.GET.get('version', '').strip()

    conf = testservers_conf()

    if version == '' or not testserver in conf:
        raise Http404

    # чтобы всякие в браузере оставшиеся открытыми страницы не перезагружались и не портили базу --
    # сверяем время, проставленное в ссылке, с текущим.
    date = datetime.strptime(r.REQUEST.get('time', ''), '%Y%m%d%H%M')
    now = datetime.now()
    delta = abs(now - date)
    #sys.stderr.write('today: %s time: %s delta: %s\n' % (now, date, delta.seconds))
    # Если расхождение слишком большое -- ничего не сохраняем
    if delta.seconds > 600:
        return HttpResponseNotFound('too big time delta')

    record, created = ReadyVersion.objects.get_or_create(testserver=testserver, version=version)
    record.save()

    return HttpResponse('OK', mimetype="application/javascript")


@login_required
@allowed(['developer', 'admin'])
def ready_authorized(r):
    """
    то же, что ready, но с авторизацией и без проверки времени
    """
    conf = testservers_conf()
    testserver = conf.keys()[0]

    version = r.GET.get('version', '').strip()

    if version == '' or not testserver in conf:
        raise Http404

    record, created = ReadyVersion.objects.get_or_create(testserver=testserver, version=version)
    record.save()
    return redirect('/testupdate')


@login_required
@allowed(['developer', 'test_engineer', 'admin'])
def steady(r):
    checks_and_defaults()

    aliases = r.GET.getlist('testserver')
    version = r.GET.get('version', '').strip()

    conf = testservers_conf()

    if version == '':
        raise Http404
    for a in aliases:
        if not a in conf:
            raise Http404

    testservers = []
    for a in aliases:
        conf[a]['alias'] = a
        testservers += [conf[a]]

    return render_to_response('testupdate/steady.html', {
        'version': version,
        'testservers': testservers,
        },
        context_instance=RequestContext(r))


@login_required
@allowed(['developer', 'test_engineer', 'admin'])
def logindex(r):
    checks_and_defaults()

    records = TestUpdateLog.objects.filter(logtime__gt=datetime.now() - timedelta(days=30)).order_by('-logtime')

    for rec in records:
        rec.logtime = rec.logtime.strftime('%Y-%m-%d %H:%M:%S')
        try:
            rec.author = LogCmd.objects.get(reqid=rec.reqid).login
        except Exception, e:
            rec.author = 'n/a'

    return render_to_response('testupdate/logindex.html', {
        'records': records,
        },
        context_instance=RequestContext(r))


@login_required
@allowed(['developer', 'test_engineer', 'admin'])
def showlog(r):
    checks_and_defaults()

    reqid = r.GET.get('reqid', '')
    testserver = r.GET.get('testserver', '')

    record = TestUpdateLog.objects.get(reqid=reqid, testserver=testserver)
    record.logtime = record.logtime.strftime('%Y-%m-%d %H:%M:%S')

    return render_to_response('testupdate/showlog.html', {
        'rec': record,
        },
        context_instance=RequestContext(r))


def checks_and_defaults():
    if not settings.TESTUPDATE_ENABLED:
        raise Http404

    try:
        settings.TESTUPDATE_TESTSERVER_ID
    except AttributeError:
        settings.TESTUPDATE_TESTSERVER_ID = '-'


