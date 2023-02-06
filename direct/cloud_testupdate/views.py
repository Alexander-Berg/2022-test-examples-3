# -* encoding: utf-8 -*-
from django.http import HttpResponse, Http404, HttpResponseNotFound
from django.shortcuts import render_to_response, redirect
from django.template import RequestContext
from django.contrib.auth.decorators import login_required
from django.db.models import Q
import math, sys, tempfile, yaml
from subprocess import call
from datetime import datetime, timedelta
from urllib import urlencode


from django.conf import settings
from releaser.rights.tools import allowed, has_right

@login_required
def index(r):
    if not settings.CLOUD_TESTUPDATE_ENABLED:
        raise Http404

    return render_to_response('cloud_testupdate/index.html', {},
        context_instance=RequestContext(r))


@login_required
@allowed(['developer', 'test_engineer'])
def update(r):
    app = r.GET.get('app')
    runtime_port = r.GET.get('runtime_port')
    version = r.GET.get('version', '').strip()

    cmd = 'direct-test-update test-cloud %s=%s %s' % (app, version, runtime_port)
    logfile = tempfile.TemporaryFile()
    status = call(cmd, shell=True, stdout=logfile, stderr=logfile)
    logfile.seek(0)
    update_log = logfile.read()

    return render_to_response('cloud_testupdate/done.html', {
            'app': app,
            'runtime_port': runtime_port,
            'version': version,
            'status': status,
            'update_log': update_log,
        },
        context_instance=RequestContext(r))


