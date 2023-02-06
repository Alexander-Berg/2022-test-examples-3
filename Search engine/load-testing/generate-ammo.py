#!./venv/bin/python

import argparse
import logging
import sys


def format_cartridge(*args):
    request_data = "\n".join(args)
    request_data += "\n\n"

    cartridge_size = len(request_data)

    return "{0}\n{1}".format(cartridge_size, request_data)


def generate_cartridge(request, headers):
    request_headers = map(lambda h: ": ".join(h), headers.iteritems())

    return format_cartridge(request, *request_headers)


def parse_args(args=None):
    parser = argparse.ArgumentParser(description="Ammo generator. Currently can generate ammo from balancer logs. "
                                                 "Works as filter: reads log lines from STDIN "
                                                 "and writes them to STDOUT",
                                     epilog="Now it uses just host and request fields from balancer logs.")

    return parser.parse_args(args)


def main(args=None):
    options = parse_args(args=args)

    for line in sys.stdin:
        line = line.strip()
        log_records = line.split('\t')

        try:
            request = log_records[2].strip('"')
            host = log_records[5].strip('"')

            print generate_cartridge(request, {"Accept-Encoding": "gzip, deflate",
                                               "Host": host,
                                               "User-Agent": "tank",
                                               "Connection": "close"})
        except IndexError as e:
            logging.error('Record parse error:\n\tLine: {0}\n\tError: {1}'.format(line, e))
            sys.exit(1)


if __name__ == '__main__':
    main()
