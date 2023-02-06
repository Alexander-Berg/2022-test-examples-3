import yatest.common


def test_printguruass():
    command = [
        yatest.common.binary_path("market/assistant/printguruass/printguruass"),
        "--filelist", yatest.common.source_path("market/assistant/printguruass/tests/tires_54469.json.gz"),
        "--format",
    ]
    out_path = "printguruass.out"
    with open(out_path, "w") as out:
        with open(yatest.common.source_path("market/assistant/printguruass/tests/requests.txt")) as std_in:
            yatest.common.execute(command, stdout=out, stdin=std_in)

    return yatest.common.canonical_file(out_path)
