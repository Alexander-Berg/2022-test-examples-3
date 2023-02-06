#!/usr/bin/python2
# encoding: utf-8
# kate: space-indent on; indent-width 4; replace-tabs on;
#
import os
import os.path
import sys
import re
import vh
import yt.wrapper as ytw
from traceback import format_exception
from subprocess import Popen, PIPE
from random import randrange
from urllib import urlopen

DOWNLOAD_FORMULA_FILE_URL = "https://fml.yandex-team.ru/download/computed/formula?%s"
SAVE_APPLIED_POOL_PARAMS_URL = "https://web.so.yandex-team.ru/ml/save_applied_pool_params/?%s"


def get_traceback():
    exc_type, exc_value, exc_traceback = sys.exc_info()
    tb = ''
    for step in format_exception(exc_type, exc_value, exc_traceback):
        try:
            tb += "\t" + step.strip() + "\n"
        except:
            pass
    return tb


def writelog(msg, isTB=False):
    if not msg:
        return
    try:
        tb = "\n"
        if isTB:
            tb = get_traceback()
        print >>sys.stderr, msg, tb
    except Exception, e:
        print >>sys.stderr, "Writelog error: %s" % str(e)


def getUUID(sep='-'):
    if not sep:
        sep = '-'
    chars, s = ['a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'], []
    for i in range(32):
        s.append(chars[int(randrange(16))])
    s.insert(8,  sep)
    s.insert(13, sep)
    s.insert(18, sep)
    s.insert(23, sep)
    return ''.join(s)


def getUniqFileName(template, existance_check_func=os.path.exists):
    uid = getUUID()
    while existance_check_func(template.format(uid)):
        uid = getUUID()
    return template.format(uid)


def doRequest(url_template, params, prompt):
    try:
        f = urlopen(url_template % '&'.join(map(lambda it: "%s=%s" % (it[0], it[1]), params.items())))
        if f.getcode() == 200:
            return f.read()
        else:
            writelog('{0} response HTTP code: {1}, body: {2}'.format(prompt, f.getcode(), f.info()))
    except Exception, e:
        print >>sys.stderr, '%s HTTP request failed: %s.%s' % (prompt, str(e), get_traceback())
    return ""


def check_mn(ref_value, mn_value):
    ft, np = ["t", "f"], ["n", "p"]
    return ft[ref_value ^ mn_value] + np[mn_value]


def matrixnet(mx_ops, formula_id, src, dst, yt_token):
    matrixnetfile = getUniqFileName('./matrixnet_%s_{0}.info' % formula_id)
    try:
        f = open(matrixnetfile, 'w')
        print >>f, doRequest(DOWNLOAD_FORMULA_FILE_URL, {"id": formula_id, "file": 'matrixnet.info'}, "Downloading matrixnet.info file")
        f.close()
    except Exception, e:
        writelog("Downloading matrixnet.info file failed: %s" % str(e), True)
    mn_info = slice_info = mx_ops_stdout = ''
    try:
        p = Popen("%s info %s" % (mx_ops, matrixnetfile), shell=True, stdin=PIPE, stdout=PIPE)
        mn_info = p.stdout.read()
    except Exception, e:
        writelog("Error while retrieving matrixnet info: %s" % str(e), True)
    writelog("Matrixnet info: %s" % mn_info)
    m = re.search(r"Slices:\t(.*?)\n", mn_info)
    if m and m.group(1):
        f = open("./slicesinfo", "w")
        f.write(m.group(1))
        f.close()
        slice_info = "--slices-info ./slicesinfo"
    cmd = '%s calc -s 4 --mr-server hahn.yt.yandex.net --mr-user so_fml --mr-src "%s" --mr-dst "%s" %s %s' % (mx_ops, src, dst, slice_info, matrixnetfile)
    try:
        p = Popen(cmd, shell=True, stdin=PIPE, stdout=PIPE, stderr=PIPE, env={'YT_TOKEN': yt_token})
        p.wait()
        mx_ops_stdout = p.stdout.read()
        if mx_ops_stdout:
            writelog("MX_OPS stdout: %s\nMX_OPS output's end" % mx_ops_stdout)
        mx_ops_stderr = p.stderr.read()
        if mx_ops_stderr:
            writelog("MX_OPS stderr: %s\nMX_OPS errors output's end" % mx_ops_stderr)
    except Exception, e:
        writelog("Error while applying model to pool: %s" % str(e), True)
    os.unlink(matrixnetfile)


