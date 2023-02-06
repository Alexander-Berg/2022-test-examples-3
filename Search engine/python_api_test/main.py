import os
import sys


def main():
    import search.plutonium.impl.python_api.yt_dynamic_tables as plutonium_api  # noqa
    import search.plutonium.impl.file_system.yt_dynamic_tables.protos.config_pb2 as config_pb2
    import search.plutonium.cpp_api.protos.states_pb2 as states_pb2
    import yt.wrapper as yt

    fs_config = config_pb2.TFileSystemConfigProto()
    fs_config.ContentTablePath = "//home/saas2/robot-saas2-worker/testing/dsp_creative/runtime_fs/content"
    fs_config.MetaTablePath = "//home/saas2/robot-saas2-worker/testing/dsp_creative/runtime_fs/meta"
    x = plutonium_api.make_compound_source(
        os.getenv("YT_PROXY"),
        yt._get_token(),  # noqa
        fs_config,
        "0",
        sys.argv[1],
        "shard",
    )
    print('--make_compound_source--\n{}\n--make_compound_source--'.format(x))

    # _ = plutonium_api.destroy_clients(os.getenv("YT_PROXY"), yt._get_token())
    # print('--destruct_client--\n{}\n--destruct_client--'.format(_))

    y = plutonium_api.get_public_states(
        os.getenv("YT_PROXY"),
        yt._get_token(),  # noqa
        "//home/saas2/robot-saas2-worker/testing/dsp_creative/public_states",
        "0",
    )
    print('--get_public_states--\n{}\n--get_public_states--'.format(y))

    # _ = plutonium_api.destroy_clients(os.getenv("YT_PROXY"))
    # print('--destruct_client--\n{}\n--destruct_client--'.format(_))

    remove_states = [
        states_pb2.TPublicState(),
    ]
    remove_states[0].Stream = "0"
    remove_states[0].SnapshotId = "x"

    z = plutonium_api.remove_public_states(
        os.getenv("YT_PROXY"),
        yt._get_token(),  # noqa
        "//home/saas2/robot-saas2-worker/testing/dsp_creative/public_states",
        remove_states,
    )
    print('--remove_states--\n{}\n--remove_states--'.format(z))

    # _ = plutonium_api.destroy_clients()
    # print('--destruct_client--\n{}\n--destruct_client--'.format(_))
