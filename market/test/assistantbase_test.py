import yatest.common


def test_assistantbase():
    config = [
        "[programs]\n",
        "root: " + yatest.common.source_path("market/assistant/assistantbase") + "\n",
        "assistant: " + yatest.common.binary_path("market/assistant/printguruass/printguruass") + "\n",
        "mboparamdumper: " + yatest.common.binary_path("market/assistant/assistantbase/mbo_parameter_dump/mbo_parameter_dump") + "\n",

        "[data]\n",
        "mbodatadir: ./\n",
        "autobase:" + yatest.common.source_path("market/assistant/assistantbase/test/market-wheels-prestable-test.xml") + "\n",

        "[output]\n",
        "dir_output: ./output\n",
        "create_subdir: False\n",
        "upload_to_sandbox: False\n",

        "[report]\n",
        "mail_to: axc@yandex-team.ru\n",
        "mail_from: axc@yandex-team.ru\n",
        "\n",
        "send_mail: False\n",
        "print_to_stdout: False\n",

        "[test]\n",
        "dir_old_result: " + yatest.common.source_path("market/assistant/assistantbase/test/assistantbase") + "/\n",
        "test_requests: " + yatest.common.source_path("market/assistant/assistantbase/test/tires_test_req_dev.txt") + "\n",
        "min_result_size: 100 * 1024\n",
        "allow_disappear: False\n",
        "allow_uniterror: False\n",

        "testing: True\n"
    ]
    config_file_path = "config_test.ini"
    with open(config_file_path, "w") as config_file:
        config_file.writelines(config)

    command = [
        yatest.common.python_path(),
        yatest.common.source_path("market/assistant/assistantbase/assistantbase.py"),
        config_file_path
    ]
    yatest.common.execute(command)

    return [
        yatest.common.canonical_file("output/assistantbase/tires_54469.json"),
        yatest.common.canonical_file("output/report.txt")
    ]
