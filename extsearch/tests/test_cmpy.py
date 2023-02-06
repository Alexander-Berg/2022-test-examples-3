import yatest.common


BINARY_PATH = 'extsearch/video/robot/cm/dups/cmpy/cmpy'
CONFIG_MODULE = 'extsearch.video.robot.cm.dups.cmpy.config'


def _get_all_targets():
    # TODO(meow): parse from main.sh & hostlist
    return [
        ['url_pairs.prepare'],
        ['url_pairs.sort_pairs'],
        ['url_pairs.sort_urls'],
        ['url_pairs.add_data'],
        ['url_pairs.sort_data'],
        ['url_pairs.add_sigs'],
        ['url_pairs.sort_sigs'],
        ['url_pairs.create_pairs'],
        ['url_pairs.match_pairs'],
        ['url_pairs.filter_matched'],
        ['url_pairs.export_portion'],
        ['url_pairs.push_solomon_points'],
        ['url_pairs.wait_sig_norm_tables'],
        ['url_pairs.normalize_sigs'],
        ['url_pairs.make_nodata'],
        ['url_pairs.store_nodata'],
        ['stats.graph_daily'],
        ['sig_spread.calc_spread'],
        ['sig_spread.push_solomon_points'],
        ['sig_spread.push_portion'],
        ['sig_spread.calc_static_grouping'],
        ['sig_spread.export_static_grouping'],
        ['sig_spread.wait_sigs_merge'],
        ['sig_spread.calc_ontoid_dup_candidates'],
    ]


def _test_target(target):
    args = [
        yatest.common.binary_path(BINARY_PATH),
        '--config-module', CONFIG_MODULE,
    ]
    args.extend(target)
    yatest.common.execute(args, env={'DRY_RUN': '1'})


def test_cmpy():
    for target in _get_all_targets():
        _test_target(target)
