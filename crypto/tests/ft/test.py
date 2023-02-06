import mock

from crypta.graph.staff.lib import StaffTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, execute, read_resource, load_fixtures


@mock.patch("crypta.lib.python.yql_runner.task.ConfMixin.crypta_env", "test")
@mock.patch(
    "crypta.graph.staff.lib.staff_exporter.StaffLoader.fetch",
    lambda *args, **kwargs: [next(read_resource("/fixtures/staff.json", by_rows=False))],
)
@clean_up(
    observed_paths=(
        "//home",
        "//statbox",
    )
)
@load_fixtures(
    ("//home/crypta/test/state/graph/v2/soup/puid_login_passport-profile_passport-dict", "/fixtures/soup.json"),
    ("//home/crypta/test/state/graph/v2/matching/vertices_no_multi_profile_by_id_type", "/fixtures/sklejka.json"),
    ("//home/passport/production/userdata/2021-01-01", "/fixtures/passport.json"),
)
@canonize_output
def test_staff_task(local_yt, conf):
    task = StaffTask()
    execute(task)

    def select_root(root):
        def select_all(table):
            return sorted(local_yt.yt_client.read_table(table, format="json").rows)

        return {table: select_all(table) for table in local_yt.yt_client.search(root, node_type="table")}

    local_yt.yt_client.unmount_table("//home/crypta/test/ids_storage/staff/dump", sync=True)

    out = {}
    out.update(select_root("//home/crypta/test/ids_storage/staff"))
    out.update(select_root("//home/crypta/test/state/graph/profiles"))
    return out
