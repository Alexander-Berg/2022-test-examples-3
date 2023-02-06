import pytest

from crypta.dmp.adobe.bin.common.python import bindings_filename


@pytest.mark.parametrize("filename,metadata", [
    ("S3_80811_20915__full_1545829759000.info", bindings_filename.Metadata(80811, "full", 1545829759000)),
    ("S3_80812_20915__iter_1545829759000.info", bindings_filename.Metadata(80812, "iter", 1545829759000)),
    ("S3_80811_20915__full_1545829759000.sync.gz", bindings_filename.Metadata(80811, "full", 1545829759000)),
    ("S3_80811_20915__full_1545829759000.sync", bindings_filename.Metadata(80811, "full", 1545829759000)),
    ("S3_80811_20915_dss_full_1545829759000.sync", bindings_filename.Metadata(80811, "full", 1545829759000)),
    ("S3_80811_20915_dss_full_1545829759000-12.sync", bindings_filename.Metadata(80811, "full", 1545829759000))
])
def test_get_bucket_status(filename, metadata):
    assert metadata == bindings_filename.parse(filename)


@pytest.mark.parametrize("filename", [
    "filename",
    "S3_80811__full_1545829759000.info",
    "S3_80811_20915_full_1545829759000.info",
    "3_80811_20915__full_1545829759000.info",
    "S3_abc_20915__full_1545829759000.info",
    "S3_80811_abc__full_1545829759000.info",
    "S3_80811_20915__unknown_1545829759000.info",
    "S3_80811_20915__full_15429759000.info",
    "S3_80811_20915__full_1545829759000.wrongextension",
    "S3_80811_20915__full_1545829759000.sync.gz.gz",
    "S3_80811_20915__full_1545829759000sinfo"
])
def test_get_bucket_status_negative(filename):
    with pytest.raises(Exception):
        bindings_filename.parse(filename)


@pytest.mark.parametrize("filename,metadata", [
    ("S3_80811_20915__full_1545829759000.info", bindings_filename.Metadata(80811, "full", 1545829759000)),
    ("S3_80812_20915__iter_1545829759000.info", bindings_filename.Metadata(80812, "iter", 1545829759000))
])
def test_get_bucket_status_info(filename, metadata):
    assert metadata == bindings_filename.parse(filename, extension_regexp=r"info")


@pytest.mark.parametrize("filename", [
    ("S3_80811_20915__full_1545829759000.sync.gz", bindings_filename.Metadata(80811, "full", 1545829759000)),
    ("S3_80811_20915__full_1545829759000.sync", bindings_filename.Metadata(80811, "full", 1545829759000)),
    ("S3_80811_20915__full_1545829759000.info2", bindings_filename.Metadata(80811, "full", 1545829759000))
])
def test_get_bucket_status_info_negative(filename):
    with pytest.raises(Exception):
        bindings_filename.parse(filename, extension_regexp=r"info")
