import data
import logging

from utils import (
    execute_match,
    create_match_tables,
    match_assert_test,
)
from data import (
    src_view,
    dst_view,
    sample_id,
    create_match_dataset,
    desc,
)


total_passed = 0


def test_match_auto(yt_client, config, view_params):
    # just to see what happen
    logger = logging.getLogger()
    logger.info(desc(view_params))
    dataset = create_match_dataset(config, view_params)
    create_match_tables(yt_client, config, dataset, view_params)
    execute_match(view_params, src_view=src_view, dst_view=dst_view, sample_id=sample_id)
    match_assert_test(yt_client, view_params)
    global total_passed
    total_passed += 1


def test_match_manual(yt_client, config):
    hashing_method = data.HM_IDENTITY
    include_original = True
    id_type = data.LAB_ID_IDFA_GAID
    key = "id"
    view_type = data.MATCHING
    scope = data.CROSS_DEVICE

    input_params = [hashing_method, include_original, id_type, key, view_type, scope]
    input_params.append(data.make_path(*input_params))

    hashing_method = data.HM_SHA256
    include_original = False
    id_type = data.LAB_ID_MM_DEVICE_ID
    key = "result"
    view_type = data.MATCHING
    scope = data.CROSS_DEVICE

    output_params = [hashing_method, include_original, id_type, key, view_type, scope]
    output_params.append(data.make_path(*output_params))
    view_params = {src_view: input_params, dst_view: output_params}
    dataset = create_match_dataset(config, view_params)
    create_match_tables(yt_client, config, dataset, view_params)
    execute_match(view_params, src_view=src_view, dst_view=dst_view, sample_id=sample_id)
    match_assert_test(yt_client, view_params)
