#!/usr/bin/python2
# encoding: utf-8
# kate: space-indent on; indent-width 4; replace-tabs on;
#
import argparse
import json
import vh
import yt.wrapper as ytw
import nirvana.mr_job_context as nv
from collections import defaultdict
from mail.so.spamstop.nirvana.models_offline_test.apply_formula_to_pool import writelog, doRequest, applyFormulaToPool

GET_COMPARED_MODELS_URL = "https://web.so.yandex-team.ru/ml/get_compared_models/?%s"
GET_COMPARING_INFO_URL = "https://web.so.yandex-team.ru/ml/get_comparing_info/?%s"


def loadJSON(file_path, prompt="input info"):
    try:
        f = open(file_path)
        info = json.loads(f.read())
        f.close()
    except Exception, e:
        writelog("Parsing input formula info error: %s." % str(e), True)
    writelog("%s: %s" % (prompt, str(info)))
    return info


def doModelsOfflineTest(compared_models, comparing_results):
    # See for details: https://wiki.yandex-team.ru/users/asamoylov/mnoniletest/
    S, formulas, r, c, N = [], [], [], [0.25, 0.25, 0.20, 0.15, 0.15], len(compared_models)
    if N < 2:
        return {}
    for j in range(N):
        formulas.append(int(compared_models[j]['formula_id']))
        S.append(0)
    writelog("OfflineTest input models: %s" % str(formulas))
    for i in range(N):
        F = []
        r.append([])
        for j in range(N):
            if i == j:
                F.append([compared_models[i]["f_measure"], i + 1])
            else:
                F.append([comparing_results[formulas[j]][formulas[i]], j + 1])
        writelog("F(%s): %s" % (i, str(F)))
        a = map(lambda f: f[1], sorted(F, key=lambda f: f[0], reverse=True))
        writelog("a(%s): %s" % (i, str(a)))
        for j in range(N):
            r[i].append(a.index(j + 1) + 1)
    writelog("r: %s" % str(r))
    for j in range(N):
        for i in range(N):
            S[j] += (c[i] if i < 5 else 0.15) * r[i][j]
    writelog("S: %s" % str(S))
    j = sorted(enumerate(S), key=lambda s: s[1])[0][0]
    return compared_models[j]


