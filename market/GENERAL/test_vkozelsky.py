import argparse
import sys
import json
import logging

_log_format = f"%(asctime)s - [%(levelname)s] - %(name)s - (%(filename)s).%(funcName)s(%(lineno)d) - %(message)s"


def get_logger():
    logging.basicConfig(stream=sys.stdout, level=logging.INFO, format=_log_format)

    logger = logging.getLogger()

    return logger


def main(args):
    # Construct the argument parser
    ap = argparse.ArgumentParser()

    ap.add_argument("-p1", "--param1", required=True,
                    help="Tag to filter by")

    ap.add_argument("-out1", "--output1", required=True,
                    help="Output JSON")

    args, unknown = ap.parse_known_args(args[1:])
    args = vars(args)

    logger = get_logger()

    result = []
    tag = str(args['param1'])
    processed_data = args['output1']

    result.append({'tag': tag})

    with open(processed_data, 'w') as f:
        json.dump(result, f)
        logger.info('Successfully write results')


main(sys.argv)
