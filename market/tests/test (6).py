# -*- coding: utf-8 -*-

from market.pylibrary.pers import pers
import yatest.common


# got from guruindexer/guruindexer/json_converter.py
def test_ok():
    input_file = yatest.common.source_path('market/pylibrary/pers/tests/data/models')
    info = pers.ModelsOpinions(input_file)
    result = [
        {
            modelid: {
                "opinions": {
                    "count": model.opinions,
                },
                "rating": {
                    "count": model.rating_total,
                    "value": model.rating_value,
                },
                "reviews": {
                    "count": model.reviews,
                }
            }
        }
        for modelid, model in sorted(info.models.items(), key=lambda model: model[0])
    ]
    assert len(result) == 61
    assert result[0] == {
        0: {
            'opinions': {'count': 0},
            'rating': {'count': 0, 'value': 0},
            'reviews': {'count': 1},
        }
    }
    assert result[-1] == {
        1969255818: {
            'opinions': {'count': 2},
            'rating': {'count': 50, 'value': 4.5},
            'reviews': {'count': 0},
        }
    }
