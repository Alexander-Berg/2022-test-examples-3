import asyncio
from datetime import datetime
from pathlib import Path

import pytest
from freezegun import freeze_time

pytestmark = [pytest.mark.asyncio]


@freeze_time('2021-10-11 13:07:54')
async def test_concurrent_logging(dir_path, client):
    channels = ['first', 'second']
    tasks = []
    for ch in channels:
        for i in range(100):
            tasks.append(client.post(ch, data=str(i)))
    await asyncio.gather(*tasks)

    for ch in channels:
        stored = (Path(dir_path) / ch / f'{datetime.utcnow().date()}.log').read_text().strip()
        stored = set(map(int, stored.split('\n')))
        assert stored == set(range(100))
