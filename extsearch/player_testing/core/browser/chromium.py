from os import environ, path, mkdir, unlink
from library.python import resource
from time import sleep
from shutil import rmtree
import logging
import subprocess


class ChromiumBrowser(object):
    NAME = 'chromium'
    DEVICE = ['desktop', 'iphone', 'android', 'iphone:devtools']

    def __init__(self, context):
        self.context = context
        self.proc = None

    @staticmethod
    def install(context):
        home = environ['HOME']
        pki_dir = path.join(home, '.pki')
        if not path.exists(pki_dir):
            mkdir(pki_dir)
        nssdb_dir = path.join(pki_dir, 'nssdb')
        if not path.exists(nssdb_dir):
            mkdir(nssdb_dir)
        for fname in ['cert9.db', 'key4.db', 'pkcs11.txt']:
            open(path.join(nssdb_dir, fname), 'w').write(resource.find('/nssdb/{}'.format(fname)))
        tmp_arc = path.join(home, 'chromium-profiles.tar')
        profiles_dir = path.join(home, 'chromium')
        if path.exists(profiles_dir):
            rmtree(profiles_dir)
        open(tmp_arc, 'w').write(resource.find('/chromium-profiles.tar'))
        subprocess.call(['tar', 'xf', tmp_arc, '-C', home])
        unlink(tmp_arc)

    def open(self, url, device, cookies):
        if device not in self.DEVICE:
            raise Exception('chromium: unsupported device {}'.format(device))
        assert self.proc is None, 'open: child is already exist'
        home = environ['HOME']
        with_devtools = device.endswith('devtools')
        user_agent = self.context.USER_AGENT.get(device)
        cmd = ['chromium-browser', '--enable-logging=stderr', '--incognito', '--start-maximized', '--disable-feature=TranslateUI']
        if environ.get('SNAIL_DOCKER'):
            cmd.append('--no-sandbox')
        if self.context.proxy_port:
            cmd.append('--proxy-server=localhost:{}'.format(self.context.proxy_port))
        if user_agent is not None:
            cmd.append('--user-agent={}'.format(user_agent))
        if with_devtools:
            cmd.append('--auto-open-devtools-for-tabs')
            cmd.append('--user-data-dir={}'.format(path.join(home, 'chromium', device.split(':')[0])))
            cmd.append(url)
        else:
            cmd.append('--app={}'.format(url))
        logging.info('Opening browser chromium with command "{}"'.format(' '.join(cmd)))
        self.proc = subprocess.Popen(cmd)
        if with_devtools:
            sleep(5)
            self.context.input_device.press_key('shift+F5').execute()

    def is_running(self):
        return self.proc is not None

    def close(self):
        assert self.proc is not None, 'close: no child'
        self.proc.terminate()
        self.proc.wait()
        self.proc = None
