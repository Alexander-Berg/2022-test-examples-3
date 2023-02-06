import os

from yatest.common import source_path

from pytest_bdd import scenarios

FEATURE_DIR = source_path("mail/pg/mdb//tests/pytest_bdd/features")


for feature_file in os.listdir(FEATURE_DIR):
    if feature_file.endswith('.feature'):
        scenarios(
            feature_file,
            features_base_dir=FEATURE_DIR,
            strict_gherkin=False
        )
