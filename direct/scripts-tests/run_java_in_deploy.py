#!/usr/bin/env python

import os
import yatest


def run_test(name, main_class, env_param, environ_patch):
    env = os.environ.copy()
    env['PATH'] = env.get('PATH', '/bin:/usr/bin') + os.pathsep + yatest.common.binary_path(".")
    if environ_patch is not None:
        env.update(environ_patch)

    shell_path = yatest.common.source_path("direct/run_java_in_deploy.sh")

    return yatest.common.canonical_execute(["bash", shell_path, "--dry-run", name, main_class, env_param],
                                            env=env, cwd=yatest.common.output_path(), save_locally=True)


def test_web_no_dc_testing():
    return run_test("direct-web", "ru.yandex.direct.web.DirectWebApp", "testing", None)


def test_web_with_dc_testing():
    return run_test("direct-web", "ru.yandex.direct.web.DirectWebApp", "testing", {"DEPLOY_NODE_DC": "iva"})
