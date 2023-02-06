import os

import pytest
import retry
import yatest.common
import yt.wrapper as yt

from crypta.lib.proto.user_data import (
    enums_pb2,
    token_dict_item_pb2,
    user_data_pb2,
)
from crypta.lib.python import (
    templater,
    test_utils,
)
from crypta.lib.python.yt import proto_utils
from crypta.siberia.bin.custom_audience.fast.bin.clients import python as clients

pytest_plugins = [
    "crypta.lib.python.solomon.test_utils.fixtures",
]

INT_2_WORD = {
    0: u"air",
    1: u"earth",
    2: u"fire",
    3: u"water",
    4: u"конь",
    5: u"мама",
    6: u"мыть",
    7: u"огонь",
    8: u"рама",
    9: u"яндекс",
}

INT_2_HOST = {
    i: "{}.yandex.local".format(i)
    for i in range(10)
}

INT_2_APP = {
    i: "{}.app.com".format(i)
    for i in range(10)
}


@pytest.fixture
def proto_files():
    return {
        "word_dict": token_dict_item_pb2.TTokenDictItem,
        "host_dict": token_dict_item_pb2.TTokenDictItem,
        "app_dict": token_dict_item_pb2.TTokenDictItem,
        "user_data": user_data_pb2.TUserData,
    }


class CustomAudience(test_utils.TestBinaryContextManager):
    bin_path = "crypta/siberia/bin/custom_audience/fast/bin/service/bin/crypta-siberia-custom-audience-service"
    app_config_template_path = "crypta/siberia/bin/custom_audience/fast/docker/templates/service_config.template.yaml"

    def __init__(self, working_dir, solomon_port, yt_stuff, user_data_encoded_dir):
        super(CustomAudience, self).__init__("Siberia CustomAudience", env={"SOLOMON_TOKEN": "fake"})

        self.working_dir = working_dir
        self.solomon_port = solomon_port
        self.yt_proxy = yt_stuff.get_server()
        self.user_data_encoded_dir = user_data_encoded_dir

        self.port_manager = yatest.common.network.PortManager()
        self.host = "localhost"

    def _prepare_start(self):
        self.port = self.port_manager.get_port()

        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")
        self._render_app_config()

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.app_config_path,
        ]

    def _on_exit(self):
        self.port_manager.release()
        self.port = None

    def _render_app_config(self):
        template_params = dict({
            "yt_proxies": [self.yt_proxy],
            "user_data_encoded_dir": self.user_data_encoded_dir,
            "port": self.port,
            "solomon_schema": "http",
            "solomon_host": "localhost",
            "solomon_port": self.solomon_port,
            "logs_dir": self.working_dir,
        })
        self.logger.info("App config template parameters = %s", template_params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, template_params)

    def _wait_until_up(self):
        client = clients.CustomAudienceClient(self.host, self.port)

        @retry.retry(tries=100, delay=0.1)
        def check_is_up():
            assert client.ready().Message == "OK"

        check_is_up()


def get_user_data(crypta_id, gender, affinities_encoded_ids):
    proto = user_data_pb2.TUserData()

    proto.CryptaID = crypta_id

    proto.Attributes.Gender = gender
    proto.Attributes.Age = enums_pb2.TAge.UNKNOWN_AGE
    proto.Attributes.Income = enums_pb2.TIncome.INCOME_B2

    for keyword_id, segment_id in [
        (546, 1302),
        (547, 1058),
        (216, 549),
        (216, 555),
        (216, 616),
    ]:
        proto.Segments.Segment.add(
            Keyword=keyword_id,
            ID=segment_id,
        )

    for token, weight in [
        (416215808, 1.0),
        (819522304, 2.0),
        (1738640396, 3.0),
        (321154560, 4.0),
        (416215808, 5.0),
    ]:
        proto.AffinitiesEncoded.AffinitiveSites.add(
            Id=token,
            Weight=weight,
        )

    for token in [321154560, 1738640396, 2033647617]:
        proto.AffinitiesEncoded.TopCommonSites.add(
            Id=token,
            Weight=1.0,
        )

    proto.AffinitiesEncoded.Words.extend(affinities_encoded_ids)
    proto.AffinitiesEncoded.Hosts.extend(affinities_encoded_ids)
    proto.AffinitiesEncoded.Apps.extend(affinities_encoded_ids)

    return proto


def get_token_dict(int_2_token):
    return [
        token_dict_item_pb2.TTokenDictItem(Id=i, Token=int_2_token[i], Weight=i)
        for i in range(10)
    ]


@pytest.fixture
def user_data_encoded():
    class UserDataEncoded(object):
        user_data = [
            get_user_data(
                crypta_id="crypta_id_{}".format(x),
                gender=enums_pb2.TGender.FEMALE if x % 2 == 0 else enums_pb2.TGender.MALE,
                affinities_encoded_ids=range(x + 1),
            )
            for x in range(10)
        ]
        word_dict = get_token_dict(INT_2_WORD)
        host_dict = get_token_dict(INT_2_HOST)
        app_dict = get_token_dict(INT_2_APP)

    return UserDataEncoded


@pytest.fixture
def custom_audience(mock_solomon_server, yt_stuff, user_data_encoded_dir):
    app_working_dir = yatest.common.test_output_path("custom_audience")
    with CustomAudience(
        working_dir=app_working_dir,
        solomon_port=mock_solomon_server.port,
        yt_stuff=yt_stuff,
        user_data_encoded_dir=user_data_encoded_dir,
    ) as ca:
        yield ca


@pytest.fixture
def ca_client(custom_audience):
    return clients.CustomAudienceClient(custom_audience.host, custom_audience.port)


@pytest.fixture
def user_data_encoded_dir(yt_stuff, user_data_encoded, proto_files):
    result = "//user_data"
    client = yt_stuff.get_yt_client()

    client.create("map_node", result, force=True)

    for dict_filename in proto_files.keys():
        path = yt.ypath_join(result, dict_filename)
        proto_utils.write_proto_table(client, getattr(user_data_encoded, dict_filename), path)

    return result
