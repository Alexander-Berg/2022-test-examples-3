# !/usr/bin/env python3


def xpath_get(json, path):
    current_subtree = json
    try:
        for x in path.strip("/").split("/"):
            if isinstance(current_subtree, dict):
                current_subtree = current_subtree.get(x)
            elif isinstance(current_subtree, list):
                current_subtree = current_subtree[int(x)]
    except:
        return None
        # pass

    return current_subtree


def is_contains(json, path):
    subtree = xpath_get(json, path)
    if subtree is None:
        return ('RetryPush', False)
    return ('Push', True)


def _is_market_wizard(data):
    return xpath_get(data, 'searchdata.docs_right/0/construct/0/counter/path') is not None


def get_sum(responds, field):
    return sum([float(resp.get(field)) for resp in responds if resp])


# the same above
def _is_company_wizard(data):
    return xpath_get(data, 'searchdata.docs_right/0/snippets/full/counter_prefix') is not None


def get_company_wizards_count(responds):
    return sum([resp.get('_is_company_wizard') for resp in responds])


def get_prices(data):
    prices = []
    try:
        if xpath_get(data, 'searchdata.docs_right/0/construct/0/showcase/items'):
            for item in xpath_get(data, 'searchdata.docs_right/0/construct/0/showcase/items'):
                if item.get('price').get('priceMax'):
                    prices.append(int(item.get('price').get('priceMax')))
                elif item.get('price').get('priceMin'):
                    prices.append(int(item.get('price').get('priceMin')))
        return ('Push', prices) if len(prices) > 0 else ('RetryDrop', None)  # need to be changed
    except:
        return ('RetryDrop', None)


def get_average_price(responds):
    average_prices = []
    for resp in responds:
        try:
            average_prices.append(sum(resp.get('get_prices')) / len(resp.get('get_prices')))
        except:
            pass
    return sum(average_prices) / len(average_prices) / 1000  # for scale to value [0-20]


def get_average_list_field(responds, field):
    average_prices = []
    for resp in responds:
        try:
            average_prices.append(sum(resp.get(field)) / len(resp.get(field)))
        except :
            pass
    return float(sum(average_prices)) / len(average_prices) / 1000  # for scale to value [0-20]


