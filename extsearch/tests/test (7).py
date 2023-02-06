import yatest.common


binary = "extsearch/geo/tools/mms_version/mms_version"


def test_all():
    return yatest.common.canonical_execute(yatest.common.binary_path(binary), save_locally=True)
