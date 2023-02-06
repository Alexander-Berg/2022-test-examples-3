import pytest
from market.idx.pylibrary.mindexer_core.banner.banner_msku_params_extractor import BannerMskuParamsLib


# newborn
#   Up to 3 months old. Newborn sizes are often identified by the age range in months (0–3) or just “newborn.”
# infant
#   3–12 months old. Infant sizes are often identified by the age range in months (3–12).
# toddler
#    1–5 years old. Toddler sizes are often identified by the age range in months (12–24) or years (1–5).
# kids
#    5–13 years old. All sizes within this age group have been manufactured to fit a child in that age range.
# adult
#    Typically teens or older. All sizes within this age group have been manufactured to fit an adult or teen.
@pytest.mark.parametrize('value, expected_age_group', [
    ('0 - 1 год', 'infant'),
    ('0+', 'adult'),
    ('0-6 месяцев', 'newborn'),
    ('1 - 3 года', 'toddler'),
    ('1 - 6 лет', 'toddler'),
    ('1,5-3 года', 'toddler'),
    ('1-1,5 года', 'toddler'),
    ('1-2 года', 'toddler'),
    ('12+', 'adult'),
    ('18+', 'adult'),
    ('3-6 месяцев', 'infant'),
    ('взрослая', 'adult'),
    ('взрослые', 'adult'),
    ('взрослый', 'adult'),
    ('все возраста', 'adult'),
    ('детская', 'kids'),
    ('детские', 'kids'),
    ('детский', 'kids'),
    ('для взрослого', 'adult'),
    ('для взрослых', 'adult'),
    ('для всех классов', 'adult'),
    ('для детей', 'kids'),
    ('для малышей', 'infant'),
    ('для ребенка', 'kids'),
    ('для юниоров', 'adult'),
    ('до 1 года', 'infant'),
    ('до 25', 'adult'),
    ('любой', 'adult'),
    ('любой возраст', 'adult'),
    ('младенец', 'newborn'),
    ('младшие классы', 'kids'),
    ('молодые', 'adult'),
    ('новорожденные', 'newborn'),
    ('от 11 лет', 'adult'),
    ('от 2 до 6 лет', 'toddler'),
    ('от 6 до 11 лет', 'kids'),
    ('от 7 лет', 'adult'),
    ('подростковый', 'adult'),
    ('подросток', 'adult'),
    ('пожилые', 'adult'),
    ('ребёнок', 'kids'),
    ('средние и старшие классы', 'adult'),
    ('юниорские', 'adult')
])
def test_age_group(value, expected_age_group):
    assert BannerMskuParamsLib.get_age_group(value) == expected_age_group
