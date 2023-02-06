# !/usr/bin/env python3

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


def get_geo_common_wizard_winner_tab(json, tab):
    try:
        import sys
        # logging.info("json="+js.dumps(json, indent=4))
        d_grouping = xpath_get(json, "searchdata.docs")
        if not d_grouping:
            raise Warning("empty d_grouping")
        for doc in d_grouping:
            snippet = xpath_get(doc, "snippets/full")
            if (xpath_get(snippet, "counter_prefix") != "/snippet/geo_common_wizard/"):
                continue
            logging.info("order of geo_common_wizard tabs = " + js.dumps(xpath_get(snippet, "data/order/0"), indent=4) + '\n')
            winner = xpath_get(snippet, "data/order/0")
            return winner == tab
        return False
    except Warning:
        raise NameError("d_grouping")
        # return False
    except:
        return False

