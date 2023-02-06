import functools
import asyncio
from typing import Callable


def full_wait_pending(func: Callable):

    @functools.wraps(func)
    async def wrapper(*args, **kwargs):
        before = set(asyncio.all_tasks())
        result = await func(*args, **kwargs)
        after = set(asyncio.all_tasks())
        for t in after - before:
            # https://a.yandex-team.ru/arc/trunk/arcadia/library/python/pyscopg2/pyscopg2/base.py?rev=r8734204#L116
            if '_check_pool_task' in str(t):
                continue
            await asyncio.wait_for(t, timeout=None)
        return result

    return wrapper
