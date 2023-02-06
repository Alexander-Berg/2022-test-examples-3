import contextlib
import os
import socket
import subprocess
import time

from typing import List
from typing import Optional
from typing import Tuple

import yatest.common
import yatest.common.network

SERVICE_HOST = 'localhost'
PORT_TO_REWRITE = '80'


# Starts a userver based service and returns when it is ready to accept requests
@contextlib.contextmanager
def start_uservice(
        *,
        service_path: str,
        static_config: str,
        port: int,
        timeout: float,
        host: str = 'localhost'
        ) -> None:
    service = subprocess.Popen([service_path, '--config', static_config])

    start_time = time.perf_counter()
    while True:
        passed_time = time.perf_counter() - start_time
        if passed_time >= timeout:
            service.terminate()
            raise TimeoutError(
                f'Waited too long for the {service_path} '
                f'on host {host} to start accepting connections.'
            )

        try:
            with socket.create_connection(
                (host, port), timeout=timeout-passed_time,
            ):
                break
        except OSError:
            time.sleep(0.1)

    yield service

    service.terminate()


def path_to_service_from_test(service_name: str) -> str:
    return os.path.join(
        'drive',
        'services',
        service_name,
    )


def file_names_to_paths(*, service_name: str, file_names: List[str]) -> List[str]:
    paths_list = []
    for file in file_names:
        full_path = os.path.join(path_to_service_from_test(service_name), 'configs', file)
        paths_list.append(yatest.common.source_path(full_path))
    return paths_list


# Updates ports, directories and DB connection params of a service configs
def prepare_configs(
        *,
        additional_rewrites: Optional[List[Tuple[str, str]]] = None,
        additional_config_files_list: Optional[List[str]] = None,
        service_name: str,
        ) -> Tuple[str, int]:
    new_dir = os.path.join(yatest.common.output_path(), service_name)
    if not os.path.exists(new_dir):
        os.mkdir(new_dir)

    config_files_names = ['static_config.yaml', 'dynamic_config.json']
    if additional_config_files_list:
        config_files_names.extend(additional_config_files_list)

    config_paths_list = file_names_to_paths(
        service_name=service_name,
        file_names=config_files_names,
    )

    new_port = yatest.common.network.PortManager().get_port()

    for config_path in config_paths_list:
        with open(config_path) as conf_file:
            conf = conf_file.read()

        conf = conf.replace('./', new_dir + '/')
        conf = conf.replace('/redis', new_dir)
        conf = conf.replace(PORT_TO_REWRITE + '\n', str(new_port) + '\n')

        if additional_rewrites:
            for old, new in additional_rewrites:
                conf = conf.replace(old, new)

        new_path = os.path.join(new_dir, os.path.basename(config_path))
        with open(new_path, 'w') as new_conf_file:
            new_conf_file.write(conf)

    return os.path.join(new_dir, 'static_config.yaml'), new_port


@contextlib.contextmanager
def setup_and_start(
        service_name: str,
        *,
        additional_config_files_list: Optional[List[str]] = None,
        timeout: float = 15.0,
        additional_config_rewrites: Optional[List[Tuple[str, str]]] = None,
        ) -> None:
    service_path=yatest.common.binary_path(
        os.path.join(
            path_to_service_from_test(service_name),
            service_name,
        )
    )

    static_config, new_port = prepare_configs(
        additional_config_files_list=additional_config_files_list,
        service_name=service_name,
        additional_rewrites=additional_config_rewrites,
    )

    with start_uservice(
            service_path=service_path,
            static_config=static_config,
            port=new_port,
            timeout=timeout,
            ):
        yield new_port