if __name__ == "__main__":
    ctx = nv.context()
    meta = ctx.get_meta()
    parameters = ctx.get_parameters()
    YT_TMP_FOLDER = str("//home/so_fml/nirvana/tmp/%s_%s" % (meta.get_workflow_uid(), meta.get_workflow_instance_uid()))
    if ytw.exists(YT_TMP_FOLDER):
        writelog("Folder '%s' exists" % YT_TMP_FOLDER)
    else:
        ytw.create('map_node', YT_TMP_FOLDER)
        writelog("After attmpt of creation folder '%s' %s" % (YT_TMP_FOLDER, 'exists' if ytw.exists(YT_TMP_FOLDER) else 'not exists'))
    parser = argparse.ArgumentParser()
    # parser.add_argument('-d', '--data',        type = str, help = "Input data parameters of formulas pairs, to which new formula will be applied")
    parser.add_argument('-f', '--formula_info',       type=str, help="Input new trained formula (model) info in JSON format")
    parser.add_argument('-a', '--acceptance_metrics', type=str, help="Acceptance metrics of a new trained formula (model) in JSON format")
    parser.add_argument('-p', '--test_pool',          type=str, help="Table in YT with test pool (features) for the new formula")
    parser.add_argument('-d', '--deepness',           type=str, help="Deepness of offline-testing (number of compared models)")
    parser.add_argument('-x', '--mx_ops',             type=str, help="Path to mx_ops executable")
    parser.add_argument('-r', '--route',              type=str, help="The type of mail for which the model is calculated")
    parser.add_argument('-t', '--token',              type=str, help="OAuth token for access to Valhalla")
    parser.add_argument('-w', '--won_formula',        type=str, help="Output with won formula info")
    args, compared_models, futures, comparing_results, k, results = parser.parse_known_args()[0], [], [], [], -1, []
    ROUTE = str(args.route if args.route else 'in')
    DEEPNESS = args.deepness if args.deepness else 4
    formula_info = loadJSON(args.formula_info, "input formula info")
    acceptance_metrics = loadJSON(args.acceptance_metrics, "input acceptance metrics info")
    try:
        compared_models += json.loads(doRequest(GET_COMPARED_MODELS_URL, {'route': ROUTE, 'deepness': DEEPNESS}, 'Retrieving of models for comparing'))
    except Exception, e:
        writelog("Parsing compared models info error: %s." % str(e), True)
    writelog("Comparing of models: %s" % str(compared_models))
    comparing_results = defaultdict(lambda: defaultdict(float))
    for i, model in enumerate(compared_models):
        if model["formula_id"] == int(formula_info["formula_id"]):
            k = i
            continue
        if i == len(compared_models) - 1 and k < 0:
            compared_models[:0] = [formula_info]
            compared_models[0].update(acceptance_metrics)
            break
        try:
            with vh.Graph() as g1:
                results.append(applyFormulaToPool(int(formula_info['formula_id']), float(formula_info['threshold']), int(model['formula_id']), str(model["path"]), args.mx_ops,
                                                  int(formula_info['formula_id']), ROUTE, YT_TMP_FOLDER, str('robot_mailspam_yt_token')))
            futures.append(vh.run_async(g1, oauth_token=args.token, label="Apply formula #%s to test pool of formula #%s" % (formula_info['formula_id'], model['formula_id']),
                                        quota="so", num_threads=12, lazy_deploy_type='mono').get_total_completion_future())
            with vh.Graph() as g2:
                results.append(applyFormulaToPool(int(model['formula_id']), float(model['threshold']), int(formula_info['formula_id']), str(args.test_pool), args.mx_ops,
                                                  int(formula_info['formula_id']), ROUTE, YT_TMP_FOLDER, str('robot_mailspam_yt_token')))
            futures.append(vh.run_async(g2, oauth_token=args.token, label="Apply formula #%s to test pool of formula #%s" % (model['formula_id'], formula_info['formula_id']),
                                        quota="so", num_threads=12, lazy_deploy_type='mono').get_total_completion_future())
        except Exception, e:
            writelog("Falied comparing for model %s: %s." % (model["formula_id"], str(e)), True)
        try:
            res = doRequest(GET_COMPARING_INFO_URL, {'formula_id': model["formula_id"]}, 'Retrieving comparing info of model %s' % model["formula_id"])
            writelog('Retrieving comparing info of model %s result: %s' % (model["formula_id"], str(res)))
            for comparing_info in json.loads(res):
                comparing_results[comparing_info['formula1']][comparing_info['formula2']] = comparing_info['f_measure']
        except Exception, e:
            writelog("Parsing compared models info error: %s." % str(e), True)
        for j, model2 in enumerate(compared_models):
            if model["formula_id"] == model2["formula_id"] or model2["formula_id"] == int(formula_info["formula_id"]):
                continue
            if model2['formula_id'] not in comparing_results[model['formula_id']]:
                with vh.Graph() as g3:
                    results.append(applyFormulaToPool(int(model['formula_id']), float(model['threshold']), int(model2['formula_id']), str(model2['path']), args.mx_ops,
                                                      int(model['formula_id']), ROUTE, YT_TMP_FOLDER, str('robot_mailspam_yt_token')))
                futures.append(vh.run_async(g3, oauth_token=args.token, label="Apply formula #%s to test pool of formula #%s" % (model['formula_id'], model2['formula_id']),
                                            quota="so", num_threads=12, lazy_deploy_type='mono').get_total_completion_future())
    result, res = {}, {}
    try:
        for i, f in enumerate(vh.as_completed(futures)):
            keeper = f.result()
            res_future = keeper.download_async(results[i])
            res = res_future.result()
            writelog("Applying result (%s): %s" % (i, str(res)))
            if 'formula1' in res and res['formula1'] and 'formula2' in res and res['formula2']:
                comparing_results[res['formula1']][res['formula2']] = res['f_measure']
    except Exception, e:
        writelog("Receiving comparing results failed: %s. Res: %s." % (str(e), str(res)), True)
    if ytw.exists(YT_TMP_FOLDER):
        ytw.remove(YT_TMP_FOLDER, recursive=True, force=True)
    writelog("Comparing results: %s" % str(comparing_results))
    try:
        result = doModelsOfflineTest(compared_models, comparing_results)
    except Exception, e:
        writelog('Calculating offline test result failed: %s. Comparing results: %s.' % (str(e), str(dict(comparing_results))), True)
    try:
        f = open(args.won_formula, 'wt')
        print >>f, json.dumps(result)
        f.close()
    except Exception, e:
        writelog('Saving result file error: %s.' % str(e), True)
