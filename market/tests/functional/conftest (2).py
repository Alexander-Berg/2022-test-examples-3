import exts.tmp
import os
import pytest


@pytest.fixture(scope="module")
def app_name():
    return "test-app"


@pytest.fixture
def temp_dir():
    with exts.tmp.temp_dir() as tmp_dir:
        yield os.path.realpath(tmp_dir)  # resolve symlinks


def work_dir(root_dir):
    wd = os.path.join(root_dir, "workdir")
    os.makedirs(wd)

    return wd


@pytest.fixture
def container(app_name, temp_dir):
    """A general Nanny container"""
    env = os.environ
    env["BSCONFIG_IPORT_PLUS_1"] = "80"
    env["BSCONFIG_IPORT_PLUS_2"] = "81"

    # work dir
    wd = work_dir(root_dir=temp_dir)

    # create a start script
    app_binary(work_dir=wd, name=app_name)

    return temp_dir, wd, env


@pytest.fixture
def container_gencfg_without_volumes(container):
    """A Nanny container with a GenCfg group"""
    temp_dir, wd, env = container

    # log dir, usually it is a volume or symlink
    log_dir = os.path.join(temp_dir, "var/logs/yandex")
    os.makedirs(log_dir, 0755)

    # create required directories, usually they are symlinks
    os.makedirs(os.path.join(wd, "pdata"), 0755)

    os.symlink(log_dir, os.path.join(wd, "logs"))

    dump_json_gencfg(work_dir=wd)

    yield temp_dir, wd, env


@pytest.fixture
def container_gencfg_with_volumes(container_gencfg_without_volumes):
    temp_dir, wd, env = container_gencfg_without_volumes

    # create "volumes"
    for d in ("cores", "persistent-data", "logs"):
        os.mkdir(os.path.join(temp_dir, d))

    # remove the directory /var/logs/yandex to create a symlink from /logs
    log_dir = os.path.join(temp_dir, "var/logs/yandex")
    os.rmdir(log_dir)
    os.symlink(os.path.join(temp_dir, "logs"), log_dir)

    # remove the directory <work_dir>/pdata to create a symlink from /persistent-data
    pdata_dir = os.path.join(wd, "pdata")
    os.rmdir(pdata_dir)
    os.symlink(os.path.join(temp_dir, "persistent-data"), pdata_dir)

    return temp_dir, wd, env


@pytest.fixture
def container_yp_lite_without_volume(container_gencfg_without_volumes):
    """A Nanny container with a YP-lite group"""
    temp_dir, wd, env = container_gencfg_without_volumes

    env["CPU_GUARANTEE"] = "4.530973c"
    env["MEM_GUARANTEE"] = "9663676416"

    dump_json_yp_lite(work_dir=wd)

    yield temp_dir, wd, env


def app_binary(work_dir, name):
    content = """#!/bin/bash

echo "bin: $0"
echo "args: $@"
"""

    bin_dir = os.path.join(work_dir, "bin")
    os.mkdir(bin_dir)

    app_binary = os.path.join(bin_dir, "%s-start.sh" % name)

    with open(app_binary, "w") as fp:
        fp.write(content)
    os.chmod(app_binary, 0755)


def dump_json_gencfg(work_dir):
    content = """
{
  "properties" : {
    "tags" : "SAS_MARKET_TEST_CLICKHOUSE_DEALER a_ctype_testing a_dc_sas a_geo_sas a_itype_marketclickhousedealer"
  },
  "container" : {
    "constraints" : {
      "cpu_guarantee" : "4.530973c",
      "cpu_limit" : "4.530973c",
      "memory_guarantee" : "9663676416",
      "memory_limit" : "9768534016"
    }
  }
}
"""
    dump_json = os.path.join(work_dir, "dump.json")

    with open(dump_json, "w") as fp:
        fp.write("%s\n" % content)


def dump_json_yp_lite(work_dir):
    content = """
{
  "properties" : {
    "DEPLOY_ENGINE" : "YP_LITE",
    "tags" : "a_geo_vla a_dc_vla a_itype_marketclickhousedealer a_ctype_testing a_prj_market a_metaprj_market"
  },
  "container" : {
    "constraints" : {
    }
  }
}
"""
    dump_json = os.path.join(work_dir, "dump.json")

    with open(dump_json, "w") as fp:
        fp.write("%s\n" % content)
