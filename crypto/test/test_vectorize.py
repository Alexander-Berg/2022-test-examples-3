from crypta.lib.nirvana.email_gender.vectorize import (
    vectorize_one, MAX_LEN)
import numpy as np

test_cases = [
    ('dmitry.mittov@yandex.ru',
     np.array([7, 51, 50, 11, 38, 54, 28, 51, 50, 11, 11, 23, 39,
               0, 0, 0, 0, 0, 0, 0], dtype=np.float16)),
    (u'имэйлик', np.zeros(MAX_LEN, dtype=np.float16))
    ]


def test_shape():
    for test_case in test_cases:
        email, answer = test_case
        vectorize_one(email).shape == (MAX_LEN,)


def test_vectorize_ok():
    for test_case in test_cases:
        email, answer = test_case
        assert np.array_equal(vectorize_one(email), answer)
