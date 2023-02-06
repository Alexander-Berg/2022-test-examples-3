from base64 import b64encode

from tractor.crypto import VersionedKeyStorage, Fernet

TASK_ID = "test_task_id"
ORG_ID = "test_org_id"
DOMAIN = "test_domain"
LOGIN = "test_login"
EMAIL = "{}@{}".format(LOGIN, DOMAIN)
EXTERNAL_SECRET = "test_external_secret"
UID = "test_uid"
KEY_VERSIONS = {"1": b64encode("tractor___secret".encode()).decode()}
GOOGLE_PROVIDER = "google"
FERNET = Fernet(VersionedKeyStorage(KEY_VERSIONS))
