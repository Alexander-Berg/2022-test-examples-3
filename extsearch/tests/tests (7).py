# coding: utf-8

import base64
from numpy.linalg import norm
import numpy as np
import json
import os
import yatest.common

from cv.imgclassifiers.tf_applicator.models.video.adcut.protos.result_pb2 import TAdCutDetectionModelResult

from http_server import HttpServer


def compare_ad_cut(expected_b64, value_b64):
    expected = TAdCutDetectionModelResult()
    expected.ParseFromString(base64.b64decode(expected_b64))

    value = TAdCutDetectionModelResult()
    value.ParseFromString(base64.b64decode(value_b64))

    assert len(expected.Points) == len(value.Points)
    for i in range(len(expected.Points)):
        expected_p = expected.Points[i]
        value_p = value.Points[i]
        assert expected_p.MinTimestamp == value_p.MinTimestamp
        assert expected_p.MaxTimestamp == value_p.MaxTimestamp
        assert abs(expected_p.Score - value_p.Score) < 1e-3


def compare_vpq3(expected_b64, value_b64):
    expected = np.frombuffer(base64.b64decode(expected_b64), dtype=np.float32)
    value = np.frombuffer(base64.b64decode(value_b64), dtype=np.float32)

    assert len(expected) == len(value)
    assert np.dot(expected, value) / (norm(expected) * norm(value))


def make_task_file(sigs, to):
    with open(to, 'w') as f:
        f.write('''StoreAudio: false
StoreVideo: false
SignaturesAlgoMask: %d''' % sigs)


def make_config_file(models, to):
    with open(to, 'w') as f:
        f.write('''Metarobot {
    NNModelsPath: "%s"
}''' % models)


def test_kaltura_adcut_v2(tmpdir):
    metarobot_bin = yatest.common.binary_path('extsearch/video/robot/rt_transcoder/metarobot/bin/faas_metarobot')

    out_dir = str(tmpdir.join('out'))
    os.mkdir(out_dir)

    task_file = str(tmpdir.join('task.pb.txt'))
    make_task_file(1024, task_file)

    config_file = str(tmpdir.join('config.pb.txt'))
    make_config_file('nn_models/', config_file)

    server = HttpServer(10800, 'zen_kaltura')
    server.start()

    try:
        yatest.common.execute([
            metarobot_bin,
            'single',
            '-i', 'http://localhost:10800/desc_5abbca90949a9ae9cb8f1179d83b12f6.json',
            '-p', task_file,
            '-o', out_dir,
            '-c', config_file,
        ])
    finally:
        server.stop()

    with open(os.path.join(out_dir, 'signatures.json'), 'r') as f:
        sigs = json.load(f)
        compare_ad_cut('Cg0IhpUIEIaVCB0GYhM/Cg0IgO8NEIDvDR3zG80+', sigs['VideoAdCutDetectionV2'])


def test_kaltura_adcut_long_video(tmpdir):
    metarobot_bin = yatest.common.binary_path('extsearch/video/robot/rt_transcoder/metarobot/bin/faas_metarobot')

    out_dir = str(tmpdir.join('out'))
    os.mkdir(out_dir)

    task_file = str(tmpdir.join('task.pb.txt'))
    make_task_file(1024, task_file)

    config_file = str(tmpdir.join('config.pb.txt'))
    make_config_file('nn_models/', config_file)

    server = HttpServer(10800, 'zen_kaltura_2')
    server.start()

    try:
        yatest.common.execute([
            metarobot_bin,
            'single',
            '-i', 'http://localhost:10800/desc_ab6507c2bfae88297494c4799880746d.json',
            '-p', task_file,
            '-o', out_dir,
            '-c', config_file,
        ])
    finally:
        server.stop()

    with open(os.path.join(out_dir, 'signatures.json'), 'r') as f:
        sigs = json.load(f)
        compare_ad_cut('Cg0IyPM0EMjzNB0xKSE/Cg0I8L03EPC9Nx1e/Jk+Cg0IsNg+ELDYPh2zWUM/Cg0I0I13ENCNdx0g9Z0+', sigs['VideoAdCutDetectionV2'])


