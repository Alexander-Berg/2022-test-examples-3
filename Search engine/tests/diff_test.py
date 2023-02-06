import pytest

from diff import DIFF_2_SERPS_5, IMAGE_DIFF_2_SERPS_EMPTY_OR_FAILED_5, calculate_diff
from test_utils import read_json_lines_test_data


@pytest.mark.parametrize("metric, input_file, expected_result", [
    (DIFF_2_SERPS_5, 'nodiff_parsed.lines', (0, [])),
    (DIFF_2_SERPS_5, 'diff_parsed.lines', (0.4, ['http://www.bolshoyvopros.ru/questions/2431045-v-chem-fishka-delat-3-veschi-po'
                                                 '-cene-2-uh.html differs from https://incity.ru/offers/akcia_3_2_09/ at position 4',
                                                 'https://incity.ru/offers/akcia_3_2_09/ differs from http://www.bolshoyvopros.ru'
                                                 '/questions/2431045-v-chem-fishka-delat-3-veschi-po-cene-2-uh.html at position 5'])
     ),
    (IMAGE_DIFF_2_SERPS_EMPTY_OR_FAILED_5, 'image_nodiff_parsed.lines', (0, [])),
    (IMAGE_DIFF_2_SERPS_EMPTY_OR_FAILED_5, 'image_diff_parsed.lines', (1, [
        'http://im0-tub-kz.yandex.net/i?id=b8c041fcc4ed6082ff1db9265ea36fa6-l&n=13 '
        'differs from '
        'http://im0-tub-kz.yandex.net/i?id=ab264c3c7ec8b7ed6aa37e1b749c88ca-l&n=13 '
        'at position 1'
    ])),
])
def test_diff(metric, input_file, expected_result):
    parsed_rows = read_json_lines_test_data(input_file)
    result = calculate_diff(metric, parsed_rows)
    assert result == expected_result


@pytest.mark.parametrize("component, expected_url", [
    ({}, None),
    ({"url.imageBigThumbHref": "big_thumb"}, "big_thumb"),
    ({"imageadd": {"url": "add_url"}}, "add_url"),
    ({"imageadd": {"candidates": ["c_url"]}}, "c_url")
])
def test_images_get_url(component, expected_url):
    assert IMAGE_DIFF_2_SERPS_EMPTY_OR_FAILED_5.get_url(component) == expected_url
