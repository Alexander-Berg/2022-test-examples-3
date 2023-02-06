import yatest.common


def make_test(input_path):
    config = """
        SERVANT_NAME\tbuker

        COLLECTIONS_DIR\t{collections_dir}

        RUNTIME_COLLECTIONS spe_lists,key-list,pages,spe_items;spe_items,key-data,filtered-pages.keydata

        LOG\tDAEMON_LOG

        FIXED_TIME\t20151019_1414
        FIXED_RANDOM_SEED\t1
        """.format(collections_dir=yatest.common.source_path("market/buker/bin/offline_buker/test/data"))

    config_file_path = "printbuker.cfg"
    with open(config_file_path, "w") as config_file:
        config_file.write(config)

    command = [
        yatest.common.binary_path("market/buker/bin/offline_buker/offline_buker"),
        "-c", config_file_path, input_path
    ]

    out_path = "buker_print.out"
    with open(out_path, "w") as out:
        with open(input_path) as input:
            yatest.common.execute(command, stdout=out, stdin=input)

    return yatest.common.canonical_file(out_path)


def make_json_reqs(input_file, output_file):
    with open(output_file, "w") as out:
        with open(input_file) as input:
            for r in input:
                out.write(r.rstrip() + "&format=json\n")


def test_buker_xml():
    input_path = yatest.common.source_path("market/buker/bin/offline_buker/test/buker_test_req.txt")
    return make_test(input_path)


def test_buker_json():
    input_path = yatest.common.source_path("market/buker/bin/offline_buker/test/buker_test_req.txt")
    json_file = "buker_test_json_req.txt"
    make_json_reqs(input_path, json_file)
    return make_test(json_file)
