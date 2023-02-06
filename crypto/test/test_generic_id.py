from __future__ import unicode_literals
from __future__ import print_function

import sys
import pytest
import hashlib

import crypta.lib.proto.identifiers.id_type_pb2 as IdType
from crypta.lib.proto.identifiers.identifiers_pb2 import TGenericID as GenericIDProto
from crypta.lib.python.identifiers import identifiers as id_lib
from crypta.lib.python.identifiers.generic_id import GenericID, generate_protobuf_config, set_random_seed
from crypta.lib.python.identifiers import yabs
import six
from six.moves import range


@pytest.mark.parametrize(
    "id_type, id_value, normalized, raw, proto_type",
    [
        ("email", "andrey.babaev@yandex.ru", "andrey-babaev@yandex.ru", "andrey.babaev@yandex.ru", IdType.EMAIL),
        (
            "mm_device_id",
            "f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf",
            "f1d2aee4fde74e18a6124eab70dc2fcf",
            "f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf",
            IdType.MM_DEVICE_ID,
        ),
        (
            "alisa_device_id",
            "XKD2AEE4-FDE7-4E18-A612-4EAB70DC2FCF",
            "XKD2AEE4-FDE7-4E18-A612-4EAB70DC2FCF",
            "XKD2AEE4-FDE7-4E18-A612-4EAB70DC2FCF",
            IdType.ALISA_DEVICE_ID,
        ),
        (
            "mmetric_device_id",
            "f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf",
            "f1d2aee4fde74e18a6124eab70dc2fcf",
            "f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf",
            IdType.MM_DEVICE_ID,
        ),
        (
            "mmetric_device_id_hash",
            "13816675514719703430",
            "13816675514719703430",
            "13816675514719703430",
            IdType.MM_DEVICE_ID_HASH,
        ),
        (
            "devid",
            "f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf",
            "f1d2aee4fde74e18a6124eab70dc2fcf",
            "f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf",
            IdType.MM_DEVICE_ID,
        ),
        (
            "devidhash",
            "13816675514719703430",
            "13816675514719703430",
            "13816675514719703430",
            IdType.MM_DEVICE_ID_HASH,
        ),
    ],
)
def test_generic_id(id_type, id_value, normalized, raw, proto_type):
    identifier = GenericID(id_type, id_value)
    assert isinstance(id_type, six.text_type)
    assert identifier.is_valid()
    assert identifier.is_significant()
    assert identifier.normalize == normalized
    assert identifier.value == raw
    assert identifier.type == proto_type


def d(value):
    try:
        return value.encode('utf-8')
    except Exception:
        return value


def s(value):
    try:
        return value.decode('utf-8')
    except Exception:
        return value


