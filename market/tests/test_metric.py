from library.python import resource

import pandas as pd

from market.tools.stupid_predict.stupid_predict import group_queries_data, predict_stupid


def json_case(file, size, predictions):
    markup = pd.read_json(resource.find(file))
    df = group_queries_data(markup)
    df = df.rename(columns={'result_query': 'marketability'})
    df['stupid_prediction'] = predict_stupid(df)
    assert len(df) == size
    assert df.stupid_prediction.sum() == predictions


def test_metric():
    json_case("toloka-orders.json", 972, 32)
    json_case("non-marketable-sample.json", 1, 1)
