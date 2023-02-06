from sqs import SQSClient, SQSQueueBalancer
from conf import Config
from player_test import PlayerTest
from log_aggregator import LogAggregatorClient
from time import time, sleep
from util import init_root_logger
from sys import exc_info
from socket import gethostname
from traceback import extract_tb
from google.protobuf.json_format import MessageToJson
import logging
import job


SETUP_TIME = 120


def heartbeat(config, logsvc=None):
    if logsvc is not None and not logsvc.ping():
        logging.error('log aggregator service not responding')
        return False
    with open(config.queuesvc.hb_file, 'w') as fd:
        hb = str(int(time()))
        logging.info('worker heartbeat {}'.format(hb))
        fd.write(hb)
    return True

if __name__ == '__main__':
    init_root_logger()
    logging.info('service started')
    config = Config()
    sqs = SQSClient(config.sqs)
    checker = PlayerTest(config)
    logsvc = LogAggregatorClient(config.logsvc, config.http.proxy_port)
    queue_balancer = SQSQueueBalancer(sqs, config.sqs.input_queues)
    output_queue = sqs.get_queue(config.sqs.output_queue) if config.sqs.output_queue else None
    config.init_workdir()
    checker.start()
    if config.env in ['prod', 'dev']:
        for i in range(SETUP_TIME / 2):
            heartbeat(config)
            sleep(2)
    while True:
        try:
            if not heartbeat(config, logsvc):
                sleep(5)
                continue
            input_queue = queue_balancer.get_queue()
            with input_queue.pop() as msg:
                if msg.empty():
                    continue
                if msg.receive_count_exceeded():
                    continue
                request = job.from_string(msg.payload())
                logging.info('request {} processing started'.format(MessageToJson(request)))
                if request.VideoCapture.Duration > 0:
                    msg.change_visibility(request.VideoCapture.Duration + 60)
                result = checker.check_player_url(request, logsvc)
                result.Source = input_queue.name
                result.Host = gethostname()
                logging.info('request {} done, is_playing: {}'.format(MessageToJson(result), result.IsPlaying))
                if not result.IsPlaying and (request.RetryCount > 0 or request.Crawl.CurrentTry < request.Crawl.RetryCount):
                    if request.RetryCount > 0:
                        request.RetryCount -= 1
                    request.Crawl.CurrentTry += 1
                    if request.Flags.Fast:
                        request.Flags.Fast = False
                    input_queue.push(request.SerializeToString())
                if output_queue is not None:
                    output_queue.push(result.SerializeToString())
        except Exception as e:
            exc_type, exc_value, exc_traceback = exc_info()
            logging.error(repr(extract_tb(exc_traceback)))
            logging.info('got error response: {}'.format(e))
