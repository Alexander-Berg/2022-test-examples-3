import os
import yt.wrapper as yt
from market.ir.smartmatcher.udf.lib.py.mapreduce import ClusterWithRejectsReducer
from market.ir.smartmatcher.udf.lib.py.constants import NEW_CLUSTER_TABLE_SCHEMA
from market.ir.smartmatcher.udf.lib.py.utils import get_default_logger, get_default_mapreduce_io
from market.ir.smartmatcher.udf.lib.py.mapreduce.helpers import create_output_table_with_ttl


def main():
    log = get_default_logger("TsarMain")
    yt.config.set_proxy(os.getenv("YT_PROXY"))
    input_table, output_table, ttl, pool, job_mem, user_slots, job_cnt = get_default_mapreduce_io(log)
    create_output_table_with_ttl(output_table, NEW_CLUSTER_TABLE_SCHEMA, ttl, log)
    mappings_to_filter_path = os.getenv("MAPPINGS_PATH")
    min_edge_score = float(os.getenv("MIN_EDGE_SCORE"))
    threshold = float(os.getenv("THRESHOLD"))
    spec = {"auto_merge": {"mode": "relaxed"}, "pool": pool}
    if user_slots > 0:
        spec["resource_limits"] = dict()
        spec["resource_limits"]["user_slots"] = user_slots
    if job_cnt > 0:
        spec["job_count"] = job_cnt
    prod_job = yt.run_reduce(
        ClusterWithRejectsReducer(mappings_to_filter_path, min_edge_score, threshold),
        reduce_by="cluster_hash",
        source_table=input_table,
        destination_table=output_table,
        sync=False,
        spec=spec,
        memory_limit=job_mem,
        local_files=[mappings_to_filter_path]
    )
    log.info("Yt started reduce: " + str(prod_job.url))
    prod_job.wait(print_progress=False)


if __name__ == "__main__":
    main()
