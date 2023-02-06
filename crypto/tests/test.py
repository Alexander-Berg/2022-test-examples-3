import pytest
from crypta.graph.acl.matching_acl import MatchingAcl


@pytest.mark.parametrize("src_id", ("idfa", "gaid", "oaid", "ifv"))
def test_acl_basic(src_id):
    acl = MatchingAcl()

    assert acl.get_pair_acl(src_id, "yandexuid") == [
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read", "write", "remove", "administer"],
            "subjects": ["crypta-robots"],
        },
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read"],
            "subjects": ["crypta-matching-basic", "crypta-matching-all"],
        },
    ]


def test_acl():
    acl = MatchingAcl()

    assert acl.get_pair_acl("fb_id", "icookie") == [
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read", "write", "remove", "administer"],
            "subjects": ["crypta-robots"],
        },
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read"],
            "subjects": ["crypta-matching-social-private", "crypta-matching-social", "crypta-matching-all"],
        },
    ]


def test_id_to_hash_acl():
    acl = MatchingAcl()
    assert acl.get_pair_acl("email", "email_md5") == [
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read", "write", "remove", "administer"],
            "subjects": ["crypta-robots"],
        },
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read"],
            "subjects": ["crypta-matching-email-phone", "crypta-matching-email", "crypta-matching-all"],
        },
    ]

    assert acl.get_pair_acl("email_md5", "email_sha256") == [
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read", "write", "remove", "administer"],
            "subjects": ["crypta-robots"],
        },
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read"],
            "subjects": [
                "crypta-matching-email-phone",
                "crypta-matching-email",
                "crypta-matching-email-hash",
                "crypta-matching-all",
            ],
        },
    ]


def test_cross_personal():
    acl = MatchingAcl()
    assert acl.get_pair_acl("phone_sha256", "email_md5") == [
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read", "write", "remove", "administer"],
            "subjects": ["crypta-robots"],
        },
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read"],
            "subjects": ["crypta-matching-email-phone", "crypta-matching-all"],
        },
    ]


def test_staff_personal():
    acl = MatchingAcl()
    assert acl.get_pair_acl("crypta_id", "staff") == [
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read", "write", "remove", "administer"],
            "subjects": ["crypta-robots"],
        },
        {
            "action": "allow",
            "inheritance_mode": "object_and_descendants",
            "permissions": ["read"],
            "subjects": ["crypta-matching-yandex-internal", "crypta-matching-all"],
        },
    ]
