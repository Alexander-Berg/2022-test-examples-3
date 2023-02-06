from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJobResult
from util import init_root_logger
import yt.wrapper as yt
from sqs import SQSClient
from conf import Config
from time import time
import os
import logging
import argparse
from extsearch.video.robot.crawling.player_testing.services.yt_writer.pipeline import PipelineHandler


def write_portion(client, pipeline, config, messages, shard):
    rows = []
    for msg in messages:
        res = None
        try:
            res = TJobResult()
            res.ParseFromString(msg.payload())
        except Exception as e:
            logging.error('failed to parse message {}: {}'.format(msg.id(), e))
            continue
        if not res.Job.Id:
            logging.error('empty job ID')
            continue
        if not res.Job.Url:
            logging.error('empty job URL')
            continue
        rows.append({'job_id': res.Job.Id,
                     'started': res.Started,
                     'url': res.Job.Url,
                     'data': msg.payload()})
        if res.Job.OutputPipeline:
            pipeline.push(res.Job.OutputPipeline, res, msg.payload())

    def make_name():
        ts = int(time())
        return str(ts) if shard is None else '{}_{}'.format(shard, ts)
    if rows:
        output_portion = yt.ypath_join(config.output_prefix, make_name())
        logging.info('writing [{}] with {} rows'.format(output_portion, len(rows)))
        client.write_table(output_portion, rows)
    try:
        pipeline.commit()
    except:
        logging.info('failed to commit pipelined data')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--output-prefix')
    ap.add_argument('--shard')
    args = ap.parse_args()
    init_root_logger()
    logging.info('service started')
    config = Config()
    sqs = SQSClient(config.sqs)
    queue = sqs.get_queue(config.sqs.output_queue)
    yt_config = config.yt_writer
    if args.output_prefix is not None:
        yt_config.output_prefix = args.output_prefix
    client = yt.YtClient(proxy=yt_config.proxy, token=os.environ.get('YT_TOKEN'))
    pipeline = PipelineHandler(client)
    while True:
        messages = []
        start = time()
        while start + yt_config.flush_timeout > time() and len(messages) < yt_config.flush_row_count:
            msg = queue.pop(visibility_timeout=yt_config.flush_timeout * 2)
            if msg.empty():
                continue
            messages.append(msg)
        write_portion(client, pipeline, yt_config, messages, args.shard)
        for msg in messages:
            msg.done()
