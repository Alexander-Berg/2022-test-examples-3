import yatest.common
import md5
import os
import sys

import search.tools.idx_ops.comparer.to_html_converter.pool_comparer_result_converter as cmp
cmp.imagesLib = False

TPrintAggDiff=cmp.TPrintAggDiff
TPrintLinedDiff=cmp.TPrintLinedDiff
TPrintStats=cmp.TPrintStats
import json

local_test = False
if local_test:
    arcadia = "/place/home/ilnurkh/SEARCH-1394/rawlimits"
    sys.path.append(arcadia + '/search/garden/sandbox-tasks/projects/common/base_search_quality/')
    sys.path.append(arcadia +'/search/garden/sandbox-tasks/')
    sys.path.append(arcadia + '/contrib/libs/protobuf/python/')
    import tree.htmldiff as htd

def get_bin_path():
    return yatest.common.binary_path("search/tools/idx_ops/comparer/pool_comparer")


def get_inp_paths():
    return ("pools_with_limits/with_tm_limits_1000.pb.gz", "pools_with_limits/with_relaxed_tm_limits_2000.pb.gz")

def get_names_paths():
    return ("names/names_bl.txt", "names/names_so.txt")

if local_test:
    def test_local():
        head = arcadia + "/search/tools/idx_ops/comparer/tests/"
        stub = htd

        ld = TPrintLinedDiff(head + "line_diff.txt", stub, True, [str(x) for x in xrange(1000)], [str(x) for x in xrange(1000)])
        ld.print_lined_diff(head + "per_line_diff.html")
        return
        if False :
            yatest.common.execute(
                [get_bin_path(),
                    "-i", get_inp_paths()[0],
                    "-t", get_inp_paths()[1],
                    "--dst-bl", head + "bl_stats",
                    "--dst-so", head + "so_stats",
                    "--dst-diff-agg", head + "agg_diff",
                    "--dst-diff-line", head + "line_diff",
                    "--dst-diff-stats", head + "diff_stats",
                ], check_exit_code=True, shell=True)

        with open(get_names_paths()[0]) as f:
            bl_names = [x.strip() for x in f.readlines()]

        with open(get_names_paths()[1]) as f:
            so_names = [x.strip() for x in f.readlines()]

        ld = TPrintLinedDiff(head + "line_diff", stub, True, bl_names, so_names)
        ld.print_lined_diff(head + "per_line_diff.html")
        ld2 = TPrintLinedDiff(head + "line_diff", stub, False, bl_names, so_names)
        ld2.print_lined_diff(head + "per_line_diff_full.html")

        ad = TPrintAggDiff(head + "agg_diff", stub, bl_names, so_names)
        ad.print_aggregated_diff(head + "aggregatins_diff.html")

        differed_features = ld.get_differed_f_keys()
        differed_features.update(ad.get_differed_f_keys())

        bl = TPrintStats(head + "bl_stats", False, stub, bl_names, so_names, differed_features)
        bl.print_stats(head + "base_line_stats.html", "BaseLine Stats")

        so = TPrintStats(head + "so_stats", False, stub, bl_names, so_names, differed_features)
        so.print_stats(head + "seconone_stats.html", "Testing one Stats")

        df = TPrintStats(head + "diff_stats", True, stub, bl_names, so_names, differed_features)
        df.print_stats(head + "diff_stats.html", "Stats of difference")

