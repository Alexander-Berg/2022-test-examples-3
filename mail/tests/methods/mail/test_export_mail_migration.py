from typing import Final, List
from unittest.mock import NonCallableMock, Mock, MagicMock, ANY

import pytest

from tractor.mail.models import (
    UserMigrationStatus,
    MessagesCollectionStats,
    UserMigrationExportInfo,
)
from tractor.tests.fixtures.common import ORG_ID
from tractor_api.methods.mail.impl.export_migration import export_user_migrations, render_export_csv

_EXPORT_INFO: Final = [
    UserMigrationExportInfo(
        login="peter",
        status=UserMigrationStatus.PREPARING,
        mailbox_messages_collection_stats=None,
        error_reason="",
    ),
    UserMigrationExportInfo(
        login="synthia",
        status=UserMigrationStatus.SYNC_NEWEST,
        mailbox_messages_collection_stats=MessagesCollectionStats(
            source_count=100,
            collected_count=0,
            failed_count=0,
        ),
        error_reason="",
    ),
    UserMigrationExportInfo(
        login="eric",
        status=UserMigrationStatus.ERROR,
        mailbox_messages_collection_stats=MessagesCollectionStats(
            source_count=100,
            collected_count=99,
            failed_count=1,
        ),
        error_reason="Failed to collect",
    ),
]
_EXPECTED_CSV: Final = f"""login,status,error_reason,external_mailbox_messages_count,collected_messages_count,failed_to_collect_messages_count
peter,preparing,"",,,
synthia,sync_newest,"",100,0,0
eric,error,"Failed to collect",100,99,1
"""


@pytest.fixture
def db_mock():
    db = NonCallableMock()
    db.make_connection = Mock(return_value=MagicMock())
    return db


def test_export_user_migrations(db_mock: NonCallableMock):
    db_mock.export_user_migrations = Mock(return_value=_EXPORT_INFO)
    stats: List[UserMigrationExportInfo] = export_user_migrations(ORG_ID, db=db_mock)
    db_mock.export_user_migrations.assert_called_once_with(ORG_ID, cur=ANY)
    assert stats == _EXPORT_INFO


def test_render_export_csv():
    csv: str = render_export_csv(_EXPORT_INFO)
    assert csv == _EXPECTED_CSV
