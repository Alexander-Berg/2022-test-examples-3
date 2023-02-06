import yatest.common as yatest

PVA_BINARY = yatest.binary_path("apphost/tools/pva/pva")

PVA_CONFIG = yatest.source_path("search/daemons/ranking_middlesearch/configs/config.json")

CONFIGS = [
    "{}_{}.conf".format(ctype, conf)
    for conf, ctypes in (
        ("loop", ("production", "hamster", "heater", "itditp")),
        ("unified_agent", ("production", "hamster")))
    for ctype in ctypes
]


def test_configs():
    output_files = {}
    for conf_name in CONFIGS:
        conf_path = yatest.output_path(conf_name)
        command = [
            PVA_BINARY,
            PVA_CONFIG,
            "WEB",
            conf_name,
            conf_path,
        ]
        yatest.execute(command)
        output_files[conf_name] = yatest.canonical_file(conf_path, local=True)
    return output_files
