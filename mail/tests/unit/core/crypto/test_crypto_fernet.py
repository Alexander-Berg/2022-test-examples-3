from mail.ipa.ipa.core.crypto import Fernet


def test_fernet():
    fernet = Fernet()
    data = b'1' * 15 * 7
    fernet.decrypt(fernet.encrypt(b'1' * 15 * 7)) == data
