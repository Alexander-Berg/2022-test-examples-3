def test_source_path():
    from yatest.common import source_path
    test_dir = source_path("mail/python/no_yamake_tests/tests")
    print(test_dir)


def test_binary_path():
    from yatest.common import binary_path
    binary_dir = binary_path("mail/devpack/ctl/devpack")
    print(binary_dir)
