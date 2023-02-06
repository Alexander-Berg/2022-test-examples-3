from datetime import datetime, timedelta, timezone
from typing import Optional

import pytest

from mail.ciao.ciao.interactions.calendar.entities import Event


@pytest.fixture
def create_event_entity(randn, rands):
    def _inner(start: Optional[datetime] = None,
               end: Optional[datetime] = None,
               name: Optional[str] = None,
               all_day: bool = False,
               ):
        if not start:
            start = datetime(2020, 2, 17, 10, 20, 30, tzinfo=timezone.utc)
        if not end:
            end = start + timedelta(minutes=10)
        if not name:
            name = rands()

        return Event(
            event_id=randn(),
            external_id=rands(),
            name=name,
            description=rands(),
            start_ts=start,
            end_ts=end,
            others_can_view=True,
            sequence=randn(),
            all_day=all_day,
        )

    return _inner
