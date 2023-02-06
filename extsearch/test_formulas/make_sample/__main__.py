import argparse
import yt.wrapper as yt
import random
from extsearch.images.recommend.lib.py.batch_sample import BatchSample
from extsearch.images.recommend.lib.py.table_row import ImageTestBatch, ImageTestItem


def add_random_map(row):
    item = ImageTestItem.from_row(row)
    item.random = random.random()
    yield item.make_row()


def read_first_rows_to_lists(table, first_list_count, second_list_count):
    first_list = []
    second_list = []
    row_count = 0
    for row in yt.read_table(table):
        test_item = ImageTestItem.from_row(row)
        if row_count < first_list_count:
            first_list.append(test_item)
        elif row_count < first_list_count + second_list_count:
            second_list.append(test_item)
        else:
            break

        row_count += 1

    return first_list, second_list


def make_statistics_batches(true_set_iterator, false_set, batch_size):
    result = []
    batch_id = 0
    false_doc_id = 0

    false_set_iterator = iter(false_set)

    for row in true_set_iterator:
        row.batch_id = batch_id
        rows = [row]
        for i in range(false_doc_id, false_doc_id + batch_size):
            try:
                false_set_row = next(false_set_iterator)
            except StopIteration:
                raise ValueError("Too small input false_set_iterator. %s %s" % (false_doc_id, i))
            false_row = row.copy()
            false_row.doc_features_i2t = false_set_row.doc_features_i2t
            false_row.doc_features_t2t = false_set_row.doc_features_t2t
            false_row.query = false_set_row.query
            false_row.document_id = false_set_row.document_id
            false_row.profile_HeuristicSegments = false_set_row.profile_HeuristicSegments
            false_row.profile_ProbabilisticSegments = false_set_row.profile_ProbabilisticSegments
            false_row.profile_LongtermInterests = false_set_row.profile_LongtermInterests
            false_row.click = False

            rows.append(false_row)

        batch = ImageTestBatch()
        batch.batch_id = batch_id
        batch.items = rows

        result.append(batch)

        false_doc_id += batch_size
        batch_id += 1

    return result


def init_yt(args):
    yt.config["proxy"]["url"] = args.server
    yt.config["pool"] = args.pool
    yt.config['pickling']['module_filter'] = lambda module: 'hashlib' not in getattr(module, '__name__', '')


def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument('--server', help='YT server', required=False, default="arnold.yt.yandex.net")
    parser.add_argument('--input-table', help='Input table with features', required=True)
    parser.add_argument('--output-table', help='Output file with batches', required=True)
    parser.add_argument('--rel-set-count', help='', required=False, type=int, default=2000)
    parser.add_argument('--random-irrel-proportion', help='Random irrel count for one rel', required=False, type=int, default=20)
    parser.add_argument('--pool', help='Yt pool', required=False, default='images_production')

    return parser.parse_args()


def main():
    args = parse_arguments()
    init_yt(args)

    with yt.TempTable(path="//tmp", attributes=ImageTestItem.get_schema()) as random_sorted_table:
        print "add random generated number"
        yt.run_map(add_random_map, args.input_table, random_sorted_table)

        print "sort by random generated number"
        yt.run_sort(random_sorted_table, sort_by="random")

        rel_user_doc_pairs, random_user_doc_pairs = read_first_rows_to_lists(random_sorted_table, args.rel_set_count,
                                                                             args.rel_set_count * args.random_irrel_proportion)
        batches = make_statistics_batches(rel_user_doc_pairs, random_user_doc_pairs, args.random_irrel_proportion)

        batch_sample = BatchSample()
        batch_sample.sample = batches
        batch_sample.to_table(args.output_table, args.server)


if __name__ == "__main__":
    main()
