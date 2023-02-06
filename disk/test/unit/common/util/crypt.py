#!/usr/bin/python
# -*- coding: utf-8 -*-
from unittest import TestCase

from mpfs.common.util.crypt import CryptAgent, AesCbcCryptAgent


class CryptTestCase(TestCase):
    secret_data = 'This is top secret!'

    def test_crypt_decrypt(self):
        crypt_agent = CryptAgent()

        hash_ = crypt_agent.encrypt(self.secret_data)
        self.assertEqual(self.secret_data, crypt_agent.decrypt(hash_))

        new_agent = CryptAgent(key='bbecbf998afa0f0efebadd1fdf4a553b', bsize=32)
        hash_ = new_agent.encrypt(self.secret_data)
        self.assertEqual(self.secret_data, new_agent.decrypt(hash_))


class CbcCryptAgentTestCase(TestCase):
    plaintext = 'This is top secret!'

    def test_encrypt_decrypt(self):
        agent = AesCbcCryptAgent('de453a0bbc8c451d8fb580f20c777e66')
        ciphertext = agent.encrypt(self.plaintext)
        self.assertEqual(self.plaintext, agent.decrypt(ciphertext))

    def test_encrypt_decrypt_urlsafe(self):
        agent = AesCbcCryptAgent('de453a0bbc8c451d8fb580f20c777e66', urlsafe=True)
        ciphertext = agent.encrypt(self.plaintext)
        self.assertEqual(self.plaintext, agent.decrypt(ciphertext))
