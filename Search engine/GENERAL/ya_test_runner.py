from yatest import common


def test():
    """Starts all tests via python3.6"""

    print("exec python3.6")
    common.canonical_execute(
        "python3.6",
        ["-m", "pytest", "tests"],
        cwd=common.test_source_path()
    )
