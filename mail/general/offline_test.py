#!/usr/bin/python2
# encoding: utf-8
# kate: space-indent on; indent-width 4; replace-tabs on;
#
import os, os.path, sys, re, argparse, json, vh
import yt.wrapper as ytw
import nirvana.mr_job_context as nv
from traceback import format_exception
from collections import defaultdict
from subprocess import Popen, PIPE
from random import randrange
from urllib import urlopen

DOWNLOAD_FORMULA_FILE_URL = "https://fml.yandex-team.ru/download/computed/formula?%s"
GET_COMPARED_MODELS_URL = "https://web.so.yandex-team.ru/ml/get_compared_models/?%s"
SAVE_APPLIED_POOL_PARAMS_URL = "https://web.so.yandex-team.ru/ml/save_applied_pool_params/?%s"
OUTPUT_JSON_FILE = './output_params.json'
MX_OPS_EXECUTABLE = './mx_ops'

def get_traceback():
    exc_type, exc_value, exc_traceback = sys.exc_info()
    tb = ''
    for step in format_exception(exc_type, exc_value, exc_traceback):
        try:
            tb += "\t" + step.strip() + "\n"
        except:
            pass
    return tb

def writelog(msg, isTB = False):
    if not msg: return
    try:
        tb = "\n"
        if isTB:
            tb = get_traceback()
        print >>sys.stderr, msg, tb
    except Exception, e:
        print >>sys.stderr, "Writelog error: %s" % str(e)

