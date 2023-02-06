import yatest.common

begemot_bin = yatest.common.binary_path("web/daemons/begemot/nonsplit_version/begemot_nonsplit_version")
shard_data_path = yatest.common.binary_path("search/begemot/data/LingBoost/search/wizard/data/wizard")
evlog_dump_path = yatest.common.binary_path("web/daemons/begemot/event_log_dump/event_log_dump")
json_inout_ops_path = yatest.common.binary_path("search/begemot/tools/json_inout_ops/json_inout_ops")
input_reqs_path = "init_and_merger_reqs.json"

# ./begemot --data ~/begemot/search/begemot/data/LingBoost/search/wizard/data/wizard/ --test-apphost-log --log evlog


def unpack_evlog(evlogpath):
    yatest.common.execute(
        [
            evlog_dump_path, evlogpath,
            "-r", " > ", evlogpath + ".unpacked"
        ],
        shell=True
    )


def test_just_startup():
    evlog = yatest.common.test_output_path("evlog")
    try:
        yatest.common.execute(
            [
                begemot_bin,
                "--data", shard_data_path,
                "--log", evlog,
                "--test"
            ],
            stdin=open("/dev/null"),
            env={
                "LINGBOOST_CACHE_DISABLED": "1",
                "LB_SMALL_EMBEDS_CACHE": "1",
            },
        )
    finally:
        try:
            unpack_evlog(evlog)
        except:
            pass


def test_run_lbinit_and_lbmerger():
    evlog = yatest.common.test_output_path("evlog")
    out = yatest.common.test_output_path("begemot_out.json")
    formated_out = yatest.common.test_output_path("formated_begemot_out.txt")

    try:
        yatest.common.execute(
            [
                begemot_bin,
                "--data", shard_data_path,
                "--log", evlog,
                "--test",
                "-j", "4",
                " > ", out,
                " < ", input_reqs_path,
            ],
            shell=True,
            env={
                "LINGBOOST_CACHE_DISABLED": "1",
                "LINGBOOST_NONMEANINGFULL_EMBEDS": "1",
                "LB_SMALL_EMBEDS_CACHE": "1",
                "Y_NO_AVX_IN_DOT_PRODUCT": "1",
            },
        )
    finally:
        try:
            unpack_evlog(evlog)
        except:
            pass

    yatest.common.execute(
        [
            json_inout_ops_path, "format",
            "-i", out,
            "-o", formated_out,
            "-h", '{Type=__testing_type_for_apphost_flags;Hook=print}',
            "-h", '{Type=saas_knn_input;Hook=print}',
            "-h", '{Type=saas_kv_input;Hook=print}',
            "-h", '{Type=saas_knn_input;Path=comp_search;Hook=knnreq}',
            "-h", '{Type=wizard;Path=rules.LingBoostApplyPantherTermsDssmRule.CompressedPantherQfufEmbeds;Hook=embeds}',
            "-h", '{Type=wizard;Path=rules.LingBoostApplyPantherTermsDssmRule.CompressedPantherMainQueryEmbeds;Hook=embeds}',
            "-h", '{Type=wizard;Path=rules.LingBoost.CompressedExpansions;Hook=qbundle}',
            "-h", '{Type=wizard;Path=rules.LingBoost.KnnRandomLogQueryFactors;Hook=print}',
            "-h", '{Type=wizard;Path=rules.LingBoostQueryFeatures.LingBoostQueryFeatures;Hook=print}',
            "-h", '{Type=wizard;Path=rules.LingBoost.QueryFrankensteinClustering;Hook=print}',
        ]
    )

    return [yatest.common.canonical_file(formated_out)]


def test_run_lbinit_and_lbmerger_for_stevenson():
    evlog = yatest.common.test_output_path("evlog")
    out = yatest.common.test_output_path("begemot_out.json")
    formated_out = yatest.common.test_output_path("formated_begemot_out.txt")

    try:
        yatest.common.execute(
            [
                begemot_bin,
                "--data", shard_data_path,
                "--log", evlog,
                "--test",
                "-j", "4",
                " > ", out,
                " < ", input_reqs_path,
            ],
            shell=True,
            env={
                "LINGBOOST_CACHE_DISABLED": "1",
                "LINGBOOST_NONMEANINGFULL_EMBEDS": "1",
                "LB_SMALL_EMBEDS_CACHE": "1",
                "Y_NO_AVX_IN_DOT_PRODUCT": "1",
            },
        )
    finally:
        try:
            unpack_evlog(evlog)
        except:
            pass

    yatest.common.execute(
        [
            json_inout_ops_path, "format",
            "-i", out,
            "-o", formated_out,
            "-h", '{Type=wizard;Path=rules.Stevenson;Hook=print}'
        ]
    )

    return [yatest.common.canonical_file(formated_out)]


