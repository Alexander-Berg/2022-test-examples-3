import mock

from crypta.graph.vavilov.lib import VavilovTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, execute, load_fixtures


@mock.patch("crypta.lib.python.yql_runner.task.ConfMixin.crypta_env", "test")
@clean_up(observed_paths=("//home",))
@load_fixtures(
    ("//home/0", "/fixtures/sklejka.0.json"),
    ("//home/1", "/fixtures/sklejka.1.json"),
    ("//home/bigb/production/public/profiles/PublicProfilesDump_2022-03-08T00:00:36+03:00", "/fixtures/bb.json"),
)
@canonize_output
def test_vavilov_task(local_yt, conf):
    def select_all(table):
        return sorted(local_yt.yt_client.read_table(table, format="json").rows)

    def select_root(root):
        return {table: select_all(table) for table in local_yt.yt_client.search(root, node_type="table")}

    result = {}

    for idx, dt in enumerate(("2021-12-20", "2021-12-21")):

        vertices_by_id = "//home/crypta/test/state/graph/v2/matching/vertices_no_multi_profile"
        vertices_by_type = "//home/crypta/test/state/graph/v2/matching/vertices_no_multi_profile_by_id_type"

        local_yt.yt_client.copy("//home/{}".format(idx), vertices_by_id, force=True, recursive=True)
        local_yt.yt_client.set("{}/@generate_date".format(vertices_by_id), dt)
        local_yt.yt_client.link(vertices_by_id, vertices_by_type, force=True, recursive=True)

        execute(VavilovTask(enabled_ids="yandexuid,email_md5,phone_md5,"))

        result[dt] = select_root("//home/crypta/test/state/graph")

    return result
