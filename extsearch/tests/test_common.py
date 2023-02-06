import yatest


BINARY_PATH = yatest.common.binary_path('extsearch/video/robot/cm/crawl/cmpy/cmpy')
CONFIG_MODULE = 'extsearch.video.robot.cm.crawl.cmpy.config'


def _test_target(target_with_args):
    command = [BINARY_PATH, '--config-module', CONFIG_MODULE] + target_with_args

    yatest.common.execute(command, env={'DRY_RUN': '1'})
