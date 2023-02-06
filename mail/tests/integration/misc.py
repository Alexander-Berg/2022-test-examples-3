from datetime import timedelta


def add_test_task(db, uid=11111, service='barbet', task='backup_user', state='in_progress', worker='test_worker',
                  processing_date=None, task_args=None, timeout=timedelta(minutes=15), request_id='test'):
    task_id = db.query(
        "SELECT code.add_task(%(uid)s, %(task)s, %(request_id)s, %(task_args)s, %(timeout)s)",
        uid=uid,
        task=task,
        task_args=task_args,
        timeout=timeout,
        request_id=request_id,
    )[0][0]

    db.execute(
        '''
            UPDATE queue.tasks
               SET service=coalesce(%(service)s, service),
                   state=coalesce(%(state)s, state),
                   worker=coalesce(%(worker)s, worker),
                   processing_date=coalesce(%(processing_date)s, processing_date)
             WHERE task_id=%(task_id)s
        ''',
        task_id=task_id,
        service=service,
        state=state,
        worker=worker,
        processing_date=processing_date,
    )

    return task_id
