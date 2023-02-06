import yatest.common
import pandas as pd
import numpy as np
from market.replenishment.algorithms.safety_tool_lib.safety_tool import SafetyTool
from market.forecast.demand_ml_forecast_validation.lib_v2.models.simple_catboost_linear_interpolate_model import \
    SimpleCatboostLinearInterpolateMultModel, HistMaxCatboostLinearInterpolateMultModel, MaxOfHistMaxAndPriceLeakCdfModel


def test_calc_safety_simple_linear_model():
    demand_model = SimpleCatboostLinearInterpolateMultModel()
    model_path = yatest.common.source_path("market/replenishment/algorithms/tests/safety_tool/resources/"
                                           "model_simple_catboost_linear.zip")

    demand_model.load_model_local(model_path)

    planning_period = 7
    baseline_demand = 0.197
    historical_maximum = 0
    historical_warehouse_maximum = 0
    target_bsa = 0.98
    key_fields = ['msku', 'region']
    expected_ss = 15
    expected_ss_diff = 3
    expected_cyclic_stock = 4.5
    expected_cyclic_stock_diff = 1
    expected_planning_period_baseline_diff = 0.001
    expected_daily_mean = 0.7
    expected_daily_mean_diff = 0.3
    expected_daily_median = 0.1
    expected_daily_median_diff = 0.2
    expected_daily_predict = 7
    expected_daily_predict_diff = 3

    assert_calc_safety(key_fields, demand_model, planning_period, baseline_demand, target_bsa, expected_cyclic_stock,
                       expected_cyclic_stock_diff, expected_planning_period_baseline_diff, expected_ss,
                       expected_ss_diff, historical_maximum, historical_warehouse_maximum, planning_period,
                       expected_daily_mean, expected_daily_mean_diff,
                       expected_daily_median, expected_daily_median_diff,
                       expected_daily_predict, expected_daily_predict_diff
                       )


def test_calc_safety_hist_max_model():
    demand_model = HistMaxCatboostLinearInterpolateMultModel()
    model_path = yatest.common.source_path("market/replenishment/algorithms/tests/safety_tool/resources/"
                                           "model_hist_max_catboost.zip")
    demand_model.load_model_local(model_path)

    planning_period = 7
    baseline_demand = 1.0
    historical_maximum = 4522.0
    historical_warehouse_maximum = 132.5
    target_bsa = 0.98
    key_fields = ['msku', 'region']
    expected_ss = 150
    expected_ss_diff = 20
    expected_cyclic_stock = 39.5
    expected_cyclic_stock_diff = 4.5
    expected_planning_period_baseline_diff = 0.001
    expected_daily_mean = 5.8
    expected_daily_mean_diff = 1.5
    expected_daily_median = 0.6
    expected_daily_median_diff = 0.25
    expected_daily_predict = 70
    expected_daily_predict_diff = 20

    assert_calc_safety(key_fields, demand_model, planning_period, baseline_demand, target_bsa, expected_cyclic_stock,
                       expected_cyclic_stock_diff, expected_planning_period_baseline_diff, expected_ss,
                       expected_ss_diff, historical_maximum, historical_warehouse_maximum, planning_period,
                       expected_daily_mean, expected_daily_mean_diff,
                       expected_daily_median, expected_daily_median_diff,
                       expected_daily_predict, expected_daily_predict_diff
                       )

    assert_calc_safety(key_fields, demand_model, planning_period, baseline_demand, target_bsa, expected_cyclic_stock,
                       expected_cyclic_stock_diff, expected_planning_period_baseline_diff, expected_ss * 1.1,
                       expected_ss_diff * 1.3, historical_maximum, historical_warehouse_maximum, planning_period + 1,
                       expected_daily_mean, expected_daily_mean_diff * 1.1,
                       expected_daily_median, expected_daily_median_diff * 1.1,
                       expected_daily_predict, expected_daily_predict_diff * 1.1
                       )


