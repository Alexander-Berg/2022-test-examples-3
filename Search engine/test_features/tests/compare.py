#!/usr/bin/env python

import optparse
import sys
import commands
import test_conf as tc
from collections import defaultdict

def pretty(s, cc):
    return cc + s + '\033[0m'

def info(h, s = ""):
    return pretty(h, '\033[95m') + s

def ok(h, s = ""):
    return pretty(h, '\033[92m') + s

def warn(h, s = ""):
    return pretty(h, '\033[93m') + s

def err(h, s = ""):
    return pretty(h, '\033[91m') + s

def pretty_result(what, cond, ok_msg, warn_msg):
    return info(what + ":\t") + (ok(ok_msg) if cond else (err(warn_msg)))

def percent(n, total):
    return "%d/%d (%0.3f%%)" % (n, total, n * 100 / float(total))

def rel_percent(x, y, thr, inv = False):
    rel = (x * 100 / float(y)) - 100
    out = "%+0.3f%%" % rel
    too_bad = abs(rel) > thr
    return ok(out) if (x > y) ^ inv else warn(out) if not too_bad else err(out)

def micros2millis(t):
    return t / 1000.

def tau_distance(l, r):
    dis = 0
    n = len(l)
    if n > 1:
        for i in xrange(n):
            for j in xrange(i + 1, n):
                dis += int((l[i] - l[j]) * (r[i] - r[j]) < 0)
    return float(dis) / (n * (n - 1) / 2.) if n else 0

def update_position_stats(l, r, stats):
    for i in xrange(len(l)):
        if l[i] != r[i]:
            if i < len(stats):
                stats[i] += 1
            else:
                stats.append(1)

def make_features_list(verbose):
    features = {}
    feature_names = []
    feature_indices = []
    feature_flags = {}

    mlfeatures = open(_CONFIG.get_mlfeatures(), 'r')
    current_name = None
    unused_indices = []
    last_indices = []
    for line in mlfeatures:
        line = line.strip()
        if line.startswith("Indices:"):
            unused_indices = [int(x) for x in line[8:].strip(' []').split(',')]
        if line.startswith("CppName:"):
            current_name = line[8:].strip(' "')
        if line.startswith("ProfileType:"):
            feature_names += [current_name + '  (' + x.strip() + ')' for x in line[12:].strip(' []').split(',')]
        if line.startswith("CorrespondingIndex:"):
            last_indices = [int(x) for x in line[19:].strip(' []').split(',')]
            feature_indices += last_indices
        if verbose and line.startswith("Flags:"):
            flags = line[6:].strip(' []')
            for i in xrange(len(last_indices)):
                feature_flags[last_indices[i]] = flags

    mlfeatures.close()
    for i in xrange(len(feature_indices)):
        idx = feature_indices[i]
        have_flag = idx in feature_flags
        features[idx] = feature_names[i] + (" [" + feature_flags[idx] + "]" if have_flag else "")
    for i in xrange(len(unused_indices)):
        features[unused_indices[i]] = "UNUSED_FEATURE_" + str(i)

    return [feature for (number, feature) in sorted(features.items())]

def init_stats(size):
    stats = []
    for i in xrange(size):
        stats.append([])
    return stats

def print_features_stats(diff_stats, total_instances, feat_list):
    for i in xrange(len(diff_stats)):
        diffs = diff_stats[i]
        size = len(diffs)
        if size:
            diffs.sort()
            if i < len(diff_stats) - 1:
                print ">> #%d %s: change cases:\t%s" % (i, feat_list[i], percent(size, total_instances))
            else:
                print ">> FML VALUE: change cases:\t%s" % (percent(size, total_instances))
            idx = int(0.95 * total_instances)
            print "   max diff: %f\t95-procentile: %f" % (max(diffs), diffs[idx] if idx < len(diffs) else 0)

def parse_line(line):
    tokens = line.split('\t')

    entry = {}
    features = []
    for token in tokens[4:]:
        feat = float(token)
        features.append(feat)

    entry["req_idx"]  = int(tokens[0])
    entry["doc_idx"]  = int(tokens[1])
    entry["score"]    = float(tokens[2])
    entry["user_int"] = float(tokens[3])
    entry["features"] = features
    return entry

def update_times(line, times):
    update = dict(item.split("~") for item in (line.split()[2:]))
    for k, v in update.items():
        times[k].append(micros2millis(int(v)))

def calc_time_stats(times):
    times.sort()
    size = len(times)
    return (sum(times) / float(size)), times[int(0.95 * size)], times[-1]

def update_stats(now, feat_stats):
    feats = now["features"]
    for i in xrange(len(feats)):
        feat_stats[i].append(float(feats[i]))

def compare(now, canon, feat_list, feat_diffs, verbose):
    doc_idx = canon["doc_idx"]
    if now["doc_idx"] != doc_idx:
        print err("error: RP size differ")

    now_feats   = now["features"]
    canon_feats = canon["features"]
    for i in xrange(min(len(canon_feats), len(now_feats))):
        diff = abs(now_feats[i] - canon_feats[i])
        if diff > tc.FP_THRESHOLD:
            feat_diffs[i].append(diff)
            if verbose:
                print "feat #%d %s:\twas = %f\tnow = %f\tdiff = %f" % (i, feat_list[i], canon_feats[i], now_feats[i], diff)

    diff = abs(now["user_int"] - canon["user_int"])
    if diff > tc.FP_THRESHOLD:
        feat_diffs[-1].append(diff)
        if verbose:
            print ">> user interest[%d]:\twas = %f\tnow = %f\tdiff = %f" % (doc_idx, canon["user_int"], now["user_int"], diff)
            print ">> line_idx = %s, was = %s\n" % (now["req_idx"], canon["req_idx"])