@vh.lazy.hardware_params(vh.HardwareParams(max_ram=1024))
@vh.lazy(object, formula1_id=vh.mkinput(int), threshold=vh.mkinput(float), formula2_id=vh.mkinput(int), yt_pool=vh.mkinput(str), mx_ops=vh.mkinput(vh.Executable),
         main_formula_id=vh.mkinput(int), route=vh.mkinput(str), yt_tmp_folder=vh.mkinput(str), yt_token=vh.Secret)
def applyFormulaToPool(formula1_id, threshold, formula2_id, yt_pool, mx_ops, main_formula_id, route, yt_tmp_folder, yt_token):
    ytw.config['proxy']['url'] = 'hahn.yt.yandex.net'
    ytw.config['token'] = yt_token.value
    if not ytw.exists(yt_tmp_folder):
        ytw.create('map_node', yt_tmp_folder)
    YT_TMP_OUTPUT = getUniqFileName("%s/applied_formula_%s_2pool_%s_{0}" % (yt_tmp_folder, formula1_id, formula2_id), ytw.exists)
    matrixnet(mx_ops, formula1_id, yt_pool, YT_TMP_OUTPUT, yt_token.value)
    workflow_info = yt_tmp_folder[yt_tmp_folder.rfind('/') + 1:].split('_')
    if not (ytw.exists(YT_TMP_OUTPUT) and ytw.row_count(YT_TMP_OUTPUT) == ytw.row_count(yt_pool)):
        writelog("Source table (%s): %s" % (ytw.row_count(yt_pool) if ytw.exists(yt_pool) else 'missing', str(yt_pool)))
        writelog("Destination table (%s): %s" % (ytw.row_count(YT_TMP_OUTPUT) if ytw.exists(YT_TMP_OUTPUT) else 'missing', str(YT_TMP_OUTPUT)))
        info = "MX_OPS error: output YT table does not exist or is empty!"
        doRequest(SAVE_APPLIED_POOL_PARAMS_URL, {
            'formula1':             formula1_id,
            'formula2':             formula2_id,
            'status':               'failed',
            'info':                 info,
            'pool':                 yt_pool,
            'main_formula_id':      main_formula_id,
            'workflow_id':          workflow_info[0],
            'workflow_instance_id': workflow_info[1],
            'route':                route
        }, 'Saving parameters of pool with applied formula')
        print >>sys.stderr, info
        sys.exit(1)
    else:
        writelog("Model applyed to pool successfully!")
    res = {'tp': 0, 'tn': 0, 'fp': 0, 'fn': 0, 'total': 0}
    for record in ytw.read_table(YT_TMP_OUTPUT, format=ytw.JsonFormat(), raw=False):
        rec_list = str(record["value"]).split("\t")
        res[check_mn(int(rec_list[0]), 1 if float(rec_list[-1]) > threshold else 0)] += 1
        res['total'] += 1
    result = {
        'formula1':             formula1_id,
        'formula2':             formula2_id,
        'precision':            res['tp'] * 1.0 / (res['tp'] + res['fp']) if res['tp'] + res['fp'] > 0 else 0,
        'recall':               res['tp'] * 1.0 / (res['tp'] + res['fn']) if res['tp'] + res['fn'] > 0 else 0,
        'accuracy':             (res['tp'] + res['tn']) * 1.0 / res['total'],
        'status':               'completed',
        'pool':                 yt_pool,
        'main_formula_id':      main_formula_id,
        'workflow_id':          workflow_info[0],
        'workflow_instance_id': workflow_info[1],
        'route':                route,
        'info':                 'success'
    }
    result['f_measure'] = 2.0 * result['precision'] * result['recall'] / (result['precision'] + result['recall']) if result['precision'] + result['recall'] > 0 else 0
    ytw.remove(YT_TMP_OUTPUT, force=True)
    doRequest(SAVE_APPLIED_POOL_PARAMS_URL, result, 'Saving parameters of pool with applied formula')
    return result
