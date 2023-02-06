from util import init_root_logger
from models.worker import YDBConnection, WorkerModel
import argparse
import logging
import json


if __name__ == "__main__":
    init_root_logger()
    logging.basicConfig(level=logging.INFO)
    ap = argparse.ArgumentParser()
    ap.add_argument('--config', required=True, type=argparse.FileType('r'))
    ap.add_argument('--output', type=argparse.FileType('w'), required=True)
    args = ap.parse_args()
    app_conf = json.load(args.config)
    db = YDBConnection(app_conf['ydb'])
    stats = dict(WorkerModel(db, app_conf['ydb']['table']).stream_stats())
    result = {}
    for ch in app_conf['channels']:
        result[ch] = stats.get(ch, 0)
    json.dump(result, args.output)
