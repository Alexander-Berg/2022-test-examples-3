# coding: utf-8

import pytest

from lib.authorization import Authorization, AuthorizationException

auth_proper_chyt = "Basic Y2h5dF9fY2hfcHVibGljOm15X3Bhc3N3b3JkX2Zvcl9jaHl0"

auth_proper_clickhouse = "Basic Y2xpY2tob3VzZV9fY3ViZXM6bXlfcGFzc3dvcmRfZm9yX2NsaWNraG91c2U="

auth_not_set_database_type = "Basic Y3ViZXM6bXlfcGFzc3dvcmRfZm9yX2NsaWNraG91c2U="  # cubes:my_password_for_clickhouse

auth_test_chyt_with_yt_fixed_cluster = "Basic Y2h5dF9fY2h5dF9tYXJrZXRfcHJvZHVjdGlvbl9faGFobjpteV9wYXNzd29yZF9mb3JfY2h5dA=="

auth_test_chyt_with_yt_fixed_cluster_error = "Basic Y2h5dF9fY2h5dF9tYXJrZXRfcHJvZHVjdGlvbl9faGFobl9fc29tZV9zaGl0Om15X3Bhc3N3b3JkX2Zvcl9jaHl0"


def test_auth_proper_chyt():
    auth = Authorization(auth_proper_chyt)

    assert auth.get_database_type() == "chyt"
    assert auth.get_username() == "ch_public"
    assert auth.get_password() == "my_password_for_chyt"
    assert auth.get_yt_fixed_cluster() is None


def test_auth_proper_clickhouse():
    auth = Authorization(auth_proper_clickhouse)

    assert auth.get_database_type() == "clickhouse"
    assert auth.get_username() == "cubes"
    assert auth.get_password() == "my_password_for_clickhouse"
    assert auth.get_yt_fixed_cluster() is None


def test_auth_not_set_database_type():
    with pytest.raises(AuthorizationException) as execinfo:
        Authorization(auth_not_set_database_type)

    assert str(execinfo.value) == "Username should be in format <database_type>__<password>"


def test_auth_test_chyt_with_yt_fixed_cluster():
    auth = Authorization(auth_test_chyt_with_yt_fixed_cluster)

    assert auth.get_database_type() == "chyt"
    assert auth.get_username() == "chyt_market_production"
    assert auth.get_password() == "my_password_for_chyt"
    assert auth.get_yt_fixed_cluster() == "hahn"


def test_auth_test_chyt_with_yt_fixed_cluster_error():
    with pytest.raises(AuthorizationException) as execinfo:
        Authorization(auth_test_chyt_with_yt_fixed_cluster_error)

    assert str(execinfo.value) == "Too many __ in your username, try <database_type>__<password>, " \
                                  "or <database_type>__<password>__<yt_cluster> formats"
