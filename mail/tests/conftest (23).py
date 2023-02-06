import os
import pytest

from .ratesrv_client import RatesrvClient
from .util import ping_or_sleep

from mail.python.config_updater import update_config
from yatest.common import execute, source_path, binary_path, output_path, network


LOCALHOST = "localhost"
BIN_PATH = "mail/ratesrv/bin/app"
CONFIG_PATH = os.path.join(binary_path("mail/ratesrv/config"), "ratesrv.yml-production")
HOSTS_CONFIG_PATH = "mail/ratesrv/package/app/config/hosts.yml"
GROUP_CONFIG_PATH = "mail/ratesrv/package/app/config/production_groups.yml"

TEST_GROUP = "testgroup"

TEST_LIMIT_NAME = "testlimit"
TEST_GC_CONFIG = {
    "check_interval": 5,  # ms
    "ttl": 500  # ms
}
TEST_RECOVERY_RATE_DEFAULT = {
    "threshold": 50,
    "recovery_rate": 10,
    "recovery_interval": 250,  # ms
    "ignore_limit": False
}

TEST_DOMAIN = "testdomain"
TEST_RECOVERY_RATE_FOR_TEST_DOMAIN = {
    "threshold": 1000,
    "recovery_rate": 100,
    "recovery_interval": 250,  # ms
    "ignore_limit": True
}

TEST_LIMIT_NAME_RARE_GC = "testlimit_static"
TEST_RARE_GC = {
    "check_interval": 1000000,
    "ttl": 1000000
}

TEST_DOMAIN_NO_RECOVERY = "testdomain_no_recovery"
TEST_RECOVERY_RATE_NO_RECOVERY = {
    "threshold": 1000000,
    "recovery_rate": 0,
    "recovery_interval": 1000000,
    "ignore_limit": True
}


@pytest.yield_fixture(scope="session")
def pm():
    manager = network.PortManager()
    try:
        yield manager
    finally:
        manager.release()


@pytest.fixture(scope="session")
def ratesrv_port(pm):
    yield pm.get_port()


@pytest.fixture(scope="session")
def ratesrv_request_prefix(ratesrv_port):
    yield "http://%s:%d/" % (LOCALHOST, ratesrv_port)


@pytest.fixture(scope="session")
def hosts_config():
    new_conf_path = output_path("ratesrv_new_hosts_config.yml")
    update_config(source_path(HOSTS_CONFIG_PATH), new_conf_path, {
        "localhost": LOCALHOST,
        "hosts": [LOCALHOST]
    })
    return new_conf_path


@pytest.fixture(scope="session")
def group_config():
    new_conf_path = output_path("ratesrv_new_group_config.yml")
    update_config(source_path(GROUP_CONFIG_PATH), new_conf_path, {
        TEST_GROUP: {
            TEST_LIMIT_NAME: {
                "part_count": 1,
                "gc": TEST_GC_CONFIG,
                "domains": {
                    "default": TEST_RECOVERY_RATE_DEFAULT,
                    TEST_DOMAIN: TEST_RECOVERY_RATE_FOR_TEST_DOMAIN
                }
            },
            TEST_LIMIT_NAME_RARE_GC: {
                "part_count": 1,
                "gc": TEST_RARE_GC,
                "domains": {
                    "default": TEST_RECOVERY_RATE_DEFAULT,
                    TEST_DOMAIN_NO_RECOVERY: TEST_RECOVERY_RATE_NO_RECOVERY
                }
            }
        }
    }, allow_new_keys=True)
    return new_conf_path


@pytest.fixture(scope="session")
def config(ratesrv_port, group_config, hosts_config):
    new_conf_path = output_path("ratesrv_new_config.yml")
    update_config(source_path(CONFIG_PATH), new_conf_path, {
        "config.system.dir": "",
        "config.system.gid": "",
        "config.system.uid": "",
        "config.system.pid": "",
        "config.log.global.sinks.0.path": output_path("yplatform.log"),
        "config.log.ratesrv.sinks.0.path": output_path("ratesrv.tskv"),
        "config.log.web_server.sinks.0.path": output_path("access.log"),
        "config.log.http_client.sinks.0.path": output_path("http_client.tskv"),
        "config.modules.module.4.configuration.groups": group_config,  # storage module
        "config.modules.module.5.configuration.ssl_context": {},  # messenger module
        "config.modules.module.6.configuration.hosts": {  # router module
            "method": "file",
            "file": hosts_config
        },
        "config.modules.module.7.configuration.tvm_host": LOCALHOST,  # tvm module
        "config.modules.module.7.configuration.wait_first_update_on_start": False,  # tvm module
        "config.modules.module.8.configuration.endpoints.listen": [{"_addr": LOCALHOST, "_port": ratesrv_port, "ssl": "off"}],  # web_server module
        "config.modules.module.8.configuration.ssl": {},
        "config.modules.module.9.configuration.tvm.enable": 0,  # tvm ratesrv

    })
    return new_conf_path


@pytest.fixture(scope="session")
def ratesrv_client(ratesrv_request_prefix, ratesrv_binary):
    """
    Require ratesrv_binary to ensure that binary is instantiated
    """
    yield RatesrvClient(ratesrv_request_prefix)


@pytest.yield_fixture(scope="session")
def ratesrv_binary(config, ratesrv_request_prefix):
    instance = execute([binary_path(BIN_PATH), config], wait=False, close_fds=True)
    local_client = RatesrvClient(ratesrv_request_prefix)
    for i in range(10):
        if ping_or_sleep(local_client):
            break
    try:
        yield instance
    finally:
        instance.terminate()
