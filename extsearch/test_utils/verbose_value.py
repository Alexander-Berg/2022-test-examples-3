import extsearch.ymusic.scripts.reindex.gendocs as gd

from collections import OrderedDict


def verbose_value(value):
    verbose = OrderedDict()
    for kv in value.split('\t'):
        k, v = kv.split('=', 1)
        verbose_v = [gd.unescape(v_part) for v_part in v.split('\v')]
        verbose[k] = verbose_v
    return verbose
