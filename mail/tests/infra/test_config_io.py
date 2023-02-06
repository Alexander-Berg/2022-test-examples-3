import pytest

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.config_io import (
    read_conf, copy_conf, get_conf_path,
    normalize, denormalize
)


@pytest.mark.cluster(CLUSTERS)
def test_copy_conf(cluster, tmpdir):
    path = copy_conf(get_conf_path(cluster), tmpdir, cluster)
    assert read_conf(path)


def test_normalize():
    d = {"a": 1}
    normalize(d)
    assert d == {"a": 1}

    d = {"a": [{"_name": "foo", "x": 1}]}
    normalize(d)
    assert d == {"a": {"foo": {"_name": "foo", "x": 1}}}

    d = {"a": [{"_name": "foo", "x": 1}, {"_name": "bar", "y": 2}]}
    normalize(d)
    assert d == {"a": {"foo": {"_name": "foo", "x": 1}, "bar": {"_name": "bar", "y": 2}}}

    d = {"a": [{"_name": "foo", "x": [{"_name": "bar"}]}]}
    normalize(d)
    assert d == {"a": {"foo": {"_name": "foo", "x": {"bar": {"_name": "bar"}}}}}

    with pytest.raises(RuntimeError):
        normalize({"a": [{"_name": "foo"}, {"_name": "foo"}]})


def test_denormalize():
    d = {"a": 1}
    denormalize(d)
    assert d == {"a": 1}

    d = {"a": {"foo": {"_name": "foo", "x": 1}}}
    denormalize(d)
    assert d == {"a": [{"_name": "foo", "x": 1}]}

    d = {"a": {"foo": {"_name": "foo", "x": 1}, "bar": {"_name": "bar", "y": 2}}}
    denormalize(d)
    assert d == {"a": [{"_name": "foo", "x": 1}, {"_name": "bar", "y": 2}]}

    d = {"a": {"foo": {"_name": "foo", "x": {"bar": {"_name": "bar"}}}}}
    denormalize(d)
    assert d == {"a": [{"_name": "foo", "x": [{"_name": "bar"}]}]}
