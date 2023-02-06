#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Customisation module for Celery
App configuration example:
>> from celery import Celery
>> from celery_custom import Consumer, TaskPool
>>
>> app = Celery('tasks', backend='rpc://', broker='amqp://guest@localhost//')
>> app.conf.CELERYD_CONSUMER = Consumer
>> app.conf.CELERYD_POOL = TaskPool
>> app.conf.CELERY_ACKS_LATE = True
"""
from __future__ import absolute_import

from celery.concurrency.prefork import TaskPool as _TaskPool
from celery.concurrency.asynpool import AsynPool as _AsynPool

from celery.worker.consumer import Consumer as _Consumer

from celery.bootsteps import RUN
from celery.exceptions import WorkerShutdown, WorkerTerminate, WorkerLostError
from celery.utils.log import get_logger

from celery.worker import state

from celery.worker.control import Panel

logger = get_logger(__name__)
error = logger.error
info = logger.info
debug = logger.debug

__all__ = ['TaskPool', 'Consumer']


@Panel.register
def set_pool(state, pool_size=1, **kwargs):
    delta = pool_size - state.consumer.pool._pool.target_processes
    state.consumer.pool.set_pool_size(pool_size)
    if delta:
        state.consumer._update_prefetch_count(delta)
    return {'ok': 'pool size will be set to %d' % pool_size}


@Panel.register
def active_queues(state):
    """Return information about the queues a worker consumes from."""

    source = (state.consumer.task_consumer.queues
              if state.consumer.pool._pool.is_consuming else
              state.consumer.pool._pool.suspended_queues)
    if state.consumer.task_consumer:
        return [dict(queue.as_dict(recurse=True))
                for queue in source]
    return []


class Consumer(_Consumer):
    def __init__(self, *args, **kwargs):
        super(Consumer, self).__init__(*args, **kwargs)
        self.loop = asynloop

    def add_task_queue(self, queue_name, exchange=None, exchange_type=None,
                       routing_key=None, **options):
        for queue in self.pool._pool.suspended_queues:
            if queue.name == queue_name:
                return

        super(Consumer, self).add_task_queue(queue_name, exchange, exchange_type,
                                             routing_key, **options)
        if not self.pool._pool.is_consuming:
            to_suspend = self.task_consumer.queues[:]
            self.pool._pool.suspended_queues.extend(to_suspend)
            for queue in to_suspend:
                self.task_consumer.cancel_by_queue(queue.name)

    def cancel_task_queue(self, queue_name):
        if self.pool._pool.is_consuming:
            super(Consumer, self).cancel_task_queue(queue_name)
        else:
            queues = self.pool._pool.suspended_queues
            for queue in queues:
                if queue.name == queue_name:
                    queues.remove(queue)


def asynloop(obj, connection, consumer, blueprint, hub, qos,
             heartbeat, clock, hbrate=2.0, RUN=RUN):
    """Non-blocking event loop consuming messages until connection is lost,
    or shutdown is requested."""
    update_qos = qos.update
    hbtick = connection.heartbeat_check
    errors = connection.connection_errors
    heartbeat = connection.get_heartbeat_interval()  # negotiated

    on_task_received = obj.create_task_handler()

    if heartbeat and connection.supports_heartbeats:
        hub.call_repeatedly(heartbeat / hbrate, hbtick, hbrate)

    consumer.callbacks = [on_task_received]
    consumer.consume()
    obj.on_ready()
    obj.controller.register_with_event_loop(hub)
    obj.register_with_event_loop(hub)

    # did_start_ok will verify that pool processes were able to start,
    # but this will only work the first time we start, as
    # maxtasksperchild will mess up metrics.
    if not obj.restart_count and not obj.pool.did_start_ok():
        raise WorkerLostError('Could not start worker processes')

    # FIXME: Use loop.run_forever
    # Tried and works, but no time to test properly before release.
    hub.propagate_errors = errors
    loop = hub.create_loop()

    # CUSTOMISATON BEGINS

    pool = obj.pool._pool
    queues = pool.suspended_queues
    # CUSTOMISATON ENDS

    try:
        while blueprint.state == RUN and obj.connection:

            # CUSTOMISATON BEGINS
            debug("PRCS %d -> %d %s" % (pool._processes,
                                        pool.target_processes,
                                        [w.pid for w in pool._pool]))

            # Shrink extra processes
            if ((pool.target_processes < pool._processes)
               or pool.switched_off) and pool.is_consuming:
                info("STOP CONSUMING")
                queues[:] = consumer.queues[:]
                for queue in queues:
                    consumer.cancel_by_queue(queue.name)
                pool.is_consuming = False
            elif (not pool.is_consuming
                  and not pool.switched_off
                  and pool.target_processes >= pool._processes
                  and len(pool._pool) == pool._processes):
                info("RESTART CONSUMING")
                for queue in queues:
                    consumer.add_queue(queue)
                queues[:] = []
                consumer.consume()
                pool.is_consuming = True
            # CUSTOMISATON ENDS

            # shutdown if signal handlers told us to.
            if state.should_stop:
                raise WorkerShutdown()
            elif state.should_terminate:
                raise WorkerTerminate()

            # We only update QoS when there is no more messages to read.
            # This groups together qos calls, and makes sure that remote
            # control commands will be prioritized over task messages.
            if qos.prev != qos.value:
                update_qos()

            try:
                next(loop)
            except StopIteration:
                loop = hub.create_loop()

    finally:
        try:
            hub.reset()
        except Exception as exc:
            error(
                'Error cleaning up after event loop: %r', exc, exc_info=1,
            )


class CustomAsynPool(_AsynPool):
    def __init__(self, *args, **kwargs):
        super(CustomAsynPool, self).__init__(*args, **kwargs)
        self.target_processes = self._processes
        self.switched_off = False
        self.is_consuming = True
        self.suspended_queues = []
        self.exiting_workers = set()

    def update_exiting(self):
        active_workers = set([w.pid for w in self._pool])
        exited_workers = self.exiting_workers - active_workers
        self.exiting_workers -= exited_workers

    def shrink(self, n=1):
        shrinked = 0
        self.update_exiting()
        for i, worker in enumerate(self._iterinactive()):

            self.exiting_workers.add(worker.pid)

            self._processes -= 1
            if self._putlock:
                self._putlock.shrink()
            worker.terminate_controlled()
            shrinked += 1
            self.on_shrink(1)
            if i >= n - 1:
                break
        else:
            pass

        self.target_processes = self._processes - (n - shrinked)

    def set_pool_size(self, pool_size=1):
        if pool_size < 1:
            pool_size = 1
            self.switched_off = True
        else:
            self.switched_off = False

        difference = pool_size - self.target_processes
        if difference > 0:
            self.grow(difference)
        elif difference < 0:
            self.shrink(-difference)

    def _iterinactive(self):
        for worker in self._pool:
            if not (self._worker_active(worker)
                    or worker.pid in self.exiting_workers):
                yield worker
        raise StopIteration()

    def grow(self, n=1):
        super(CustomAsynPool, self).grow(n)
        self.target_processes += n

    def on_job_ready(self, job, i, obj, inqW_fd):
        extra_processes = self._processes - self.target_processes
        if extra_processes:
            self.shrink(extra_processes)
        debug("WORKER PID %d" % self._cache[job]._worker_pid)
        super(CustomAsynPool, self).on_job_ready(job, i, obj, inqW_fd)


class TaskPool(_TaskPool):
    Pool = CustomAsynPool

    def set_pool_size(self, pool_size=1):
        self._pool.set_pool_size(pool_size)

    def _get_info(self):
        result = super(TaskPool, self)._get_info()
        result["is_consuming"] = self._pool.is_consuming
        result["target_processes"] = (0 if self._pool.switched_off
                                      else self._pool.target_processes)
        return result
