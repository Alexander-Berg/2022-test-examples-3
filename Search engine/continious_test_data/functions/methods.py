# !/usr/bin/env python3

import numpy as np
import logging  # for debug
import json as js
from collections import Counter


def xpath_get(json, path):
    current_subtree = json
    try:
        for x in path.strip("/").split("/"):
            if isinstance(current_subtree, dict):
                current_subtree = current_subtree.get(x)
            elif isinstance(current_subtree, list):
                current_subtree = current_subtree[int(x)]
            else:
                return None
    except:
        return None
        # pass

    return current_subtree


def check_contains(json, path):
    subtree = xpath_get(json, path)
    if subtree is None:
        return 'RetryPush', False
    return 'Push', True


def check_field(json, path, value):
    contain_value = xpath_get(json, path)
    if contain_value != value:
        return 'RetryPush', False
    return 'Push', True


def get_number_contains_tree(json, path, leaf_path):
    subforest = xpath_get(json, path)
    if subforest is not list:
        return 'RetryPush', None
    for pos, tree in enumerate(subforest):
        if check_contains(tree, leaf_path)[1] == True:
            return 'Push', pos
    return 'Push', None


def get_number_with_field_tree(json, path, leaf_path, value):
    subforest = xpath_get(json, path)
    if not isinstance(subforest, list):
        return 'RetryPush', None
    for pos, tree in enumerate(subforest):
        if check_field(tree, leaf_path, value)[1] == True:
            print(value, pos)
            return 'Push', pos
    return 'Push', None


def check_number_of_occurrences(json, need_string, count_occurrences):
    json_string = js.dumps(json)
    if json_string.count(need_string) >= count_occurrences:
        return 'Push', True
    return 'RetryPush', False


def count_if(json, root_path, leaf_path, check_function):
    cnt = 0
    sub_forest = xpath_get(json, root_path)
    for subtree in sub_forest:
        try:
            leaf = xpath_get(subtree, leaf_path)
            if check_function(leaf):
                cnt += 1
        except:
            pass
    return 'Push', cnt

# for iznanka web recommendation


def check_web_recommendation(tree):
    try:
        if 'http' in tree.get('url') and '.' in tree.get('host') and tree.get('title') is not None:
            return 'Push', True
    except:
        return 'RetryPush', False


def check_web_recommendations(json, path):
    return check_sub_forest(json, path, check_web_recommendation)


def count_if_good_subtrees(json, path, check_function):
    return count_if(json, path, '', check_function)


def check_sub_forest(json, path, check_function):
    sub_forest = xpath_get(json, path)
    for subtree in sub_forest:
        if not check_function(subtree):
            return 'RetryDrop', None
    return 'Push', True


def check_subtree_good(json, path, check_function):
    subtree = xpath_get(json, path)
    if subtree is None or not check_function(subtree):
        return 'RetryPush', False
    return 'Push', True


def get_need_value(json, path):
    subtree = xpath_get(json, path)
    if subtree is None:
        return 'RetryDrop', None
    return 'Push', subtree


def get_info_by_tree(json, root_path, leaf_path, map_function, reduce_function):
    try:
        sub_forest = xpath_get(json, root_path)
        values = []
        for subtree in sub_forest:
            try:
                values.append(map_function(xpath_get(subtree, leaf_path)))
            except:
                pass
        return 'Push', reduce_function(values)
    except:
        return 'RetryDrop', None


def get_average_by_tree(json, root_path, leaf_path):
    return get_info_by_tree(json, root_path, leaf_path, float, np.mean)


def get_median_by_tree(json, root_path, leaf_path):
    return get_info_by_tree(json, root_path, leaf_path, float, np.median)


def len_with_none(arr):
    try:
        return len(arr)
    except:
        return 0


def get_avg_len_by_tree(json, root_path, leaf_path):
    return get_info_by_tree(json, root_path, leaf_path, len_with_none, np.mean)


def list_len(json, path):
    contained_list = xpath_get(json, path)
    return 'Push', len(xpath_get(json, path)) if contained_list else 'RetryPush', 0


def get_counter(responds, field, none_value=None):
    return Counter([resp.get(field) for resp in responds])


def get_reduce_function(responds, field, func, none_value=None):
    if none_value is None:
        return func([float(resp.get(field)) for resp in responds if resp.get(field) is not None])
    else:
        return func([float(resp.get(field)) if resp.get(field) is not None else none_value for resp in responds])


def get_median(responds, field, none_value=None):
    result = get_reduce_function(responds, field, np.median, none_value)
    return result if not np.isnan(result) else 0


def get_sum(responds, field, none_value=None):
    return get_reduce_function(responds, field, np.sum, none_value)


def get_average(responds, field, none_value=None):
    result = get_reduce_function(responds, field, np.mean, none_value)
    return result if not np.isnan(result) else 0


def get_prices(data):
    prices = []
    try:
        if xpath_get(data, 'searchdata.docs_right/0/construct/0/showcase/items'):
            for item in xpath_get(data, 'searchdata.docs_right/0/construct/0/showcase/items'):
                if item.get('price').get('priceMax'):
                    prices.append(int(item.get('price').get('priceMax')))
                elif item.get('price').get('priceMin'):
                    prices.append(int(item.get('price').get('priceMin')))
        return ('Push', prices) if len(prices) > 0 else ('RetryDrop', [0])  # need to be changed
    except:
        return 'RetryDrop', [0]


def get_average_price(responds):
    average_prices = []
    for resp in responds:
        try:
            average_prices.append(sum(resp.get('get_prices')) / len(resp.get('get_prices')))
        except:
            pass
    return 1.0 * sum(average_prices) / len(average_prices) / 1000  # for scale to value [0-20]


def get_average_list_field(responds, field):
    average_prices = []
    for resp in responds:
        try:
            average_prices.append(sum(resp.get(field)) / len(resp.get(field)))
        except:
            pass
    return 1.0 * sum(average_prices) / len(average_prices) / 1000 if average_prices else 0   # for scale to value [0-20]


def photo_count(data):
    try:
        geopath = 'app_host/result/docs/0/snippets/full/GeoMetaSearchData/'
        return 'Push', int(xpath_get(data, geopath + 'features/0/properties/Photos/count'))
    except:
        return 'Push', 0    # then change on retry


def avg_len_address_street(data):
    try:
        street_lens = []
        geopath = 'app_host/result/docs/0/snippets/full/GeoMetaSearchData/features'
        for feature in xpath_get(data, geopath):
            street_len = 0
            for component in xpath_get(feature, 'properties/CompanyMetaData/Address/Components'):
                try:
                    if component['kind'] == 'street':
                        street_len = len(component['name'])
                except:
                    return 'RetryDrop', 0
            street_lens.append(street_len)
        return 'Push', sum(street_lens) / len(street_lens)
    except:
        return 'RetryDrop', 0

