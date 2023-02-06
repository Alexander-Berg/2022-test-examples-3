# -*- coding: utf-8 -*-
from enum import IntEnum


class OperationState(IntEnum):
    WAITING = 0
    EXECUTING = 1
    FAILED = 2
    DONE = 3
    ABORTED = 4
    REJECTED = 5
    COMPLETED = 6
