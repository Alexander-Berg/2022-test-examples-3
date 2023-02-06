from sendr_taskqueue.worker.base.entites import BaseTaskType, BaseWorkerType

# Типы Task и Worker взяты из существующих тестовых миграций postgre/migrations


class TaskType(BaseTaskType):
    MAP = 'map'
    REDUCE = 'reduce'


class WorkerType(BaseWorkerType):
    MAPPER = 'mapper'
    REDUCER = 'reducer'
