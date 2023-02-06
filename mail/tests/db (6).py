import sqlalchemy as sa

from sendr_aiopg import StorageAnnotatedMeta, StorageBase, StorageContextBase
from sendr_taskqueue.worker.storage.db.entities import Task, Worker
from sendr_taskqueue.worker.storage.db.mappers.task import get_task_mapper
from sendr_taskqueue.worker.storage.db.mappers.worker import get_worker_mapper

from .entities import TaskType, WorkerType

metadata = sa.MetaData(schema='sendr_qtools')

TaskMapper = get_task_mapper(metadata, TaskType, Task)

WorkerMapper = get_worker_mapper(metadata, WorkerType, Worker)


class Storage(StorageBase, metaclass=StorageAnnotatedMeta):
    task: TaskMapper
    worker: WorkerMapper


class StorageContext(StorageContextBase):
    STORAGE_CLS = Storage
