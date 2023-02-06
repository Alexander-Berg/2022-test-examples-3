import yatest.common
import json
import logging

logger = logging.getLogger("test_logger")


def load_aspects_json():
    aspects_json = yatest.common.source_path('extsearch/geo/aspects/aspects.json')
    with open(aspects_json) as f:
        data = json.load(f)
    return data


def load_aspects_domain_json():
    aspects_json = yatest.common.source_path('extsearch/geo/aspects/aspects_domain.json')
    with open(aspects_json) as f:
        data = json.load(f)
    return data


def test_ids_existence_in_all_files():
    logger.info("loading aspects.json")
    aspects = load_aspects_json()
    logger.info("loading aspects_domain.json")
    aspects_domain = load_aspects_domain_json()

    logger.info(
        "check that aspects.json and aspects_domain.json has the same num of keys ({} and {})".format(
            len(aspects.keys()), len(aspects_domain.keys())
        )
    )
    assert len(aspects.keys()) == len(aspects_domain.keys())
    diff_keys = set(aspects.keys()) ^ set(aspects_domain.keys())
    logger.info(f"different aspect_ids for aspects.json and aspects_domain.json {diff_keys}")
    assert len(diff_keys) == 0


def check_obligatory_and_optional_fields(obligatory_fields, optional_fields, aspects):
    for aspect_id, aspect_fields in aspects.items():
        logger.info(f"{aspect_id} has fields: {set(aspect_fields)}")
        assert (obligatory_fields & set(aspect_fields)) == obligatory_fields
        assert len(set(aspect_fields) - obligatory_fields - optional_fields) == 0


def test_aspects_fields():
    obligatory_fields = set(["hr_description", "rubric_ids"])
    optional_fields = set(
        [
            "photo_tag",
            "feature_enum_value",
            "feature_id",
            "rubric_id",
            "flags",
            "tags",
            "menu_tags",
            "need_photo",
            "cv_text",
            "cv_threshold",
            "dish_aspect_ids",
            "cuisine_aspect_ids",
            "status",
            "afisha_tag",
        ]
    )

    aspects = load_aspects_json()
    check_obligatory_and_optional_fields(obligatory_fields, optional_fields, aspects)


def test_aspects_domain_fields():
    obligatory_fields = set(["names"])
    optional_fields = set(["bert", "menu_regexps", "regexps"])
    aspects = load_aspects_domain_json()
    check_obligatory_and_optional_fields(obligatory_fields, optional_fields, aspects)
