import datetime
from collections import defaultdict
import crypta.lib.proto.identifiers.id_type_pb2 as IdType
from crypta.lib.proto.identifiers import (
    ext_pb2,
)
from crypta.lib.python.identifiers import identifiers as id_lib
from crypta.lib.python.identifiers.generic_id import (
    set_random_seed,
)
from crypta.graph.soup.config.python import (  # noqa
    LOG_SOURCE as log_source,
    SOURCE_TYPE as source_type,
    Edges,
)

avito_hash = "avito_hash"
cryptaId = "cryptaId"
email = "email"
email_md5 = "email_md5"
gaid = "gaid"
idfa = "idfa"
login = "login"
mm_device_id = "mm_device_id"
phone = "phone"
phone_md5 = "phone_md5"
puid = "puid"
uuid = "uuid"
vk_id = "vk_id"
yandexuid = "yandexuid"

valid_avito_hash_1 = "a" * 32
invalid_cryptaid_1 = "invalid_cryptaid_1"
invalid_cryptaid_2 = "invalid_cryptaid_2"
valid_cryptaid_1 = "1234561438214031"
valid_cryptaid_2 = "1234561438214032"
valid_cryptaid_3 = "1234561438214033"
valid_cryptaid_4 = "1234561438214034"
valid_cryptaid_5 = "1234561438214035"
valid_cryptaid_6 = "1234561438214036"
valid_cryptaid_7 = "1234561438214037"
valid_cryptaid_8 = "1234561438214038"
invalid_email_1 = "invalid_email_1"
invalid_email_2 = "invalid_email_2"
valid_email_1 = "test@test.ru"
valid_email_2 = "test1@test.com"
valid_email_3 = "test2@ya.ru"
valid_email_4 = "test3@yandex.com"
valid_email_md5_1 = "b" * 32
invalid_gaid_1 = "invalid_gaid_1"
invalid_gaid_2 = "invalid_gaid_2"
valid_gaid_1 = "a" * 8 + "-" + "a" * 4 + "-" + "a" * 4 + "-" + "a" * 4 + "-" + "a" * 12
valid_gaid_2 = "b" * 32
valid_gaid_3 = "c" * 32
invalid_idfa_1 = "invalid_idfa_1"
invalid_idfa_2 = "invalid_idfa_2"
valid_idfa_1 = "C" * 8 + "-" + "C" * 4 + "-" + "C" * 4 + "-" + "C" * 4 + "-" + "C" * 12
invalid_login_1 = "invalid...login_1"
invalid_login_2 = "invalid...login_2"
valid_login_1 = "test"
valid_login_2 = "test1"
valid_login_3 = "test2@yandex.ru"
valid_login_4 = "test3@yandex.com"
invalid_mm_device_id_1 = "invalid_mm_device_id_1"
invalid_mm_device_id_2 = "invalid_mm_device_id_2"
valid_mm_device_id_1 = "B" * 8 + "-" + "B" * 4 + "-" + "B" * 4 + "-" + "B" * 4 + "-" + "B" * 12
valid_mm_device_id_2 = "c" * 32
valid_mm_device_id_3 = "b" * 8 + "-" + "b" * 4 + "-" + "b" * 4 + "-" + "b" * 4 + "-" + "b" * 12
valid_mm_device_id_4 = "C" * 32
invalid_phone_1 = "invalid_phone_1"
invalid_phone_2 = "invalid_phone_2"
valid_phone_1 = "+79161234561"
valid_phone_2 = "+79161234562"
valid_phone_3 = "89161234563"
valid_phone_4 = "89161234564"
valid_phone_5 = "89161234565"
valid_phone_md5_1 = "1" * 32
valid_phone_md5_2 = "2" * 32
valid_phone_md5_3 = "3" * 32
invalid_puid_1 = "invalid_puid_1"
invalid_puid_2 = "invalid_puid_2"
valid_puid_1 = "123451"
valid_puid_2 = "123452"
invalid_uuid_1 = "invalid_uuid_1"
invalid_uuid_2 = "invalid_uuid_2"
valid_uuid_1 = "d" * 32
valid_uuid_2 = "e" * 32
valid_uuid_3 = "f" * 32
valid_uuid_4 = "g" * 32
valid_vk_id_1 = "1"
valid_vk_id_2 = "2"
valid_vk_id_3 = "3"
invalid_yandexuid_1 = "invalid_yandexuid_1"
invalid_yandexuid_2 = "invalid_yandexuid_2"
valid_yandexuid_1 = "77777771438246801"
valid_yandexuid_2 = "77777771438246802"
valid_yandexuid_3 = "077777771438246803"
valid_yandexuid_4 = "077777771438246804"
valid_yandexuid_5 = "77777771438246805"
valid_yandexuid_6 = "77777771438246806"
valid_yandexuid_7 = "077777121438246804"

