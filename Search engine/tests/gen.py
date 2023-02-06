#!/usr/bin/python

import random
import sys

def make_random_tsv_line(nFeat = 100):
    qid = random.randint(1, 10 ** 5)
    relev = random.choice(["0", "0.07", "0.14", "0.41", "0.61"])
    url = "www.random-%s.com" % random.randint(1, 10 ** 10)
    group = random.randint(0, 100)

    line = '%s\t%s\t%s\t%s' % (qid, relev, url, group)

    for i in range(nFeat):
        f = round(random.randint(0, 31) / 31.0, 3)
        if f == 0.0:
            fStr = '0'
        elif f == 1.0:
            fStr = '1'
        else:
            fStr = str(f)

        line = line + '\t' + fStr

    return line

def make_pool_file(nLines, nFeat, seed, out):
    random.seed(seed)

    for i in range(nLines):
        line = make_random_tsv_line(nFeat)
        out.write(line + '\n')

if __name__ == '__main__':
    if len(sys.argv) == 1:
        nLines = 1000
        nFeat = 100
        seed = 1984
    else:
        nLines = int(sys.argv[1])
        nFeat = int(sys.argv[2])
        seed = int(sys.argv[3])

    make_pool_file(nLines, nFeat, seed, sys.stdout)
