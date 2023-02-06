import allure
import re

YUID_WITH_ALL = "//crypta/production/state/graph/dicts/yuid_with_all"
FP_ALL_LIST = ["//crypta/production/storage/storage/fp/2016-04-09",
               "//crypta/production/storage/storage/fp/2016-04-10",
               "//crypta/production/storage/storage/fp/2016-04-11"]
PUID_LOGIN = "//crypta/production/state/graph/2016-04-11/passport/puid_login"


def check_column_in_table(yt, path, column_name, percent):
    table = list(yt.read_table(path, raw=False))
    filtred_by_param = filter(lambda record: column_name in record and record[column_name], table)
    result = 100 * float(len(filtred_by_param)) / float(len(table))
    allure.attach("Column '{0}' in table '{1}'".format(column_name, path.split("/")[-1]),
                  "actual:{0}; expected:{1}".format(str(result), str(percent)))
    assert int(result) >= int(percent)


def get_precent_yuidfp_in_table(yt, table_path):
    """
    We count how many yuid in table. Result Percentage
    """
    table = get_all_tables(yt, FP_ALL_LIST)
    yuids_count_in_all_fp = len(list(set(get_param_from_str("yandexuid", record["key"]) for record in table)))
    yuid_row_count = len(list(yt.read_table(table_path, raw=False)))
    return 100 * float(yuids_count_in_all_fp) / float(yuid_row_count)


def get_param_from_str(name, str_params):
    try:
        value = re.compile(name + "=(.*?)(?:\\t|$)").findall(str_params)[0]
    except:
        value = ""
    return value


def get_table_column_names(yt, table_path):
    table_records = list(yt.read_table("//crypta/production/state/graph/dicts/yuid_with_all", raw=False))
    return {column_name for record in table_records for column_name in record.keys()}


def get_table_parameter_values(yt, table, parameter):
    return set(record[parameter] for record in yt.read_table(table, raw=False))


def get_all_tables(yt, tables_list):
    all_tables_data = []
    for table_path in tables_list:
        all_tables_data += list(yt.read_table(table_path, raw=False))
    return all_tables_data
