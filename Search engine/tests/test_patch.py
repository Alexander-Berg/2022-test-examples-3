import copy
import json
from pathlib import Path

import pytest

from swap import swap_components, patch_serpsets, PATCHES, swap_search_results, set_right_alignment_for_first_wizard, \
    ComponentAlignments, ComponentTypes

TEST_DATA_DIRECTORY = Path("tests") / "data"
INPUT_SERPSETS_DIRECTORY = TEST_DATA_DIRECTORY / "input_serpsets"
OUTPUT_SERPSETS_DIRECTORY = TEST_DATA_DIRECTORY / "output_serpsets"


def load_json(filename):
    with open(filename, encoding="utf-8") as f:
        return json.load(f)


def save_json(data, filename):
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(data, f)


@pytest.fixture
def get_serpset():
    p = INPUT_SERPSETS_DIRECTORY / "28276150.json"
    return load_json(p)


@pytest.fixture
def get_serpset_metadata():
    p = TEST_DATA_DIRECTORY / "input_serpset_info.json"
    return load_json(p)


def test_swap_components(get_serpset):
    first_serp = get_serpset[0]
    components = first_serp["components"]
    original_components = copy.deepcopy(components)
    swap_components(first_serp, 1, 3)

    # 1 and 3 are swapped
    assert components[1] == original_components[3]
    assert components[3] == original_components[1]
    # All the rest are left intact
    assert components[:1] == original_components[:1]
    assert components[2] == original_components[2]
    assert components[4:] == original_components[4:]


def test_no_swap_for_out_of_bounds_indices(get_serpset):
    first_serp = get_serpset[0]
    components = first_serp["components"]
    original_components = copy.deepcopy(components)
    swap_components(first_serp, 0, len(components))
    assert components == original_components


def test_swap_search_results(get_serpset):
    first_serp = get_serpset[0]
    components = first_serp["components"]
    original_components = copy.deepcopy(components)
    swap_search_results(first_serp, 1, 3)

    # 2 and 4 are swapped (the shift is due to a wizard at 1)
    assert components[2] == original_components[4]
    assert components[4] == original_components[2]
    # All the rest are left intact
    assert components[:2] == original_components[:2]
    assert components[3] == original_components[3]
    assert components[5:] == original_components[5:]


def test_set_right_alignment_for_first_wizard(get_serpset):
    first_serp = get_serpset[0]
    components = first_serp["components"]
    original_components = copy.deepcopy(components)
    set_right_alignment_for_first_wizard(first_serp)

    assert original_components[0]["componentInfo"]["type"] != ComponentTypes.WIZARD
    assert original_components[1]["componentInfo"]["type"] == ComponentTypes.WIZARD
    assert original_components[1]["componentInfo"]["alignment"] == ComponentAlignments.LEFT
    assert components[1]["componentInfo"]["alignment"] == ComponentAlignments.RIGHT

    assert components[:1] == original_components[:1]
    assert components[2:] == original_components[2:]


def test_patch(get_serpset, get_serpset_metadata, tmpdir):
    tmp_path = Path(tmpdir)
    test_input = "TEST_INPUT"
    test_output = "TEST_OUTPUT"
    input_filename = "28276150.json"
    input_metadata_filename = "test_input_serpset_info.json"
    input_metadata_file = Path(input_metadata_filename)
    output_metadata_filename = "test_output_serpset_info.json"
    output_metadata_file = Path(output_metadata_filename)

    path_test_input = tmp_path / test_input
    path_test_input.mkdir()
    path_test_output = tmp_path / test_output
    path_test_output.mkdir()

    with open(path_test_input / input_filename, "w", encoding="utf-8") as f:
        json.dump(get_serpset, f)
    with open(input_metadata_filename, "w", encoding="utf-8") as f:
        json.dump(get_serpset_metadata, f)
    assert path_test_input.exists()
    assert (path_test_input / input_filename).exists()

    patches = PATCHES = [
        (lambda serp: swap_components(serp, 1, 4), "swap 1 and 4"),
        (lambda serp: swap_components(serp, 1, 3), "swap 1 and 3"),
    ]

    patch_serpsets(
        patches=patches,
        input_directory=path_test_input,
        output_directory=path_test_output,
        metadata_input_filename=input_metadata_file,
        metadata_output_filename=output_metadata_file,
    )

    out_0 = "28276150_0.json"
    out_1 = "28276150_1.json"
    out_full_0 = path_test_output / out_0
    out_full_1 = path_test_output / out_1
    assert out_full_0.exists()
    assert out_full_1.exists()
    assert len(list(path_test_output.iterdir())) == len(PATCHES)
    assert output_metadata_file.exists()

    assert load_json(TEST_DATA_DIRECTORY / out_0) == load_json(out_full_0)
    assert load_json(TEST_DATA_DIRECTORY / out_1) == load_json(out_full_1)
    assert load_json(TEST_DATA_DIRECTORY / "output_serpset_info.json") == load_json(output_metadata_file)


def test_patch_multiple_serpsets(tmpdir):
    tmp_path = Path(tmpdir)
    test_input = "TEST_INPUT"
    test_output = "TEST_OUTPUT"
    input_filenames = [
        "28276150.json",
        "28274207.json",
    ]
    input_metadata_filename = "test_input_serpset_info.json"
    input_metadata_file = Path(input_metadata_filename)
    output_metadata_filename = "test_output_serpset_info.json"
    output_metadata_file = Path(output_metadata_filename)

    path_test_input = tmp_path / test_input
    path_test_input.mkdir()
    path_test_output = tmp_path / test_output
    path_test_output.mkdir()

    save_json(load_json(TEST_DATA_DIRECTORY / "input_serpset_info_two_serpsets.json"), input_metadata_filename)
    for input_serpset_filename in input_filenames:
        serpset_data = load_json(INPUT_SERPSETS_DIRECTORY / input_serpset_filename)
        save_json(serpset_data, path_test_input / input_serpset_filename)

    assert path_test_input.exists()
    assert (path_test_input / input_serpset_filename).exists()

    patches = PATCHES = [
        (lambda serp: swap_components(serp, 1, 4), "swap components 1 and 4"),
        (lambda serp: swap_components(serp, 1, 3), "swap components 1 and 3"),
    ]

    patch_serpsets(
        patches=patches,
        input_directory=path_test_input,
        output_directory=path_test_output,
        metadata_input_filename=input_metadata_file,
        metadata_output_filename=output_metadata_file,
    )

    assert len(list(path_test_output.iterdir())) == len(PATCHES) * len(list(path_test_input.iterdir()))
    # i.e. number of output serpsets = number of patches * number of input serpsets
    assert output_metadata_file.exists()
    for ss_id in ["28276150", "28274207"]:
        for patch_no in [0, 1]:
            filename = "{}_{}.json".format(ss_id, patch_no)
            assert load_json(OUTPUT_SERPSETS_DIRECTORY / filename) == load_json(path_test_output / filename)
    assert load_json(TEST_DATA_DIRECTORY / "output_serpset_info_two_serpsets.json") == load_json(output_metadata_file)
