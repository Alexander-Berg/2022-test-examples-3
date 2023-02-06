import pytest

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.config import Conf
from mail.nwsmtp.tests.lib.confdict import ConfDict
from mail.nwsmtp.tests.lib.config_io import read_conf, get_conf_path, copy_conf


@pytest.mark.cluster(CLUSTERS)
def test_prepare_config_dir_and_init_conf(cluster, tmpdir):
    path = copy_conf(get_conf_path(cluster), tmpdir, cluster)
    obj = read_conf(path)

    conf = Conf(obj, path)
    assert conf.modules


@pytest.mark.mxbackout
def test_iter_values_yields_only_leaves(cluster, tmpdir):
    path = copy_conf(get_conf_path(cluster), tmpdir, cluster)
    obj = read_conf(path)

    conf = Conf(obj, path)
    for v in conf.values():
        assert not isinstance(v, (dict, ConfDict))
