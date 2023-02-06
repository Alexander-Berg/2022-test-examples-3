import os
import pytest

from yql_utils import yql_binary_path


def conf_di():
    import crypta.lib.python.bt.conf.conf as conf
    import crypta.lib.python.bt.conf.resource_conf as resource_conf
    import crypta.graph.data_import.stream.proto.config_pb2 as proto_config

    conf.use(resource_conf.find("/crypta/graph/data_import/stream"))
    return proto_config.TStreamConfig()


def conf_hh():
    import crypta.graph.households.proto.config_pb2 as proto_config

    return proto_config.THouseholdsConfig()


def conf_vav():
    import crypta.graph.vavilov.proto.config_pb2 as proto_config

    return proto_config.TVavilovConfig()


def conf_fp():
    import crypta.graph.fingerprint.proto.config_pb2 as proto_config

    return proto_config.TFingerprintConfig()


def conf_fpc():
    import crypta.lib.python.bt.conf.conf as conf
    import crypta.lib.python.bt.conf.resource_conf as resource_conf
    import crypta.graph.fpc.proto.config_pb2 as proto_config

    conf.use(resource_conf.find("/crypta/graph/fpc"))
    return proto_config.TCryptaFpcConfig()


@pytest.fixture(scope="session")
def conf():
    import crypta.lib.python.bt.conf.conf as conf

    for get_proto in [conf_di, conf_hh, conf_fpc, conf_fp, conf_vav]:
        try:
            proto = get_proto()
        except ImportError:
            continue
        else:
            break
    else:
        raise RuntimeError("Error creating proto config")

    proto.Yt.Token = "Fake"
    proto.Yt.Pool = "fake-pool"

    proto.YqlEmbedded.IsEmbedded = True
    proto.YqlEmbedded.MrjobBin = yql_binary_path("yql/tools/mrjob/mrjob")
    proto.YqlEmbedded.UdfResolverBin = yql_binary_path("yql/tools/udf_resolver/udf_resolver")
    dirs = [yql_binary_path("yql/udfs")]

    try:
        dirs.append(yql_binary_path("ydb/library/yql/udfs"))
    except Exception:
        pass

    proto.YqlEmbedded.UdfsDir = ";".join(dirs)
    proto.YqlEmbedded.LogLevel = "INFO"

    conf.use_proto(proto)
    return conf


@pytest.yield_fixture(scope="session")
def local_yt(request, conf):
    from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

    os.environ["YT_USE_SINGLE_TABLET"] = "1"
    yt = YtStuff(config=YtConfig(wait_tablet_cell_initialization=True, proxy_port=9013))
    yt.start_local_yt()

    url = "localhost:{port}".format(port=yt.yt_proxy_port)

    conf.yt._I_know_what_I_do_set("proxy", dict(url=url, name="local_yt"))
    conf.proto.Yt.Proxy = url

    try:
        yield yt
    finally:
        try:
            yt.stop_local_yt()
        except OSError:
            # sometimes local yt stop with errors
            pass
