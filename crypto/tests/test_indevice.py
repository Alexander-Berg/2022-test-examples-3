import pytest
import mock

from crypta.graph.soupy_indevice.lib import (
    build_indevice,
    soup_tables_from_dir,
)
from crypta.graph.soupy_indevice.lib.util import setup_logging


@mock.patch.dict("os.environ", {"ENV_TYPE": "DEVELOP"})
@pytest.mark.usefixtures("indevice_soup", "idstorage", "indevice_conf")
class TestIndevice(object):

    @pytest.mark.parametrize("collapse_uuids", [True, False])
    def test_indevice(self, yt_stuff, collapse_uuids):
        """ Checks if indevice is built correctly """

        setup_logging()

        ytc = yt_stuff.get_yt_client()
        build_indevice(
            soup_tables_from_dir("//soup", fallback_date="0000-00-00")[0],
            "//idstorage",
            "//indevice",
            "//indevice-sizes",
            "//indevice-bad-edges",
            "//indevice-bad-ids",
            workdir="//workdir",
            collapse_uuids=collapse_uuids
        )

        result_tables = [
            "//indevice",
            "//indevice-sizes",
            "//indevice-bad-edges",
            "//indevice-bad-ids",
        ]

        result_tables += ytc.list("//workdir", absolute=True)

        result_data = {
            t: sorted(list(ytc.read_table(t))) for t in result_tables
        }

        return result_data
