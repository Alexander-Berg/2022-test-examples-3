TEST_FILTER = []


def _is_accepted_by_test_filter(nodeid):
    if nodeid in TEST_FILTER:
        return True

    for tf in TEST_FILTER:
        if nodeid.endswith('::' + tf):
            return True
        if nodeid.startswith(tf + '::'):
            return True


FEATURES = []


def _is_accepted_by_features(nodeid):
    for feature in FEATURES:
        if nodeid.startswith('test_{}.py::'.format(feature)):
            return True


def _is_selected(item):
    nodeid = item.nodeid
    if TEST_FILTER and not _is_accepted_by_test_filter(nodeid):
        return False
    if FEATURES and not _is_accepted_by_features(nodeid):
        return False
    return True


def pytest_collection_modifyitems(items, config):
    selected_items = []
    deselected_items = []

    for item in items:
        if _is_selected(item):
            selected_items.append(item)
        else:
            deselected_items.append(item)

    config.hook.pytest_deselected(items=deselected_items)
    items[:] = selected_items
