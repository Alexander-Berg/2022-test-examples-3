X_TOKEN_CLIENT_ID = "629b670f45cc4b5d8946b16fa664700b"
X_TOKEN_CLIENT_SECRET = "f21d0dcb2a604c35ad91fb5610f9fdac"

AUTH_TOKEN_CLIENT_ID = "b095a5052d3d4db5acd467f06d4bf110"
AUTH_TOKEN_CLIENT_SECRET = "4323d07576a84489976522f0cb05ddc0"

DEVICE_ID = "xeno-tests-device"
DEVICE_ID_2 = "xeno-tests-device-2"
UUID = "6d6120697365742075752074a6469"  # echo "i am test uuid" | hexdump -e '"%x"'

AUTH_TYPE_OAUTH = "oauth"
AUTH_TYPE_PASSWORD = "pass"
AUTH_TYPE_PASSWORD_EX = "pass_ex"

IMAP_SSL_PORT = 993
IMAP_NO_SSL_PORT = 143
SMTP_SSL_PORT = 465
SMTP_NO_SSL_PORT = 25

TEST_USERS = {
    "common_mailru_user": {
        "login": "roman.romanovich.1945@inbox.ru",
        "password": "asd123qwe",
        "auth_type": AUTH_TYPE_OAUTH,
        "application": "mailru-o2-mail",
        "external_uid": 13943282432452240300,
        "refresh_token": "69df19042f10745b8da65d5e14b1f4b49a92735237363830",
        "imap_host": "imap.mail.ru",
        "smtp_host": "smtp.mail.ru",
        "drafts_path": "&BCcENQRABD0EPgQyBDgEOgQ4-",
        "sent_path": "&BB4EQgQ,BEAEMAQyBDsENQQ9BD0ESwQ1-",
        "need_authorize": True,
        "need_clear": True,
    },
    "not_owned_user": {
        "login": "sergei.sergeevich.1999@bk.ru",
        "password": "asd123qwe",
        "auth_type": AUTH_TYPE_OAUTH,
        "application": "mailru-o2-mail",
        "external_uid": 5176083493526407040,
        "refresh_token": "5d1e57d00f167ddaa2e9b917fd1f613cb6b6565037363830",
        "imap_host": "imap.mail.ru",
        "drafts_path": "&BCcENQRABD0EPgQyBDgEOgQ4-",
        "sent_path": "&BB4EQgQ,BEAEMAQyBDsENQQ9BD0ESwQ1-",
        "need_authorize": True,
        "need_clear": False,
    },
    "common_gmail_user": {
        "login": "roman.romanovich.1945@gmail.com",
        "password": "poptest02",
        "auth_type": AUTH_TYPE_OAUTH,
        "application": "google-oauth2",
        "external_uid": 108245632938009281311,
        "refresh_token": "1/Nv6BZbVaSnfZUv8M7MmNOdM3JiP-Gu4hCEZJgUGFRKgqBQh0y4lG46rrYKnV46e1",
        "imap_host": "imap.gmail.com",
        "need_authorize": True,
        "need_clear": False,
    },
    "common_outlook_user": {
        "login": "roman.romanovich.1945@outlook.com",
        "password": "poptest02",
        "auth_type": AUTH_TYPE_OAUTH,
        "application": "microsoft",
        "external_uid": "93ad8be1d4352dddbd675235fe55bf85",
        "refresh_token": "M.R3_BAY.-CZHZGqOf4Nw1xm4KjoXsA9VJTxhlkcZCDm!4R7nLcqAcDZmk7TRrASKh"
        "ceuy4kDBZNXxlqX6uaU1*hhJmXhvYIz!*lzFsh4Re8PTQF9mI!o3tnFYbxhK1a6JVwPN!IYLAtxzAyeaQbN"
        "1aRkekrmVSNv!8euIAuQ0YbH9afYR9Cz4vD7fID1GfiPYFCA50Y!kR!WAmihnN0LnUfwzx56IQApDaHydxo"
        "ftnXICSQRndG*zDVvOa2WJaAdETCt3v2kUAmSfi*euPAARBPCbQ5MfeuCowdkALWpz1l42bpF2FV92YS0pX"
        "!hsgy5ISkFBXYrkbw$$",
        "imap_host": "imap-mail.outlook.com",
        "need_authorize": True,
        "need_clear": False,
    },
    "common_yandex_user": {
        "login": "roman.romanovich.1945@yandex.ru",
        "password": "poptest02",
        "auth_type": AUTH_TYPE_PASSWORD,
        "need_authorize": False,
        "need_clear": False,
    },
    "common_yandex_pdd_user": {
        # https://wiki.yandex-team.ru/rtec/accounts/#konnektvtestinge
        "login": "pdd-test@xeno.rtec-domain.ru",
        "password": "simple12345",
        "auth_type": AUTH_TYPE_PASSWORD,
        "need_authorize": False,
        "need_clear": False,
    },
    "common_zoho_user": {
        "login": "devops.devopsovich@zoho.com",
        "password": "poptest02",
        "auth_type": AUTH_TYPE_PASSWORD_EX,
        "imap_host": "imap.zoho.com",
        "imap_port": IMAP_SSL_PORT,
        "imap_ssl": "yes",
        "smtp_host": "smtp.zoho.com",
        "smtp_port": SMTP_SSL_PORT,
        "smtp_ssl": "yes",
        "drafts_path": "&BCcENQRABD0EPgQyBDgEOgQ4-",
        "sent_path": "&BB4EQgQ,BEAEMAQyBDsENQQ9BD0ESwQ1-",
        "need_authorize": True,
        "need_clear": False,
    },
    "mailru_user_with_missed_folder": {
        "login": "igorigorvich@inbox.ru",
        "password": "asd123qwe1123asd",
        "auth_type": AUTH_TYPE_OAUTH,
        "application": "mailru-o2-mail",
        "external_uid": 13156913202757274808,
        "refresh_token": "1d4b9f424f3d7b2690ae9d3fd1852c010b85e8bf37363830",
        "imap_host": "imap.mail.ru",
        "smtp_host": "smtp.mail.ru",
        "need_authorize": True,
        "need_clear": False,
    },
}