else :
    def test_comparer():
        yatest.common.execute(
            [get_bin_path(),
                "-i", get_inp_paths()[0],
                "-t", get_inp_paths()[1],
                "--dst-bl", "bl_stats",
                "--dst-so", "so_stats",
                "--dst-diff-agg", "agg_diff",
                "--dst-diff-line", "line_diff",
                "--dst-diff-stats", "diff_stats",
            ], check_exit_code=True, shell=True)
        return [yatest.common.canonical_file(f, local=False) for f in ["bl_stats", "so_stats", "agg_diff", "line_diff", "diff_stats"]]

    def test_to_html_conversation():
        with open(get_names_paths()[0]) as f:
            bl_names = [x.strip() for x in f.readlines()]

        with open(get_names_paths()[1]) as f:
            so_names = [x.strip() for x in f.readlines()]

        stub = None

        ld = TPrintLinedDiff("line_diff", stub, True, bl_names, so_names)
        ld.print_lined_diff("per_line_diff.html")
        ld2 = TPrintLinedDiff("line_diff", stub, False, bl_names, so_names)
        ld2.print_lined_diff("per_line_diff_full.html")

        ad = TPrintAggDiff("agg_diff", stub, bl_names, so_names)
        ad.print_aggregated_diff("aggregatins_diff.html")

        differed_features = ld.get_differed_f_keys()
        differed_features.update(ad.get_differed_f_keys())

        bl = TPrintStats("bl_stats", False, stub, bl_names, so_names, differed_features)
        bl.print_stats("base_line_stats.html", "BaseLine Stats")
        bl = TPrintStats("so_stats", False, stub, bl_names, so_names, differed_features)
        bl.print_stats("seconone_stats.html", "Testing one Stats")
        bl = TPrintStats("diff_stats", True, stub, bl_names, so_names, differed_features)
        bl.print_stats("diff_stats.html", "Stats of difference")

        return [yatest.common.canonical_file(f, local=False) for f in ["per_line_diff.html", "per_line_diff_full.html", "aggregatins_diff.html", "base_line_stats.html", "seconone_stats.html", "diff_stats.html"]]

    def test_to_html_conversation2():
        bl_names = None
        so_names = None

        stub = None

        ld = TPrintLinedDiff("line_diff", stub, True, bl_names, so_names)
        ld.print_lined_diff("nn_per_line_diff.html")
        ld2 = TPrintLinedDiff("line_diff", stub, False, bl_names, so_names)
        ld2.print_lined_diff("nn_per_line_diff_full.html")

        ad = TPrintAggDiff("agg_diff", stub, bl_names, so_names)
        ad.print_aggregated_diff("nn_aggregatins_diff.html")

        differed_features = ld.get_differed_f_keys()
        differed_features.update(ad.get_differed_f_keys())

        bl = TPrintStats("bl_stats", False, stub, bl_names, so_names, differed_features)
        bl.print_stats("nn_base_line_stats.html", "BaseLine Stats")
        bl = TPrintStats("so_stats", False, stub, bl_names, so_names, differed_features)
        bl.print_stats("nn_seconone_stats.html", "Testing one Stats")
        bl = TPrintStats("diff_stats", True, stub, bl_names, so_names, differed_features)
        bl.print_stats("nn_diff_stats.html", "Stats of difference")

        return [yatest.common.canonical_file(f, local=False) for f in ["nn_per_line_diff.html", "nn_per_line_diff_full.html", "nn_aggregatins_diff.html", "nn_base_line_stats.html", "nn_seconone_stats.html", "nn_diff_stats.html"]]

    def test_comparer_nodiff():
        yatest.common.execute(
            [get_bin_path(),
                "-i", get_inp_paths()[0],
                "-t", get_inp_paths()[0],
                "--dst-bl", "bl_stats_nd",
                "--dst-so", "so_stats_nd",
                "--dst-diff-agg", "agg_diff_nd",
                "--dst-diff-line", "line_diff_nd",
                "--dst-diff-stats", "diff_stats_nd",
            ], check_exit_code=True, shell=True)
        return [yatest.common.canonical_file(f, local=False) for f in ["bl_stats_nd", "so_stats_nd", "agg_diff_nd", "line_diff_nd", "diff_stats_nd"]]

    def test_to_html_conversation_nodiff():
        bl_names = None
        so_names = None

        ld = TPrintLinedDiff("line_diff", None, True, bl_names, so_names)
        ld.print_lined_diff("nd_per_line_diff.html")
        ld2 = TPrintLinedDiff("line_diff", None, False, bl_names, so_names)
        ld2.print_lined_diff("nd_per_line_diff_full.html")

        ad = TPrintAggDiff("agg_diff_nd", None, bl_names, so_names)
        ad.print_aggregated_diff("nd_aggregatins_diff.html")

        differed_features = ld.get_differed_f_keys()
        differed_features.update(ad.get_differed_f_keys())

        bl = TPrintStats("bl_stats_nd", False, None, bl_names, so_names, differed_features)
        bl.print_stats("nd_base_line_stats.html", "BaseLine Stats")
        bl = TPrintStats("so_stats_nd", False, None, bl_names, so_names, differed_features)
        bl.print_stats("nd_seconone_stats.html", "Testing one Stats")
        bl = TPrintStats("diff_stats_nd", True, None, bl_names, so_names, differed_features)
        bl.print_stats("nd_diff_stats.html", "Stats of difference")

        return [yatest.common.canonical_file(f, local=False) for f in ["nd_per_line_diff.html", "nd_per_line_diff_full.html", "nd_base_line_stats.html", "nd_seconone_stats.html", "nd_diff_stats.html"]]

    def test_to_html_conversation_onotole():
        with open("joined_stats_resource/EvalRangesDetailedDstFile.json") as f:
            detailed_joined_names_info = json.loads(f.read())

        path = "joined_stats_resource/joined_stats.json"
        st = TPrintStats(path, False)

        st.print_joined_stats(detailed_joined_names_info, "joined_feats_stats.html", "Joined Features Stats")
        return [yatest.common.canonical_file("joined_feats_stats.html", local=False)]