def getUUID(sep = '-'):
    if not sep: sep = '-'
    chars, s = ['a', 'b', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'], []
    for i in range(32):
        s.append(chars[int(randrange(16))])
    s.insert(8,  sep)
    s.insert(13, sep)
    s.insert(18, sep)
    s.insert(23, sep)
    return ''.join(s)

def getUniqFileName(template, existance_check_func = os.path.exists):
    uid = getUUID()
    while existance_check_func(template.format(uid)):
        uid = getUUID()
    return template.format(uid)

def loadJSON(file_path, prompt = "input info"):
    try:
        f = open(file_path)
        info = json.loads(f.read())
        f.close()
    except Exception, e:
        writelog("Parsing input formula info error: %s." % str(e), True)
    writelog("%s: %s" % (prompt, str(info)))
    return info

def matrixnet(mx_ops, formula_id, src, dst):
    matrixnetfile = getUniqFileName('./matrixnet_%s_{0}.info' % formula_id)
    f = open(matrixnetfile, 'w')
    print >>f, doRequest(DOWNLOAD_FORMULA_FILE_URL, {"id": formula1_id, "file": 'matrixnet.info'}, "Downloading matrixnet.info file")
    f.close()
    p = Popen("%s info %s" % (mx_ops, matrixnetfile), shell = True, stdin = PIPE, stdout = PIPE)
    m = re.search(r"Slices:\t(.*?)\n", p.stdout.read())
    slice_info = ''
    if m and m.group(1):
        slice_info = m.group(1)
        f = open("./slicesinfo", "w")
        f.write(slice_info)
        f.close()
        slice_info = "--slices-info ./slicesinfo"
    cmd = '%s calc -s 4 --mr-server hahn.yt.yandex.net --mr-user so_fbl --mr-src "%s" --mr-dst "%s" %s %s' % (mx_ops, src, dst, slice_info, matrixnetfile)
    p = Popen(cmd, shell = True, stdin = PIPE, stdout = PIPE, stderr = PIPE)
    p.wait()
    mx_ops_stdout = p.stdout.read()
    if mx_ops_stdout:
        print "MX_OPS stdout: %s\nMX_OPS output's end" % mx_ops_stdout
    mx_ops_stderr = p.stderr.read()
    if mx_ops_stderr:
        print "MX_OPS stderr: %s\nMX_OPS errors output's end" % mx_ops_stderr
    os.unlink(matrixnetfile)

def check_mn(ref_value, mn_value):
    ft, np = ["t", "f"], ["n", "p"]
    return ft[ref_value ^ mn_value] + np[mn_value]

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

@vh.lazy(object, formula1_id=vh.mkinput(int), threshold=vh.mkinput(float), formula2_id=vh.mkinput(int), yt_pool=vh.mkinput(str), mx_ops_path=vh.mkinput(str), main_formula_id=vh.mkinput(int), route=vh.mkinput(str))
def applyFomulaToPool(formula1_id, threshold, formula2_id, yt_pool, mx_ops_path, main_formula_id, route):
    YT_TMP_OUTPUT = getUniqFileName("%s/applied_formula_%s_2pool_%s_{0}" % (YT_TMP_FOLDER, formula1_id, formula2_id), ytw.exists)
    matrixnet(mx_ops_path, formula1_id, yt_pool, YT_TMP_OUTPUT)
    if not (ytw.exists(YT_TMP_OUTPUT) and ytw.row_count(YT_TMP_OUTPUT) == ytw.row_count(yt_pool)):
        info = "MX_OPS error: output YT table does not exist or is empty!"
        doRequest(SAVE_APPLIED_POOL_PARAMS_URL, {'formula1': formula_id, 'formula2': formula2_id, 'status': 'failed', 'info': info, 'main_formula_id': main_formula_id, 'route': route}, 'Saving parameters of pool with applied formula')
        print >>sys.stderr, info
        sys.exit(1)
    matrixnet_result_list, res = [], {'tp': 0, 'tn': 0, 'fp': 0, 'fn': 0, 'total': 0}
    for record in ytw.read_table(YT_TMP_OUTPUT, format = ytw.JsonFormat(), raw = False):
        rec_list = str(record["value"]).split("\t")
        res[check_mn(int(rec_list[0]), 1 if float(rec_list[-1]) > args.threshold else 0)] += 1
        res['total'] += 1
    result = {
        'formula1':  formula1_id,
        'formula2':  formula2_id,
        'precision': res['tp'] * 1.0 / (res['tp'] + res['fp']) if res['tp'] + res['fp'] > 0 else 0,
        'recall':    res['tp'] * 1.0 / (res['tp'] + res['fn']) if res['tp'] + res['fn'] > 0 else 0,
        'accuracy':  (res['tp'] + res['tn']) * 1.0 / res['total'],
        'status':    'completed',
        'main_formula_id': main_formula_id,
        'route':     route
    }
    result['f_measure'] = 2.0 * result['precision'] * result['recall'] / (result['precision'] + result['recall']) if result['precision'] + result['recall'] > 0 else 0
    ytw.remove(YT_TMP_OUTPUT, force = True)
    doRequest(SAVE_APPLIED_POOL_PARAMS_URL, result, 'Saving parameters of pool with applied formula')
    return result

def doModelsOfflineTest(compared_models, comparing_results):
    S, formulas, r, c, N = [], [], [], [0.25, 0.25, 0.20, 0.15, 0.15], len(compared_models)
    if N < 2:
        return {}
    for j in range(N):
        formulas.append(int(compared_models[j]['formula_id'])); S.append(0)
    writelog("OfflineTest input models: %s" % str(formulas))
    for i in range(N):
        F = []; r.append([])
        for j in range(N):
            if i != j:
                F.append([compared_models[i]["f_measure"], i + 1])
            else:
                F.append([comparing_results[i][j]["f_measure"], j + 1])
        a = map(lambda f: f[1], sorted(F, key = lambda f: f[0], reverse = True))
        for j in range(N):
            r[i].append(a.index(j + 1) + 1)
    for j in range(N):
        for i in range(N):
            S[j] += (c[i] if i < 5 else 0.15) * r[i][j]
    j = sorted(enumerate(S), key = lambda s: s[1])[0][0]
    return compared_models[j]

if __name__ == "__main__":
    ctx = nv.context()
    meta = ctx.get_meta()
    MATRIXNET_FILE, YT_FEATURES = './matrixnet.info', ''
    YT_TMP_FOLDER = "//home/so_fml/nirvana/tmp/%s_%s" % (meta.get_workflow_uid(), meta.get_workflow_instance_uid())
    if not ytw.exists(YT_TMP_FOLDER):
        ytw.create('map_node', path = YT_TMP_FOLDER)
    parser = argparse.ArgumentParser()
    #parser.add_argument('-d', '--data',        type = str, help = "Input data parameters of formulas pairs, to which new formula will be applied")
    parser.add_argument('-f', '--formula_info',       type = str, help = "Input new trained formula (model) info in JSON format")
    parser.add_argument('-a', '--acceptance_metrics', type = str, help = "Acceptance metrics of a new trained formula (model) in JSON format")
    parser.add_argument('-p', '--test_pool',          type = str, help = "Table in YT with test pool (features) for the new formula")
    parser.add_argument('-d', '--deepness',           type = str, help = "Deepness of offline-testing (number of compared models)")
    parser.add_argument('-x', '--mx_ops',             type = str, help = "Path to mx_ops executable")
    parser.add_argument('-r', '--route',              type = str, help = "The type of mail for which the model is calculated")
    parser.add_argument('-t', '--token',              type = str, help = "OAuth token for access to Valhalla")
    parser.add_argument('-w', '--won_formula',        type = str, help = "Output with won formula info")
    args, compared_models, futures, comparing_results, formulas = parser.parse_known_args()[0], [], [], [], []
    ROUTE = args.route if args.route else 'in'
    DEEPNESS = args.deepness if args.deepness else 4
    formula_info = loadJSON(args.formula_info, "input formula info")
    acceptance_metrics = loadJSON(args.acceptance_metrics, "input acceptance metrics info")
    try:
        compared_models += json.loads(doRequest(GET_COMPARED_MODELS_URL, {'route': ROUTE, 'deepness': DEEPNESS}, 'Retrieving of models for comparing'))
    except Exception, e:
        writelog("Parsing compared models info error: %s." % str(e), True)
    writelog("Comparing of models: %s" % str(compared_models))
    for model in compared_models:
        if model["formula_id"] == int(formula_info["formula_id"]):
            continue
        try:
            with vh.Graph() as g1:
                applyFomulaToPool(int(formula_info['formula_id']), float(formula_info['threshold']), int(model['formula_id']), str(model["path"]), str(args.mx_ops), int(formula_info['formula_id']), str(ROUTE))
            futures.append(vh.run_async(g1, oauth_token=args.token, label="Apply formula to another formula's test pool", quota="so", num_threads=10, lazy_deploy_type='mono').get_total_completion_future())
            with vh.Graph() as g2:
                applyFomulaToPool(int(model['formula_id']), float(model['threshold']), int(formula_info['formula_id']), str(args.test_pool), str(args.mx_ops), int(formula_info['formula_id']), str(ROUTE))
            futures.append(vh.run_async(g2, oauth_token=args.token, label="Apply formula to another formula's test pool", quota="so", num_threads=10, lazy_deploy_type='mono').get_total_completion_future())
        except Exception, e:
            writelog("Falied comparing for model %s: %s." % (model["formula_id"], str(e)), True)
    comparing_results, result = defaultdict(lambda: defaultdict(dict)), {}
    try:
        for f in vh.as_completed(futures):
            res = f.result()
            comparing_results[res['formula1']][res['formula2']] = res['f_measure']
    except Exception, e:
        writelog("Receiving comparing results failed: %s. Res: %s." % (str(e), str(res)), True)
    ytw.remove(YT_TMP_FOLDER, recursive = True, force = True)
    try:
        result = doModelsOfflineTest(compared_models, comparing_results)
    except Exception, e:
        writelog('Calculating offline test result failed: %s. Comparing results: %s.' % (str(e), str(dict(comparing_results))), True)
    try:
        f = open(won_formula, 'wt')
        print >>f, json.dumps(result)
        f.close()
    except Exception, e:
        writelog('Saving result file error: %s.' % str(e), True)
