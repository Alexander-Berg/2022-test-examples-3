from selenium.webdriver import Chrome, ChromeOptions
from selenium.webdriver.common.by import By
from os import environ
from time import sleep


class ChromiumWebDriver():
    NAME = 'chromedriver'
    DEVICE = ['desktop', 'android', 'iphone']

    def __init__(self, context):
        self.context = context
        self.driver = None
        self.options = ChromeOptions()
        self.options.add_argument('--incognito')
        self.options.add_argument('--start-fullscreen')
        if context.proxy_port:
            self.options.add_argument('--proxy-server=localhost:{}'.format(context.proxy_port))
            self.options.add_argument('--ignore-certificate-errors')
        if environ.get('SNAIL_DOCKER'):
            self.options.add_argument('--no-sandbox')

    @staticmethod
    def install(context):
        pass

    def open(self, url, device, cookies):
        if device == 'android':
            mobile_emu = {'deviceName': 'Pixel 2 XL'}
            self.options.add_experimental_option('mobileEmulation', mobile_emu)
        self.driver = Chrome(chrome_options=self.options)
        self.driver.get(url)
        if cookies:
            self._set_cookie(cookies)
            self.driver.get(url)  # open 2nd time

    def _set_cookie(self, cookies):
        vec = cookies.split('; ')
        for item in vec:
            pos = item.find('=')
            if pos != -1:
                self.driver.add_cookie({'name': item[:pos], 'value': item[pos + 1:]})

    def execute_script(self, text, timeout):
        if not self.driver:
            raise Exception('Chrome not initialized')
        self.driver.execute_script(text)
        for i in range(timeout):
            try:
                log = self.driver.find_element(By.ID, 'xsnailresult')
                if log:
                    return log.text
            except:
                sleep(1)

    def close(self):
        if self.driver is None:
            return
        self.driver.quit()
        self.driver = None

    def is_running(self):
        return self.driver is not None
