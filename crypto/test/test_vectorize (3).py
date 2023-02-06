from crypta.lib.nirvana.login_gender.vectorize import (
    vectorize_one, MAX_LEN)
import numpy as np

test_cases = [
    ('dmitry.mittov',
     np.array([7, 51, 50, 11, 38, 54, 28, 51, 50, 11, 11, 23, 39,
               0, 0, 0, 0, 0, 0, 0], dtype=np.float16)),
    (u'логинчик', np.zeros(MAX_LEN, dtype=np.float16))
    ]


def test_shape():
    for test_case in test_cases:
        login, answer = test_case
        vectorize_one(login).shape == (MAX_LEN,)


def test_vectorize_ok():
    for test_case in test_cases:
        login, answer = test_case
        assert np.array_equal(vectorize_one(login), answer)
