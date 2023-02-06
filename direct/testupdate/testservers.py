# -* encoding: utf-8 -*-
from django.db.models import Q
import math, sys, re, tempfile, yaml
from subprocess import call, STDOUT

from django.conf import settings
from releaser.testupdate.models import *


def test_update(server_alias, version, **O):
    if 'conf' in O:
        conf = O['conf']
    else:
        conf = yaml.load(open(settings.TESTUPDATE_TESTSERVERS_FILE))

    if not server_alias in conf:
        raise Exception(("testserver '%s' not found" % server_alias))
    if not re.match(r'^[~a-zA-Z0-9\.\-]+$', version):
        raise Exception("incorrect version '%s'" %  version)

    host = conf[server_alias]['hostname']
    reqid = O['reqid'] if 'reqid' in O else 0

    logfile = tempfile.TemporaryFile()
    cmd = 'ssh -o StrictHostKeyChecking=no updater@%s %s' % ( host, version )
    status = call(cmd, shell=True, stdout=logfile, stderr=logfile)
    logfile.seek(0)
    testupdate_log = logfile.read()

    #sys.stderr.write("status: %s\nlog:\n%s\n" % (status, testupdate_log))

    rec = TestUpdateLog.objects.create(
            reqid=reqid,
            testserver=server_alias,
            version=version,
            status=status,
            logtext=testupdate_log
            )

    return status


conf = None

# TODO сделать параметр "отдавать все, или без cocaine" + возможно, другие фильтры
def testservers_conf():
    global conf
    if conf == None:
        conf = yaml.load(open(settings.TESTUPDATE_TESTSERVERS_FILE))

    filtered_conf = {}
    for s in conf:
        if not 'cocaine' in conf[s] or conf[s]['cocaine'] == 0:
            filtered_conf[s] = conf[s]
    return filtered_conf