def test_run_lbinit_and_lbmerger_with_blenderknn_init():
    evlog = yatest.common.test_output_path("evlog")
    out = yatest.common.test_output_path("begemot_out.json")
    formated_out = yatest.common.test_output_path("formated_begemot_out.txt")

    try:
        yatest.common.execute(
            [
                begemot_bin,
                "--data", shard_data_path,
                "--log", evlog,
                "--test",
                "--additional-cgi", "'&wizextra=lboost/BlenderKnnEnabled=1'",
                "-j", "4",
                " > ", out,
                " < ", input_reqs_path
            ],
            shell=True,
            env={
                "LINGBOOST_CACHE_DISABLED": "1",
                "LINGBOOST_NONMEANINGFULL_EMBEDS": "1",
                "LB_SMALL_EMBEDS_CACHE": "1",
                "Y_NO_AVX_IN_DOT_PRODUCT": "1",
            },
        )
    finally:
        try:
            unpack_evlog(evlog)
        except:
            pass

    yatest.common.execute(
        [
            json_inout_ops_path, "format",
            "-i", out,
            "-o", formated_out,
            "-h", '{Type=__testing_type_for_apphost_flags;Hook=print}',
            "-h", '{Type=blender_saas_knn_setup;Hook=print}',
            "-h", '{Type=blender_saas_knn_setup;Path=comp_search;Hook=knnreq}',
            "-h", '{Type=blender_saas_knn_external_setup;Hook=print}',
        ]
    )

    return [yatest.common.canonical_file(formated_out)]


def test_replacing_embed():
    evlog = yatest.common.test_output_path("evlog")
    out = yatest.common.test_output_path("begemot_out.json")
    formated_out = yatest.common.test_output_path("formated_begemot_out.txt")

    try:
        yatest.common.execute(
            [
                "head", "-20", input_reqs_path, " | ",
                begemot_bin,
                "--data", shard_data_path,
                "--log", evlog,
                "--test",
                "--additional-cgi",
                "'&wizextra=lboost/ReplacingEmbed=[1]'",
                "-j", "4",
                " > ", out
            ],
            shell=True,
            env={
                "LINGBOOST_CACHE_DISABLED": "1",
                "LINGBOOST_NONMEANINGFULL_EMBEDS": "1",
                "LB_SMALL_EMBEDS_CACHE": "1"
            },
        )
    finally:
        try:
            unpack_evlog(evlog)
        except:
            pass

    yatest.common.execute(
        [
            json_inout_ops_path, "format",
            "-i", out,
            "-o", formated_out,
            "-h", '{Type=__testing_type_for_apphost_flags;Hook=print}',
            "-h", '{Type=saas_knn_input;Hook=print}',
            "-h", '{Type=saas_kv_input;Hook=print}',
            "-h", '{Type=saas_knn_input;Path=comp_search;Hook=knnreq}',
            "-h", '{Type=wizard;Path=rules.LingBoost.CompressedExpansions;Hook=qbundle}',
            "-h", '{Type=wizard;Path=rules.LingBoostApplyPantherTermsDssmRule.CompressedPantherQfufEmbeds;Hook=embeds}',
            "-h", '{Type=wizard;Path=rules.LingBoostApplyPantherTermsDssmRule.CompressedPantherMainQueryEmbeds;Hook=embeds}',
            "-h", '{Type=wizard;Path=rules.LingBoost.KnnRandomLogQueryFactors;Hook=print}',
            "-h", '{Type=wizard;Path=rules.LingBoostQueryFeatures.LingBoostQueryFeatures;Hook=print}',
        ]
    )

    return [yatest.common.canonical_file(formated_out)]
