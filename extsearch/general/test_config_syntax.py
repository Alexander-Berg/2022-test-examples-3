import extsearch.video.ugc.service.protos.service_pb2 as service_pb2
from google.protobuf import text_format

import pytest
import yatest.common


@pytest.fixture(scope="session", params=[
    "clients_testing.cfg",
    "clients_prod.cfg",
    "clients_testing_yateam.cfg",
    "clients_prod_yateam.cfg",
])
def config_file(request):
    return request.param


def test_config_syntax(config_file):
    with open(yatest.common.source_path("extsearch/video/ugc/config/{}".format(config_file)), "r") as fd:
        clients = service_pb2.TVHUploadLinkFactoryConfig()
        text_format.Parse(fd.read(), clients)

        assert len(clients.ClientService) > 0, "no clients defined in config"


def test_config_abc_present(config_file):
    with open(yatest.common.source_path("extsearch/video/ugc/config/{}".format(config_file)), "r") as fd:
        clients = service_pb2.TVHUploadLinkFactoryConfig()
        text_format.Parse(fd.read(), clients)
        errors = {}
        for client in clients.ClientService:
            if client.HasField('BroadcastParams'):
                if client.BroadcastParams.HasField('S3Params'):
                    if not client.BroadcastParams.S3Params.ABC:
                        errors['{}'.format(client.TvmId)] = client.Name
        assert not errors, "no abc service in {} for the following: \n{}".format(config_file, errors)
