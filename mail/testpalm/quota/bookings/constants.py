from dataclasses import dataclass
from datetime import datetime


@dataclass
class BookingItem:
    date: datetime
    id: int
    title: str
    status: str
    booked_quota: float
    spent_quota: float


@dataclass(frozen=True)
class Quota:
    mobilemail_android: str = 'qs_testpalm_separate_mobmail_android_ru'
    mobilemail_ios: str = 'qs_testpalm_separate_mobmail_ios_ru'


@dataclass(frozen=True)
class BookingStatus:
    closed: str = 'CLOSED'
    cancelled: str = 'CANCELLED'
    active: str = 'ACTIVE'
