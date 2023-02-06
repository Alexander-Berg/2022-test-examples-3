import os
import re

import yatest.common

from crypta.lib.python import templater

CONFIG_TEMPLATE = """
from rtmapreduce.config.presets.test import *

TableRegistry.add_tables([
    {% for table in inputs+outputs %}
    TestTable("{{ table }}", **{{ table_attrs.get(table, {}) }}),
    {% endfor %}
])

TaskRegistry.add_tasks([
    TestTask("{{ task }}",
         SrcTables={{ inputs }},
         DstTables={{ outputs }},
         Options={{ options }},
         Attrs={{ attrs }},
     ),
])
"""


def create_manifest_for_single_op(binary, op, inputs, outputs, options=None, attrs=None, table_attrs=None, **kwargs):
    options = options or []
    attrs = attrs or []
    table_attrs = table_attrs or {}

    prefix = re.search(r"^lib(.+)-dynlib\.so$", os.path.basename(binary)).group(1)
    task = "{}:{}".format(prefix, op)

    content = templater.render_template(CONFIG_TEMPLATE, dict(task=task, inputs=inputs, outputs=outputs, options=options, attrs=attrs, table_attrs=table_attrs))

    config = yatest.common.test_output_path("task_config.cfg")
    with open(config, "w") as f:
        f.write(content)

    return dict({
        "input_tables": inputs,
        "output_tables": outputs,
        "config": config,
        "dynlibs": [binary],
    }, **kwargs)


def get_options(resource_service_url_prefix, juggler_url_prefix, frozen_time=None):
    result = [
        "--caching-proxy-url-prefix {}".format(resource_service_url_prefix),
        "--caching-proxy-cluster rtmr-vla",

        "--juggler-url-prefix {}".format(juggler_url_prefix),

        "--updater-retry-count 5",
        "--updater-retry-interval-sec 1",
        "--updater-update-interval-sec 300",

        "--reporter-resource-ok-age-sec 1",
        "--reporter-resource-ok-repeat-sec 1000",
        "--reporter-report-interval-sec 1",

        "--wait-for-update",
    ]
    if frozen_time is not None:
        result.append("--frozen-time {}".format(frozen_time))
    return result
