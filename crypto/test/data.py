DATA = {
    "FreshExists__StateExists": {
        "meta": "meta",
        "bindings": "bindings",
        "fresh": {
            "1400000100_1400001000": {
                "meta": "1400000100_1400001000_meta",
                "bindings": "1400000100_1400001000_bindings"
            }
        },
        "CRYPTA_FROZEN_TIME": 1400001100,
        "inactive-segments-ttl": 10000,
        "bindings-ttl": 10000,
        "should-drop-input": True
    },
    "FreshExists__StateMissing": {
        "fresh": {
            "1400000100_1400001000": {
                "meta": "1400000100_1400001000_meta",
                "bindings": "1400000100_1400001000_bindings"
            }
        },
        "CRYPTA_FROZEN_TIME": 1400001100,
        "inactive-segments-ttl": 10000,
        "bindings-ttl": 10000,
        "should-drop-input": True
    },
    "FreshMissing__StateExists": {
        "meta": "meta",
        "bindings": "bindings",
        "CRYPTA_FROZEN_TIME": 1400001100,
        "inactive-segments-ttl": 10000,
        "bindings-ttl": 10000,
        "should-drop-input": True
    },
    "FreshMissing__StateMissing": {
        "CRYPTA_FROZEN_TIME": 1400001100,
        "inactive-segments-ttl": 10000,
        "bindings-ttl": 10000,
        "should-drop-input": True
    },
    "TariffChanging": {
        "meta": "meta",
        "fresh": {
            "1400000400_1400001000": {
                "meta": "1400000400_1400001000_meta",
                "bindings": "1400000400_1400001000_bindings"
            }
        },
        "CRYPTA_FROZEN_TIME": 1400001100,
        "inactive-segments-ttl": 10000,
        "bindings-ttl": 10000,
        "should-drop-input": True
    },
    "ValidInvalidMix": {
        "meta": "meta",
        "bindings": "bindings",
        "fresh": {
            "1400000100_1400001000": {
                "meta": "1400000100_1400001000_meta",
                "bindings": "1400000100_1400001000_bindings"
            },
            "1400000400_1400001000": {
                "meta": "1400000400_1400001000_meta",
                "bindings": "1400000400_1400001000_bindings"
            }
        },
        "CRYPTA_FROZEN_TIME": 1400001100,
        "inactive-segments-ttl": 10000,
        "bindings-ttl": 10000,
        "should-drop-input": True
    },
    "ArchiveWithOlderTimestampThanState": {
        "meta": "meta",
        "bindings": "bindings",
        "fresh": {
            "1399999999_1400001000": {
                "meta": "1399999999_1400001000_meta",
                "bindings": "1399999999_1400001000_bindings"
            }
        },
        "CRYPTA_FROZEN_TIME": 1400001100,
        "inactive-segments-ttl": 10000,
        "bindings-ttl": 10000,
        "should-drop-input": True
    }
}
