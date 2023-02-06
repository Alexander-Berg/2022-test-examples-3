#!/usr/bin/env python
from flask import Flask, redirect, request, render_template, url_for
from waitress import serve
import yt.wrapper as yt
import argparse
import jinja2
import re
from datetime import datetime
from hashlib import md5
from extsearch.video.robot.crawling.player_testing.viewer.config import Config  # noqa
import library.python.resource as pyres


app = Flask(__name__)
app.config['DEBUG'] = False
app.config.from_object('extsearch.video.robot.crawling.player_testing.viewer.config.Config')
app.jinja_loader = jinja2.DictLoader(dict(pyres.iteritems(
    prefix='/templates/',
    strip_prefix=True,
)))


def get_navigation():
    return [
        {'url': url_for(index.__name__), 'name': 'Database'},
        {'url': url_for(host_add.__name__), 'name': 'Host check'}
    ]


def fix_host(src_host):
    host = None
    if not src_host:
        return host
    try:
        vec = src_host.strip().split('/')
        if vec[0] in ['http:', 'https:']:
            host = vec[2]
        else:
            host = vec[0]
        host = host.lower().encode('idna')
        if not re.match(r'^[a-z0-9\.\-]+$', host):
            return None
    except Exception:
        pass
    return host


def query_db(host_hint, db, result):
    cond = 'WHERE is_substr("{}", Host)'.format(host_hint) if host_hint else ''
    query_pattern = '* FROM [{prefix}/{db}.db] {cond} ORDER BY Timestamp DESC LIMIT 10'
    for row in yt.select_rows(query_pattern.format(prefix=app.config['PLAYER_HOME'], db=db, cond=cond)):
        result.append({
            'host': row['Host'].decode('idna'),
            'platform': row['Platform'] if db == 'raw' else '',
            'player_id': '__{}_{}_{}__'.format(db, row['PlayerSource'], row['PlayerType']) if row['PlayerSource'] else '',
            'timestamp': datetime.fromtimestamp(row['Timestamp']).strftime('%Y-%m-%d'),
            'status': row['Status'],
            'popup': 'Yes' if db == 'raw' and not row['NoPopups'] else '',
            'info': row['Info'] or '',
            'sample': row['Sample'] or ''
        })


def query_hosts(host_hint=None):
    hosts = []
    query_db(host_hint, 'raw', hosts)
    query_db(host_hint, 'html5', hosts)
    return hosts


def render(name, **kwargs):
    return render_template(name, navigation=get_navigation(), nav_active=request.path, **kwargs)


@app.route('/push', methods=['POST', 'GET'])
def host_push():
    host = fix_host(request.args.get('host'))
    if not host:
        return "host missing", 400
    data = [{'Host': host, 'Requester': request.args.get('from'), 'Priority': app.config['HIGH_PRIO']}]
    yt.write_table(yt.ypath_join(app.config['PLAYER_HOME'], 'incoming', md5(host).hexdigest()), data)
    return 'host {} successfully planned for check'.format(host), 200


@app.route('/')
def root():
    return redirect(url_for(index.__name__))


@app.route('/index')
def index():
    return render('index.html', hosts=query_hosts(fix_host(request.args.get('host'))))


@app.route('/add')
def host_add():
    return 'host checking is temporarily unavailable, contact dev team in urgent cases', 503  # render('add.html')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--bind-port', default=app.config['BIND_PORT'], type=int)
    args = ap.parse_args()
    serve(app, host='::', port=args.bind_port)
