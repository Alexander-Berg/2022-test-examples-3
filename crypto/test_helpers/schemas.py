ALLOWED_ATTRIBUTES = {"strict", "unique_keys"}
ALLOWED_COLUMN_FIELDS = {"expression", "name", "required", "sort_order", "type_v3"}


def _filter_dict(d, allowed_keys):
    return {key: value for key, value in d.iteritems() if key in allowed_keys}


def _filter_attributes(attributes):
    return _filter_dict(attributes, ALLOWED_ATTRIBUTES)


def _filter_single_column(column):
    return _filter_dict(column, ALLOWED_COLUMN_FIELDS)


def _filter_columns(columns):
    return [_filter_single_column(column) for column in columns]


def get_schema_for_canonization(schema):
    schema_filters = {
        "$attributes": _filter_attributes,
        "$value": _filter_columns,
    }
    return {key: filter_(schema[key]) for key, filter_ in schema_filters.iteritems()}
