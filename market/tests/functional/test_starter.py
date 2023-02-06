import sys

import yatest.common

STARTER_BIN = yatest.common.binary_path("market/sre/tools/starter/starter")


def test_version():
    res = yatest.common.execute([STARTER_BIN, "-version"])

    assert res.exit_code == 0
    assert res.std_err == ""


def test_app_name_is_missed():
    res = yatest.common.execute([STARTER_BIN, "-dry-run"], check_exit_code=False)

    sys.stderr.write("stderr: %s\n" % res.std_err)

    assert res.exit_code == 1
