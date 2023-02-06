import argparse
import collections
import filecmp
import os
import sys


def parse_docs(filename):
    result = collections.defaultdict(list)

    with open(filename) as input_file:
        current_word = ""

        for line in input_file.readlines():
            line = line.strip()

            if not line:
                continue
            elif line[0] == '[' and line[-1] == ']':
                assert current_word
                result[current_word].append(line)
            else:
                current_word = line

    return result


def diff(original, new, head):
    print "--- {}".format(os.path.realpath(original))
    print "+++ {}".format(os.path.realpath(new))
    print "********"

    if filecmp.cmp(original, new):
        return 0

    exit_code = 0

    original_docs = parse_docs(original)
    new_docs = parse_docs(new)

    original_keys = frozenset(original_docs.keys())
    new_keys = frozenset(new_docs.keys())

    if len(original_keys) != len(new_keys):
        print "- TERM COUNT: {}".format(len(original_keys))
        print "+ TERM COUNT: {}".format(len(new_keys))
        head -= 1
        exit_code = 1

    all_keys = sorted(list(original_keys | new_keys))

    for key in all_keys:
        if key not in original_keys:
            print "+ TERM: '{}'".format(key)
            print "+     {}".format(new_docs[key])
            head -= 1
            exit_code = 1

        elif key not in new_keys:
            print "- TERM: '{}'".format(key)
            print "-     {}".format(original_docs[key])
            head -= 1
            exit_code = 1

        elif original_docs[key] != new_docs[key]:
            print "TERM: '{}'".format(key)
            print "-     {}".format(original_docs[key])
            print "+     {}".format(new_docs[key])
            head -= 1
            exit_code = 1

        if head <= 0:
            break

    return exit_code


def main(argv):
    parser = argparse.ArgumentParser(description="diff tool for idx_print output")

    parser.add_argument("--head", type=int, default=1000, help="Output lines limit")
    parser.add_argument("original", help="Previous file version")
    parser.add_argument("new", help="Current file version")

    args = parser.parse_args()

    return diff(args.original, args.new, args.head)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
