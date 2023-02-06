def get_orgs_info_schema():
    return [
        {'name': 'permalink', 'type': 'int64'},
        {'name': 'address', 'type': 'string'},
        {'name': 'region_id', 'type': 'int32'},
        {'name': 'region_name', 'type': 'string'},
        {'name': 'country_name', 'type': 'string'},
        {'name': 'geo_id', 'type': 'int32'},
        {'name': 'lat', 'type': 'double'},
        {'name': 'lon', 'type': 'double'},
        {'name': 'main_rubric_id', 'type': 'int64'},
        {'name': 'main_rubric_name_ru', 'type': 'string'},
        {'name': 'name', 'type': 'string'},
        {'name': 'publishing_status', 'type': 'string'},
    ]


def get_caesar_info_schema():
    return [
        {'name': 'bannerid', 'type': 'int64'},
        {'name': 'groupbannerid', 'type': 'int64'},
        {'name': 'banner_body', 'type': 'string'},
        {'name': 'rt_sadovaya_vector', 'type_v3': {'type_name': 'list', 'item': 'double'}},
    ]
