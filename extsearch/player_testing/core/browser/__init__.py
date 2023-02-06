import logging
from time import sleep
from .chromium import ChromiumBrowser
from .chromedriver import ChromiumWebDriver
from .firefox import FirefoxBrowser


class Browser(object):
    BACKEND = {
        ChromiumBrowser.NAME: ChromiumBrowser,
        ChromiumWebDriver.NAME: ChromiumWebDriver,
        FirefoxBrowser.NAME: FirefoxBrowser
    }

    USER_AGENT = {
        'iphone': 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.2 Mobile/15E148 Safari/604.1',
        'android': 'Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.96 Mobile Safari/537.36'
    }

    def __init__(self, display, input_device, crop_tool, proxy_port):
        self.display = display
        self.input_device = input_device
        self.proxy_port = proxy_port
        self.impl = None
        self.crop_tool = crop_tool
        self.crop_params = {}
        self.crop_box = None

    def install(self):
        for back in self.BACKEND.itervalues():
            back.install(self)

    def calibrate(self):
        pass
        # for name, back in self.BACKEND.iteritems():
        #    for device in back.DEVICE:
        #        self._calibrate_viewport(name, device)

    def _calibrate_viewport(self, family, device):
        self.open(self.crop_tool.get_url(), family=family, device=device)
        sleep(10)
        try:
            cb = self.crop_tool.calc_viewport()
            self.crop_params[(family, device)] = cb
            logging.info('crop_box: {} {}: {}'.format(family, device, cb))
        finally:
            self.close()

    def get_viewport(self):
        return self.crop_box

    def open(self, url, family=ChromiumBrowser.NAME, device=ChromiumBrowser.DEVICE[0], cookies=None):
        assert self.impl is None, 'open: already running'
        if family not in self.BACKEND:
            raise Exception('open: unknown browser family {}'.format(family))
        self.impl = self.BACKEND[family](self)
        try:
            self.impl.open(url, device, cookies)
        except Exception as e:
            logging.info('open: {}'.format(e))
            self.impl = None
            raise e
        self.crop_box = self.crop_params.get((family, device))

    def execute_script(self, text, timeout):
        return self.impl.execute_script(text, timeout)

    def is_running(self):
        return self.impl is not None and self.impl.is_running()

    def close(self):
        if self.impl is None:
            logging.info('close: browser impl is None')
            return
        try:
            self.impl.close()
        finally:
            self.impl = None
