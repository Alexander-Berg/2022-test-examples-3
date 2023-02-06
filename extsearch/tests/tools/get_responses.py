#!/usr/bin/env python2.7


import json
import socket
import time
import requests
import tempfile
import sys
from argparse import ArgumentParser, FileType
from subprocess import Popen, PIPE


def replace_dynamic_fields(ans_js):
    for ans in ans_js['answers']:
        data = ans.get('binary', {}).get('data', {})
        if 'SegmentId' in data.get('Head', {}):
            data['Head']['SegmentId'] = 'SEGMENT_ID_SUBST'
        for search_prop in data.get('SearcherProp', []):
            if search_prop['Key'] == 'HostName':
                search_prop['Value'] = 'HOST_NAME_SUBST'
        for grouping in data.get('Grouping', []):
            for group in grouping.get('Group', []):
                for doc in group.get('Document', []):
                    doc['ArchiveInfo']['Mtime'] = 'MTIME_SUBST'


def ping(url, tries, timeout=1):
    while tries > 0:
        try:
            resp = requests.get(url, timeout=1)
            if resp.status_code == 200:
                return
        except:
            time.sleep(timeout)
        tries -= 1
    raise Exception('can not start server')


def reserve_port():
    # Do not discard returned socket until the service binds to the port,
    # or it will be freed.
    sock = socket.socket(socket.AF_INET6)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(("::", 0))
    port = sock.getsockname()[1]
    return sock, port


class Daemon():

    def __init__(self, args):
        self.stderr = sys.stderr
        self.tries = 30

        self.args = args
        self.test_config = tempfile.NamedTemporaryFile()

        self.proc = None
        self.server_sock, self.server_port = reserve_port()
        self.apphost_sock, self.apphost_port = reserve_port()

        self.url = 'localhost:{port}'.format(port = self.apphost_port)
        self.ping_url = 'http://localhost:{port}'.format(port = self.apphost_port)

        self.fill_test_config(args)
        daemon_args = [args.daemon, self.test_config.name]
        self.proc = Popen(daemon_args, shell=False, stdout=PIPE, stderr=PIPE)
        ping(self.ping_url, self.tries)

    def __del__(self):
        self.proc.kill()
        out, err = self.proc.communicate()
        print >>self.stderr, out
        print >>self.stderr, err



    def fill_test_config(self, args):
        substs = [
            ('$SERVER_PORT', self.server_port),
            ('$APPHOST_PORT', self.apphost_port),
            ('$GEODB_PATH', args.geodb),
            ('$DB_PATH', args.db),
        ]
        with open(args.daemon_cfg, 'r') as fi:
            subst_config = fi.read()
        for templ, val in substs:
            subst_config = subst_config.replace(templ, str(val))
        with open(self.test_config.name, 'w') as fo:
            fo.write(subst_config)


def ensure_relative_exec(path):
    assert path and path[0] in './'


def get_args():
    ap = ArgumentParser(description='This script runs and shoots fastres2 daemon')
    ap.add_argument('--servant', required=True, help='path to servant_client')
    ap.add_argument('--apphost_ops', required=True, help='path to app_host_ops')
    ap.add_argument('--daemon', required=True, help='path to fastres2 daemon')
    ap.add_argument('--daemon_cfg', required=True, help='path to fastres2 daemon config')
    ap.add_argument('--geodb', required=True, help='path to geodb')
    ap.add_argument('--db', required=True, help='path to fastres2 db')
    ap.add_argument('--requests', required=True, help='requests')
    args = ap.parse_args()
    ensure_relative_exec(args.servant)
    ensure_relative_exec(args.apphost_ops)
    ensure_relative_exec(args.daemon)
    return args


def main():
    args = get_args()
    daemon = Daemon(args)
    servant_cmd = [args.servant, daemon.url, '-P', args.requests]
    process = Popen(servant_cmd, stdout=PIPE, stderr=PIPE)
    out_serv, err_serv = process.communicate()
    assert process.returncode == 0
    for line in out_serv.strip().split('\n'):
        apphost_ops_cmd = [args.apphost_ops, 'print-context', '-t' 'service_response']
        process = Popen(apphost_ops_cmd, stdout=PIPE, stderr=PIPE, stdin=PIPE)
        out_ops, err_ops = process.communicate(input=line)
        assert process.returncode == 0
        ans_js = json.loads(out_ops)
        replace_dynamic_fields(ans_js)
        print json.dumps(ans_js, indent=1)


if __name__ == '__main__':
    main()
