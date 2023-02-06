from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.synchronizers import (
    ProcessingSynchronizer,
)


@pytest.fixture
def direct_synchronizer():
    class DirectSynchronizerMock(ProcessingSynchronizer):
        # noinspection PyMissingConstructor
        def __init__(self):
            self.process_data = AsyncMock()
            self.initialize = AsyncMock()

    return DirectSynchronizerMock()