empty_dates = []
none_dates = None

bar = "bar"
mm = "mm"
mobile_redir_bind = "mobile-redir-bind"
passport_dict = "passport-dict"
preproc = "preproc"
sbapi_lookup = "sbapi-lookup"
wl = "wl"

app_metrica = "app-metrica"
app_metrica_socket_android = "app-metrica-socket-android"
app_metrica_socket_ios = "app-metrica-socket-ios"
app_metrica_sdk = "app-metrica-sdk"
avito = "avito"
md5 = "md5"
login_to_email = "login-to-email"
passport_profile = "passport-profile"
yabro_android = "yabro-android"
yabro_ios = "yabro-ios"


def private_date(yuid):
    assert type(yuid) == str
    assert 17 <= len(yuid) <= 20
    return [datetime.datetime.fromtimestamp(int(yuid[-10:])).strftime('%Y-%m-%d')]


def nonprivate_dates(yuid):
    assert type(yuid) == str
    assert 17 <= len(yuid) <= 20
    return [
        datetime.datetime.fromtimestamp(int(yuid[-10:])).strftime('%Y-%m-%d'),
        datetime.datetime.fromtimestamp(int(yuid[-10:]) - 60 * 60 * 24).strftime('%Y-%m-%d')
    ]


def get_id_type_name(id_type_int):
    return IdType.EIdType.DESCRIPTOR.values_by_number[id_type_int].GetOptions().Extensions[ext_pb2.Name]


def random_id_as_vertex(cls):
    return (cls.next(), get_id_type_name(cls.ID_TYPE))


def generate_test_dates_graph():
    set_random_seed(1)
    cryptaId = "123"
    uuid1 = random_id_as_vertex(id_lib.Uuid)
    mm_device_id1 = random_id_as_vertex(id_lib.MmDeviceId)
    uuid2 = random_id_as_vertex(id_lib.Uuid)
    yuid1 = random_id_as_vertex(id_lib.Yandexuid)
    email1 = random_id_as_vertex(id_lib.Email)
    email_md51 = random_id_as_vertex(id_lib.EmailMd5)
    uuid3 = random_id_as_vertex(id_lib.Uuid)

    direct_mm_device_id_uuid = (log_source.DISTR_HISTORICAL.Name, source_type.DISTR_HISTORICAL.Name)
    direct_yuid_uuid = (log_source.WATCH_LOG.Name, source_type.APP_URL_REDIR.Name)
    yuid_email = (log_source.WATCH_LOG.Name, source_type.ADV_BLOCK.Name)
    email_email_md5 = (log_source.SOUP_PREPROCESSING.Name, source_type.MD5_HASH.Name)

    edges = [
        mm_device_id1 + uuid1 + (["2018-01-01", "2019-04-26"], cryptaId) + direct_mm_device_id_uuid,
        mm_device_id1 + uuid2 + (["2018-01-12", "2018-01-28", "2018-08-14"], cryptaId) + direct_mm_device_id_uuid,
        yuid1 + uuid2 + (["2017-01-10", "2020-02-20"], cryptaId) + direct_yuid_uuid,
        yuid1 + email1 + (["2017-08-21", "2017-08-22"], cryptaId) + yuid_email,
        email1 + email_md51 + ([], cryptaId) + email_email_md5,
        mm_device_id1 + uuid3 + (["2005-01-01", "2018-06-07"], cryptaId) + direct_mm_device_id_uuid,
        yuid1 + uuid3 + (["2017-05-05", "2018-05-06"], cryptaId) + direct_yuid_uuid,
    ]

    expected_dates_list = [
        [yuid1, uuid2, ["2017-01-10", "2020-02-20"], "y1_u2"],
        [yuid1, uuid3, ["2017-05-05", "2018-06-07"], "y1_u3"],
        [yuid1, mm_device_id1, ["2017-05-05", "2018-08-14"], "y1_md1"],
        [yuid1, uuid1, ["2018-01-01", "2018-08-14"], "y1_u1"],
        [yuid1, email1, ["2017-08-21", "2017-08-22"], "y1_e1"],
        [yuid1, email_md51, ["2017-08-21", "2017-08-22"], "y1_emd51"],

        [email_md51, email1, [None, None], "emd51_e1"],
        [email_md51, yuid1, ["2017-08-21", "2017-08-22"], "emd51_y1"],
        [email_md51, uuid2, ["2017-08-21", "2017-08-22"], "emd51_u1"],
        [email_md51, uuid3, ["2017-08-21", "2017-08-22"], "emd51_u3"],
        [email_md51, mm_device_id1, ["2017-08-21", "2017-08-22"], "emd51_md1"],
        [email_md51, uuid1, [None, None], "emd51_u1"],

        [email1, email_md51, [None, None], "e1_emd5"],
        [email1, yuid1, ["2017-08-21", "2017-08-22"], "e1_y1"],
        [email1, uuid2, ["2017-08-21", "2017-08-22"], "e1_u1"],
        [email1, uuid3, ["2017-08-21", "2017-08-22"], "e1_u3"],
        [email1, mm_device_id1, ["2017-08-21", "2017-08-22"], "e1_md1"],
        [email1, uuid1, [None, None], "e1_u1"],

        [uuid3, mm_device_id1, ["2005-01-01", "2018-06-07"], "u3_md1"],
        [uuid3, uuid1, ["2018-01-01", "2018-06-07"], "u3_u1"],
        [uuid3, uuid2, ["2017-05-05", "2018-06-07"], "u3_u2"],
        [uuid3, yuid1, ["2017-05-05", "2018-06-07"], "u3_y1"],
        [uuid3, email1, ["2017-08-21", "2017-08-22"], "u3_e1"],
        [uuid3, email_md51, ["2017-08-21", "2017-08-22"], "u3_emd51"],
    ]

    expected_dates = defaultdict(dict)
    for (id, id_type), (target_id, target_id_type), (date_begin, date_end), name in expected_dates_list:
        expected_dates[(id_type, target_id_type)][(id, target_id)] = {
            "id": id,
            "id_type": id_type,
            "target_id": target_id,
            "target_id_type": target_id_type,
            "date_begin": date_begin,
            "date_end": date_end,
            "name": name
        }

    return edges, expected_dates


