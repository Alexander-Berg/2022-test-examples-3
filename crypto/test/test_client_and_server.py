import os
import string

from crypta.lib.python.sftp.client import SftpClient
from crypta.lib.python.sftp.testing_server import SftpTestingServer

import yatest.common


def test_base_sftp_methods():
    with SftpTestingServer() as sftp_server:
        tmp_dir = sftp_server.root_dir
        sftp_dir = "dir"
        name = "name"
        new_name = "new_name"
        downloaded_name = yatest.common.test_output_path("downloaded_file")
        path = os.path.join(sftp_dir, name)
        new_path = os.path.join(sftp_dir, new_name)

        with SftpClient(host=sftp_server.host, port=sftp_server.port, key_file=sftp_server.key_file) as sftp_client:
            sftp_client.mkdir(sftp_dir)

            sftp_client.upload(yatest.common.test_source_path("data/file.txt"), path)
            assert sftp_client.listdir(sftp_dir) == [name]

            sftp_client.rename(path, new_path)
            assert sftp_client.listdir(sftp_dir) == [new_name]

            sftp_client.download(new_path, downloaded_name)

            sftp_client.remove(new_path)
            assert not sftp_client.listdir(sftp_dir)

            sftp_client.rmdir(sftp_dir)
            assert not sftp_client.listdir()

    assert not os.path.exists(tmp_dir), "Server doesn't rm tmp dir '{}'".format(tmp_dir)

    return yatest.common.canonical_file(downloaded_name, local=True)


def test_big_file_upload_and_download():
    file_name = 'big_file'
    downloaded_file_name = 'downloaded_big_file'
    file_path = os.path.join(os.getcwd(), file_name)
    downloaded_file_path = os.path.join(os.getcwd(), downloaded_file_name)
    big_string = (string.ascii_letters + "\n") * 10**6

    with open(file_name, 'w') as f:
        f.write(big_string)

    with SftpTestingServer() as sftp_server:
        with SftpClient(host=sftp_server.host, port=sftp_server.port, key_file=sftp_server.key_file) as sftp_client:
            sftp_client.upload(file_path, file_name)
            assert sftp_client.listdir() == [file_name]

            sftp_client.download(file_name, downloaded_file_path)

            with open(downloaded_file_name, "r") as f:
                assert f.read() == big_string


def base_test():
    with SftpTestingServer() as sftp_server:
        dir_name = "dir"
        dir_new_name = "new_dir"

        with SftpClient(host=sftp_server.host, port=sftp_server.port, key_file=sftp_server.key_file) as sftp_client:
            sftp_client.mkdir(dir_name)
            sftp_client.rename(dir_name, dir_new_name)
            assert sftp_client.listdir() == [dir_new_name]

            sftp_client.rmdir(dir_new_name)
            assert not sftp_client.listdir()


def test_many_times():
    for i in range(10):
        base_test()


def test_username_password_auth():
    username = "USERNAME"
    password = "PASSWORD"
    with SftpTestingServer(auths=[(username, password)]) as sftp_server:
        with SftpClient(host=sftp_server.host, port=sftp_server.port, username=username, password=password) as sftp_client:
            assert not sftp_client.listdir()
