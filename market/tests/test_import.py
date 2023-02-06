from market.robotics.cv.library.py.image_loader import image_loader


def test_import():
    loader = image_loader.ImageLoader()
    assert hasattr(loader, 'load')
