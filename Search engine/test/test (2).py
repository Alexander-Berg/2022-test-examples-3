from search.gta.utils.sbf.python import (
    ScalableBloomFilter,
)

import random
import string
import pytest


def generate_random_string():
    return ''.join(
        random.choice(string.ascii_uppercase + string.digits) for _ in range(10)
    ).encode('utf8')


@pytest.fixture(scope="function")
def random_string():
    return generate_random_string()


@pytest.fixture(scope="function")
def other_random_string():
    return generate_random_string()


def test_bloom_methods(random_string):
    sbf = ScalableBloomFilter(7, 8)
    assert not sbf.has(random_string)
    sbf.add(random_string)

    assert sbf.has(random_string)


def test_bloom_many_registers():
    sbf = ScalableBloomFilter(10, 10)
    for i in range(1000):
        sbf.add(str(i))
    assert sum([int(sbf.has(str(i))) for i in range(1000)]) == 1000

    # low false positive
    assert sum([int(sbf.has(str(i + 1000))) for i in range(1000)]) < 10


def test_bloom_many_registers_false_positive():
    sbf = ScalableBloomFilter(14, 8)
    for i in range(10000):
        sbf.add(str(i))
    assert sum([int(sbf.has(str(i))) for i in range(10000)]) == 10000

    # high false positive
    assert sum([int(sbf.has(str(i + 10000))) for i in range(10000)]) > 10


def test_bloom_sugar_methods(random_string):
    sbf = ScalableBloomFilter(7, 8)
    assert random_string not in sbf
    sbf += random_string
    assert random_string in sbf


def test_bloom_dumps_loads(random_string):
    sbf = ScalableBloomFilter(7)
    assert random_string not in sbf
    sbf.add(random_string)
    assert random_string in sbf
    loaded_bf = ScalableBloomFilter(7)
    for register in sbf.dump_registers():
        loaded_bf.load_register(register['register'])
    assert random_string in loaded_bf


def test_bloom_pop_register():
    sbf = ScalableBloomFilter(7, 8)
    for i in range(100):
        sbf.add(str(i))
    size_1 = sbf.get_register_size()
    assert size_1 > 1
    register = sbf.pop_register()
    assert register
    assert sbf.get_register_size() == size_1 - 1
    assert sum([int(sbf.has(str(i))) for i in range(100)]) < 100
    sbf.load_register(register['register'])
    assert sum([int(sbf.has(str(i))) for i in range(100)]) == 100


def test_bloom_merge(random_string, other_random_string):
    sbf_1 = ScalableBloomFilter(7, 8)
    sbf_2 = ScalableBloomFilter(7, 8)

    assert random_string not in sbf_1
    assert other_random_string not in sbf_1
    sbf_1 += random_string
    assert random_string in sbf_1
    assert other_random_string not in sbf_1

    assert random_string not in sbf_2
    assert other_random_string not in sbf_2
    sbf_2 += other_random_string
    assert random_string not in sbf_2
    assert other_random_string in sbf_2

    sbf_1.merge(sbf_2)
    assert random_string in sbf_1
    assert other_random_string in sbf_1


# def test_bench_million():
#     import time
#     import sys
#     for ex in (
#             16,
#             18,
#             20,  # 1  M
#             22,
#     ):
#         N = 2 ** ex
#         sbf = ScalableBloomFilter(ex-2)
#         for i in range(N):
#             sbf.add(str(i))
#         s = random_string()
#         t_0 = time.time()
#         s_0 = s[0]
#         for i in range(10000):
#             if s[0] == s_0:
#                 if s in sbf:
#                     pass
#         t_not_inside = (time.time() - t_0) * 100
#         sys.stderr.write("N = {: 10d} time_not_inside = {:.4f} microseconds\n".format(N, t_not_inside))
#         t_0 = time.time()
#         s_0 = '0'
#         for i in range(10000):
#             if s_0[0] == '0':
#                 if s_0 in sbf:
#                     pass
#         t_inside = (time.time() - t_0) * 100
#         sys.stderr.write("N = {: 10d} time_inside     = {:.4f} microseconds\n".format(N, t_inside))
#         sys.stderr.write("\n")