def main(options):
    feat_list  = make_features_list(options.verbose)
    feat_stats = init_stats(len(feat_list))
    feat_diffs = init_stats(len(feat_list) + 1)

    out_new = options.out_canon + ".new"
    cmd = "../test_features -t -f %s --fd %s --rc %s --uc %s --cp %s --areas %s > %s 2> errlog.txt"
    cmd = cmd % (options.fml, options.fml_dir, options.rearr_data, options.user_data, options.cp_trie, options.areas, out_new)
    print info("Running test binary...\n") , cmd
    (status, out) = commands.getstatusoutput(cmd)
    if (status > 0):
        print err("Test returned exit status %s, aborting..." % status)
        sys.exit(1)

    print info("\nComparing results...")

    canon_file = open(options.out_canon, 'r')
    out_file   = open(out_new, 'r')

    rearrs = 0
    total_reqs = 0
    total_cases = 0
    position_stats = []
    times       = defaultdict(list)
    times_canon = defaultdict(list)
    print "%s(canon) vs %s" % (canon_file.readline().rstrip(), out_file.readline().rstrip())
    while True:
        line1 = canon_file.readline().rstrip()
        line2 = out_file.readline().rstrip()
        if not line1 and not line2:
            break

        if line1.find("times") > 0:
            # skip "cold start" results
            if total_reqs > 1:
                update_times(line2, times)
                update_times(line1, times_canon)
        elif line1.find("new order") > 0:
            total_reqs += 1
            if line1 != line2:
                rearrs += 1
                canon  = [int(i) for i in line1.split()[3:]]
                result = [int(i) for i in line2.split()[3:]]
                update_position_stats(canon, result, position_stats)
                if options.verbose:
                    print "WARNING: order changed"
                    print "CANON: %s\n  NOW: %s" % (line1, line2)
                    print "EFFECT: %f" % tau_distance(canon, result)
                    print "---------------------------------------------------------------------------"
        else:
            canon  = parse_line(line1)
            result = parse_line(line2)
            update_stats(result, feat_stats)
            compare(result, canon, feat_list, feat_diffs, options.verbose)
            total_cases += 1

    pos_changes  = len(position_stats)
    feat_changes = sum(len(s) for s in feat_diffs)

    print pretty_result("\nDoc order changed", not rearrs, "NO", ("YES, in %s" % percent(rearrs, total_reqs)))
    print pretty_result("Positions changed", not pos_changes, "NO", "YES")
    for i in xrange(pos_changes):
        print "#%d position change cases: %s" % (i, percent(position_stats[i], total_reqs))
    print pretty_result("Features slipped", not feat_changes, "NO", "YES")

    if options.verbose:
        print_features_stats(feat_diffs, total_cases, feat_list)
        print info("\nThe following features are zeroes:")
        for i in xrange(len(feat_stats)):
            if not sum(feat_stats[i]):
                print ">> #%d %s" % (i, feat_list[i])

        print info("\nNonzero feat stats:")
        for i in xrange(len(feat_stats)):
            s = sum(feat_stats[i])
            if s:
                l = len(feat_stats[i])
                avg = s * 1.0 / l
                nonzero = sum(1 for f in feat_stats[i] if f) * 1.0 / l
                var = sum((f - avg) ** 2 for f in feat_stats[i]) * 1.0 / l
                print("avg: {} var: {} nonzero: {}".format(avg, var, nonzero))
                print ">> #%d %s" % (i, feat_list[i])

    print info("\nPerformance stats:")
    for tag in times.keys():
        (avg, p95, max_t)       = calc_time_stats(times[tag])
        (c_avg, c_p95, c_max_t) = calc_time_stats(times_canon[tag])
        avg_rel = rel_percent(avg, c_avg, tc.DT_THRESHOLD_PERCENT, True)
        p95_rel = rel_percent(p95, c_p95, tc.DT_THRESHOLD_PERCENT, True)
        max_rel = rel_percent(max_t, c_max_t, tc.DT_THRESHOLD_PERCENT, True)
        line = " - %s\ttime avg = %0.3f ms (%s)\t 95-p = %0.3f ms (%s) \tmax = %0.3f ms (%s)"
        print line % (tag, avg, avg_rel, p95, p95_rel, max_t, max_rel)

    canon_file.close();
    out_file.close();

_ARC_ROOT = 6 * "../"
_CONFIG = tc.TestConfig(_ARC_ROOT + "arcadia/", _ARC_ROOT + "arcadia_tests_data/")

parser = optparse.OptionParser()
parser.add_option("--fml", dest = "fml", default = tc.CUR_FORMULA)
parser.add_option("--fml-dir", dest = "fml_dir", default = _CONFIG.get_formulas_path())
parser.add_option("--to ", dest = "out_canon", default = _CONFIG.get_out_file(tc.CUR_FORMULA))
parser.add_option("--rdata", dest = "rearr_data", default = _CONFIG.get_rearr_ctx())
parser.add_option("--udata ", dest = "user_data", default = _CONFIG.get_user_ctx())
parser.add_option("--cptrie ", dest = "cp_trie",  default = _CONFIG.get_cp_trie())
parser.add_option("--areas ", dest = "areas",  default = _CONFIG.get_areas())
parser.add_option("--verbose", action = "store_true", dest = "verbose", default = False)

(options, args) = parser.parse_args()

sys.exit(main(options))
