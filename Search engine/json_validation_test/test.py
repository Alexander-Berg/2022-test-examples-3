import json
import os
import glob
import yatest.common

profile_path = yatest.common.source_path('search/scraper/profile')
master_profile_path = yatest.common.source_path('search/scraper/master_profile')
metrics_template_path = yatest.common.source_path('search/metrics_templates')


def try_to_load_json(path):
    with open(path) as f:
        parsed = json.load(f)
        assert parsed is not None


def test_profiles_json_load():
    for profile in glob.glob(os.path.join(profile_path, '*.json')):
        try_to_load_json(profile)

    for profile in glob.glob(os.path.join(master_profile_path, '*.json')):
        try_to_load_json(profile)


def test_metrics_templates_json_load():
    for vertical in glob.glob(os.path.join(metrics_template_path, '*')):
        for template in glob.glob(os.path.join(vertical, '*.json')):
            try_to_load_json(template)
