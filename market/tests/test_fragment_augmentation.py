import pytest
import os.path
from PIL import Image, ImageChops
from yatest.common import work_path
from market.robotics.cv.library.py.synthgen.augmentation import properties
from market.robotics.cv.library.py.synthgen import augmentation
from market.robotics.cv.library.py.synthgen import constants

# only for local debug and visualisation
SAVE_RESULTS = False
SAVE_RESULTS_FILEPATH = "/Users/ilinvalery/Downloads/test_images"


@pytest.fixture
def base_positive_property():
    return properties.BaseProperty(apply=True)


@pytest.fixture
def base_negative_property():
    return properties.BaseProperty(apply=False)


@pytest.fixture
def rotate_value_property():
    return properties.ValueProperty(
        apply=True,
        value=45
    )


@pytest.fixture
def brightness_value_property():
    return properties.ValueProperty(
        apply=True,
        value=1.5
    )


@pytest.fixture
def contrast_value_property():
    return properties.ValueProperty(
        apply=True,
        value=2.0
    )


@pytest.fixture
def color_value_property():
    return properties.ValueProperty(
        apply=True,
        value=2.0
    )


@pytest.fixture
def sharpness_value_property():
    return properties.ValueProperty(
        apply=True,
        value=3.0
    )


@pytest.fixture
def scale_value_property():
    return properties.ValueProperty(
        apply=True,
        value=2.0
    )


@pytest.fixture
def noise_value_property():
    return properties.ValueProperty(
        apply=True,
        value=25.0
    )


@pytest.fixture
def image() -> Image.Image:
    test_image_filepath = os.path.join(work_path(), "1_0.jpg")
    return Image.open(test_image_filepath)


def save_test_folder() -> str:
    path = SAVE_RESULTS_FILEPATH
    if not os.path.exists(path):
        os.makedirs(path)
    return path


def save_image(image: Image.Image, filename, extension='png'):
    if SAVE_RESULTS:
        path = os.path.join(save_test_folder(), f"{filename}.{extension}")
        image.save(path)


def compare_images(lhs: Image.Image, rhs: Image.Image) -> bool:
    diff = ImageChops.difference(lhs, rhs)
    if diff.getbbox():
        return False
    return True


def test_vflip_positive(image, base_positive_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.VFLIP_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, base_positive_property)
    assert compare_images(new_image.transpose(Image.FLIP_TOP_BOTTOM), image)
    assert new_image.size == image.size
    save_image(new_image, test_vflip_negative.__name__)


def test_vflip_negative(image, base_negative_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.VFLIP_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, base_negative_property)
    assert new_image == image


def test_hflip_positive(image, base_positive_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.HFLIP_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, base_positive_property)
    assert compare_images(new_image.transpose(Image.FLIP_LEFT_RIGHT), image)
    assert new_image.size == image.size
    save_image(new_image, test_hflip_positive.__name__)


def test_hflip_negative(image, base_negative_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.HFLIP_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, base_negative_property)
    assert new_image == image


def test_shuffle_positive(image, base_positive_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.CHANNEL_SHUFFLE_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, base_positive_property)
    assert new_image.size == image.size
    save_image(new_image, test_shuffle_positive.__name__)


def test_shuffle_negative(image, base_negative_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.CHANNEL_SHUFFLE_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, base_negative_property)
    assert new_image == image


def test_grayscale(image, base_positive_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.GRAYSCALE_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, base_positive_property)
    assert new_image.size == image.size
    save_image(new_image, test_grayscale.__name__)


def test_noise(image, noise_value_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.NOISE_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, noise_value_property)
    assert new_image.size == image.size
    save_image(new_image, test_noise.__name__)


def test_scale(image, scale_value_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.SCALE_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, scale_value_property)
    assert new_image.size[0] == 2 * image.size[0]
    assert new_image.size[1] == 2 * image.size[1]
    save_image(new_image, test_scale.__name__)


def test_brightness(image, brightness_value_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.BRIGHTNESS_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, brightness_value_property)
    assert new_image.size == image.size
    save_image(new_image, test_brightness.__name__)


def test_contrast(image, contrast_value_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.CONTRAST_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, contrast_value_property)
    assert new_image.size == image.size
    save_image(new_image, test_contrast.__name__)


def test_color(image, color_value_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.COLOR_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, color_value_property)
    assert new_image.size == image.size
    save_image(new_image, test_color.__name__)


def test_sharpness(image, sharpness_value_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.SHARPNESS_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, sharpness_value_property)
    assert new_image.size == image.size
    save_image(new_image, test_sharpness.__name__)


def test_rotate(image, rotate_value_property):
    aug_feature = augmentation._AUGMENTATIONS[constants.ROTATE_AUGMENTATION_NAME]
    new_image = aug_feature.apply(image, rotate_value_property)
    assert new_image.size == (371, 371)
    save_image(image, "original")
    save_image(new_image, test_rotate.__name__)