@pytest.mark.parametrize(
    "cls, id_value, is_valid, normalized, id_type",
    [
        (id_lib.SspUserId, "", False, None, IdType.SSP_USER_ID),
        (id_lib.SspUserId, "123", False, None, IdType.SSP_USER_ID),
        (id_lib.SspUserId, "123@", False, None, IdType.SSP_USER_ID),
        (id_lib.SspUserId, "10001@", False, None, IdType.SSP_USER_ID),
        (id_lib.SspUserId, "10005@abc", True, "10005@ABC", IdType.SSP_USER_ID),
        (id_lib.SspUserId, "10007@abc", True, "10007@ABC", IdType.SSP_USER_ID),
        (id_lib.VkId, "1234", True, None, IdType.VK_ID),
        (id_lib.VkId, "", False, None, IdType.VK_ID),
        (id_lib.OkId, "1234", True, None, IdType.OK_ID),
        (id_lib.OkId, "73645sdas", False, None, IdType.OK_ID),
        (id_lib.CryptaId, "1234", True, None, IdType.CRYPTA_ID),
        (id_lib.CryptaId, "73654sdas", False, None, IdType.CRYPTA_ID),
        (id_lib.Login, "log.in@ya.ru", True, "log-in", IdType.LOGIN),
        (id_lib.Login, "login@ya@gmail.com", False, "login@ya@gmail.com", IdType.LOGIN),
        (id_lib.Mac, "00:11:22:33:ee:c1", True, "00:11:22:33:ee:c1", IdType.MAC),
        (id_lib.Mac, "00-00-00-00-00-01", True, "00:00:00:00:00:01", IdType.MAC),
        (id_lib.Mac, "00:11:22:33:ee:u1", False, "00:11:22:33:ee:u1", IdType.MAC),
        (
            id_lib.Sha256,
            "0123456789abcdefABCDEF98765432100123456789abcdefABCDEF9876543210",
            True,
            "0123456789abcdefabcdef98765432100123456789abcdefabcdef9876543210",
            IdType.SHA256,
        ),
        (
            id_lib.Sha256,
            "0123456789abcdefABCDEF987654321000123456789abcdefABCDEF98765432100",
            False,
            "0123456789abcdefABCDEF987654321000123456789abcdefABCDEF98765432100",
            IdType.SHA256,
        ),
        (id_lib.Md5, "0123456789abcdefABCDEF9876543210", True, "0123456789abcdefabcdef9876543210", IdType.MD5),
        (id_lib.Md5, "0123456789abcdefABCDEF98765432100", False, "0123456789abcdefABCDEF98765432100", IdType.MD5),
        (id_lib.Oaid, "f1d2aee4-fde7-4e18-a612-4eab70dc2fcf", True, None, IdType.OAID),
        (id_lib.Gaid, "f1d2aee4-fde7-4e18-a612-4eab70dc2fcf", True, None, IdType.GAID),
        (id_lib.Gaid, "f1d2aee4-fde7-4e18-a612-4eab70dc2Qcf", False, None, IdType.GAID),
        (id_lib.EdadealUid, "f1d2aee4-fde7-4e18-a612-4eab70dc2fcf", True, None, IdType.EDADEAL_UID),
        (id_lib.EdadealUid, "f1d2aee4-fde7-4e18-a612-4eab70dc2Qcf", False, None, IdType.EDADEAL_UID),
        (id_lib.Idfa, "F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCF", True, None, IdType.IDFA),
        (id_lib.Idfa, "F1D2AEE4-fDE7-4E18-A612-4EAB70DC2FCF-0", False, None, IdType.IDFA),
        (id_lib.Ifv, "F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCF", True, None, IdType.IFV),
        (id_lib.Ifv, "F1D2AEE4-fDE7-4E18-A612-4EAB70DC2FCF-0", False, None, IdType.IFV),
        (id_lib.Uuid, "f1d2aee4fde74e18a6124eab70dc2fcf", True, None, IdType.UUID),
        (id_lib.Uuid, "f1d2aee4fde74e18a6124eab70dc2Fcf", False, None, IdType.UUID),
        (id_lib.Puid, "12341234", True, None, IdType.PUID),
        (id_lib.Puid, "4294967df2967645", False, None, IdType.PUID),
        (id_lib.Phone, "+7(918) 393 80 34", True, "+79183938034", IdType.PHONE),
        (id_lib.Phone, "+89168751594", False, "+89168751594", IdType.PHONE),
        (id_lib.Email, "andrey@yandex.com", True, "andrey@yandex.ru", IdType.EMAIL),
        (id_lib.Email, ".@a.ru", False, ".@a.ru", IdType.EMAIL),
        (id_lib.MmDeviceId, "f1d2aee4fde74e18a6124eab70dc2fcf", True, None, IdType.MM_DEVICE_ID),
        (id_lib.MmDeviceId, "f1d2aee4-fde7-4e18-a612-4ed2-0d2fcf", False, None, IdType.MM_DEVICE_ID),
        (id_lib.MmDeviceId, "f1d2aee4fde74e18a6124ed20d2fcf", False, None, IdType.MM_DEVICE_ID),
        (id_lib.IdfaGaid, "F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCF", True, None, IdType.IDFA_GAID),
        (id_lib.IdfaGaid, "f1d2aee4-fde7-4e18-a612-4eab70dc2fcf", True, None, IdType.IDFA_GAID),
        (id_lib.IdfaGaid, "f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf", False, None, IdType.IDFA_GAID),
        (id_lib.Icookie, "10113701529442803", True, None, IdType.ICOOKIE),
        (id_lib.Icookie, "i11100188541530035229", False, None, IdType.ICOOKIE),
        (id_lib.Yandexuid, "10113701529442803", True, None, IdType.YANDEXUID),
        (id_lib.Yandexuid, "1231200188541530035229", False, None, IdType.YANDEXUID),
    ],
)
def test_identifier_x_class(cls, id_value, is_valid, normalized, id_type):
    """ Should check is identifier python implementation work correctly """
    identifier = cls(id_value)
    # check validation / normalization
    assert identifier.is_valid() == is_valid
    assert identifier.is_significant() == is_valid
    assert identifier.value == id_value
    assert identifier.normalize == (normalized or id_value)
    assert identifier.type == id_type
    if not is_valid:
        return
    assert is_valid
    if cls is not id_lib.Phone:
        assert s(identifier.md5) == s(hashlib.md5(d(identifier.normalize)).hexdigest())
        assert s(identifier.sha256) == s(hashlib.sha256(d(identifier.normalize)).hexdigest())
    assert identifier.half > 0

    # WARN: mm_device_id and idfa-gaid is shit
    if isinstance(identifier, id_lib.MmDeviceId):
        normalized = id_value.replace("-", "").lower()

    # check protobuffing
    proto = identifier.to_proto()
    assert proto.SerializeToString() == identifier.serialize()
    assert isinstance(proto, GenericIDProto)
    restored = cls(proto)
    assert restored.is_valid()
    assert restored.is_significant()
    assert restored.value == (normalized or id_value)
    assert restored.normalize == (normalized or id_value)
    assert restored.type == id_type
    assert proto == restored.to_proto()


