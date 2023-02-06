from datetime import timedelta


def add_test_task(db, uid, task='backup_user', task_args=None, timeout=timedelta(minutes=15), request_id='test'):
    return db.code.add_task(
        i_uid=uid,
        i_task=task,
        i_task_args=task_args,
        i_timeout=timeout,
        i_request_id=request_id
    )


def change_task_state(db, task_id, state=None, worker=None, processing_date=None):
    if state:
        db.queue.tasks.update({'state': state}, task_id=task_id)
    if worker:
        db.queue.tasks.update({'worker': worker}, task_id=task_id)
    if processing_date:
        db.queue.tasks.update({'processing_date': processing_date}, task_id=task_id)
