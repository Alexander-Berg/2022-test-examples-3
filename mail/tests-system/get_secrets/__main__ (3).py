from library.python.vault_client import instances
import sys


TESTING_SECRETS_ID = "sec-01e64854y6b1nt3f5jt250fdnj"


def main():
    client = instances.Production()
    secret = client.get_secret(TESTING_SECRETS_ID)
    version = client.get_version(secret["secret_versions"][0]["version"])
    for name in sys.argv[1:]:
        print(name + "=" + version["value"][name])


if __name__ == "__main__":
    main()
