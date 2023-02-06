import logging
import os
import socket
import sys


DEFAULT_CONFIGURATION = {
    "st.user-agent": "market-report-automator",
    "st.base-url": "https://st-api.yandex-team.ru",
    "st.reference-format": r".*Report binaries stored at: (\S*)",
    "report.binaries": "/var/lib/yandex/report-testing-assistant/binaries",
    "report.configuration.generator": "/usr/lib/yandex/market-report-configs/market_search_cfg_generator.py",
    "report.configuration.generator.template": "/etc/yandex/market-report-configs/template.cfg",
    "report.configuration.generator.default": "/var/lib/search/report-data/backends/{}.cfg".format(socket.getfqdn()),
    "report.configuration.static": "/etc/yandex/{servant}/{servant}.cfg",
    "report.logs": "/var/lib/yandex/report-testing-assistant/logs",
    "rta.port": "23384",
    "rta.registry": "/var/lib/yandex/report-testing-assistant/registry",
    "rta.ssh-key": "/var/lib/yandex/report-testing-assistant/ssh-key",
}


def _configure_logger():
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler(stream=sys.stdout)
    handler.setLevel(logging.DEBUG)
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    handler.setFormatter(formatter)
    logger.addHandler(handler)


def configure(configuration_path):
    _configure_logger()
    configuration = DEFAULT_CONFIGURATION
    if os.path.exists(configuration_path):
        logging.info("Loading configuration from `{}`".format(configuration_path))
        with open(configuration_path, "r") as stream:
            for number, line in enumerate(stream, 1):
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                try:
                    option, value = map(str.strip, line.split("=", 1))
                except:
                    logging.error("Failed to parse the line `{}`".format(line))
                    raise
                configuration[option] = value
    logging.debug("Use configuration {}".format(configuration))
    return configuration
