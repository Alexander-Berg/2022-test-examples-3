import datetime
import io
import os
import sys
import time

import yatest.common

STARTER_BIN = yatest.common.binary_path("market/sre/tools/starter/starter")


def parse_start_script_args(args):
    import argparse
    import shlex

    parser = argparse.ArgumentParser()
    parser.add_argument("--logdir")
    parser.add_argument("--httpport")
    parser.add_argument("--debugport")
    parser.add_argument("--tmpdir")
    parser.add_argument("--datadir")
    parser.add_argument("--extdatadir")
    parser.add_argument("--environment")
    parser.add_argument("--dc")
    parser.add_argument("--cpu-count")

    return vars(parser.parse_args(shlex.split(args)))


class TestGencfgContainer(object):
    def test_start_script_args(self, container_gencfg_without_volumes, app_name):
        """Tests all arguments are passed"""
        _, work_dir, env = container_gencfg_without_volumes

        res = yatest.common.execute([STARTER_BIN, "-dry-run", "-app-name", app_name], cwd=work_dir, env=env)

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

        start_script, args_str = res.std_out.split(" ", 1)
        args = parse_start_script_args(args_str)

        assert start_script == os.path.join(work_dir, "bin", "%s-start.sh" % app_name)

        assert args["logdir"] == os.path.join(work_dir, "logs", app_name)
        assert args["httpport"] == "80"
        assert args["debugport"] == "81"
        assert args["tmpdir"] == os.path.join(work_dir, "tmp")
        assert args["datadir"] == os.path.join(work_dir, "pdata", app_name)
        assert args["extdatadir"] == os.path.join(work_dir, "data-getter")
        assert args["environment"] == "testing"
        assert args["dc"] == "sas"
        assert args["cpu_count"] == "5"  # cpu-count

    def test_start_script_run(self, container_gencfg_without_volumes, app_name):
        """Tests run"""
        temp_dir, work_dir, env = container_gencfg_without_volumes

        res = yatest.common.execute([STARTER_BIN, "-app-name", app_name, "-root-dir", temp_dir], cwd=work_dir, env=env)

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

    def test_required_directories_are_created_wo_volumes(self, container_gencfg_without_volumes, app_name):
        """Tests directories are created"""
        temp_dir, work_dir, env = container_gencfg_without_volumes

        res = yatest.common.execute([STARTER_BIN, "-app-name", app_name, "-root-dir", temp_dir], cwd=work_dir, env=env)

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

        # must always exist
        assert os.path.exists(os.path.join(temp_dir, "var/logs/yandex", app_name)) is True
        # when volume /persistent-data does not mounted is a symlink
        assert os.path.exists(os.path.join(work_dir, "pdata", app_name)) is True
        # must exist
        assert os.path.exists(os.path.join(work_dir, "tmp")) is True

    def test_required_directories_are_created_with_volumes(self, container_gencfg_with_volumes, app_name):
        """Tests directories are created"""
        temp_dir, work_dir, env = container_gencfg_with_volumes

        res = yatest.common.execute([STARTER_BIN, "-app-name", app_name, "-root-dir", temp_dir], cwd=work_dir, env=env)

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

        # must exist
        assert os.path.exists(os.path.join(temp_dir, "var/logs/yandex", app_name)) is True
        # must exist
        assert os.path.exists(os.path.join(temp_dir, "persistent-data", app_name)) is True

        # because /cores is not a volume in a test environment,
        # we test here that a symlink /cores -> /var/logs/yandex/test-app/hprof is not created
        hprof_dir = os.path.join(work_dir, "logs", app_name, "hprof")
        assert os.path.exists(hprof_dir) is False

        # must exist
        assert os.path.exists(os.path.join(work_dir, "tmp")) is True

    def test_rotation_of_hprof_files(self, container_gencfg_without_volumes, app_name):
        """Tests the rotation of hprof files by number of files"""
        temp_dir, work_dir, env = container_gencfg_without_volumes

        hprof_dir = os.path.join(temp_dir, "var/logs/yandex", app_name, "hprof")
        os.makedirs(hprof_dir, 0755)  # in the real life it is created by a start script

        now = datetime.datetime.now()
        hprof_files = 5
        max_hprof_files = 2  # is hardcoded in `java-application.go`

        # create hprof files with different ages
        for i in range(1, hprof_files):
            time_from_now = time.mktime((now - datetime.timedelta(hours=i)).timetuple())
            f = os.path.join(hprof_dir, "%d-hours-before-now.hprof" % i)
            with io.open(f, "w") as fd:
                fd.write(u"")
            os.utime(f, (time_from_now, time_from_now))

        # rotate hprof files
        res = yatest.common.execute([STARTER_BIN, "-app-name", app_name, "-root-dir", temp_dir], cwd=work_dir, env=env)

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

        files = os.listdir(hprof_dir)

        # check the number of hprof files
        assert len(files) == max_hprof_files

        # check left files are the youngest ones
        oldest_file_time = time.mktime((now - datetime.timedelta(hours=max_hprof_files)).timetuple())
        for f in files:
            assert os.stat(os.path.join(hprof_dir, f)).st_mtime >= oldest_file_time


class TestYpLite(object):
    def test_start_script_args(self, container_yp_lite_without_volume, app_name):
        """Tests all arguments are passed"""
        _, work_dir, env = container_yp_lite_without_volume

        res = yatest.common.execute([STARTER_BIN, "-dry-run", "-app-name", app_name], cwd=work_dir, env=env)

        sys.stderr.write("stdout: %s\n" % res.std_out)

        assert res.exit_code == 0
        assert res.std_err == ""

        start_script, args_str = res.std_out.split(" ", 1)
        args = parse_start_script_args(args_str)

        assert start_script == os.path.join(work_dir, "bin", "%s-start.sh" % app_name)

        assert args["logdir"] == os.path.join(work_dir, "logs", app_name)
        assert args["httpport"] == "80"
        assert args["debugport"] == "81"
        assert args["tmpdir"] == os.path.join(work_dir, "tmp")
        assert args["datadir"] == os.path.join(work_dir, "pdata", app_name)
        assert args["extdatadir"] == os.path.join(work_dir, "data-getter")
        assert args["environment"] == "testing"
        assert args["dc"] == "vla"
        assert args["cpu_count"] == "5"  # cpu-count
