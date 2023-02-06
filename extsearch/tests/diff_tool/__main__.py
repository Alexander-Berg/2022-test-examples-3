import argparse
import math
import sys
import json


class CanonizedJsonComparator(object):

    def __init__(self, threshold):
        self.threshold = 1e-5 if threshold is None else threshold

    def diff_map(self, l, r, path):
        if set(l.keys()) != set(r.keys()):
            sys.stderr.write('Mismatch dict keys in {}:\n<<<\n{}\n>>>\n{}\n===\n'.format(path, json.dumps(l), json.dumps(r)))
            return False
        match = True
        for key in l.keys():
            match &= self.diff_item(l[key], r[key], '%s/%s' % (path, key))
        return match

    def diff_vec(self, l, r, path):
        if len(l) != len(r):
            sys.stderr.write('Mismatch list length in {}:\n<<<\n{}\n>>>\n{}\n===\n'.format(path, json.dumps(l), json.dumps(r)))
            return False
        match = True
        for i in range(len(l)):
            match &= self.diff_item(l[i], r[i], '%s/%d' % (path, i))
        return match

    def diff_item(self, l, r, path):
        if type(l) != type(r):
            sys.stderr.write('Mismatch of types in {}:\n<<<\n{}\n>>>\n{}\n===\n'.format(path, json.dumps(l), json.dumps(r)))
            return False
        if type(l) is dict:
            return self.diff_map(l, r, path)
        if type(l) is list:
            return self.diff_vec(l, r, path)
        if type(l) is float:
            abserr = abs(l - r)
            denom = max(abs(l), abs(r))
            relerr = abserr / denom if denom else 0.0
            minerr = min(abserr, relerr)
            if math.isnan(minerr) or minerr > self.threshold:
                sys.stderr.write('Mismatch {} with eps {}:\n<<<\n{}\n>>>\n{}\n===\n'.format(path, str(self.threshold), json.dumps(l), json.dumps(r)))
                return False
            else:
                return True
        if l != r:
            sys.stderr.write('Mismatch {}:\n<<<\n{}\n>>>\n{}\n===\n'.format(path, json.dumps(l), json.dumps(r)))
            return False
        return True

    def match(self, gold, test):
        with open(gold, 'rt') as left_file, open(test, 'rt') as right_file:
            json_gold = json.load(left_file)
            json_test = json.load(right_file)
            match = self.diff_item(json_gold, json_test, "")
            if not match:
                sys.stderr.write('The jsons are different:\n<<<\n{}\n>>>\n{}\n===\n'.format(json.dumps(json_gold), json.dumps(json_test)))
            return match
        return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--threshold", '-t', help="Error threshold", metavar='F', type=float)
    parser.add_argument("gold", help="Canonized file")
    parser.add_argument("test", help="Produced file")
    args = parser.parse_args()

    try:
        cmp = CanonizedJsonComparator(args.threshold)
        if not cmp.match(args.gold, args.test):
            return 1
        return 0
    except Exception as e:
        sys.stderr.write('diff_tool: Error: {}\n'.format(e.message))
        return 1


if __name__ == "__main__":
    sys.exit(main())
