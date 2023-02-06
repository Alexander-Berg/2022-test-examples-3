import hashlib
import crypta.lab.proto.matching_pb2 as Matching
import crypta.lab.proto.view_pb2 as View
import crypta.lab.proto.hashing_pb2 as Hashing

from crypta.lib.python.identifiers.identifiers import GenericID

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
idfagaid = "idfa_gaid"

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
valid_phone_md5_1 = "1" * 32
valid_phone_md5_2 = "2" * 32
invalid_puid_1 = "invalid_puid_1"
invalid_puid_2 = "invalid_puid_2"
valid_puid_1 = "123451"
valid_puid_2 = "123452"
invalid_uuid_1 = "invalid_uuid_1"
invalid_uuid_2 = "invalid_uuid_2"
valid_uuid_1 = "d" * 32
valid_uuid_2 = "e" * 32
valid_uuid_3 = "f" * 32
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

READY = View.ESampleViewState.Value("READY")

LAB_ID_UNKNOWN = Matching.ELabIdentifierType.Value("LAB_ID_UNKNOWN")
LAB_ID_YANDEXUID = Matching.ELabIdentifierType.Value("LAB_ID_YANDEXUID")
LAB_ID_IDFA_GAID = Matching.ELabIdentifierType.Value("LAB_ID_IDFA_GAID")
LAB_ID_EMAIL = Matching.ELabIdentifierType.Value("LAB_ID_EMAIL")
LAB_ID_PHONE = Matching.ELabIdentifierType.Value("LAB_ID_PHONE")
LAB_ID_LOGIN = Matching.ELabIdentifierType.Value("LAB_ID_LOGIN")
LAB_ID_MM_DEVICE_ID = Matching.ELabIdentifierType.Value("LAB_ID_MM_DEVICE_ID")
LAB_ID_PUID = Matching.ELabIdentifierType.Value("LAB_ID_PUID")
LAB_ID_UUID = Matching.ELabIdentifierType.Value("LAB_ID_UUID")
LAB_ID_CRYPTA_ID = Matching.ELabIdentifierType.Value("LAB_ID_CRYPTA_ID")

get_type = {
    LAB_ID_UNKNOWN: "unknown",
    LAB_ID_YANDEXUID: yandexuid,
    LAB_ID_IDFA_GAID: idfagaid,
    LAB_ID_EMAIL: email,
    LAB_ID_PHONE: phone,
    LAB_ID_LOGIN: login,
    LAB_ID_MM_DEVICE_ID: mm_device_id,
    LAB_ID_PUID: puid,
    LAB_ID_UUID: uuid,
    LAB_ID_CRYPTA_ID: cryptaId,
}

valid_normalized = {
    LAB_ID_YANDEXUID: (valid_yandexuid_1, valid_yandexuid_2),
    LAB_ID_IDFA_GAID: (valid_idfa_1, valid_gaid_1),
    LAB_ID_EMAIL: (valid_email_1, valid_email_2),
    LAB_ID_PHONE: (valid_phone_1, valid_phone_2),
    LAB_ID_LOGIN: (valid_login_1, valid_login_2),
    LAB_ID_MM_DEVICE_ID: (valid_mm_device_id_1, valid_mm_device_id_2),
    LAB_ID_PUID: (valid_puid_1, valid_puid_2),
    LAB_ID_UUID: (valid_uuid_1, valid_uuid_2),
    LAB_ID_CRYPTA_ID: (valid_cryptaid_1, valid_cryptaid_7),
}

for type, values in valid_normalized.items():
    for value in values:
        assert GenericID(get_type[type], value).is_valid(), "type: " + Matching.ELabIdentifierType.Name(type) + " value: " + value + " is not valid"
        assert GenericID(get_type[type], value).normalize == value, "type: " + Matching.ELabIdentifierType.Name(type) + " value: " + value + " is not normalized"

valid_unnormalized = {
    LAB_ID_YANDEXUID: (valid_yandexuid_3, valid_yandexuid_4),
    LAB_ID_IDFA_GAID: (valid_gaid_2, valid_gaid_3),
    LAB_ID_EMAIL: (valid_email_3, valid_email_4),
    LAB_ID_PHONE: (valid_phone_3, valid_phone_4),
    LAB_ID_LOGIN: (valid_login_3, valid_login_4),
    LAB_ID_MM_DEVICE_ID: (valid_mm_device_id_3, valid_mm_device_id_4),
}

