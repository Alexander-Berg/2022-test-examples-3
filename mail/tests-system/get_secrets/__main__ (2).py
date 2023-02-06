from library.python.vault_client import instances


TESTING_SECRETS_ID = "sec-01d88td2vep432md93wyvcgxcf"


def main():
    client = instances.Production()
    secret = client.get_secret(TESTING_SECRETS_ID)
    version = client.get_version(secret["secret_versions"][0]["version"])
    for name, content in version["value"].iteritems():
        with open(name, "w") as f:
            f.write(content)


if __name__ == "__main__":
    main()
