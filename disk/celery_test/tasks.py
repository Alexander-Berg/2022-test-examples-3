# -*- coding: utf-8 -*-

from __future__ import absolute_import


import random
import signal
import logging

from pprint import pformat
from time import time
from copy import copy
from celery import Celery, Task
from celery.signals import (import_modules, task_prerun, before_task_publish,
                            setup_logging, worker_ready)
from billiard import current_process

from celery_custom import Consumer, TaskPool


QUEUE2_RABBITMQ = {
    'user': 'disk_test_queller',
    'password': 'eeTh5ohtho',
    'vhost': 'disk_test_queller',
}

QUEUE2_RABBITMQ_HOSTS = [
    'queue01f.dst.yandex.net:5672',
    'queue02f.dst.yandex.net:5672',
]

logging.basicConfig(filename='example.log', level=logging.INFO)

phantom_handler = logging.FileHandler('ouput.phout')
phantom_handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(message)s')
phantom_handler.setFormatter(formatter)
logging.getLogger('phout').addHandler(phantom_handler)
log = logging.getLogger('phout')


def get_random_hosts():
    hosts = copy(QUEUE2_RABBITMQ_HOSTS)
    random.shuffle(hosts)
    return hosts


# Celery URL должен быть такой 'amqp://<rabbitmq_user_name>:<rabbitmq_user_password>@<rabbitmq_host>/<rabbitmq_vhost>'
# Например 'amqp://mpfs_test:mpfs_test@queue01f.dst.yandex.net:5672/disk_test_queller'
BROKER_URL_TEMPLATE = 'amqp://%(user)s:%(password)s@%%(host)s/%(vhost)s' % QUEUE2_RABBITMQ
BROKER_URLS = [BROKER_URL_TEMPLATE % {'host': host} for host in get_random_hosts()]

app = Celery('mpfs',
             broker=BROKER_URLS,)

app.conf.CELERYD_CONSUMER = Consumer
app.conf.CELERYD_POOL = TaskPool
app.conf.CELERYD_PREFETCH_MULTIPLIER = 1
app.conf.CELERY_ACKS_LATE = True
app.conf.CELERY_DEFAULT_QUEUE = 'submit'
app.conf.CELERY_TASK_SERIALIZER = 'json'
app.conf.CELERYD_HIJACK_ROOT_LOGGER = False


# этот класс должен быть строго после задания настроек app выше, т.к. он их уже использует при импорте
class BaseTask(Task):
    max_retries = None

    def get_current_worker_name(self):
        p = current_process()
        if p._name == 'MainProcess':
            return None
        return p.initargs[1]

    def on_failure(self, exc, task_id, args, kwargs, einfo):
        worker = self.get_current_worker_name()
        if worker:
            worker = self.get_current_worker_name()
            kwargs['context'].update({'error': str(einfo.exception),
                                      'traceback': einfo.traceback,
                                      'worker': worker})

            self.retry(kwargs=kwargs, exc=exc, throw=False,
                       countdown=0, queue='submit')

        self._log_error(einfo, kwargs)

    def on_success(self, retval, task_id, args, kwargs):
        worker = self.get_current_worker_name()
        if worker:
            worker = self.get_current_worker_name()
            kwargs['context'].update({'finished': int(time() * 1000),
                                     'worker': worker})

            self.request.retries -= 1
            self.retry(kwargs=kwargs, throw=False, countdown=0, queue='completed')

        self._log_success(kwargs)

    def __call__(self, *args, **kwargs):
        worker = self.get_current_worker_name()
        if worker:
            # выполняется не в текущем потоке, а сабмитится в очередь
            kwargs['context'].update({'started': int(time() * 1000),
                                     'worker': worker})

            self.request.retries -= 1
            self.retry(kwargs=kwargs, throw=False, countdown=0, queue='started')
            self.request.retries += 1

        self._log_start()
        return super(BaseTask, self).__call__(*args, **kwargs)

    def _log_start(self):
        logging.info('Task %s started (try %d), name: %s' % (self.request.id, self.request.retries, self.name))

    def _log_success(self, kwargs):
        process_time, lifetime = None, None
        if kwargs.get('context'):
            process_time = (kwargs['context']['finished'] - kwargs['context']['started']) / 1000.0
            lifetime = (kwargs['context']['finished'] - kwargs['context']['created']) / 1000.0
        logging.info('Task %s finished, name: %s (processed: %.2f sec, lifetime: %.2f sec)' %
                     (self.request.id, self.name, process_time, lifetime))
        log.info('%.3f\t%s\t0\t0\t0\t%d\t%d\t0\t0\t0\t0\t200' % (time(), '', int(process_time * 1000000), int(lifetime * 1000000)))

    def _log_error(self, einfo, kwargs):
        logging.error('Task %s failed (try %d), name: %s' % (self.request.id, self.request.retries, self.name))
        logging.error(einfo.traceback)
        task_data = copy(kwargs)
        task_data.pop('context')
        logging.error(pformat(task_data))


@app.task(base=BaseTask)
def mpfs_fake_task(sleep_interval, context=None, **kwargs):
    import time
    time.sleep(sleep_interval)


@before_task_publish.connect
def setup_task_context_params(body, *args, **kwargs):
    if 'context' not in body['kwargs']:
        body['kwargs']['context'] = {}
    if 'created' not in body['kwargs']['context']:
        body['kwargs']['context']['created'] = int(time() * 1000)
