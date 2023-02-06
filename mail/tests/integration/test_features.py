import os

from pytest_bdd import scenarios
from yatest.common import source_path

FEATURE_DIR = source_path("mail/husky/tests/integration/features")


for feature_file in os.listdir(FEATURE_DIR):
    if feature_file.endswith('.feature'):
        scenarios(
            feature_file,
            features_base_dir=FEATURE_DIR,
            strict_gherkin=False
        )
