from mail.python.theatre.utils.nested_job import nested_job


def func_job():
    pass


def func_wrapper(job_f):
    job_f()


def test_nested_job_function():
    func = nested_job(func_wrapper, func_job)
    assert repr(func).startswith("<function func_wrapper --> func_job")


async def coro_job():
    pass


async def coro_wrapper(job_coro):
    await job_coro()


def test_nested_job_coroutine_function():
    coro = nested_job(coro_wrapper, coro_job)()
    try:
        assert repr(coro).startswith("<coroutine object coro_wrapper --> coro_job")
    finally:
        coro.close()
