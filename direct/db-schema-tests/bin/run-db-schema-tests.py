#!/usr/bin/python
# -*- coding: utf-8 -*-
# vim: set expandtab:tabstop=4:softtabstop=4:shiftwidth=4:nowrap
# $Id$

import glob
import errno
import json
import os
import subprocess
import sys
import tempfile
import urllib
import urllib2
import xml.etree.ElementTree

DB_SCHEMA_SVN_URL = 'svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia/direct/db_schema/db'
LAST_REV_FILE = '/var/lib/db-schema-tests/last_rev'
CIFRONT_URL = 'https://javadirect-dev.yandex-team.ru/cifront/buildbot_status'

def run():
    work_dir = tempfile.mktemp(prefix='db-schema-tests.', dir='/tmp/temp-ttl/ttl_1d')
    os.mkdir(work_dir)
    os.chdir(work_dir)
    last_rev = 3000000
    try:
        last_rev = int(open(LAST_REV_FILE).read().rstrip())
    except IOError as e:
        if e.errno == errno.ENOENT:
            pass
        else:
            raise
    log_xml = subprocess.check_output(['svn', 'log', DB_SCHEMA_SVN_URL, '-r{}:HEAD'.format(last_rev + 1), '--xml', '--limit', '10'])
    root = xml.etree.ElementTree.fromstring(log_xml)
    revs = sorted(int(e.get('revision')) for e in root.findall('logentry'))
    if not revs:
        sys.stderr.write('nothing to do, exiting\n')
        sys.exit(0)
    subprocess.check_call(['svn', 'checkout', DB_SCHEMA_SVN_URL + '@' + str(revs[0]), 'db_schema'])
    subprocess.check_call(['svn', 'checkout', 'svn+ssh://svn.yandex.ru/direct/trunk/unit_tests', '--depth', 'empty'])
    subprocess.check_call(['svn', 'update', '--set-depth', 'infinity', 'unit_tests/db_schema'])
    subprocess.check_call(['svn', 'update', '--set-depth', 'infinity', 'unit_tests/deploy'])
    test_passed = {}
    with open('Settings.pm', 'w') as f:
        f.write('\n'.join([
            'package Settings;',
            '$Settings::ROOT="{}";'.format(work_dir),
            '$Yandex::DBSchema::DB_SCHEMA_ROOT=$Settings::ROOT."/db_schema";',
            '1;']))
    for rev in revs:
        print '### revision {}'.format(str(rev))
        subprocess.check_call(['svn', 'update', 'db_schema', '-r', str(rev)])
        test_passed[rev] = {}
        for t in glob.glob('unit_tests/db_schema/*.t') + ['unit_tests/deploy/migr_syntax.t']:
            test_passed[rev][t] = (subprocess.call(['prove', t]) == 0)
    f = open(LAST_REV_FILE, 'w+')
    f.write(str(revs[-1]))
    os.chmod(LAST_REV_FILE, 0666)
    events = []
    for rev in test_passed:
        failed = (len([t for t in test_passed[rev] if not test_passed[rev][t]]) > 0)
        if failed:
            text = 'failed'
        else:
            text = 'build successful'
        events.append(
            {
                'event': 'buildFinished',
                'payload': {
                    'build': {
                        'text': [text],
                        'builderName': 'db-schema-tests',
                        'properties': [['revision', str(rev)], ['buildnumber', None]],
                    }
                }
            }
        )
    data = urllib.urlencode({'packets': json.dumps(events)})
    urllib2.urlopen(CIFRONT_URL, data)

if __name__ == '__main__':
    run()

