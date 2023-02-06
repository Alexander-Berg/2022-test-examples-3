import contextlib
import os
from typing import List
from typing import Optional
from typing import Tuple

import start_uservice
import yatest.common
import yatest.common.network

SERVICE_HOST = 'localhost'


def path_to_service_from_test(service_name: str) -> str:
    return os.path.join(
        'market',
        'dev-exp',
        'no-codegen-userver-samples',
        service_name,
    )


def file_names_to_paths(*, service_name: str, file_names: List[str]) -> List[str]:
    paths_list = []
    for file in file_names:
        full_path = os.path.join(path_to_service_from_test(service_name), file)
        paths_list.append(yatest.common.source_path(full_path))
    return paths_list


# Updates ports, directories and DB connection params of a service configs
def prepare_configs(
        *,
        port: int,
        additional_rewrites: Optional[List[Tuple[str, str]]] = None,
        additional_config_files_list: Optional[List[str]] = None,
        service_name: str,
        postgresql: Optional[str] = None,
        mongo: Optional[str] = None,
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

        conf = conf.replace('/etc/' + service_name, new_dir)
        conf = conf.replace('/var/cache/' + service_name, new_dir)
        conf = conf.replace('/var/log/' + service_name, new_dir)
        conf = conf.replace('/var/run/' + service_name, new_dir)
        conf = conf.replace(str(port), str(new_port))

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
        rewrite_port: int,
        additional_config_files_list: Optional[List[str]] = None,
        timeout: float = 15.0,
        postgresql: Optional[str] = None,
        mongo: Optional[str] = None,
        additional_config_rewrites: Optional[List[Tuple[str, str]]] = None,
        ) -> None:
    service_path=yatest.common.binary_path(
        os.path.join(
            path_to_service_from_test(service_name),
            'userver-samples-' + service_name,
        )
    )

    static_config, new_port = prepare_configs(
        port=rewrite_port,
        additional_config_files_list=additional_config_files_list,
        service_name=service_name,
        postgresql=postgresql,
        mongo=mongo,
        additional_rewrites=additional_config_rewrites,
    )

    with start_uservice.start_uservice(
            service_path=service_path,
            static_config=static_config,
            port=new_port,
            timeout=timeout,
            ):
        yield new_port
