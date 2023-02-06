from crypta.lib.python.nirvana.nirvana_helpers.nirvana_transaction import NirvanaTransaction


def test_nirvana_transaction(local_yt, nirvana_operation_environ):
    yt_client = local_yt.get_yt_client()
    with NirvanaTransaction(yt_client) as transaction:
        assert transaction.transaction_id is not None
