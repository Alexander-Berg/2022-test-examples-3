import mock
from selenium import webdriver

from library.python import resource

from travel.avia.revise.extractor.report import DefaultScreenShotMaker


def test_image_conversion():
    driver = mock.create_autospec(webdriver.Remote)
    driver.get_screenshot_as_png.return_value = resource.find('/sample.png')
    actual = DefaultScreenShotMaker(driver=driver).make('some_name')
    driver.get_screenshot_as_png.assert_called_once()
    assert actual.error is None
    assert actual.name == 'some_name'
    expected = resource.find('/sample.jpg')
    assert actual.image == expected
