CASES = {
    "MetaInvalid__BindingsInvalid": {
        "1400000000": {
            "meta": "invalid_meta.yson",
            "bindings": "invalid_bindings.yson"
        }
    },
    "MetaInvalid__BindingsMissing": {
        "1400000000": {
            "meta": "invalid_meta.yson"
        }
    },
    "MetaInvalid__BindingsValid": {
        "1400000000": {
            "meta": "invalid_meta.yson",
            "bindings": "valid_bindings.yson"
        }
    },
    "MetaDuplicate__BindingsValid": {
        "1400000000": {
            "meta": "duplicate_meta.yson",
            "bindings": "valid_bindings.yson"
        }
    },
    "MetaValid__BindingsInvalid": {
        "1400000000": {
            "meta": "valid_meta.yson",
            "bindings": "invalid_bindings.yson"
        }
    },
    "MetaValid__BindingsDuplicate": {
        "1400000000": {
            "meta": "valid_meta.yson",
            "bindings": "duplicate_bindings.yson"
        }
    },
    "MetaValid__BindingsMissing": {
        "1400000000": {
            "meta": "valid_meta.yson"
        }
    },
    "MetaValid__BindingsValid": {
        "1400000000": {
            "meta": "valid_meta.yson",
            "bindings": "valid_bindings.yson"
        }
    },
    "ValidInvalidMix": {
        "1300000000": {
            "meta": "valid_meta.yson",
            "bindings": "invalid_bindings.yson"
        },
        "1400000000": {
            "meta": "valid_meta.yson",
            "bindings": "valid_bindings.yson"
        }
    },
    "InputWithEmptyTimestampDir": {
        "1400000000": {}
    },
    "InputWithNonTimestampDir": {
        "xxx": {
            "meta": "valid_meta.yson",
            "bindings": "valid_bindings.yson"
        }
    },
    "MetaNoTariff__BindingsValid": {
        "1400000000": {
            "meta": "no_tariff_meta.yson",
            "bindings": "valid_bindings.yson"
        }
    },
}
