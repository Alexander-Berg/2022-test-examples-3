import sys
from os.path import join as pj

import yatest.common
from extsearch.video.robot.cm.library import test

CMPY_PACKAGE_PATH = yatest.common.binary_path('extsearch/video/robot/cm/vicont/packages/cmpy')


def test_dry_run():
    hostlist_path = pj(CMPY_PACKAGE_PATH, 'hostlist')
    target_types_path = pj(CMPY_PACKAGE_PATH, 'target_types.scenario')
    targets_path = pj(CMPY_PACKAGE_PATH, 'targets.scenario')
    run_script_path = pj(CMPY_PACKAGE_PATH, 'run_target.sh')

    for target_with_args in test.parse_targets_with_args(hostlist_path, target_types_path, targets_path):
        sys.stderr.write('Run target: "' + ' '.join(target_with_args) + '"\n')
        yatest.common.execute(
            [run_script_path] + target_with_args,
            env={
                'CMPY_DIR': CMPY_PACKAGE_PATH,
                'DRY_RUN': '1',
            },
        )
