from checkers import *
from tools import *


def test_simple(geocounter_app):
    req = prepare_request(Bboxes.WHOLE_WORLD)
    resp = geocounter_app.grpc_get_counts(req)
    check_counts(resp, expected_total=100, expected_matched=100,
                 expected_filter_values=[
                     ('stars-1', 0),
                     ('stars-2', 2),
                     ('stars-3', 2),
                     ('stars-4', 4),
                     ('stars-5', 0),
                 ])

def test_selected_filter(geocounter_app):
    req = prepare_request(Bboxes.WHOLE_WORLD, initial_filter_groups=[
        ('star', [('stars-3', Filter('star', ['three']))]),
    ])
    resp = geocounter_app.grpc_get_counts(req)
    check_counts(resp, expected_total=100, expected_matched=2,
                 expected_filter_values=[
                     ('stars-1', 2),
                     ('stars-2', 4),
                     ('stars-3', 2),
                     ('stars-4', 6),
                     ('stars-5', 2),
                 ])
