import argparse
import numpy as np
from extsearch.images.recommend.lib.py.batch_sample import BatchSample


def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument('--server', help='YT server', required=False, default="hahn.yt.yandex.net")
    parser.add_argument('--input-table', help='Input table with features', required=True)

    return parser.parse_args()


def get_positive_positions(batch_sample):
    result = []
    for batch in batch_sample.sample:
        for index in range(0, len(batch.items)):
            val = batch.items[index]
            if val.click:
                result.append(float(index))
    return result


def get_batch_size(batch_sample):
    return len(batch_sample.sample[0].items)


def print_percent_of_success(ratio, batch_sample):
    batch_size = get_batch_size(batch_sample)
    number_to_filter = batch_size * ratio
    percent_of_success = sum(1 for position in get_positive_positions(batch_sample) if position <= number_to_filter) * 100.0 / float(len(batch_sample.sample))
    print "percent of first {}% positions: border {}, percent of success {}% ".format(ratio * 100, number_to_filter, percent_of_success)


def main():
    args = parse_arguments()

    batch_sample = BatchSample.from_table(args.input_table, args.server)
    batch_sample.sort_batches()
    print "average_position = ", np.average(get_positive_positions(batch_sample))
    print "median_position = ", np.median(get_positive_positions(batch_sample))

    print_percent_of_success(0.1, batch_sample)
    print_percent_of_success(0.05, batch_sample)


if __name__ == "__main__":
    main()
