import yaml

from functools import partial

from library.python.testing.recipe import declare_recipe
from yatest.common import work_path, build_path, source_path
from mail.unistat.cpp.cython.canonize.recipe import (
    start_unistat,
    stop_unistat,
    prepare_resources,
)


def write_config(config, path):
    with open(path, 'w') as f:
        f.write(yaml.dump(config))


def start_sharpei_unistat(get_config, argv):
    config = get_config()
    config_path = work_path("unistat-config.yml")
    write_config(config, config_path)

    script_path = source_path("mail/sharpei/unistat/cython/sharpei_unistat.py")
    unistat_cmd = (
        f"{build_path('mail/sharpei/unistat/cython/cunistat')} {script_path} --cfg_path {config_path}"
    )

    _, logs = prepare_resources(dog_name='sharpei')
    start_unistat(script_path, unistat_cmd, logs, config["port"])


def prepare_unistat_daemon(get_config):
    start = partial(start_sharpei_unistat, get_config)
    declare_recipe(start, stop_unistat)
