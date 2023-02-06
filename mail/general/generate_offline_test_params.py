#!/usr/bin/python2
# encoding: utf-8
# kate: space-indent on; indent-width 4; replace-tabs on;
#
import sys, argparse, json
import nirvana.job_context as nv
from urllib import urlopen
from traceback import format_exception

GET_COMPARED_MODELS_URL = "https://so-web.n.yandex-team.ru/ml/get_compared_models/?%s"

def get_traceback():
    exc_type, exc_value, exc_traceback = sys.exc_info()
    tb = ''
    for step in format_exception(exc_type, exc_value, exc_traceback):
        try:
            tb += "\t" + step.strip() + "\n"
        except:
            pass
    return tb

def doRequest(url_template, params, prompt):
    try:
        f = urlopen(url_template % '&'.join(map(lambda it: "%s=%s" % (it[0], it[1]), params.items())))
        if f.getcode() == 200:
            return f.read()
        else:
            print >>sys.stderr, '{0} response HTTP code: {1}, body: {2}'.format(prompt, f.getcode(), f.info())
    except Exception, e:
        print >>sys.stderr, '%s HTTP request failed: %s.%s' % (prompt, str(e), get_traceback())
    return ""

def loadJSON(file_path, prompt = "input info"):
    try:
        f = open(file_path)
        info = json.loads(f.read())
        f.close()
    except Exception, e:
        print >>sys.stderr, "Parsing input formula info error: %s.%s" % (str(e), get_traceback())
    retrun info

if __name__ == "__main__":
    ctx = nv.context()
    meta = ctx.get_meta()
    parser = argparse.ArgumentParser()
    parser.add_argument('-f', '--formula_info',       type = str, help = "Input new trained formula (model) info in JSON format")
    parser.add_argument('-a', '--acceptance_metrics', type = str, help = "Acceptance metrics of a new trained formula (model) in JSON format")
    parser.add_argument('-p', '--test_pool',          type = str, help = "Table in YT with test pool (features) for the new formula")
    parser.add_argument('-d', '--deepness',           type = str, help = "Deepness of offline-testing (number of compared models)")
    parser.add_argument('-r', '--route',              type = str, help = "The type of mail for which the model is calculated")
    parser.add_argument('-o', '--output_set',         type = str, help = "Output JSON-file with parameters of other formulas, to which new formula will be applied")
    args, output_data = parser.parse_known_args()[0], {}
    ROUTE = args.route if args.route else 'in'
    DEEPNESS = args.deepness if args.deepness else 4
    formula_info = loadJSON(args.formula_info, "input formula info")
    acceptance_metrics = loadJSON(args.acceptance_metrics, "input acceptance metrics info")
    try:
        compared_models = json.loads(doRequest(GET_COMPARED_MODELS_URL, {'route': ROUTE, 'deepness': DEEPNESS}, 'Retrieving of models for comparing'))
    except Exception, e:
        print >>sys.stderr, "Parsing compared models info error: %s.%s" % (str(e), get_traceback())
    for model in compared_models:
        if model["formula_id"] == formula_info["formula_id"]:
            continue
        output_data.append({
            "formula1": {"id": formula_info["formula_id"], "pool": args.test_pool, "threshold": formula_info["threshold"], "f_measure": acceptance_metrics["f_measure"]},
            "formula2": {"id": model["formula_id"], "pool": model["path"], "threshold": model["threshold"], "f_measure": model["f_measure"]},
            "meta":     {"workflow_id": meta.get_workflow_uid(), "workflow_instance_id": meta.get_workflow_instance_uid()}
        })
        output_data.append({
            "formula1": {"id": model["formula_id"], "pool": model["path"], "threshold": model["threshold"], "f_measure": model["f_measure"]},
            "formula2": {"id": formula_info["formula_id"], "pool": args.test_pool, "threshold": formula_info["threshold"], , "f_measure": acceptance_metrics["f_measure"]},
            "meta":     {"workflow_id": meta.get_workflow_uid(), "workflow_instance_id": meta.get_workflow_instance_uid()}
        })
    try:
        f = open(args.output_set, 'wt')
        print >>f, json.dumps(output_data)
        f.close()
    except Exception, e:
        print >>sys.stderr, 'Saving result JSON-data error: %s.%s' % (str(e), get_traceback())
