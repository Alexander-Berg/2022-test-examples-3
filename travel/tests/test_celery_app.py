# -*- coding: utf-8 -*-
import json

import mock
import pytest

from travel.avia.revise.revise_task.celery_app import do_review


@pytest.fixture()
def raw_task():
    return json.dumps({
        'price_value': 100,
        'shown_price_value': 100,
        'partner': 'ozon',
        'redirect_params': json.dumps({'url': 'some_url'}),
        'date_forward': '2020-12-12'
    })


def test_do_review(raw_task):
    data = ['first', 'second', {'price': 100, 'currency': 'RUB'}]

    def extractor(*args, **kwargs):
        for item in data:
            yield item

    write_review = review_with_patch(raw_task, extractor)
    write_review.assert_called_once()

    revise_report = write_review.call_args.kwargs['review']
    assert revise_report['shown_result'] == 'pass'
    assert revise_report['screenshots'] == []
    assert revise_report['shown_price_diff_abs'] == .0
    assert json.loads(revise_report['revise_data']) == data


def test_do_review_with_exception(raw_task):
    data = ['first', 'second']

    def extractor(*args, **kwargs):
        for item in data:
            yield item
        raise Exception('bad review')

    write_review = review_with_patch(raw_task, extractor)
    write_review.assert_called_once()

    revise_report = write_review.call_args.kwargs['review']
    expected_fields = {
        'description': 'Can not fetch review error: bad review',
        'result': 'can_not_fetch_review_error',
        'screenshots': [],
        'shown_result': 'can_not_fetch_review_error',
    }
    for key, value in expected_fields.items():
        assert revise_report[key] == value


def description_test_helper(original_text, result_text):
    raw_task = json.dumps({
        'price_value': 100,
        'shown_price_value': 100,
        'partner': 'ozon',
        'redirect_params': json.dumps({'url': 'some_url'}),
        'date_forward': '2020-12-12',
        'description': original_text,
    })

    def extractor(*args, **kwargs):
        pass

    write_review = review_with_patch(raw_task, extractor)
    write_review.assert_called_once()

    revise_report = write_review.call_args.kwargs['review']
    assert revise_report['description'] == result_text


def test_normal_description_field():
    description_test_helper('a' * 255, 'a' * 255)


def test_long_description_field():
    description_test_helper('a' * 256, 'a' * 252 + '...')


def review_with_patch(raw_task, extractor):
    with mock.patch('travel.avia.revise.revise_task.celery_app.review_writer.write_review') as write_review:
        with mock.patch('travel.avia.revise.extractor.extract._get_module_extractor', return_value=extractor):
            with mock.patch('travel.avia.revise.revise_task.celery_app.get_driver'):
                do_review(raw_task, 'some_id')
        return write_review
