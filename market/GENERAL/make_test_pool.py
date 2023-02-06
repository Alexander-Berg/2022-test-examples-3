#!/usr/bin/python
# -*- coding: utf-8 -*-


import random
import datetime
import json
import yt.wrapper as yt
from utils.class_utils import set_config
from utils.class_utils import create_empty_table
from utils.class_utils import get_days_from_to
from utils.class_utils import JoinHistoryReducer
from utils.class_utils import RenameFeaturesMapper
from utils.class_utils import OrdersLogMapper
from utils.class_utils import RenameMapper
from utils.class_utils import ItemReducer
from utils.class_utils import ItemPairReducer
from utils.class_utils import JoinTargetValue
from utils.class_utils import JoinCryptaFeatures
from utils.class_utils import ComputeCEFeatures
from utils.class_utils import DuplicateColumn


class MakeAllCategsSamples(object):
    """Make negative samples for pool"""

    def __init__(self, hids_dict={}):
        self.hids_ids = set(hids_dict.keys())
        self.hids_dict = hids_dict

    def __call__(self, rec):
        restricted_hids = set([rec["target_hyper_id"]])
        allowed_hids = self.hids_ids.difference(restricted_hids)
        allowed_hids = list(allowed_hids)
        for hid in allowed_hids:
            rec["target_hyper_id"] = hid
            rec["target_hyper_id_name"] = self.hids_dict[hid]
            yield rec


def main():
    set_config()

    date_from = datetime.date(2018, 5, 15)
    date_to = datetime.date(2018, 5, 15)

    f_lines = open("learn_data/popular_hids_list", 'r').readlines()
    hids_dict = {}
    for line in f_lines:
        hids_dict[int(line.strip().split("\t")[1])] = line.strip().split("\t")[0]

    # input_tables
    sample_orders_table = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/orders"
    orders_table_prefix = "//home/market/production/mstat/robot-yt-sender/all_clicks_orders_one_day_ago/"
    idf_table = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/idf_statistics"
    watched_hids_history_table_prefix = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/hids_history/"
    crypta_profile_table_prefix = "//home/market/production/yamarec/master/features/user/crypta_profile/"
    item_statistics_table_prefix = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/item_statistics/"
    pair_item_statistics_table_prefix = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/pair_item_statistics/"
    tmp_1 = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/tmp_1"

    # output_tables
    output_table = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/test_pool/analized_pool"
    sample_table_orders = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/test_pool/orders"
    tmp_negative_sample_table = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/test_pool/negative_samples"
    fml_features_table = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/test_pool/fml/features"
    fml_features_names_table = "//home/market/development/yamarec/tamiko/Recommendation/popular_formula/test_pool/fml/factor_names"

    create_empty_table(tmp_1)
    dates = get_days_from_to(date_from, date_to)

    for date in dates:
        with yt.TempTable() as tmp_table:

            tmp_table = tmp_1
            orders_table = orders_table_prefix + str(date)
            yt.run_map(OrdersLogMapper(hids_dict), orders_table, sample_table_orders)

            orders_table = orders_table_prefix + str(date)
            crypta_profile_table = crypta_profile_table_prefix + str(date)
            item_statistics_table = item_statistics_table_prefix + str(date)
            pair_item_statistics_table = pair_item_statistics_table_prefix + str(date)
            start_date = date - datetime.timedelta(days=7)
            all_history_dates = get_days_from_to(start_date, date)
            watched_hids_history_tables = [watched_hids_history_table_prefix + str(date) for date in all_history_dates]

            yt.run_map(OrdersLogMapper(hids_dict=hids_dict,partition_size=0.05), orders_table, sample_orders_table)
            yt.concatenate(watched_hids_history_tables, tmp_table)


            # join watched categories history and orders
            reduce_keys = ["yandexuid"]
            yt.run_sort(tmp_table, output_table, sort_by=reduce_keys)
            yt.run_sort(sample_table_orders, sample_table_orders, sort_by=reduce_keys)
            yt.run_reduce(JoinHistoryReducer(), [output_table, sample_table_orders], tmp_table, reduce_by=reduce_keys)

            # make negative samples pool
            create_empty_table(tmp_negative_sample_table)
            yt.run_map(MakeAllCategsSamples(hids_dict=hids_dict), tmp_table, tmp_negative_sample_table)

            # join target
            yt.run_map(JoinTargetValue(1), tmp_table, tmp_table)
            yt.run_map(JoinTargetValue(0), tmp_negative_sample_table, tmp_negative_sample_table)
            create_empty_table(output_table)

            yt.concatenate([tmp_negative_sample_table, tmp_table], output_table)


            # join item features for target category
            reduce_keys = ["target_hyper_id"]
            yt.run_map(RenameMapper(), item_statistics_table, item_statistics_table)  # mapping for later reduce
            yt.run_sort(item_statistics_table, item_statistics_table, sort_by=reduce_keys)
            yt.run_sort(output_table, tmp_table, sort_by=reduce_keys)

            reduce_keys = ["target_hyper_id"]
            yt.run_reduce(ItemReducer(), [tmp_table, item_statistics_table], tmp_table, reduce_by=reduce_keys)
            yt.run_sort(tmp_table, tmp_1, sort_by=reduce_keys)
            # join pair features to pool
            yt.run_map(RenameMapper(3), pair_item_statistics_table,
                       pair_item_statistics_table)  # add watched hid prefix to all pair features
            yt.run_sort(tmp_table, tmp_table, sort_by="target_hyper_id")


            for i in range(1):
                reduce_keys = ["target_hyper_id", "hid_" + str(i + 1)]
                yt.run_sort(tmp_table, tmp_table, sort_by=reduce_keys)
                yt.run_sort(pair_item_statistics_table, pair_item_statistics_table, sort_by=reduce_keys)
                yt.run_reduce(ItemPairReducer(i), [tmp_table, pair_item_statistics_table], tmp_table, reduce_by=reduce_keys)

            # join user crypta features
            yt.run_map(DuplicateColumn(column_name="user_id", duplicated_column_name="yandexuid"), tmp_table, tmp_table)
            reduce_keys = ["user_id_type", "user_id"]
            yt.run_sort(tmp_table, tmp_table, sort_by=reduce_keys)
            yt.run_reduce(JoinCryptaFeatures(), [tmp_table, crypta_profile_table], tmp_table, reduce_by=reduce_keys)

            # compute ce features
            idf_info = yt.read_table(idf_table, raw=True, format="json").read()
            idf_dict = json.loads(idf_info)
            yt.run_map(ComputeCEFeatures(idf_dict), tmp_table, output_table)

            # make_final_pool
            f_lines = open("learn_data/factor_names.tsv", "r").readlines()
            features_dict = {}
            for line in f_lines:
                k, v = line.split("\t")
                features_dict[k] = v.strip()
            print features_dict.values()

            #create_empty_table(fml_features_table)
            yt.run_map(RenameFeaturesMapper(factor_names_dict=features_dict), output_table, fml_features_table,
                       job_io={"control_attributes": {"enable_row_index": True}},
                       format=yt.YsonFormat(control_attributes_mode="row_fields"), )

            yield_list = []
            for k, v in features_dict.items():
                yield_list.append({"key": k, "value": v})

            create_empty_table(fml_features_names_table)
            yt.write_table(fml_features_names_table, yield_list, raw=False)



if __name__ == "__main__":
    main()


