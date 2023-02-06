
import config
import json
import requests
import os
import task_generator


def fill_queue(args):
    conf, _ = config.init('test_writer')

    sqs_config = conf.SqsConfig
    sqs_map = conf.SqsMap.TestWriter

    def forward(query):
        return query

    task_generator.create_map(sqs_map, sqs_config)
    task_generator.loop_process(sqs_map, sqs_config, forward)


def monitor_queue(args):
    conf, _ = config.init('monitor_queue')
    client = task_generator.make_client(None, conf.SqsConfig)
    queues_for_check = [
        conf.SqsMap.Extractor.SourceQueueName,
        conf.SqsMap.DeepHD.SourceQueueName,
        conf.SqsMap.Pusher.SourceQueueName
    ]
    for i in queues_for_check:
        try:
            queue_url = client.get_queue_url(QueueName=i)['QueueUrl']
            q_attrs = client.get_queue_attributes(QueueUrl=queue_url, AttributeNames=['All'])
            print i, ' '.join(["notvisible_msg=%(ApproximateNumberOfMessagesNotVisible)s",
                                "delayed_msg=%(ApproximateNumberOfMessagesDelayed)s",
                                    "num_of_msg=%(ApproximateNumberOfMessages)s"]) % q_attrs['Attributes'], q_attrs
        except Exception as e:
            print e


def clean_queue(args):
    conf = config.init('test_reader')

    sqs_config = conf.SqsConfig
    sqs_map = conf.SqsMap.TestReader

    def forward(query):
        return query

    task_generator.create_map(sqs_map, sqs_config)
    task_generator.loop_process(sqs_map, sqs_config, forward)


def test(args):
    conf = config.init('test_reader')

    sqs_config = conf.SqsConfig
    sqs_map = conf.SqsMap.TestReader

    sqs_client = task_generator.make_client(sqs_map, sqs_config)
    q_url = sqs_client.get_queue_url(QueueName=conf.SqsMap.DeepHD.TargetQueueName)['QueueUrl']
    sqs_client.delete_queue(QueueUrl=q_url)


def test_webhook(args):
    conf, _ = config.init('text_webhook')
    task_id = os.getenv('TASK_ID', None)
    assert task_id is not None
    f = requests.get('https://sandbox.yandex-team.ru/api/v1.0/task/%s' % task_id)
    f = json.loads(f.text)
    formats = f['output_parameters']['formats']
    stream_url = f['output_parameters']['stream_url']
    stream = f['output_parameters']['stream']
    task_id = os.getenv('OVERRIDE_TASK_ID', task_id)
    data = {
        'status': 'VIDEO_FULL_UPLOADED',
        'stream': stream,
        'stream_url': stream_url,
        'formats': formats,
        'task_id': task_id,
    }
    webhook_url = f['input_parameters']['webhook_payload_url']
    webhook_url = webhook_url.split('/')
    if os.getenv('LOCALHOST', '') == '1':
        webhook_url[0] = 'http:'
    if os.getenv('LOCALHOST', '') == '1':
        webhook_url[2] = 'localhost:5001'
    else:
        webhook_url[2] = conf.VHConfig.HostApi
    if os.getenv('LOCALHOST', '') == '1':
        webhook_url = webhook_url[:3] + webhook_url[4:]
    is_id = os.getenv('INPUT_STREAM_ID', '')
    if is_id != '':
        webhook_url[-1] = is_id
    webhook_url = '/'.join(webhook_url)
    req = requests.post(webhook_url, json=data)
    print req.url, req.text, req.status_code
