from context import SessionContext


def test_table_mapping(session_context: SessionContext):
    session_context.check_table_mapping()