@pytest.mark.parametrize("email", ["EmAiL@e.co", "some-email-co@e.com.com", "my-name@yadex.ru"])
def email_hashing(email):
    """ Shoul check is correct email hashing """
    identifier = id_lib.Email(email)
    assert identifier.is_valid()
    assert identifier.is_significant()
    assert s(identifier.md5) == s(hashlib.md5(identifier.normalize).hexdigest())
    assert s(identifier.sha256) == s(hashlib.sha256(identifier.normalize).hexdigest())
    assert identifier.half > 0


@pytest.mark.parametrize("phone", ["+7771234567", "+7(918) 393 80 34", "+1(123) 456 78 90"])
def phone_hashing(phone):
    """ Shoul check is correct phone hashing """
    identifier = id_lib.Phone(phone)
    assert identifier.is_valid()
    assert identifier.is_significant()
    assert s(identifier.md5) == s(hashlib.md5(identifier.normalize[1:]).hexdigest())
    assert s(identifier.sha256) == s(hashlib.sha256(identifier.normalize[1:]).hexdigest())
    assert identifier.half > 0


def to_camel_case(snake_name):
    return "".join(map(lambda word: word.title(), snake_name.split("_")))


@pytest.mark.parametrize("proto", IdType.EIdType.keys())
def test_identifier_factory(proto):
    """ Should check is IdentifierFactory correctly create clases """
    if IdType.EIdType.Value(proto) in id_lib.NOT_IMPLEMENTED_IDENTIFIERS:
        assert not hasattr(id_lib, to_camel_case(proto))
        return
    cls = getattr(id_lib, to_camel_case(proto))
    identifier = cls("some-value")
    assert identifier.ID_TYPE == IdType.EIdType.Value(proto)
    assert isinstance(identifier, cls)
    assert identifier.value == "some-value"


@pytest.mark.parametrize("cls", [getattr(id_lib, class_name) for class_name in id_lib.__all__])
def test_next(cls):
    """ Shoul check is Next method correct """
    assert issubclass(cls, GenericID)
    random_id = cls.next()
    assert random_id
    assert cls(random_id).is_valid()
    assert cls(random_id).is_significant()


@pytest.mark.parametrize(
    "cls",
    sum(
        [
            [getattr(id_lib, class_name) for class_name in id_lib.__all__],
            [
                lambda value: GenericID("yandexuid", value),
                lambda value: GenericID("email", value),
                lambda value: GenericID("phone", value),
                lambda value: GenericID("crypta_id", value),
            ],
        ],
        [],
    ),
)
def test_is_none_allowed(cls):
    """ Should check is correctly catch None as identifier param """
    identifier = cls(None)
    assert isinstance(identifier, GenericID)
    assert not identifier.is_valid()
    assert not identifier.is_significant()
    assert not identifier.normalize
    assert not identifier.value


@pytest.mark.parametrize(
    "cls, unicode_trash, is_correct",
    [
        (id_lib.Email, "строка-с-кирилицей@яндекс.рф", False),
        (id_lib.Email, "строка-с-кирилицей@yandex.ru", False),
        (id_lib.PartnerId, "строка-с-кирилицей", False),
        (id_lib.Login, "строка-с-кирилицей", False),
        (id_lib.Phone, "+7我記得個美好的時刻9151234567", False),
        (id_lib.Phone, "+7\x049\x04\x041512\x0434567", False),
        (id_lib.Phone, "+79151234567", True),
    ],
)
def test_unicode_identifiers(cls, unicode_trash, is_correct):
    """ Should check is unicode correctly validate """
    assert isinstance(unicode_trash, six.text_type)
    identifier = cls(unicode_trash)
    assert isinstance(identifier.normalize, str)
    assert identifier.is_valid() == is_correct
    assert identifier.is_significant() == is_correct
    assert identifier.normalize == six.ensure_str(unicode_trash)
    assert identifier.value == six.ensure_str(unicode_trash)


