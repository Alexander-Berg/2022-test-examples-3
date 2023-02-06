#! /usr/bin/python

import argparse
from clear import clear_local

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--uids", nargs="+", required=True)
    args = parser.parse_args()
    for uid in args.uids:
        print("clear uid {}".format(uid))
        clear_local(uid)
