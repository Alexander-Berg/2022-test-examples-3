from crypta.lib.python.yql import test_helpers


def add_ydb_token_to_yql(config, ydb_token):
    test_helpers.add_ydb_token_to_yql(config.YdbTokenName, ydb_token)
