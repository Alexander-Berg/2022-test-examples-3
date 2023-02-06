from library.python import resource

_DATA_RESOURCES = {}


class ResourceLoaderExcetpion(Exception):
    pass


def get_json(relative_path) -> str:
    _load_scenario_data()
    if relative_path in _DATA_RESOURCES:
        return _DATA_RESOURCES[relative_path]
    else:
        return '{}'


def _load_scenario_data():
    global _DATA_RESOURCES
    if _DATA_RESOURCES == {}:
        prefix = 'resfs/file/crm/supskills/direct_skill/tests/scenario_tests/scenario_data/'
        for key, val in resource.iteritems(prefix=prefix, strip_prefix=True):
            try:
                _DATA_RESOURCES[key] = val.decode('utf8')
            except UnicodeDecodeError:
                raise ResourceLoaderExcetpion(f'Error: Can not decode {key} to "utf8"')
