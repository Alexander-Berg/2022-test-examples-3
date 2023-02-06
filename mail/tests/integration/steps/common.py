import logging
import json
import yaml
from mail.devpack.lib import helpers
from mail.husky.husky.types import Status
from mail.husky.devpack.components.worker import Husky
from ora2pg.app import config_file
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file

QUERIES = load_from_my_file(__file__)
log = logging.getLogger(__name__)


def plan_task(context, task, task_args, **kwargs):
    for key, default in {
        "uid": context.user.uid,
        "status": Status.Pending,
        "shard_id": context.config.default_shard_id,
    }.items():
        if key not in kwargs:
            kwargs[key] = default
    cur = qexec(
        context.huskydb_conn,
        QUERIES.add_task,
        task=task,
        task_args=json.dumps(task_args),
        **kwargs
    )
    return cur.fetchone()[0]


def read_app_config(context):
    husky = context.coordinator.components[Husky]
    app_config_path = husky.app_config_path
    return config_file.yaml_read_config(open(app_config_path, 'r'), '( ͡° ͜ʖ ͡°)')


def write_app_config(context, config, do_restart):
    husky = context.coordinator.components[Husky]
    app_config_path = husky.app_config_path
    helpers.write2file(yaml.dump(config), app_config_path)
    if do_restart:
        husky.stop()
        husky.start()


def update_app_config(context, update):
    app_config = read_app_config(context)
    app_config.update(update)
    write_app_config(context, app_config, do_restart=True)
