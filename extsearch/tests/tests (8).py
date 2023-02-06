# coding: utf-8

import re
import os
import yatest.common


def censor_bandwidth(playlist_file):
    content = None
    with open(playlist_file, 'r') as f:
        content = f.read()

    content = re.sub('BANDWIDTH=\d+', 'BANDWIDTH=XXX', content)
    with open(playlist_file, 'w') as f:
        f.write(content)


def censor_timestamp(webhook_file):
    content = None
    with open(webhook_file, 'r') as f:
        content = f.read()

    content = re.sub('"LowResCreatedAt":\d+', '"LowResCreatedAt":0', content)
    content = re.sub('"AllResCreatedAt":\d+', '"AllResCreatedAt":0', content)
    with open(webhook_file, 'w') as f:
        f.write(content)


def test_q(tmpdir):
    transcoder = yatest.common.binary_path('extsearch/video/robot/rt_transcoder/transcoder/bin/faas_transcoder')

    tmp_dir = str(tmpdir.join('tmp'))
    os.mkdir(tmp_dir)
    out_dir = str(tmpdir.join('out'))
    os.mkdir(out_dir)

    out_info_file = os.path.join(out_dir, 'out-info.json')
    webhook_file = os.path.join(out_dir, 'webhook.json')

    yatest.common.execute([
        transcoder,
        'single',
        '--ffmpeg', './pack/ffmpeg',
        '--jpegtran', './jpegtran',
        '--tmpdir', tmp_dir,
        '--task', 'test_q/task.json',
        '--info', 'test_q/info.json',
        '--metarobot', os.path.join(out_dir, 'metarobot.json'),
        '--webhook', webhook_file,
        '--in-files', 'test_q',
        '--out-files', out_dir,
        '--out-info', out_info_file,
    ])

    out_files = sorted([f for root, _, files in os.walk(out_dir) for f in files])

    censor_bandwidth(os.path.join(out_dir, 'v.m3u8'))
    censor_timestamp(webhook_file)
    censor_timestamp(out_info_file)

    return [
        out_files,
        yatest.common.canonical_file(webhook_file),
        yatest.common.canonical_file(out_info_file),
        yatest.common.canonical_file(os.path.join(out_dir, 'v.m3u8')),
        yatest.common.canonical_file(os.path.join(out_dir, 'v_43_240p.m3u8')),
        yatest.common.canonical_file(os.path.join(out_dir, 'v_43_360p.m3u8')),
    ]