for type, values in valid_unnormalized.items():
    for value in values:
        assert GenericID(get_type[type], value).is_valid(), "type: " + Matching.ELabIdentifierType.Name(type) + " value: " + value + " is not valid"
        assert GenericID(get_type[type], value).normalize != value, "type: " + Matching.ELabIdentifierType.Name(type) + " value: " + value + " must be unnormalized"

invalid = {
    LAB_ID_YANDEXUID: (invalid_yandexuid_1, invalid_yandexuid_2),
    LAB_ID_IDFA_GAID: (invalid_idfa_1, invalid_idfa_2),
    LAB_ID_EMAIL: (invalid_email_1, invalid_email_2),
    LAB_ID_PHONE: (invalid_phone_1, invalid_phone_2),
    LAB_ID_LOGIN: (invalid_login_1, invalid_login_2),
    LAB_ID_MM_DEVICE_ID: (invalid_mm_device_id_1, invalid_mm_device_id_2),
    LAB_ID_PUID: (invalid_puid_1, invalid_puid_2),
    LAB_ID_UUID: (invalid_uuid_1, invalid_uuid_2),
    LAB_ID_CRYPTA_ID: (invalid_cryptaid_1, invalid_cryptaid_2),
}

for id_type, values in invalid.items():
    for value in values:
        assert not GenericID(get_type[id_type], value).is_valid(),\
            "type: " + Matching.ELabIdentifierType.Name(id_type) + " value: " + value + " must be invalid"

valid_value = {
    LAB_ID_UNKNOWN: "UNKNOWN",
    LAB_ID_YANDEXUID: valid_yandexuid_1,
    LAB_ID_IDFA_GAID: valid_idfa_1,
    LAB_ID_EMAIL: valid_email_1,
    LAB_ID_PHONE: valid_phone_1,
    LAB_ID_LOGIN: valid_login_1,
    LAB_ID_MM_DEVICE_ID: valid_mm_device_id_1,
    LAB_ID_PUID: valid_puid_1,
    LAB_ID_UUID: valid_uuid_1,
    LAB_ID_CRYPTA_ID: valid_cryptaid_1,
}

for id_type, value in valid_value.items():
    if id_type == LAB_ID_UNKNOWN:
        continue
    assert GenericID(get_type[id_type], value).is_valid(),\
        "type: " + Matching.ELabIdentifierType.Name(id_type) + " value: " + value + " is not valid"

without_normalized = [
    LAB_ID_PUID,
    LAB_ID_UUID,
    LAB_ID_CRYPTA_ID,
]

HM_IDENTITY = Hashing.EHashingMethod.Value("HM_IDENTITY")
HM_MD5 = Hashing.EHashingMethod.Value("HM_MD5")
HM_SHA256 = Hashing.EHashingMethod.Value("HM_SHA256")

MATCHING = View.ESampleViewType.Value("MATCHING")
CRYPTA_ID_STATISTICS = View.ESampleViewType.Value("CRYPTA_ID_STATISTICS")

CROSS_DEVICE = Matching.EMatchingScope.Value("CROSS_DEVICE")
IN_DEVICE = Matching.EMatchingScope.Value("IN_DEVICE")

src_view = "src"
dst_view = "dst"
sample_id = "sample"
input_keys = ["id"]
output_keys = ["result"]

match_types = (
    LAB_ID_UNKNOWN,
    LAB_ID_YANDEXUID,
    LAB_ID_IDFA_GAID,
    LAB_ID_EMAIL,
    LAB_ID_PHONE,
    LAB_ID_LOGIN,
    LAB_ID_MM_DEVICE_ID,
    LAB_ID_PUID,
    LAB_ID_UUID,
    LAB_ID_CRYPTA_ID,
)


def make_path(hashing_method, include_original, id_type, key, view_type, scope):
    return "//home/match/{hashing_method}___{include_original}___{id_type}___{key}___{view_type}___{scope}".format(
        hashing_method=Hashing.EHashingMethod.Name(hashing_method),
        include_original="include_original_" + str(include_original),
        id_type="id_type_" + Matching.ELabIdentifierType.Name(id_type),
        key="table_key_" + str(key),
        view_type=View.ESampleViewType.Name(view_type),
        scope=Matching.EMatchingScope.Name(scope),
    )


