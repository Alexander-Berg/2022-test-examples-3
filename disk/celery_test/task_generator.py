#!/usr/bin/python
import multiprocessing
import sys
import time


from tasks import mpfs_fake_task


def rate_limited(max_per_second, func):
    min_interval = 1.0 / float(max_per_second)
    last_time_called = [0.0]

    def rate_limited_func(*args, **kargs):
        elapsed = time.clock() - last_time_called[0]
        left_to_wait = min_interval - elapsed
        if left_to_wait > 0:
            time.sleep(left_to_wait)
        ret = func(*args, **kargs)
        last_time_called[0] = time.clock()
        return ret

    return rate_limited_func


def submit(args):
    def submit_task(param):
        mpfs_fake_task.apply_async((param,))

    limited = rate_limited(args['rps'], submit_task)

    for i in xrange(0, args['tasks_count']):
        limited(5)

if __name__ == '__main__':
    rps = int(sys.argv[1])
    tasks_count = int(sys.argv[2])

    thread_count = 1

    pool = multiprocessing.Pool(thread_count)
    pool.map(submit, [{'rps': float(rps) / thread_count,
                       'tasks_count': tasks_count / thread_count}] * thread_count)
