# coding=utf-8
from enrichments import SINSIG_ENRICHMENT, IMAGES_RELEVANCE_ENRICHMENT
from test_utils import read_json_test_data


def test_workflow_sinsig_web_normalization_same_url():
    original_assessment = read_json_test_data('normalization/web_sinsig/workflow_assessment_sameurl.json')
    normalize_assessment = SINSIG_ENRICHMENT.normalize(original_assessment, 'workflow')
    assert normalize_assessment['algorithm'] == 'n/a'
    assert normalize_assessment['probability'] == 'n/a'
    assert normalize_assessment['tag'] is None
    assert normalize_assessment['assessment_result'] == original_assessment['result']
    assert normalize_assessment['judgement_item']['qurl_id'] == 'b9106e19cbd3ceb14087b43845dd15b86360b4e89c2f8bdcb77f29f2'
    assert 'submit_ts' in normalize_assessment


def test_workflow_sinsig_web_normalization_remove_www():
    original_assessment = read_json_test_data('normalization/web_sinsig/workflow_assessment_removewww.json')
    normalize_assessment = SINSIG_ENRICHMENT.normalize(original_assessment, 'workflow')
    assert normalize_assessment['algorithm'] == 'n/a'
    assert normalize_assessment['probability'] == 'n/a'
    assert normalize_assessment['tag'] is None
    assert normalize_assessment['assessment_result'] != original_assessment['result']
    assert normalize_assessment['assessment_result']['task']['url'] == 'kinopoisk.ru/film/prizrachnyy-gonshchik-2007-3948'
    assert normalize_assessment['assessment_result']['qurl_id'] == 'cb92b0e76990175c1b316fefe77a8917874a44c62378bd8dbda29d7a'
    assert normalize_assessment['assessment_result']['task']['qurl_id'] == 'cb92b0e76990175c1b316fefe77a8917874a44c62378bd8dbda29d7a'
    assert 'submit_ts' in normalize_assessment


def test_database_sinsig_web__missing_url():
    original_assessment = read_json_test_data('normalization/web_sinsig/database_assessment_no_url.json')
    normalize_assessment = SINSIG_ENRICHMENT.normalize(original_assessment, 'database')
    assert normalize_assessment is None


def test_database_sinsig_web__remove_www():
    original_assessment = read_json_test_data('normalization/web_sinsig/database_assessment_removewww.json')
    normalize_assessment = SINSIG_ENRICHMENT.normalize(original_assessment, 'database')
    assert normalize_assessment['algorithm'] == 'n/a'
    assert normalize_assessment['probability'] == 0.0
    assert normalize_assessment['tag'] == ''
    assert normalize_assessment['assessment_result'] != original_assessment['assessment_result']
    assert normalize_assessment['assessment_result']['task']['url'] == 'et-ee.facebook.com/hansapost.ee'
    assert normalize_assessment['assessment_result']['qurl_id'] == '86165f06bfd88ca9d77582bbadfecf5aae5edb1b00fc59136d6580fd'
    assert normalize_assessment['assessment_result']['task']['qurl_id'] == '86165f06bfd88ca9d77582bbadfecf5aae5edb1b00fc59136d6580fd'
    assert 'submit_ts' in normalize_assessment


def test_workflow_normalization_images_relevance():
    original_assessment = read_json_test_data('normalization/images_relevance/workflow_assessment.json')
    normalize_assessment = IMAGES_RELEVANCE_ENRICHMENT.normalize(original_assessment, 'workflow')
    ji = normalize_assessment['judgement_item']
    assert ji
    assert ji['query'] == 'купалле'
    assert ji['region_id'] == 157
    assert ji['device'] == 'DESKTOP'
    assert ji['country'] == 'BY'
    assert ji['image_url'] == 'karl-marks.ru/wp-content/uploads/2019/07/3d3983241194170b18f2e7fa2bd34bc2.jpg'
    assert normalize_assessment['assessment_result']['relevance'] == 'RELEVANT_PLUS'
    assert normalize_assessment['algorithm'] == 'majority_vote'
    assert normalize_assessment['submit_ts'] == 1562690187687
    assert normalize_assessment['probability'] == 1.0
    assert normalize_assessment['raw_number'] == 1
    assert normalize_assessment['tag'] is None


def test_database_normalization_images_relevance():
    original_assessment = read_json_test_data('normalization/images_relevance/database_assessment.json')
    normalize_assessment = IMAGES_RELEVANCE_ENRICHMENT.normalize(original_assessment, 'database')
    ji = normalize_assessment['judgement_item']
    assert ji
    assert ji['query'] == 'чемпионат италии'
    assert ji['region_id'] == 47
    assert ji['device'] == 'DESKTOP'
    assert ji['country'] == 'RU'
    assert ji['image_url'] == 'pic.sport.ua/images/news/0/10/92/orig_418523.jpg'
    assert normalize_assessment['assessment_result']['relevance'] == 'IRRELEVANT'
    assert normalize_assessment['algorithm'] == 'majority_vote'
    assert normalize_assessment['submit_ts'] == 1544728801990
    assert normalize_assessment['probability'] == 1.0
    assert normalize_assessment['raw_number'] == 1
    assert normalize_assessment['tag'] == 'images_validate_2018-10-12T11_17_19.193Z'
