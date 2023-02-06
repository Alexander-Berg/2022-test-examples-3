import os
import sys

import yatest.common

STARTER_BIN = yatest.common.binary_path("market/sre/tools/starter/starter")


def parse_start_script_args(args):
    import argparse
    import shlex

    parser = argparse.ArgumentParser()
    parser.add_argument("--config")
    parser.add_argument("--host")
    parser.add_argument("--port")
    parser.add_argument("--log-path")
    parser.add_argument("--env")
    parser.add_argument("--dc")
    parser.add_argument("--listen-threads")
    parser.add_argument("--root-path")

    return vars(parser.parse_args(shlex.split(args)))


class TestGencfgContainer(object):
    def test_start_script_args(self, container_gencfg_without_volumes, app_name):
        _, work_dir, env = container_gencfg_without_volumes

        res = yatest.common.execute(
            [STARTER_BIN, "-dry-run", "-app-name", app_name, "-app-type", "cpp"], cwd=work_dir, env=env
        )

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

        start_script, args_str = res.std_out.split(" ", 1)
        args = parse_start_script_args(args_str)

        assert start_script == os.path.join(work_dir, "bin", app_name)

        assert args["config"] == os.path.join(work_dir, "conf", app_name)
        assert args["log_path"] == os.path.join(work_dir, "logs", app_name, "%s.log" % app_name)  # log-path
        assert args["port"] == "80"
        assert args["env"] == "testing"
        assert args["dc"] == "sas"
        assert args["root_path"] == work_dir  # root-path
        assert args["listen_threads"] == "5"  # listen-threads


class TestYpLite(object):
    def test_start_script_args(self, container_yp_lite_without_volume, app_name):
        _, work_dir, env = container_yp_lite_without_volume

        res = yatest.common.execute(
            [STARTER_BIN, "-dry-run", "-app-name", app_name, "--app-type", "cpp"], cwd=work_dir, env=env
        )

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

        start_script, args_str = res.std_out.split(" ", 1)
        args = parse_start_script_args(args_str)

        assert start_script == os.path.join(work_dir, "bin", app_name)

        assert args["config"] == os.path.join(work_dir, "conf", app_name)
        assert args["log_path"] == os.path.join(work_dir, "logs", app_name, "%s.log" % app_name)  # log-path
        assert args["port"] == "80"
        assert args["env"] == "testing"
        assert args["dc"] == "vla"
        assert args["root_path"] == work_dir  # root-path
        assert args["listen_threads"] == "5"  # listen-threads
