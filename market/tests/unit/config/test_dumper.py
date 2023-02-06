import yamarec1.config.dumper
import yamarec1.config.loader


def test_config_can_be_dumped_to_json():
    string = "a:\n b = c.d - 1\n c:\n  d = 2\nA: __extend__(a)\n c.d = 4"
    config = yamarec1.config.loader.load_from_string(string)
    result = yamarec1.config.dumper.dump_to_json(config)
    assert result == {
        "a": {
            "b": "1",
            "c": {
                "d": "2"
            }
        },
        "A": {
            "b": "3",
            "c": {
                "d": "4"
            }
        }
    }