def test_all_id_lib():
    """ Should check is identifier factory correctly bind all identifiers """
    assert set(id_lib.__all__) == set(
        (
            "AlisaDeviceId",
            "AndroidId",
            "AutoId",
            "AvitoHash",
            "AvitoId",
            "CryptaId",
            "DirectClientId",
            "DistrR1",
            "DistrUi",
            "DitId",
            "Duid",
            "EdadealUid",
            "Email",
            "EmailMd5",
            "EmailSha256",
            "FbId",
            "Gaid",
            "Hostname",
            "Icookie",
            "Idfa",
            "IdfaGaid",
            "Ifv",
            "Imei",
            "KinopoiskId",
            "Login",
            "Mac",
            "MacExt",
            "MacExtMd5",
            "Md5",
            "MmDeviceId",
            "MmDeviceIdHash",
            "Oaid",
            "OkId",
            "PartnerId",
            "Phone",
            "PhoneMd5",
            "PhoneSha256",
            "Puid",
            "Sha256",
            "SspUserId",
            "Uuid",
            "VkId",
            "XuniqGuid",
            "YamoneyId",
            "Yandexuid",
            "Ysclid",
        )
    )


def test_proto_config():
    """ Should check is proto config generated ok """
    assert generate_protobuf_config()
    # print to stderr for simple copy into YQL tests
    print("\nGenericID Protobuf YQL config\n{}\n===".format(generate_protobuf_config()), file=sys.stderr)


@pytest.mark.parametrize(
    "cls, seed, identifiers",
    [
        (
            id_lib.Email,
            77,
            (
                "ldya5bm@gmail.com",
                "pu357@rambler.ru",
                "ep65c6s@gmail.com",
                "i7x8l0x@mail.ru",
                "lhfdo37g@gmail.com",
                "632yryvk@mail.ru",
                "4labt5@gmail.com",
                "reyum7r@yandex.ru",
                "hip81@yandex.ru",
                "e2zwfx0@gmail.com",
            ),
        ),
        (
            id_lib.Gaid,
            1337,
            (
                "17621314-cce2-3004-12b5-700cc325b61d",
                "8b95d3de-2271-453e-9b60-eecf81ec4d3c",
                "4e5280d2-7291-edc7-3b67-c6a7d2cb7ab3",
                "e97caa56-17e0-f06d-aaec-f33ac7c1d047",
                "44d74a55-d367-9a61-d5b0-b2d6763e1b01",
                "94410d55-f498-83eb-a11c-ee30b097c684",
                "13f46afb-c75f-cb48-d5b8-bd1e11560604",
                "368e0e4c-9c0b-278a-42a2-7d4750cb7ddf",
                "3c16960f-90b8-08b5-f9e6-a8bcae73fbb9",
                "19fee109-38a6-98f4-55f3-ccb52646faa4",
            ),
        ),
        # Yandexuid do not freeze by SEED because has timestamp in range
        # [09.07.1998, 20:00:00, NOW()], so each call with seed, has same RANDOM_PART,
        # but different TIMESTAMP_PART
    ],
)
def test_set_random_seed(cls, seed, identifiers):
    """ Should check is random seed freeze correctly """
    for step in range(3):
        set_random_seed(seed)
        for identifier in identifiers:
            assert cls.next() == identifier


@pytest.mark.parametrize(
    "convert_func, gid, result",
    [
        (
            yabs.convert_to_profile_uniq,
            GenericID("yandexuid", "123"),
            "y123"
        ),
        (
            yabs.convert_to_profile_uniq,
            GenericID("puid", "123"),
            "p123"
        ),
        (
            yabs.convert_to_profile_uniq,
            GenericID("mm_device_id", "00000000-0000-0000-0000-000000000123"),
            "mmdi/00000000000000000000000000000123"),
        (
            yabs.convert_to_profile_uniq,
            GenericID("email", "foo@example.com"),
            None),
        (
            yabs.convert_to_storage_id,
            GenericID("yandexuid", "123"),
            "y123"),
        (
            yabs.convert_to_storage_id,
            GenericID("puid", "123"),
            "p123"),
        (
            yabs.convert_to_storage_id,
            GenericID("mm_device_id", "00000000-0000-0000-0000-000000000123"),
            "mm_device_id/00000000000000000000000000000123"),
        (
            yabs.convert_to_storage_id,
            GenericID("email", "foo@example.com"),
            None),
    ]
)
def test_convert_to_functions(convert_func, gid, result):
    assert convert_func(gid.type, gid.normalize) == result