def test_calc_safety_max_hist_max_priceleak_model():
    demand_model = MaxOfHistMaxAndPriceLeakCdfModel()
    model_path = yatest.common.source_path("//home/market/production/replenishment/safety_stock/models/"
                                            "models_MaxOfHistMaxFilterPriceLeak_21_04")
    demand_model.load_model_local(model_path)

    planning_period = 7
    baseline_demand = 1.0
    historical_maximum = 4522.0
    historical_warehouse_maximum = 132.5
    target_bsa = 0.98
    key_fields = ['msku', 'region']
    expected_ss = 150
    expected_ss_diff = 20
    expected_cyclic_stock = 39.5
    expected_cyclic_stock_diff = 4.5
    expected_planning_period_baseline_diff = 0.001
    expected_daily_mean = 5.8
    expected_daily_mean_diff = 1.5
    expected_daily_median = 0.6
    expected_daily_median_diff = 0.25
    expected_daily_predict = 70
    expected_daily_predict_diff = 20

    assert_calc_safety(key_fields, demand_model, planning_period, baseline_demand, target_bsa, expected_cyclic_stock,
                       expected_cyclic_stock_diff, expected_planning_period_baseline_diff, expected_ss,
                       expected_ss_diff, historical_maximum, historical_warehouse_maximum, planning_period,
                       expected_daily_mean, expected_daily_mean_diff,
                       expected_daily_median, expected_daily_median_diff,
                       expected_daily_predict, expected_daily_predict_diff
                       )

    assert_calc_safety(key_fields, demand_model, planning_period, baseline_demand, target_bsa, expected_cyclic_stock,
                       expected_cyclic_stock_diff, expected_planning_period_baseline_diff, expected_ss * 1.1,
                       expected_ss_diff * 1.3, historical_maximum, historical_warehouse_maximum, planning_period + 1,
                       expected_daily_mean, expected_daily_mean_diff * 1.1,
                       expected_daily_median, expected_daily_median_diff * 1.1,
                       expected_daily_predict, expected_daily_predict_diff * 1.1
                       )


def assert_calc_safety(key_fields, demand_model, planning_period, baseline_demand, target_bsa, expected_cyclic_stock,
                       expected_cyclic_stock_diff, expected_planning_period_baseline_diff, expected_ss,
                       expected_ss_diff, historical_maximum, historical_warehouse_maximum, max_horizon,
                       expected_daily_mean, expected_daily_mean_diff,
                       expected_daily_median, expected_daily_median_diff,
                       expected_daily_predict, expected_daily_predict_diff
                       ):
    tool = SafetyTool(demand_model)
    horizons = np.arange(1, max_horizon + 1, dtype=int)
    assert horizons.min() == 1
    assert horizons.max() == max_horizon
    assert len(horizons) == max_horizon
    dataset = pd.DataFrame({
        'horizon': horizons
    }).assign(region=300,
              msku=123,
              baseline=baseline_demand,
              historical_maximum=historical_maximum,
              historical_warehouse_maximum=historical_warehouse_maximum,
              planning_period=7,
              )
    assert len(dataset) == max_horizon
    cdfs = demand_model.cdf(dataset,
                            key_fields=key_fields,
                            sample_count=1000)
    assert len(cdfs) == 1
    cdf = next(iter(cdfs.values()))
    assert cdf is not None
    actual_ss = cdf(target_bsa)
    assert abs(actual_ss - expected_ss) < expected_ss_diff
    res = tool.calc_safety(dataset, key_fields=key_fields,
                           sample_count=1000,
                           target_bsa=target_bsa,
                           max_horizon=max_horizon)
    assert res is not None
    ff, ss, baseline = res.cyclic_stock, res.safety_stock, res.planning_period_baseline
    for x in [ff, ss, baseline]:
        assert len(x) == max_horizon
    assert abs(ff[2] - expected_cyclic_stock) < expected_cyclic_stock_diff
    assert abs(ss[2] - expected_ss) < expected_ss_diff
    assert abs(baseline[2] - baseline_demand * planning_period) < expected_planning_period_baseline_diff
    assert abs(res.daily_baseline[2] - baseline_demand) < expected_planning_period_baseline_diff
    assert abs(res.daily_mean[2] - expected_daily_mean) < expected_daily_mean_diff
    assert abs(res.daily_median[2] - expected_daily_median) < expected_daily_median_diff
    assert abs(res.daily_predict[2] - expected_daily_predict) < expected_daily_predict_diff
