import os

import yatest.common

from crypta.lib.python.yt.test_helpers import tests
from crypta.siberia.bin.common.yt_describer.proto.yt_describer_config_pb2 import TYtDescriberConfig
import crypta.siberia.bin.common.yt_describer.py as yt_describer


def test_yt_describer(yt_stuff, input_table, output_table, id_to_crypta_id_table, crypta_id_user_data_table):
    os.environ["YT_TOKEN"] = "__FAKE_YT_TOKEN__"

    yt_client = yt_stuff.get_yt_client()
    yt_client.config["pool"] = "fake-pool"

    crypta_id_user_data = crypta_id_user_data_table(
        'crypta_id_user_data.yson',
        '//home/crypta/qa/siberia/crypta_id_user_data',
    )

    config = TYtDescriberConfig(
        IdToCryptaIdTable=id_to_crypta_id_table.cypress_path,
        CryptaIdUserDataTable=crypta_id_user_data.cypress_path,
        MaxSampleSize=2,
        TmpDir="//home/crypta/qa",
        InputTable=input_table.cypress_path,
        OutputTable=output_table.cypress_path,
    )

    def func():
        with yt_client.Transaction() as tx:
            yt_describer.describe(yt_client, tx, config)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=func,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (id_to_crypta_id_table, tests.TableIsNotChanged()),
            (crypta_id_user_data, tests.TableIsNotChanged()),
            (input_table, tests.TableIsNotChanged()),
        ],
        output_tables=[
            (output_table, [tests.Diff(), tests.SchemaEquals(yt_describer.get_output_schema())]),
        ],
        return_result=False,
    )