match_input = []
for key in input_keys:
    for match_type in match_types:
        if match_type == LAB_ID_UNKNOWN:
            continue
        path = make_path(HM_IDENTITY, True, match_type, key, MATCHING, CROSS_DEVICE)
        match_input.append([HM_IDENTITY, True, match_type, key, MATCHING, CROSS_DEVICE, path])
    path = make_path(HM_MD5, True, LAB_ID_EMAIL, key, MATCHING, CROSS_DEVICE)
    match_input.append([HM_MD5, True, LAB_ID_EMAIL, key, MATCHING, CROSS_DEVICE, path])
    path = make_path(HM_MD5, True, LAB_ID_PHONE, key, MATCHING, CROSS_DEVICE)
    match_input.append([HM_MD5, True, LAB_ID_PHONE, key, MATCHING, CROSS_DEVICE, path])

match_output = []
for hashing_method in [HM_IDENTITY, HM_MD5, HM_SHA256]:
    for include_original in [True, False]:
        for id_type in match_types:
            for key in output_keys:
                for view_type in [MATCHING, CRYPTA_ID_STATISTICS]:
                    for scope in [CROSS_DEVICE, IN_DEVICE]:
                        if view_type == MATCHING and id_type == LAB_ID_UNKNOWN:
                            continue
                        if view_type == CRYPTA_ID_STATISTICS and (id_type != LAB_ID_UNKNOWN or hashing_method != HM_IDENTITY or include_original is False or scope != CROSS_DEVICE):
                            continue
                        path = make_path(hashing_method, include_original, id_type, key, view_type, scope)
                        match_output.append([hashing_method, include_original, id_type, key, view_type, scope, path])

indevice_types = [
    LAB_ID_YANDEXUID,
    LAB_ID_MM_DEVICE_ID,
    LAB_ID_EMAIL,
    LAB_ID_PHONE,
    LAB_ID_PUID,
    LAB_ID_LOGIN,
    LAB_ID_UUID,
    LAB_ID_CRYPTA_ID,
]
match_params = []
for input_params in match_input:
    for output_params in match_output:
        if input_params[2] != output_params[2]:
            if output_params[5] == IN_DEVICE and ((input_params[2] not in indevice_types) or (output_params[2] not in indevice_types)):
                continue
            match_params.append({src_view: input_params, dst_view: output_params})

match_params_crossdevice = []
for params in match_params:
    if params[dst_view][5] == CROSS_DEVICE and params[dst_view][4] == MATCHING and params[src_view][0] == HM_IDENTITY and params[dst_view][0] != HM_IDENTITY:
        match_params_crossdevice.append(params)
match_params_indevice = [params for params in match_params if params[dst_view][5] == IN_DEVICE and params[dst_view][4] == MATCHING]
match_params_statistics = [params for params in match_params if params[dst_view][4] == CRYPTA_ID_STATISTICS]

# random.shuffle(match_params_crossdevice)
# random.shuffle(match_params_indevice)
# random.shuffle(match_params_statistics)


match_params_auto = match_params_crossdevice[:2] + match_params_indevice[:2] + match_params_statistics[:2]


def desc(params):
    return "\nsrc_view: " + params[src_view][-1] + "\ndst_view: " + params[dst_view][-1]


match_params_auto_description = [desc(params) for params in match_params_auto]


