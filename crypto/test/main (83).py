import pytest
import yatest.common

from crypta.siberia.bin.common import test_helpers


BINARY = yatest.common.binary_path("crypta/siberia/bin/expirator/bin/crypta-siberia-expirator")


def test_zero_exit_code(local_ydb, mock_siberia_core_server, config_path, env, to_delete_1, to_delete_2, to_delete_3, to_keep_1, to_keep_2, to_keep_3):
    data = [to_delete_1, to_delete_2, to_delete_3, to_keep_1, to_keep_2, to_keep_3]
    test_helpers.upload_user_sets_table(local_ydb, [test_helpers.generate_user_set_db_row(**item) for item in data])

    yatest.common.execute([BINARY, "--config", config_path], env=env)

    return sorted(mock_siberia_core_server.commands, key=lambda x: x["user_set_id"])


def test_non_zero_exit_code(local_ydb, mock_siberia_core_server, config_path, env, to_delete_1, to_delete_not_found, to_keep_1, to_keep_2, to_keep_3):
    data = [to_delete_1, to_delete_not_found, to_keep_1, to_keep_2, to_keep_3]
    test_helpers.upload_user_sets_table(local_ydb, [test_helpers.generate_user_set_db_row(**item) for item in data])

    with pytest.raises(yatest.common.ExecutionError):
        yatest.common.execute([BINARY, "--config", config_path], env=env)

    return sorted(mock_siberia_core_server.commands, key=lambda x: x["user_set_id"])
