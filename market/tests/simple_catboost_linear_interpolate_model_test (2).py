import pytest

from market.forecast.torch_demand.lib.models.catboost_model_params import test_simple_catboost_model_params

from market.forecast.torch_demand.lib.models.catboost_linear_interpolate_model import \
    CatboostLinearInterpolateMultModel, CatboostCdfLinearInterplatedModel

import pandas as pd
import tempfile


@pytest.mark.parametrize("model", [
    CatboostCdfLinearInterplatedModel(
        name="SimpleCatboostLinearInterpolatedModel", **test_simple_catboost_model_params),
    CatboostLinearInterpolateMultModel(
        name="SimpleCatboostLinearInterpolateMultModel", **test_simple_catboost_model_params),

    # TODO вернуть тест распределенных обучений, разобравшись с пулами
    # AbstractCatboostCdfLinearInterplatedModel(
    #     name="SimpleCatboostLinearInterpolatedModel", distribute_fit=True, **simple_catboost_model_params),
    # AbstractCatboostLinearInterpolateMultModel(
    #     name="SimpleCatboostLinearInterpolateMultModel", distribute_fit=True, **simple_catboost_model_params),
])
def test_smoke(model):
    """
    Простой тест который проверяет может ли модель выучить равномерное дискретное распределение величин 1 и 2
    """

    train_dataset = pd.DataFrame(
        [
            dict(baseline=0.99, horizon=1, target=1),
            dict(baseline=1.01, horizon=1, target=2),

            dict(baseline=0.99, horizon=1, target=2),
            dict(baseline=1.01, horizon=1, target=1),
        ]
    )

    predict_dataset = pd.DataFrame(
        [
            dict(baseline=1, horizon=1, key_1=1, key_2=1)
        ]
    )

    with tempfile.NamedTemporaryFile() as f:
        model.fit(train_dataset)
        model.save_model_local(f.name)

        del model

        model = CatboostCdfLinearInterplatedModel(
            name="SimpleCatboostLinearInterpolatedModel", **test_simple_catboost_model_params)
        model.load_model_local(f.name)

    cdfs = model.cdf(predict_dataset, sample_count=1000, key_fields=['key_1', 'key_2'])

    assert abs(model.predict_quantile(predict_dataset, 0.75) - 2) < 0.1
    assert abs(model.predict_quantile(predict_dataset, 0.25) - 1) < 0.1

    cdf = cdfs[(1, 1)]

    assert abs(cdf(1) - 2) < 0.1
    assert abs(cdf(0.75) - 2) < 0.1
    assert abs(cdf(0.25) - 1) < 0.1
    assert abs(cdf(0) - 1) < 0.1
