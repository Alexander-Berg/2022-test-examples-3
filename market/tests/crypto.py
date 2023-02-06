import itertools as it

from sandbox import common


class TestEncryption(object):
    def test__data_extraction(self):
        data = common.utils.random_string(27)
        aes = common.crypto.AES()

        for use_salt, use_base64 in it.product((False, True), (False, True)):
            enc = aes.encrypt(data, use_base64, use_salt)
            assert aes.decrypt(enc, use_base64) == data,\
                "Fail with use_salt={}, use_base64={}".format(use_salt, use_base64)
