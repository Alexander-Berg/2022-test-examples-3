import yatest.common

from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def test_uslugi_occupations(local_yt, local_yt_and_yql_env, config_file, config):
    diff = tests.Diff()
    test = tests.TestNodesInMapNode(tests_getter=[diff], tag="uslugi_contractors")

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/profile/services/precalculate_tables/bin/crypta-profile-precalculate-tables"),
        args=[
            "--config", config_file,
            "uslugi_contractors",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("occupations.yson", config.UslugiContractorOccupationsTable), tests.TableIsNotChanged()),
            (tables.YsonTable("puids.yson", config.UslugiContractorPuidsTable), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (cypress.CypressNode(config.UslugiContractorsDir), [test]),
        ],
        env=local_yt_and_yql_env,
    )