def test_kaltura_vpq3_and_adcut_v2(tmpdir):
    metarobot_bin = yatest.common.binary_path('extsearch/video/robot/rt_transcoder/metarobot/bin/faas_metarobot')

    out_dir = str(tmpdir.join('out'))
    os.mkdir(out_dir)

    task_file = str(tmpdir.join('task.pb.txt'))
    make_task_file(512+1024, task_file)

    config_file = str(tmpdir.join('config.pb.txt'))
    make_config_file('nn_models/', config_file)

    server = HttpServer(10801, 'zen_kaltura')
    server.start()

    try:
        yatest.common.execute([
            metarobot_bin,
            'single',
            '-i', 'http://localhost:10801/desc_5abbca90949a9ae9cb8f1179d83b12f6.json',
            '-p', task_file,
            '-o', out_dir,
            '-c', config_file,
        ])
    finally:
        server.stop()

    with open(os.path.join(out_dir, 'signatures.json'), 'r') as f:
        sigs = json.load(f)
        compare_ad_cut('Cg0IhpUIEIaVCB0GYhM/Cg0IgO8NEIDvDR3zG80+', sigs['VideoAdCutDetectionV2'])
        compare_vpq3('fCukPeBpTb3DOLs8VTUHvBqrtrzMHrm8I0G9PVz1xTzx5gu+yPEHPf+Ajj1PHSu9Vp19vTjm7TyQvvc9gy\/EPU0+1bvykmk9XOndvEgVKz1+JQQ+f\/KKPVQkLT0hJkq9mAUCPc+LyDzSt5A9oUdVvN\/8yz2ZzQS+b5mB'
                     'vZisjDwTP4q9RVzXvARM87xr1I29f9DOPSx84j0+qqQ8th0pPUIJoTy1Oj69nz4HPduSLT1Coto9rzCgPWhHuD19hw+9cNfOPMJMIb2h0US98pjDPR1nJ72P\/xK9v3PrvfkNwr0dmsA9SzQ0PRzJHL1dmTa90zofvms71r'
                     'sWv3o8EpnDPUzGxj2Et1C9aGPKvFFwe71H42M8\/l\/JO8\/NMz2CixQ8aUQOPT5CBj2O2gg9op+SPQ6oXT1LZBk8OlUbPSgjgzoJQ1Y9zq\/jvODbwj36Lp29PMH9vGEpTr0yJPa8E0hWPRmWZz2m4fs7v4UmPX5jYzyD6'
                     '2G8QsaLPWSUO7uC6SI9V9vmvT0nZz0jMBo+Zu+OvIDGHz2KfXa9MCmXPXR4aLydgYU7OkfyOlRmbzx5Rx+9pw2TvB0Arr3\/Vw89JE5fPBC5f72NID29ObZXvfTmF77lAAQ9A\/TdvbtKzT1SMg0+7JSdPauKOrwzMtc97N'
                     'cOPrrL4jyDfBw9b8pTPIIBMTxrgqo8GwDQO3kWiL3zQvE8PUOOvdHAcr1ZAcM8aGj+vVK4sD1sSna9Fp02vjbsX7zzhRQ9YTmsvT\/UV71JyAs+5ZpMPGlifT1c6b48ZQUmPq\/ATj0LmAy9+Es6Pc2zkj1dG4s85dllPeC'
                     'bizzUlC69SZbRPdv8hr2grCE841IXuwSeMr1nUl695yhUPT9SkjwcXZq8pOL3OkpZFj1z5MO88WqKvPWd67x9xPc8H3WIvPo3MDxriZM9yJr6vMRv87103z69DAKsPd6TCL3L1WQ9Ir39PB2hGb1IBMi7hyyFPewp+Lqjdu'
                     '89YwwEPkTQUb23J8C8oZBjPd5KA721L4o93xzWvYfIHz0WP4E9Wia\/vdxcWzxsCqW9ieb4vbM9j7yVeNQ7WvNivQSF3b3HG5Q9i1WVvIhkU70ub+48fZMyPcaTwL1evKa9K3WAvXjTvD1+bKw8NRh5PXehZ72WGUc8hIvX'
                     'OyiV2DyZtr09Ph8LPQLXNLyB1VS8ovtgvC4DKT3QapQ9NEHyu1zdhj3Mjrm8x3BCPaP+gD3A\/Ia9DlqMvAaNuj3EOKO7cjPMvd05p73BPce7WWT\/PCFrIr5pc7q9', sigs['VideoPlusQueryV3CV'])


def test_kaltura_adcut_v2_no_cuts(tmpdir):
    metarobot_bin = yatest.common.binary_path('extsearch/video/robot/rt_transcoder/metarobot/bin/faas_metarobot')

    out_dir = str(tmpdir.join('out'))
    os.mkdir(out_dir)

    task_file = str(tmpdir.join('task.pb.txt'))
    make_task_file(1024, task_file)

    config_file = str(tmpdir.join('config.pb.txt'))
    make_config_file('nn_models/', config_file)

    server = HttpServer(10802, 'zen_kaltura_1')
    server.start()

    try:
        yatest.common.execute([
            metarobot_bin,
            'single',
            '-i', 'http://localhost:10802/desc_0bdf1b72c9ada5f18550a1a71a3f652d.json',
            '-p', task_file,
            '-o', out_dir,
            '-c', config_file,
        ])
    finally:
        server.stop()

    return yatest.common.canonical_file(os.path.join(out_dir, 'signatures.json'))