def create_match_dataset(config, view_params):
    dataset = dict()
    hashing_method = 0
    id_type = 2
    key = 3
    path = 6
    vertices = config.paths.graph.vertices_no_multi_profile
    vertices_by_crypta_id = config.paths.graph.vertices_by_crypta_id
    src_path = view_params[src_view][path]

    dst_idtype = view_params[dst_view][id_type]
    valid_id = valid_value[dst_idtype]

    assert view_params[src_view][hashing_method] != HM_SHA256
    dataset[vertices_by_crypta_id] = [
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype], cryptaId: valid_cryptaid_1},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype] + "_invalid", cryptaId: valid_cryptaid_1},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype], cryptaId: valid_cryptaid_2},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype] + "_invalid", cryptaId: valid_cryptaid_2},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype], cryptaId: valid_cryptaid_3},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype] + "_invalid", cryptaId: valid_cryptaid_3},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype], cryptaId: valid_cryptaid_4},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype] + "_invalid", cryptaId: valid_cryptaid_4},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype], cryptaId: valid_cryptaid_5},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype] + "_invalid", cryptaId: valid_cryptaid_5},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype], cryptaId: valid_cryptaid_6},
        {"ccIdType": get_type[dst_idtype], "id": valid_id, "id_type": get_type[dst_idtype] + "_invalid", cryptaId: valid_cryptaid_6},
    ]

    src_idtype = view_params[src_view][id_type]
    src_key = view_params[src_view][key]
    dataset[src_path] = []
    dataset[vertices] = []

    valid_normalized_value_1, valid_normalized_value_2 = valid_normalized[src_idtype]
    if view_params[src_view][hashing_method] == HM_MD5:
        valid_normalized_value_1 = hashlib.md5(valid_normalized_value_1).hexdigest()
        valid_normalized_value_2 = hashlib.md5(valid_normalized_value_2).hexdigest()
    dataset[src_path].append({"garbage": "valid normalized", src_key: valid_normalized_value_1, "type": get_type[src_idtype]})
    if not (view_params[src_view][id_type] == LAB_ID_CRYPTA_ID and view_params[dst_view][view_type] == CRYPTA_ID_STATISTICS):
        dataset[src_path].append({"garbage": "valid normalized", src_key: valid_normalized_value_2, "type": get_type[src_idtype]})
    dataset[vertices].append({"id": valid_normalized_value_1, "id_type": get_type[src_idtype], cryptaId: valid_cryptaid_1})
    dataset[vertices].append({"id": "..invalid vaild_normalized..", "id_type": get_type[src_idtype], cryptaId: valid_cryptaid_2})

    invalid_value_1, invalid_value_2 = invalid[src_idtype]
    dataset[src_path].append({"garbage": "invalid", src_key: invalid_value_1, "type": get_type[src_idtype]})
    dataset[src_path].append({"garbage": "invalid", src_key: invalid_value_2, "type": get_type[src_idtype]})
    dataset[vertices].append({"id": invalid_value_1, "id_type": get_type[src_idtype], cryptaId: valid_cryptaid_3})
    dataset[vertices].append({"id": invalid_value_2, "id_type": get_type[src_idtype], cryptaId: valid_cryptaid_4})

    if src_idtype not in without_normalized:
        valid_unnormalized_value_1, valid_unnormalized_value_2 = valid_unnormalized[src_idtype]
        if view_params[src_view][hashing_method] == HM_MD5:
            valid_unnormalized_value_1 = hashlib.md5(GenericID(get_type[src_idtype], valid_unnormalized_value_1).normalize).hexdigest()
            valid_unnormalized_value_2 = hashlib.md5(GenericID(get_type[src_idtype], valid_unnormalized_value_2).normalize).hexdigest()
        dataset[src_path].append({"garbage": "valid_unnormalized", src_key: valid_unnormalized_value_1, "type": get_type[src_idtype]})
        dataset[src_path].append({"garbage": "valid_unnormalized", src_key: valid_unnormalized_value_2, "type": get_type[src_idtype]})
        dataset[vertices].append({"id": GenericID(get_type[src_idtype], valid_unnormalized_value_1).normalize, "id_type": get_type[src_idtype], cryptaId: valid_cryptaid_5})
        dataset[vertices].append({"id": "..invalid valid unnormalized..", "id_type": get_type[src_idtype], cryptaId: valid_cryptaid_6})

    indevice_table = config.paths.indevicebytypes.base_path
    type_in = get_type[src_idtype]
    if view_params[src_view][hashing_method] == HM_MD5:
        type_in += "_md5"
    type_out = get_type[dst_idtype]
    if type_in == cryptaId:
        type_in = "crypta_id"
    if type_out == cryptaId:
        type_out = "crypta_id"
    if type_in == "crypta_id" or type_out == "crypta_id":
        indevice_table += "/" + type_in + "/" + type_out
    else:
        indevice_table += "/" + type_in + "/direct/" + type_out
    dataset[indevice_table] = []
    dataset[indevice_table].append({"id": valid_normalized_value_1, "id_type": get_type[src_idtype], "target_id": valid_id, "target_id_type": get_type[dst_idtype]})
    if src_idtype not in without_normalized:
        valid_unnormalized_value_1, _ = valid_unnormalized[src_idtype]
        record = {"id": GenericID(get_type[src_idtype], valid_unnormalized_value_1).normalize, "id_type": get_type[src_idtype], "target_id": valid_id, "target_id_type": get_type[dst_idtype]}
        dataset[indevice_table].append(record)

    return dataset
