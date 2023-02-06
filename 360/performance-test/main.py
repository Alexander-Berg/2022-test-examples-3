#! /usr/bin/env python
# -*- coding: utf-8 -*-

from sandbox import common
import logging
import time
import sandbox.common.types.task
import os
import sys

logger = logging.getLogger()
logger.addHandler(logging.StreamHandler())
logger.setLevel(logging.DEBUG)

AUTH_TOKEN = os.environ['AUTH_TOKEN']
TASK_TYPE = os.environ['TASK_TYPE']
CONFIG_PATH = os.path.join(os.path.dirname(__file__), 'config.yaml')
COMMIT = os.environ['COMMIT']
TOUCH_URL = os.environ['TOUCH_URL']

RECORD_CONFIG = open(CONFIG_PATH).read().replace('TOUCH_URL', TOUCH_URL)

api = common.rest.Client(auth=AUTH_TOKEN, logger=logger)

custom_fields = [
    dict(
        name='snapshot_id',
        value=COMMIT
    ),
    dict(
        name='mds_upload',
        value=True
    ),
    dict(
        name='record_config',
        value=RECORD_CONFIG
    ),
    dict(
        name='s3_url',
        value='https://s3.mds.yandex.net'
    ),
    dict(
        name='spec',
        value='1837d78b0e316d5d0f01b8175ef64feadbccd041'
    )
]

# Асихронная операция - создаем задачу
task = api.task(type=TASK_TYPE, description='Создаем задачу типа %s' % TASK_TYPE, custom_fields=custom_fields, owner='MAIL')

# Асихронная операция - отправляем задачу на выполнение
api.batch.tasks.start = dict(
    comment='Запускаем задачу %s %s' % (TASK_TYPE, task['id']),
    id=[task['id']]
)

status = None

retryCount = 20

print 'Ждем sandbox таску: https://sandbox.yandex-team.ru/task/%s/' % task['id']

while True:
  status = api.task[task['id']].read()['status']

  if status in (
    set(sandbox.common.types.task.Status.Group.BREAK) | set(sandbox.common.types.task.Status.Group.FINISH)
  ):
    break

  if retryCount == 0:
    raise Exception('Задача не была завершена за 20 минут')

  print 'Status: %s' % status
  print 'Задача не завершена, ждем еще 60 секунд'

  time.sleep(60)

print status

if status in (
  set(sandbox.common.types.task.Status.Group.SCHEDULER_FAILURE) | set (sandbox.common.types.task.Status.Group.FAIL_ON_ANY_ERROR)
):
  sys.exit('Задача завершилась с ошибкой!')

print 'Задача завершена!'
