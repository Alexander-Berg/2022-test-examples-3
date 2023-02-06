import yatest.common


def test_binary_help(relative_path):
    absolute_path = yatest.common.binary_path(relative_path)

    res = yatest.common.execute(command=[absolute_path, "--help"], check_exit_code=False, wait=True, timeout=60)

    assert 0 == res.exit_code, "Exit code: {}\nstdout:\n{}\nstderr:{}".format(res.exit_code, res.stdout, res.stderr)
    assert not res.stderr, "Non-empty stderr:\n{}".format(res.stderr)
