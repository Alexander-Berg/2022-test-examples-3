from tests_common.pytest_bdd import (
    given,
)

from mail.devpack.lib.components.sharpei import SharpeiCloud


@given("cloud sharpei is started")
def step_cloud_sharpei_is_started(context):
    context.iam_server.reset()
    context.yc_server.reset()
    context.coord.components[SharpeiCloud].restart()
    context.sharpei_api = context.coord.components[SharpeiCloud].api()
