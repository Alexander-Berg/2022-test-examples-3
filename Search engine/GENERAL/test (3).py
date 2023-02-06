import yatest.common as yatest

VERTICAL = "VERTICAL"
PVA_BINARY = yatest.binary_path("apphost/tools/pva/pva")

CONFIG_PATH = yatest.source_path("search/daemons/noapacheupper/instancectl_conf/config.json")

INSTANCECTL_CONFIGS = [
    "instancectl_production_fast_data_pushclient.conf",
    "instancectl_production_fast_data.conf",
    "instancectl_production.conf",
    "instancectl_hamster_fast_data_pushclient.conf",
    "instancectl_hamster_fast_data.conf",
    "instancectl_hamster.conf",
]

PUSH_CLIENT_CONFIGS = [
    "push-client.conf.tmpl",
    "push-client_hamster.conf.tmpl",
]

UNIFIED_AGENT_CONFIGS = [
    "unified_agent.conf.tmpl",
]


def test_instancectl():
    output_files = {}
    for instancectl_conf in INSTANCECTL_CONFIGS:
        instancectl_conf_path = yatest.output_path(instancectl_conf)
        command = [
            PVA_BINARY,
            CONFIG_PATH,
            VERTICAL,
            instancectl_conf,
            instancectl_conf_path,
        ]
        yatest.execute(command)
        output_files[instancectl_conf] = yatest.canonical_file(instancectl_conf_path, local=True)
    return output_files


def test_push_client():
    output_files = {}
    for push_client_conf in PUSH_CLIENT_CONFIGS:
        push_client_conf_path = yatest.output_path(push_client_conf)
        command = [
            PVA_BINARY,
            CONFIG_PATH,
            VERTICAL,
            push_client_conf,
            push_client_conf_path,
        ]
        yatest.execute(command)
        output_files[push_client_conf] = yatest.canonical_file(push_client_conf_path, local=True)
    return output_files


def test_unified_agent():
    output_files = {}
    for unified_agent_conf in UNIFIED_AGENT_CONFIGS:
        unified_agent_conf_path = yatest.output_path(unified_agent_conf)
        command = [
            PVA_BINARY,
            CONFIG_PATH,
            VERTICAL,
            unified_agent_conf,
            unified_agent_conf_path,
        ]
        yatest.execute(command)
        output_files[unified_agent_conf] = yatest.canonical_file(unified_agent_conf_path, local=True)
    return output_files
