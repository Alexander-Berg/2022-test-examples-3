import observation


def test_observation_intersected():
    all_metrics = ["m1", "m2"]
    left_serpset_metrics = {
        "m1": [1., 2., 3., 4., 5.],
        "m2": [2., 2., 3., 3., 2.]
    }
    right_serpset_metrics = {
        "m1": [0., 0., 3., 4., 1.],
        "m2": [3., 3., 3., 3., 4.]
    }
    left_matched_indices = [0, 2, 3]
    right_matched_indices = [1, 2, 4]
    observation_result = observation.calculate_observation_in_intersected_mode(
        all_metrics, left_serpset_metrics, right_serpset_metrics, left_matched_indices, right_matched_indices)
    assert len(observation_result) == 2
    assert "m1" in observation_result
    assert observation_result["m1"] == {'leftValue': (1. + 3. + 4.) / 3, 'rightValue': (0. + 3. + 1.) / 3,
                                        'leftQueryAmount': 3, 'rightQueryAmount': 3,
                                        'diff': (1. + 3. + 4.) / 3 - (0. + 3. + 1.) / 3, 'pValue': 0}
    assert "m2" in observation_result
    assert observation_result["m2"] == {'leftValue': (2. + 3. + 3.) / 3, 'rightValue': (3. + 3. + 4.) / 3,
                                        'leftQueryAmount': 3, 'rightQueryAmount': 3,
                                        'diff': (2. + 3. + 3.) / 3 - (3. + 3. + 4.) / 3, 'pValue': 0}
