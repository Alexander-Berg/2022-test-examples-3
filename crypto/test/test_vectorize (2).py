from crypta.lib.nirvana.email_organization.vectorize import (
    vectorize_one, MAX_LEN, DOMAIN_DICT_SIZE)
import numpy as np

yandexru = np.zeros(DOMAIN_DICT_SIZE, dtype=np.float16)
yandexru[0] = 1.
other = np.zeros(DOMAIN_DICT_SIZE, dtype=np.float16)
other[-1] = 1.

test_cases = [
    ('dmitry.mittov@yandex.ru',
     (np.array([7, 51, 50, 11, 38, 54, 28, 51, 50, 11, 11, 23, 39,
                0, 0, 0, 0, 0, 0, 0], dtype=np.float16),
      yandexru)),
    (u'имэйлик', (np.zeros(MAX_LEN, dtype=np.float16), other))
    ]


def test_shape():
    for test_case in test_cases:
        email, answer = test_case
        login_vector, domain_vector = vectorize_one(email)
        assert login_vector.shape == (MAX_LEN,)
        assert domain_vector.shape == (DOMAIN_DICT_SIZE,)


def test_vectorize_ok():
    for test_case in test_cases:
        email, (answer_login, answer_domain) = test_case
        login_vector, domain_vector = vectorize_one(email)
        assert np.array_equal(login_vector, answer_login)
        assert np.array_equal(domain_vector, answer_domain)
