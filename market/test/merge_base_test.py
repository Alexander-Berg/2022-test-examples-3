import yatest.common


def test_auto_dump():
    command = [
        yatest.common.python_path(),
        yatest.common.source_path("market/assistant/assistantbase/auto_base/merge_bases.py"),
        "--autobase", yatest.common.source_path("market/assistant/assistantbase/test/autobase.json"),
        "--parambase", yatest.common.source_path("market/assistant/assistantbase/test/paramdata.json"),
        "--config", yatest.common.source_path("market/assistant/assistantbase/auto_base/merge_config.json")
    ]
    out_path = "tires_54469.json"
    yatest.common.execute(command)

    return yatest.common.canonical_file(out_path)
