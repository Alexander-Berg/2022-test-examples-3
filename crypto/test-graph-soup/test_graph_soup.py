import pytest
import allure
import json
import yatest

from datetime import datetime, timedelta


def get_file_uploads():
    geodata = yatest.common.build_path("crypta/graph/v1/tests/sandbox-data/geodata4.bin")
    url_to_groups = yatest.common.build_path("crypta/graph/v1/tests/sandbox-data/UrlToGroups.yaml")
    dt = datetime.now() - timedelta(days=1)
    return [
        (geodata, "//statbox/statbox-dict-last/geodata4.bin"),
        (url_to_groups, dt.strftime("//statbox/statbox-dict-by-name/UrlToGroups.yaml/%Y-%m-%d")),
    ]


@pytest.mark.usefixtures("graph_soup")
@pytest.mark.usefixtures("ytlocal")
@pytest.mark.usefixtures("crypta_env")
@pytest.mark.usefixtures("crypta_env_soup")
@pytest.mark.usefixtures("stream_import_dyntable")
class TestGraphSoup(object):
    def test_graph_soup_run(self, graph_soup):
        allure.attach("YT errors", (json.dumps(graph_soup.report.errors, sort_keys=True, indent=4)))
        allure.attach("Max execution time", str(graph_soup.report.max_time))
        assert not graph_soup.report.errors
        assert graph_soup.report.max_time < timedelta(minutes=10)
        if graph_soup.run_status is None:
            assert 0, "Graph run status is not defined"
        assert graph_soup.run_status, "Graph fail"

    def test_verify_soup(self, graph_soup):
        yt = graph_soup.yt
        canon_soup = dict()
        for t in yt.search("//crypta/production/state/graph/v2/soup", node_type="table"):
            canon_soup[str(t)] = sorted(list(yt.read_table(t)))

        file_path = "soup.json"
        with open(file_path, "w") as out_file:
            json.dump(canon_soup, out_file, sort_keys=True, indent=4)
        return [yatest.common.canonical_file(file_path)]
