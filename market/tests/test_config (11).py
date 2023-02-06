import pytest
from typing import List
from market.robotics.cv.library.py.synthgen.config import common
from market.robotics.cv.library.py.synthgen import constants


@pytest.fixture
def augmentation_tricks() -> List[str]:
    return [
        constants.VFLIP_AUGMENTATION_NAME,
        constants.HFLIP_AUGMENTATION_NAME,
        constants.ROTATE_AUGMENTATION_NAME,
        constants.BRIGHTNESS_AUGMENTATION_NAME,
        constants.CHANNEL_SHUFFLE_AUGMENTATION_NAME,
        constants.SCALE_AUGMENTATION_NAME
    ]


def test_read_config(config):
    assert isinstance(config, common.Config)


def test_general(config):
    assert isinstance(config.general, common.GeneralConfig)
    assert config.general.sample_count == 500
    assert config.general.crops_count_min == 0
    assert config.general.crops_count_max == 5
    assert config.general.output_file == "out.tsv"
    assert config.general.s3_bucket_name == "synthetic_dataset"
    assert config.general.s3_folder_path == "/path/to/folder"
    assert config.general.toloka_labeling_files == ["input_1.tsv", "input_2.tsv"]
    assert config.general.ignore_labeling_files == ["ignore_1.tsv", "ignore_2.tsv"]
    assert config.general.background_files == ["backgrounds_1.tsv", "backgrounds_2.tsv"]
    assert config.general.border_size == 20


def test_synthetic(config, augmentation_tricks):
    assert isinstance(config.synthetic, common.SyntheticConfig)
    assert isinstance(config.synthetic.crop_augmentation, common.CropAugmentationConfig)
    assert isinstance(config.synthetic.crop_position, common.CropPositionConfig)
    assert set(config.synthetic.crop_augmentation.apply) == set(augmentation_tricks)
    assert config.synthetic.crop_position.out_border_min == 0.3
    assert config.synthetic.crop_position.out_border_max == 0.7
    assert config.synthetic.crop_position.on_intersect == "skip"


def test_augmentation_props(config, augmentation_tricks):
    assert len(config.synthetic.crop_augmentation.props) == len(augmentation_tricks)
    count_not_allowed = 0
    for prop in config.synthetic.crop_augmentation.props:
        assert prop.type in augmentation_tricks
        assert prop.prob == 0.5
        if prop.type in [constants.VFLIP_AUGMENTATION_NAME, constants.HFLIP_AUGMENTATION_NAME,
                         constants.CHANNEL_SHUFFLE_AUGMENTATION_NAME]:
            assert isinstance(prop, common.BaseAugmentationConfig)
        elif prop.type == constants.SCALE_AUGMENTATION_NAME:
            assert isinstance(prop, common.RangeAugmentationConfig)
            assert prop.low == 0.8
            assert prop.high == 1.2
        elif prop.type == constants.ROTATE_AUGMENTATION_NAME:
            assert isinstance(prop, common.RangeAugmentationConfig)
            assert prop.low == 0
            assert prop.high == 360
        elif prop.type == constants.BRIGHTNESS_AUGMENTATION_NAME:
            assert isinstance(prop, common.RangeAugmentationConfig)
            assert prop.low == 0.5
            assert prop.high == 1.5
        else:
            count_not_allowed += 1
    assert count_not_allowed == 0
