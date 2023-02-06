import yatest.common


def test_auto_dump():
    command = [
        yatest.common.python_path(),
        yatest.common.source_path("market/assistant/assistantbase/auto_base/autobase_reader.py"),
        "--autobase", yatest.common.source_path("market/assistant/assistantbase/test/market-wheels-prestable-test.xml"),
    ]
    out_path = "auto_dump.out"
    with open(out_path, "w") as out:
        yatest.common.execute(command, stdout=out)

    return yatest.common.canonical_file(out_path)
