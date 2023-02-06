from os import environ, path, mkdir, unlink, listdir
from os.path import join as pj
from library.python import resource
from shutil import rmtree
import logging
import subprocess


class FirefoxBrowser(object):
    NAME = 'firefox'
    DEVICE = ['desktop', 'iphone', 'android']
    PROFILE_ARC = 'firefox-profile.tar'
    PROFILE_NAME = 'ulitka'

    def __init__(self, context):
        self.context = context
        self.proc = None

    @staticmethod
    def _install_profile():
        home = environ['HOME']
        firefox_dir = path.join(home, '.mozilla', 'firefox')
        subprocess.call(['tar', 'xf', path.join(home, FirefoxBrowser.PROFILE_ARC), '-C', firefox_dir])
        profile_dir = path.join(firefox_dir, 'ulitka')
        for fname in ['cert9.db', 'key4.db']:
            open(path.join(profile_dir, fname), 'w').write(resource.find('/nssdb/{}'.format(fname)))

    @staticmethod
    def install(context):
        home = environ['HOME']
        mozilla_dir = path.join(home, '.mozilla')
        if not path.exists(mozilla_dir):
            mkdir(mozilla_dir)
        firefox_dir = path.join(mozilla_dir, 'firefox')
        if not path.exists(firefox_dir):
            mkdir(firefox_dir)
        profile_arc = path.join(home, FirefoxBrowser.PROFILE_ARC)
        open(profile_arc, 'w').write(resource.find('/firefox-profile.tar'))
        FirefoxBrowser._install_profile()
        open(path.join(firefox_dir, 'install.ini'), 'w').write('''
            [4F96D1932A9F858E]
            Default=ulitka
            Locked=1''')
        open(path.join(firefox_dir, 'profiles.ini'), 'w').write('''
            [Install4F96D1932A9F858E]
            Default=ulitka
            Locked=1

            [Profile0]
            Name=ulitka
            IsRelative=1
            Path={}'''.format(FirefoxBrowser.PROFILE_NAME))

    def _clear_cache(self):
        FirefoxBrowser._install_profile()
        home = environ['HOME']
        cache_dir = path.join(home, '.cache', 'mozilla', 'firefox')
        if path.exists(cache_dir):
            rmtree(cache_dir)

    def _device_emu_config(self, device):
        home = environ['HOME']
        ua = self.context.USER_AGENT.get(device)
        script = []
        if ua is not None:
            script.append('user_pref("general.useragent.override", "{}");'.format(ua))
        if device == 'iphone':
            script.append('user_pref("media.webm.enabled", false);')
            script.append('user_pref("media.mediasource.enabled", false);')
        if script:
            user_js = path.join(home, '.mozilla', 'firefox', FirefoxBrowser.PROFILE_NAME, 'user.js')
            open(user_js, 'w').write('\n'.join(script))

    def open(self, url, device, cookies):
        if device not in self.DEVICE:
            raise Exception('firefox: unsupported device {}'.format(device))
        assert self.proc is None, 'open: child is already exist'
        self._clear_cache()
        self._device_emu_config(device)
        cmd = ['firefox', '-private', url]
        env = environ.copy()
        if self.context.proxy_port:
            env['http_proxy'] = 'http://localhost:{}'.format(self.context.proxy_port)
            env['https_proxy'] = 'http://localhost:{}'.format(self.context.proxy_port)
        logging.info('Opening browser firefox with command "{}"'.format(' '.join(cmd)))
        self.proc = subprocess.Popen(cmd, env=env)

    def is_running(self):
        return self.proc is not None

    def close(self):
        assert self.proc is not None, 'close: no child'
        self.proc.terminate()
        self.proc.wait()
        self.proc = None
        tmpdir = environ.get('TMPDIR', '/tmp')
        trash = filter(lambda v: v.startswith('tmpaddon'), listdir(tmpdir))
        logging.info('clearing firefox tmp {}'.format(trash))
        for fname in trash:
            unlink(pj(tmpdir, fname))
