# coding: utf-8

import json
import os
import yatest.common


def update_floats(js):
    if isinstance(js, list):
        for i in range(len(js)):
            js[i] = update_floats(js[i])
        return js
    if isinstance(js, dict):
        for k, v in js.items():
            js[k] = update_floats(v)
        return js
    if isinstance(js, float):
        denom = 255.0
        return int(js * denom) * 1.0 / denom
    return js


def update_file(js):
    tmpname = js + '.tmp'
    with open(tmpname, 'w') as w:
        for i in open(js).readlines():
            url, val = i.strip().split('\t', 1)
            try:
                val = json.loads(val)
                val = update_floats(val)
                val = json.dumps(val, sort_keys=True, separators=(',', ':'))
            except:
                pass
            print >> w, '%s\t%s' % (url, val)
    os.remove(js)
    os.rename(tmpname, js)


def test():
    dataPath = 'video/trigger_regression/videoneuralnettrigger'
    outFile = 'json.out'
    yatest.common.execute([
        yatest.common.binary_path('extsearch/video/quality/classifiers/danet_image_processor/bin/test'),
        '-i', yatest.common.data_path(dataPath + '/in.calcbin'),
        '-o', outFile,
        '-c', yatest.common.data_path(dataPath),
        '-n', '20'])
    update_file(outFile)
    return yatest.common.canonical_file(outFile)
