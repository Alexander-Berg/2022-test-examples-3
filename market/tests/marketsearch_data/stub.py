#!/usr/bin/env python
# coding: utf-8

import logging
import os
import sys
import json

DATA_FILE = 'torrent_stub_data_file'


def touch(filepath):
    makedirs(os.path.dirname(filepath))
    with open(filepath, 'w') as fobj:
        fobj.write('')


def makedirs(dirpath):
    if not os.path.isdir(dirpath):
        os.makedirs(dirpath)


def save_download_dir(dist_version, path):
    with open(DATA_FILE, 'r') as f:
        data = json.load(f)
    data[dist_version] = path
    log = logging.getLogger('')
    log.info(str(data))
    with open(DATA_FILE, 'w') as f:
        json.dump(data, f)


def get_download_dir(dist_version):
    with open(DATA_FILE, 'r') as f:
        data = json.load(f)
    return data.get(dist_version)


def stub():
    logging.basicConfig(level=logging.DEBUG, format='STUB %(process)d | %(message)s')
    log = logging.getLogger('')
    log.debug('cmd: "%s"', ' '.join(sys.argv))

    prog, args = sys.argv[0], sys.argv[1:]
    monitorings = [
        '/home/monitor/agents/modules/advq_local.py',
        '/home/monitor/agents/modules/phrase_srv_monitor.py',
    ]
    for monitoring in monitorings:
        if prog.find(monitoring) != -1:
            log.info('print 0;OK')
            print('0;OK')
            return

    if os.path.basename(prog) == 'generate.sh':
        return

    cmd = args[0]
    if cmd == 'start_dist':
        dist_name, metafile_url, download_dir, on_complete_flag_path = args[1:5]
        log.info('touch %s', on_complete_flag_path)
        log.info('babaka dist_name: %s, metafile_url: %s, download_dir: %s',
                 dist_name, metafile_url, download_dir)
        if not metafile_url.startswith('http://'):
            raise RuntimeError('bad metafile_url: %s' % metafile_url)
        touch(on_complete_flag_path)
    elif cmd == 'start_dist_version':
        dist_name, dist_version, metafile_url, download_dir, on_complete_flag_path = args[1:6]
        save_download_dir(dist_version, download_dir)
        log.info('touch %s', on_complete_flag_path)
        log.info('babaka dist_name: %s, dist_version: %s, metafile_url: %s, download_dir: %s',
                 dist_name, dist_version, metafile_url, download_dir)
        if not metafile_url.startswith('http://'):
            raise RuntimeError('bad metafile_url: %s' % metafile_url)
        touch(on_complete_flag_path)
    elif cmd == 'stop_dist':
        dist_name, move_to_dir = args[1:3]
        makedirs(move_to_dir)
        filepath = os.path.join(move_to_dir, dist_name)
        log.info('touch %s', filepath)
        touch(filepath)
    elif cmd == 'stop_dist_version':
        dist_name, dist_version, move_to_dir = args[1:4]
        if move_to_dir == '-c':
            move_to_dir = None
        download_dir = get_download_dir(dist_version)
        filepath = os.path.join(download_dir, 'cataloger/toca.xml')
        log.info('touch %s', filepath)
        touch(filepath)
        if move_to_dir is not None:
            os.rename(download_dir, move_to_dir)


if '__main__' == __name__:
    if not os.path.exists(DATA_FILE):
        with open(DATA_FILE, 'w') as f:
            f.write('{}')
    stub()
