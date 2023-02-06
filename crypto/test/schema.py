from crypta.lib.python.yt import schema_utils
from yt import yson


def get_phone_lemmas_schema():
    return schema_utils.yt_schema_from_dict({
        'price_segment': 'string',
        'model_lemmas': 'any',
    })


def get_reqans_log_schema():
    return yson.YsonList([
        {
            'name': 'UserId',
            'type': 'any',
            'required': False,
            'type_v3': {
                'type_name': 'optional',
                'item': {
                    'type_name': 'struct',
                    'members': [
                        {
                            'name': 'YandexUid',
                            'type': {
                                'type_name': 'optional',
                                'item': 'string',
                            },
                        },
                    ],
                },
            },
        },
        {
            'name': '_logfeller_timestamp',
            'type': 'uint64',
            'required': True,
            'type_v3': 'uint64',
        },
        {
            'name': 'Msp',
            'type': 'any',
            'required': False,
            'type_v3': {
                'type_name': 'optional',
                'item': {
                    'type_name': 'struct',
                    'members': [
                        {
                            'name': 'CorrectedQuery',
                            'type': {
                                'type_name': 'optional',
                                'item': 'string',
                            },
                        },
                        {
                            'name': 'Relev',
                            'type': {
                                'type_name': 'optional',
                                'item': 'int32',
                            },
                        },
                    ],
                },
            },
        },
        {
            'name': 'Query',
            'type': 'string',
            'required': False,
            'type_v3': {
                'type_name': 'optional',
                'item': 'string',
            },
        },
        {
            'name': 'UiLanguage',
            'type': 'string',
            'required': False,
            'type_v3': {
                'type_name': 'optional',
                'item': 'string',
            },
        },
    ])