def generate_test_fuzzy_direct_graph():
    cryptaId = "1234"
    set_random_seed(4)

    idfa1 = random_id_as_vertex(id_lib.Idfa)
    yandexuid1 = random_id_as_vertex(id_lib.Yandexuid)
    yandexuid2 = random_id_as_vertex(id_lib.Yandexuid)
    yandexuid3 = random_id_as_vertex(id_lib.Yandexuid)
    email1 = random_id_as_vertex(id_lib.Email)

    #        f           f
    #  y1 ------ idfa1 ----- y2
    #            /  \       /
    #         i /    \  i  /
    #          /      \___/
    #         y3
    #        /
    #       /
    #      e1
    #

    fuzzy_edge = (log_source.FUZZY2_INDEVICE.Name, source_type.PROBABILISTIC2.Name)
    adv_edge = (log_source.EVENT_LOG.Name, source_type.APP_ADV.Name)
    webvisor_edge = (log_source.WEBVISOR_LOG.Name, source_type.WEBVISOR.Name)
    dates = ["2021-05-10", "2021-05-11"]

    edges = [
        idfa1 + yandexuid1 + (dates, cryptaId) + fuzzy_edge,  # fuzzy
        idfa1 + yandexuid2 + (dates, cryptaId) + fuzzy_edge,  # fuzzy
        yandexuid2 + idfa1 + (dates, cryptaId) + adv_edge,    # direct
        yandexuid3 + idfa1 + (dates, cryptaId) + adv_edge,    # direct
        yandexuid3 + email1 + (dates, cryptaId) + webvisor_edge,
    ]

    cryptaId = (cryptaId, "crypta_id")
    expected_fuzzy_pairs = [
        (yandexuid1, cryptaId),
        (yandexuid1, idfa1),

        (yandexuid1, yandexuid2),
        (yandexuid1, yandexuid3),
    ]
    expected_direct_pairs = [
        (idfa1, yandexuid2),
        (idfa1, yandexuid3),
        (yandexuid2, cryptaId),
        (yandexuid3, cryptaId),
        (idfa1, cryptaId),

        (yandexuid3, yandexuid2),
    ]

    union_with_reverted_edges = lambda edges: set(edges).union(map(lambda (a, b): (b, a), edges))

    return edges, union_with_reverted_edges(expected_fuzzy_pairs), union_with_reverted_edges(expected_direct_pairs)


