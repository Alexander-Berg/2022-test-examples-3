from pytest_bdd import scenarios
from .conftest import get_path

scenarios(
    "unfreeze_user.feature",
    features_base_dir=get_path("mail/hound/tests/integration/features/unfreeze_user.feature"),
    strict_gherkin=False
)
