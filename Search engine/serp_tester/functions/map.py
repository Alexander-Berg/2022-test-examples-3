# !/usr/bin/env python3

import logging  # for debug
import json as js
import re

def xpath_get(json, path):
    current_subtree = json
    path = path.replace('//', '@#')
    try:
        for x in path.strip("/").split("/"):
            x = x.replace('@#','/')
            if isinstance(current_subtree, dict):
                current_subtree = current_subtree.get(x)
            elif isinstance(current_subtree, list):
                current_subtree = current_subtree[int(x)]
            else:
                return None
    except:
        return None

    return current_subtree


def check_contains(json, path):
    subtree = xpath_get(json, path)
    if subtree is None:
        logging.debug(js.dumps(json, indent=2) + '\n' + path)
        return False
    return True

def check_contains_snippet(json, counter_prefix):
    snippets = xpath_get(json, "searchdata.docs.*.snippets") or xpath_get(json, "searchdata.docs.*.snippets.full")
    if snippets is None:
        return False
    for snippet in snippets:
        if xpath_get(snippet, 'counter_prefix') == counter_prefix:
            return True
    return False

def check_phone(phone):
    try:
        result = True if re.match(r'[\d\(\)+-]+', phone) else False
    except Exception:
        return False
    return result

def check_realty_phone(json, path):
    try :
        phone_sales_department = xpath_get(json, path+'/SalesDepartment/phone')
        phone = xpath_get(json, path+'/phone')
        result = check_phone(phone) or check_phone(phone_sales_department)
    except Exception:
        return False
    return result


def check_path_val(json, path, val):
    subtree = xpath_get(json, path)
    return subtree == val