dataset = (
    (valid_vk_id_2, vk_id, valid_email_1, email, empty_dates, valid_cryptaid_1, mm, app_metrica_sdk),

    (valid_email_1, email, valid_avito_hash_1, avito_hash, empty_dates, valid_cryptaid_1, preproc, avito),
    (valid_email_1, email, valid_email_md5_1, email_md5, empty_dates, valid_cryptaid_1, preproc, md5),
    (valid_email_1, email, valid_login_1, login, empty_dates, valid_cryptaid_1, preproc, login_to_email),
    (valid_login_1, login, valid_puid_1, puid, empty_dates, valid_cryptaid_1, passport_dict, passport_profile),

    (valid_vk_id_1, vk_id, valid_puid_1, puid, empty_dates, valid_cryptaid_1, passport_dict, md5),
    (valid_vk_id_1, vk_id, valid_gaid_1, gaid, empty_dates, valid_cryptaid_1, mm, yabro_android),

    (valid_gaid_1, gaid, valid_mm_device_id_1, mm_device_id, empty_dates, valid_cryptaid_1, mm, app_metrica),
    (valid_idfa_1, idfa, valid_mm_device_id_1, mm_device_id, empty_dates, valid_cryptaid_1, mm, app_metrica),
    (valid_mm_device_id_1, mm_device_id, valid_uuid_1, uuid, empty_dates, valid_cryptaid_1, mm, app_metrica),
    (valid_yandexuid_1, yandexuid, valid_idfa_1, idfa, nonprivate_dates(valid_yandexuid_1), valid_cryptaid_1, wl,
     app_metrica_socket_ios),
    (valid_yandexuid_1, yandexuid, valid_gaid_2, gaid, nonprivate_dates(valid_yandexuid_1), valid_cryptaid_1, wl,
     app_metrica_socket_android),
    (valid_yandexuid_1, yandexuid, valid_uuid_1, uuid, empty_dates, valid_cryptaid_1, mobile_redir_bind,
     app_metrica_sdk),
    (valid_yandexuid_2, yandexuid, valid_uuid_1, uuid, nonprivate_dates(valid_yandexuid_2), valid_cryptaid_1, wl,
     app_metrica_socket_android),
    (valid_yandexuid_2, yandexuid, valid_uuid_2, uuid, nonprivate_dates(valid_yandexuid_2), valid_cryptaid_1, wl,
     app_metrica_socket_ios),
    (valid_yandexuid_3, yandexuid, valid_uuid_2, uuid, private_date(valid_yandexuid_3), valid_cryptaid_1, bar,
     yabro_android),
    (valid_yandexuid_3, yandexuid, valid_uuid_3, uuid, none_dates, valid_cryptaid_1, sbapi_lookup, yabro_ios),

    (valid_yandexuid_4, yandexuid, valid_uuid_3, uuid, private_date(valid_yandexuid_4), valid_cryptaid_1, bar, md5),
    (valid_yandexuid_4, yandexuid, valid_phone_1, phone, private_date(valid_yandexuid_4), valid_cryptaid_1, preproc,
     app_metrica),

    (valid_phone_1, phone, valid_phone_md5_1, phone_md5, none_dates, valid_cryptaid_1, preproc, md5),
    (valid_phone_1, phone, valid_yandexuid_5, yandexuid, private_date(valid_yandexuid_1), valid_cryptaid_1, preproc,
     md5),
    (valid_yandexuid_5, yandexuid, valid_phone_2, phone, private_date(valid_yandexuid_1), valid_cryptaid_1, bar,
     app_metrica),
    (valid_phone_2, phone, valid_phone_md5_2, phone_md5, empty_dates, valid_cryptaid_1, preproc, md5),
    (valid_phone_md5_2, phone_md5, valid_yandexuid_6, yandexuid, empty_dates, valid_cryptaid_1, bar, yabro_ios),
    (valid_yandexuid_6, yandexuid, valid_phone_3, phone, empty_dates, valid_cryptaid_1, mm, md5),

    (valid_vk_id_3, vk_id, valid_yandexuid_6, yandexuid, private_date(valid_yandexuid_6), valid_cryptaid_1, preproc,
     yabro_ios),

    (valid_phone_5, phone, valid_phone_md5_3, phone_md5, none_dates, valid_cryptaid_8, preproc, md5),
    (valid_phone_5, phone,  valid_yandexuid_6, yandexuid, ["2019-08-01", "2019-08-02", "2019-08-03"], valid_cryptaid_8, preproc, md5),
    (valid_yandexuid_6, yandexuid, valid_uuid_4, uuid, ["2019-08-02", "2019-08-02", "2019-08-12"], valid_cryptaid_8, wl, app_metrica_socket_ios),

) + tuple(generate_test_dates_graph()[0]) + tuple(generate_test_fuzzy_direct_graph()[0])
