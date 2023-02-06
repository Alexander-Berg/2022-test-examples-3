from os.path import join as pj

import yatest.common
from extsearch.video.robot.cm.library import test

CMPY_PACKAGE_PATH = yatest.common.binary_path('extsearch/video/robot/cm/vpq/packages/cmpy')


def test_cmpy():
    hostlist_path = pj(CMPY_PACKAGE_PATH, 'hostlist')
    target_types_path = pj(CMPY_PACKAGE_PATH, 'target_types.scenario')
    targets_path = pj(CMPY_PACKAGE_PATH, 'targets.scenario')
    run_script_path = pj(CMPY_PACKAGE_PATH, 'run_target.sh')

    for target_with_args in test.parse_targets_with_args(hostlist_path, target_types_path, targets_path):
        yatest.common.execute(
            [run_script_path] + target_with_args,
            env={
                'CMPY_DIR': CMPY_PACKAGE_PATH,
                'DRY_RUN': '1',
            },
        )
