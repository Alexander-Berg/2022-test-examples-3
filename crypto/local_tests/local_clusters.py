import os

import mapreduce.yt.python.yt_stuff as vanilla
import yatest


def get_yt_config(yt_id, cell_tag):
    return vanilla.YtConfig(
        wait_tablet_cell_initialization=True,
        node_config={
            "tablet_node": {
                "resource_limits": {
                    "tablet_dynamic_memory": 100 * 1024 * 1024,
                    "tablet_static_memory": 100 * 1024 * 1024,
                }
            }
        },
        yt_id=yt_id,
        yt_work_dir=yatest.common.output_path(yt_id),
        cell_tag=cell_tag,
    )


def start_local_clusters(names):
    os.environ["YT_USE_SINGLE_TABLET"] = "1"

    result = {}

    for cell_tag, name in enumerate(names):
        config = get_yt_config(name, cell_tag)

        os.makedirs(config.yt_work_dir)

        yt = vanilla.YtStuff(config)
        yt.start_local_yt()

        yt.get_yt_client().set("//sys/@cluster_name", name)

        result[name] = yt

    cluster_config = {yt.yt_id: yt.get_cluster_config() for yt in result.values()}

    for yt in result.values():
        yt.get_yt_client().set("//sys/clusters", cluster_config)

    return result
