# -*- coding: utf-8 -*-

from contextlib import contextmanager
import calendar
import datetime
import dateutil.parser
import os
import shutil
import six
import stat
import subprocess
import tempfile
import unittest

from mock import patch
from parameterized import parameterized, param

from market.idx.pylibrary.downloader.downloader.download import (
    ARGS,
    create_downloader,
    DEFAULT_TIMEOUT,
    DownloaderWithFallback,
    DownloadError,
    FileStubDownloader,
    PycURLDownloader,
    ZoraClientError,
    ZoraDownloader,
)
from market.idx.pylibrary.downloader.downloader.return_codes import RETCODE
from market.idx.pylibrary.downloader.downloader.url_relative_resolution import UrlRelativeResolution
from market.idx.pylibrary.downloader.downloader.validator import LightUriValidator

validator = LightUriValidator()


class UriTester(unittest.TestCase):
    def test_bad(self):

        self.assertFalse(validator.validate("ftp://www.yandex.ru/chargo?id=888"))
        self.assertFalse(validator.validate("www.yandex.ru"))

        self.assertFalse(validator.validate(""))
        self.assertFalse(validator.validate("    "))
        self.assertFalse(validator.validate("1"))
        self.assertFalse(validator.validate("http://1"))
        self.assertFalse(validator.validate("http://example"))

        self.assertFalse(validator.validate("http;//www.yandex.ru"))
        self.assertFalse(validator.validate("htt://www.yandex.ru"))
        self.assertFalse(validator.validate("http://yandex"))
        self.assertFalse(validator.validate("http://.yandex.ru"))

        self.assertFalse(validator.validate("""http://www.
        yandex.
        ru"""))
        self.assertFalse(validator.validate("http://www.\x0ayandex.ru/"))
        self.assertFalse(validator.validate("http://www.\x0dyandex.ru/"))
        self.assertFalse(validator.validate(r'http://www.yandex.ru/\n'))
        self.assertFalse(validator.validate(r'http://www.yandex.ru/\r'))
        self.assertFalse(validator.validate(r'http://www.yandex.ru/\x0a'))
        self.assertFalse(validator.validate(r'http://www.yandex.ru/\x0d'))

        self.assertFalse(validator.validate("http://www\x00yandex\x00ru"))
        self.assertFalse(validator.validate(r'http://www\x00yandex\x00ru'))
        self.assertFalse(validator.validate("http://www.texpales.ru/index.php?\
option=com_virtuemart&amp;page=shop.market_export&amp;market=export_for_yandex&amp;\
dwl=0&amp;vmcchk=1&amp;Itemid=64\r\n\r\n'"))

        # 512 characters
        self.assertFalse(validator.validate("http://www.longurl.ru/offers?\
id=98273984729837492837492837492834792837492384792834792384792384792384792384792834729384729384792384792384792384729\
384729384729834792834792384792384729387398472984729384742389018934701328470183247012834701328470182347012834701238470\
1283470128397401283740123847012834701239847012387401238740128374012839470128347012387401238470123874012384703129847012\
398470123840124870123847012348971320498721071430712409812374021983470128347021389470213847023847021384701238974012398470213897403780"))
        # 513 characters
        self.assertFalse(validator.validate("http://www.longurl.ru/offers?\
id=98273984729837492837492837492834792837492384792834792384792384792384792384792834729384729384792384792384792384729384\
729384729834792834792384792384729387398472984729384742389018934701328470183247012834701328470182347012834701238470128347\
0128397401283740123847012834701239847012387401238740128374012839470128347012387401238470123874012384703129847012398470123\
8401248701238470123489713204987210714307124098123740219834701283470213894702138470238470213847012389740123984702138974037800"))

    def test_good(self):

        # MARKETINDEXER-28645 - Не валидировать порт в url фида
        self.assertTrue(validator.validate("http://example.net:2013"))
        self.assertTrue(validator.validate("https://example.net:80"))
        self.assertTrue(validator.validate("https://example.net:88"))
        self.assertTrue(validator.validate("http://[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:2013/offer?id=9"))
        self.assertTrue(validator.validate("https://[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:2013/offer?id=9"))

        self.assertTrue(validator.validate("http://www.yandex.ru"))
        self.assertTrue(validator.validate("http://ввв.яндекс.рф"))
        self.assertTrue(validator.validate("http://yandex.ru"))
        self.assertTrue(validator.validate("http://ya.ru"))
        self.assertTrue(validator.validate("http://www.market.yandex.ru/"))
        self.assertTrue(validator.validate("https://www.market.yandex.ru/"))
        self.assertTrue(validator.validate("http://example.net:88"))
        self.assertTrue(validator.validate("http://example.net:99"))
        self.assertTrue(validator.validate("http://cs-elliptics.yandex.net:88/offer?id=398742"))
        self.assertTrue(validator.validate("http://cs-elliptics.yandex.net:99/offer?id=398742"))
        self.assertTrue(validator.validate("http://example.net:80/hello_world"))
        self.assertTrue(validator.validate("https://example.net:443/hellow_security_world"))
        self.assertTrue(validator.validate("http://www.yandex.uk"))
        self.assertTrue(validator.validate("http://www.yandex.aero"))
        self.assertTrue(validator.validate("http://192.168.1.1/offer?id=5"))

        self.assertTrue(validator.validate("http://ya.ru/parameter?id={value}"))
        self.assertTrue(validator.validate("http://milinsky.tools.mgcom.ru/feeds/output.cgi?source=svyaznoy_yandex_yaroslavl&\
set_city_id=201&set_utm_source=yandexmarketya&set_utm_medium=cpc&set_utm_campaign=pricelist&\
set_utm_content=[id]&set_utm_term=[svyaznoy_utm_term]&where_price=>100"))
        self.assertTrue(validator.validate(r"https://user:password\@svn.yandex.ru/market/market/trunk/testshops/testdontdelete.xml"))
        self.assertTrue(validator.validate("http://[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]/offer?id=9"))
        self.assertTrue(validator.validate("http://[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:88/offer?id=9"))
        self.assertTrue(validator.validate("https://[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]/offer?id=9"))
        self.assertTrue(validator.validate("https://[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:443/offer?id=9"))
        self.assertTrue(validator.validate("https://yuri:manushkin@[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:443/offer?id=9"))
        self.assertTrue(validator.validate("https://dima@[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]:443/offer?id=9"))
        self.assertTrue(validator.validate("http://www.texpales.ru/index.php?" +
                                           "option=com_virtuemart&amp;page=shop.market_export&amp;" +
                                           "market=export_for_yandex&amp;dwl=0&amp;vmcchk=1&amp;" +
                                           r"Itemid=64''''{my_[lovely]_p.a.r.a.m.e.r.t.e.r!!!!!\@:~_-+***}'''' "))

        # 511 characters
        self.assertTrue(validator.validate("http://www.longurl.ru/offers?\
id=982739847298374928374928374928347928374923847928347923847923847923847923847928347293847293847923847923847923847293847\
2938472983479283479238479238472938739847298472938474238901893470132847018324701283470132847018234701283470123847012834701\
28397401283740123847012834701239847012387401238740128374012839470128347012387401238470123874012384703129847012398470123840\
12487012384701234897132049872107143071240981237402198347012834702138947021384702384702138470123897401239847021389740378"))
        self.assertTrue(validator.validate('http://eurolabpribor.ru/?mode=yml&v=4'))
        self.assertTrue(validator.validate('HTTP://eurolabpribor.ru/'))
        self.assertTrue(validator.validate('HtTp://EuRoLaprRiBor.RU'))

        # it's very bad, but good
        self.assertTrue(validator.validate(r"http://{[s][o][n][y][a]}:v.o.v.c.h.e.n.k.o\@w{w}w.ya%20n[d]ex.ru:80/''''{my_[lovely]_p.a.r.a.m.e.r.t.e.r!!!!!\@:~_-+***}''''"))


class UrlRelativeResolutionTest(unittest.TestCase):
    """
    Special tests from rfc3986: http://tools.ietf.org/html/rfc3986#section-5.4
    """

    def setUp(self):
        self.url_relative_resolution = UrlRelativeResolution()

    def _equal(self, relative_url, result_url, base_url='http://a/b/c/d;p?q'):
        target_url = self.url_relative_resolution.get_target_url(base_url, relative_url)
        self.assertEqual(target_url, result_url)

    def test_normal_examples(self):
        self._equal("g:h", "g:h")
        self._equal("g", "http://a/b/c/g")
        self._equal("./g", "http://a/b/c/g")
        self._equal("g/", "http://a/b/c/g/")
        self._equal("/g", "http://a/g")
        self._equal("//g", "http://g")
        self._equal("?y", "http://a/b/c/d;p?y")
        self._equal("g?y", "http://a/b/c/g?y")
        self._equal("#s", "http://a/b/c/d;p?q#s")
        self._equal("g#s", "http://a/b/c/g#s")
        self._equal("g?y#s", "http://a/b/c/g?y#s")
        self._equal(";x", "http://a/b/c/;x")
        self._equal("g;x", "http://a/b/c/g;x")
        self._equal("g;x?y#s", "http://a/b/c/g;x?y#s")
        self._equal("", "http://a/b/c/d;p?q")
        self._equal(".", "http://a/b/c/")
        self._equal("./", "http://a/b/c/")
        self._equal("..", "http://a/b/")
        self._equal("../", "http://a/b/")
        self._equal("../g", "http://a/b/g")
        self._equal("../..", "http://a/")
        self._equal("../../", "http://a/")
        self._equal("../../g", "http://a/g")

    def test_abnomal_examples(self):
        self._equal("../../../g", "http://a/g")
        self._equal("../../../../g", "http://a/g")
        self._equal("/./g", "http://a/g")
        self._equal("/../g", "http://a/g")
        self._equal("g.", "http://a/b/c/g.")
        self._equal(".g", "http://a/b/c/.g")
        self._equal("g..", "http://a/b/c/g..")
        self._equal("..g", "http://a/b/c/..g")
        self._equal("./../g", "http://a/b/g")
        self._equal("./g/.", "http://a/b/c/g/")
        self._equal("g/./h", "http://a/b/c/g/h")
        self._equal("g/../h", "http://a/b/c/h")
        self._equal("g;x=1/./y", "http://a/b/c/g;x=1/y")
        self._equal("g;x=1/../y", "http://a/b/c/y")
        self._equal("g?y/./x", "http://a/b/c/g?y/./x")
        self._equal("g?y/../x", "http://a/b/c/g?y/../x")
        self._equal("g#s/./x", "http://a/b/c/g#s/./x")
        self._equal("g#s/../x", "http://a/b/c/g#s/../x")

    def test_real_shops_from_testing(self):
        self._equal('/published/SC/html/scripts/get_file.php/',
                    'http://quban.ru/published/SC/html/scripts/get_file.php/',
                    'http://quban.ru/published/SC/html/scripts/get_file.php?getFileParam=R2V0WWFuZGV4')
        self._equal('/internet-magazin/yml/2',
                    'http://videoaccent.ru/internet-magazin/yml/2',
                    'http://videoaccent.ru/internet-magazin?mode=yml&v=2')
        self._equal('/?mode=yml&v=3',
                    'http://play-way.ru/?mode=yml&v=3',
                    'http://play-way.ru/magazin?mode=yml&v=3')
        self._equal('index.php?route=feed/yandex_market',
                    'http://www.shop2game.ru/index.php?route=feed/yandex_market',
                    'http://www.shop2game.ru/yandex.php')
        self._equal('./',
                    'http://www.datasales.ru/',
                    'http://www.datasales.ru/market_09-08-2013.xml')
        self._equal('/magazin/yml/4',
                    'http://torgsport.ru/magazin/yml/4',
                    'http://torgsport.ru/?mode=yml&v=4')
        self._equal('/magazin/yml/4',
                    'http://neoshop.pro/magazin/yml/4',
                    'http://neoshop.pro/magazin?mode=yml&v=4')
        self._equal('/magazin/yml/2',
                    'http://lafarg-market.ru/magazin/yml/2',
                    'http://lafarg-market.ru/magazin?mode=yml&v=2')
        self._equal('/market.xml/',
                    'http://malyshastic.ru/market.xml/',
                    'http://malyshastic.ru/market.xml')
        self._equal('/shop/yml/4',
                    'http://eurolabpribor.ru/shop/yml/4',
                    'http://eurolabpribor.ru/?mode=yml&v=4')
        self._equal('/magazin/yml/2',
                    'http://sappo.ru/magazin/yml/2',
                    'http://sappo.ru/magazin?mode=yml&v=2')
        self._equal('/?mode=yml&v=2',
                    'http://elektrots.ru/?mode=yml&v=2',
                    'http://elektrots.ru/magazin?mode=yml&v=2')

    def test_absolute_url(self):
        self._equal('http://www.yandex.ru',
                    'http://www.yandex.ru',
                    'http://yandex.ru')
        self._equal('http://elektrots.su/magazin?mode=yml&v=2',
                    'http://elektrots.su/magazin?mode=yml&v=2',
                    'http://elektrots.ru/magazin?mode=yml&v=2')

    def test_remove_dot_segments(self):
        urr = self.url_relative_resolution
        self.assertEqual('', urr.remove_dot_segments(''))
        self.assertEqual('', urr.remove_dot_segments('.'))
        self.assertEqual('', urr.remove_dot_segments('..'))
        self.assertEqual('', urr.remove_dot_segments('././.'))
        self.assertEqual('', urr.remove_dot_segments('../../../'))
        self.assertEqual('', urr.remove_dot_segments('./../.././../.'))
        self.assertEqual('a/', urr.remove_dot_segments('../../../.././././././a/b/.././'))
        self.assertEqual('/a/', urr.remove_dot_segments('/../../../.././././././a/b/.././'))
        self.assertEqual('/a/b/c/', urr.remove_dot_segments('/a/./b/./c/./././d/../.'))
        self.assertEqual('/a/b/c', urr.remove_dot_segments('/../../../../a/b/././c'))


class ARGSTester(unittest.TestCase):
    def test_args1(self):
        args = ARGS("URL", "DEST", "HEADERS")
        self.assertEqual(args.verbose, 0)
        self.assertEqual(args.timeout, DEFAULT_TIMEOUT)
        self.assertTrue(args.user is None)
        self.assertTrue(args.passwd is None)
        self.assertTrue(args.http_headers is None)
        self.assertFalse(args.force)
        self.assertEqual(args.retry_count, 3)

    def test_args2(self):
        class OA(object):
            pass
        oArgs = OA()
        oArgs.url = 'URL'
        oArgs.destination = 'URL DEST'
        oArgs.header_answer_destination = "URL HEAD OTHER DEST"
        oArgs.http_headers = ['one', 'two']
        args = ARGS.makeARGS(oArgs)

        self.assertEqual(args.url, oArgs.url)
        self.assertEqual(args.destination, oArgs.destination)
        self.assertEqual(args.header_answer_destination, oArgs.header_answer_destination)
        self.assertTrue(isinstance(args.http_headers, list))
        self.assertEqual(args.verbose, 0)
        self.assertTrue(args.user is None)
        self.assertFalse(args.force)
        self.assertEqual(args.retry_count, 3)

    def test_args3(self):
        class OA(object):
            pass
        oArgs = OA()
        oArgs.url = 'URL'
        oArgs.header_answer_destination = "URL HEAD OTHER DEST"

        self.assertRaises(TypeError, ARGS.makeARGS, oArgs)

    def test_password_is_masked_in_str(self):
        args = ARGS('http://url.ru', 'dest', 'headers', user='user', passwd='supersecret')
        self.assertNotIn('supersecret', str(args))


class DownloaderTester(unittest.TestCase):
    def setUp(self):
        self.downloader = create_downloader()
        self.tmpdir = "./temp"
        if os.path.exists(self.tmpdir):
            shutil.rmtree(self.tmpdir, True)
        os.mkdir(self.tmpdir)

    def tearDown(self):
        shutil.rmtree(self.tmpdir, True)

    def _make_dir(self, directory_name):
        current_dir = os.path.join(self.tmpdir, directory_name)
        os.mkdir(current_dir)
        return current_dir

    def _get_args(self, url, current_dir_name):
        current_dir = self._make_dir(current_dir_name)
        return ARGS(url, os.path.join(current_dir, "downloaded.file"), os.path.join(current_dir, "headers.xml"), 0)

    def _assert_return_values(self, retcode, args):
        self.assertEqual(0, retcode)
        self.assertTrue(0 != os.stat(args.destination).st_size)
        self.assertTrue(0 != os.stat(args.header_answer_destination).st_size)
        self.assertTrue(-1 != open(args.header_answer_destination, "r").read().find("Status: 200"))

    def test_download_general(self):
        args = self._get_args("https://ya.ru", "general")
        retcode, msg = self.downloader.download(args)
        print(msg)
        self._assert_return_values(retcode, args)

    def test_download_general_ru(self):
        args = self._get_args("http://яндекс.рф", "general_ru")
        args.set_force()
        retcode, msg = self.downloader.download(args)
        print(msg)
        self._assert_return_values(retcode, args)

    def test_download_redirect(self):
        args = self._get_args("https://ya.ru", "redirect")
        args.add_http_header('If-Modified-Since: Mon, 12 Aug 2013 15:00:00 GMT')
        retcode, msg = self.downloader.download(args)
        print(msg)
        self._assert_return_values(retcode, args)

    def test_download_force(self):
        if os.path.exists("/bin/bash"):
            args = self._get_args("file://localhost/bin/bash", "force_bash")
            args.set_force()
            retcode, msg = self.downloader.download(args)
            print(msg)
            self.assertEqual(0, retcode)

        args = self._get_args('https://ya.ru', 'force_and_redirect')
        retcode, msg = self.downloader.download(args)
        print(msg)
        self._assert_return_values(retcode, args)

    def test_cannot_download(self):
        args = self._get_args("http://0.ru", 'cannot_download')
        retcode, msg = self.downloader.download(args)
        print(msg)
        self.assertEqual(RETCODE.ERROR_DOWNLOADING_FILE, retcode)


class TestDownloadersFactory(unittest.TestCase):
    def test_pycurl_downloader(self):
        self.assertIs(type(create_downloader()), PycURLDownloader)
        self.assertIs(type(create_downloader(False)), PycURLDownloader)
        with tempfile.NamedTemporaryFile() as f:  # Almost Zora Client :)
            os.chmod(f.name, os.stat(f.name).st_mode | stat.S_IEXEC)
            self.assertIs(
                type(create_downloader(False, f.name)),
                PycURLDownloader
            )

    def test_zora_downloader(self):
        self.assertRaises(ZoraClientError, create_downloader, True, None)
        for path in ('/nowhere', '/etc'):
            self.assertRaises(ZoraClientError, create_downloader, True, path)
        with tempfile.NamedTemporaryFile() as f:
            self.assertRaises(ZoraClientError, create_downloader, True, f.name)
        with tempfile.NamedTemporaryFile() as f:  # Almost Zora Client :)
            os.chmod(f.name, os.stat(f.name).st_mode | stat.S_IEXEC)
            self.assertIs(
                type(create_downloader(True, f.name)),
                ZoraDownloader
            )

    def test_downloader_with_fallback(self):
        with tempfile.NamedTemporaryFile() as f:  # Almost Zora Client :)
            os.chmod(f.name, os.stat(f.name).st_mode | stat.S_IEXEC)
            self.assertIs(
                type(create_downloader(True, f.name, True)),
                DownloaderWithFallback
            )


class TestZoraDownloader(unittest.TestCase):

    def setUp(self):
        self._fake_zora_client = tempfile.NamedTemporaryFile()
        os.chmod(
            self._fake_zora_client.name,
            os.stat(self._fake_zora_client.name).st_mode | stat.S_IEXEC
        )
        self._downloader = create_downloader(True, self._fake_zora_client.name)

    def test_zora_cmdline_generation(self):
        args = ['http://ya.ru', '/dev/null', '/dev/null', 0, 42]
        cmdline = (
            '{0} fetch -e -f0 --with-url=no --input=- --request-type=o --source=market-offersrobot '
            '--output=-"{2}" --headers-filename="{2}.tmp" '
            '--send-timeout={5} --total-timeout={5} '
            '--use-range-download --range-download-chunk-size=10485759 '
            '--redirect-mode=last --max-redirects=10 --max-rejects=100'
        ).format(
            self._fake_zora_client.name, *args
        )
        self.assertEqual(self._downloader.cmdline(ARGS(*args)), cmdline)

        stdin = '{0}'.format(*args)

        auth = ['root', 'alpine']
        args += auth
        stdin += ' login={0} password={1}'.format(*auth)
        self.assertEqual(self._downloader.cmdline(ARGS(*args)), cmdline)

        args.append(['Accept: text/xml'])
        self.assertEqual(self._downloader.cmdline(ARGS(*args)), cmdline)
        args[-1].append('If-Modified-Since: ')
        with self.assertRaises(ZoraClientError):
            self._downloader.stdin(ARGS(*args))
        mtime = 'Wed, 19 Oct 2005 10:50:00 GMT'
        dt = dateutil.parser.parse(mtime)
        args[-1][-1] += mtime
        stdin += ' ifmfsince={0}'.format(int(calendar.timegm(dt.utctimetuple())))
        self.assertEqual(self._downloader.stdin(ARGS(*(args))), stdin)
        args[-1].append('If-Modified-Since: {0}'.format(mtime))
        with self.assertRaises(ZoraClientError):
            self._downloader.stdin(ARGS(*args))

    def test_zora_ifmsince(self):
        url = 'http://ya.ru'
        mtime = 'Wed, 19 Oct 2005 10:50:00 +0000'
        headers = ['Accept: text/xml', 'If-Modified-Since: ' + mtime]
        args = ARGS(url=url, destination='', header_answer_destination='', http_headers=headers)

        dt = dateutil.parser.parse(mtime)
        stdin = url + ' ifmfsince={0}'.format(int(calendar.timegm(dt.utctimetuple())))
        self.assertEqual(self._downloader.stdin(args), stdin)

    def test_zora_invalid_ifmsince(self):
        url = 'http://ya.ru'
        mtime = 'Thu, 01 Jan 1970 00:00:00 GMT'
        headers = ['Accept: text/xml', 'If-Modified-Since: ' + mtime]
        args = ARGS(url=url, destination='', header_answer_destination='', http_headers=headers)

        self.assertEqual(self._downloader.stdin(args), url)

    def test_zora_invalid_ifmsince2(self):
        url = 'http://ya.ru'
        mtime = 'Thu, 01 Jan 1970 00:00:01 GMT'
        headers = ['Accept: text/xml', 'If-Modified-Since: ' + mtime]
        args = ARGS(url=url, destination='', header_answer_destination='', http_headers=headers)

        dt = dateutil.parser.parse(mtime)
        stdin = url + ' ifmfsince={0}'.format(int(calendar.timegm(dt.utctimetuple())))
        self.assertEqual(self._downloader.stdin(args), stdin)

    def test_zora_invalid_ifmsince3(self):
        url = 'http://ya.ru'
        mtime = 'and today is only yesterday\'s tomorrow'
        headers = ['Accept: text/xml', 'If-Modified-Since: ' + mtime]
        args = ARGS(url=url, destination='', header_answer_destination='', http_headers=headers)

        with self.assertRaises(ZoraClientError):
            self._downloader.stdin(args)

    def test_zora_quote_space(self):
        url = 'http://ya.ru/ file'
        args = ARGS(url=url, destination='', header_answer_destination='')

        self.assertEqual(self._downloader.stdin(args), 'http://ya.ru/%20file')

    @parameterized.expand([
        param(
            status=None,
            effect=ZoraClientError('ZoraClient retcode != 0'),
            expected_code=RETCODE.ZORA_INTERNAL_ERROR
        ),
        param(
            status=None,
            effect=subprocess.TimeoutExpired('subprocess', 'Timeout', 'Expired'),
            expected_code=RETCODE.REQUEST_TIMED_OUT
        ),
        param(
            status=None,
            effect=DownloadError(RETCODE.ERROR_DOWNLOADING_FILE, 'Some errors in headers'),
            expected_code=RETCODE.ERROR_DOWNLOADING_FILE
        ),
        param(
            status=None,
            effect=DownloadError(RETCODE.ERROR_OPENING_HEADERS_FILE, 'Some errors in headers'),
            expected_code=RETCODE.ERROR_OPENING_HEADERS_FILE
        ),
        param(
            status=200,
            effect=None,
            expected_code=RETCODE.SUCCESSFULLY_DOWNLOADED
        ),
        param(
            status=100,
            effect=None,
            expected_code=RETCODE.BAD_SERVER_RESPONSE
        ),
        param(
            status=404,
            effect=None,
            expected_code=RETCODE.BAD_SERVER_RESPONSE
        ),
    ])
    def test_return_code(self, status, effect, expected_code):
        '''check that wrapped function return normal value
        '''
        do_download_mock = patch(
            'market.idx.pylibrary.downloader.downloader.download.ZoraDownloader._do_download',
            autospec=True,
            side_effect=effect
        ) if effect is not None else patch(
            'market.idx.pylibrary.downloader.downloader.download.ZoraDownloader._do_download',
            autospec=True,
            return_value=status  # код http из заголовков
        )
        with do_download_mock:
            with tempfile.NamedTemporaryFile() as f:
                args = ARGS(
                    url='http://ya.ru',
                    destination='',
                    header_answer_destination=f.name,
                    retry_count=0
                )
                retcode, errmsg = self._downloader.download(args)
                self.assertEqual(retcode, expected_code)

    def test_timezones(self):
        '''
        Check if convertation used in code is correct
        '''
        time_string = 'Wed, 19 Oct 2005 10:50:00 GMT'
        dt = dateutil.parser.parse(time_string)
        time_utc = int(calendar.timegm(dt.utctimetuple()))
        dt = datetime.datetime.utcfromtimestamp(time_utc)
        self.assertEqual(dt.strftime('%a, %d %b %Y %H:%M:%S GMT'), time_string)

    def test_headers_parser(self):
        # TODO: тестировать реализацию – неправильно.
        @contextmanager
        def response(content, cls=None):
            with tempfile.NamedTemporaryFile() as f:
                f.write(six.ensure_binary(content))
                f.flush()

                if cls is None:
                    yield f.name
                else:
                    with self.assertRaises(cls):
                        yield f.name

        with self.assertRaises(ZoraClientError):
            self._downloader._parse_response('/nowhere')
        with response('\n', ZoraClientError) as r:
            self._downloader._parse_response(r)
        with response('Lorem ipsum\nDolor sit amet\n', ZoraClientError) as r:
            self._downloader._parse_response(r)
        with response('Lorem ipsum\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 OK\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 OK OK\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 OK OK\n\nX-Yandex-Http-Code: 1000', ZoraClientError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 OK OK\n\nX-Yandex-Http-Code: qwerty', ZoraClientError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 -1 OK\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 99 OK\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 1000 OK\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 500 ERROR\nX-Yandex-Http-Code: 1032\n\n', ZoraClientError) as r:  # zora 500
            self._downloader._parse_response(r)
        with response('HTTP/1.1 1006 ERROR\nX-Yandex-Http-Code: 1006\n\n', DownloadError) as r:
            self._downloader._parse_response(r)
        with response('HTTP/1.1 200\n\n') as r:
            self._downloader._parse_response(r)
            headers = {'Status': '200'}
            self.assertEqual(self._downloader.response_headers, headers)
        with response('HTTP/1.1 200 OK\n\n') as r:
            self._downloader._parse_response(r)
            headers = {'Status': '200'}
            self.assertEqual(self._downloader.response_headers, headers)
        with response('HTTP/1.1 301 Moved Permanently\n\n') as r:
            self._downloader._parse_response(r)
            headers = {'Status': '301'}
            self.assertEqual(self._downloader.response_headers, headers)
        with response('HTTP/1.1 200 OK\nContent-Type: text/html\n\n') as r:
            self._downloader._parse_response(r)
            headers = {'Status': '200', 'Content-Type': 'text/html'}
            self.assertEqual(self._downloader.response_headers, headers)
        with response('HTTP/1.1 200 OK\nX-Yandex-Http-Code: 0\n\n') as r:
            self._downloader._parse_response(r)
            headers = {'Status': '200', 'X-Yandex-Http-Code': '0'}
            self.assertEqual(self._downloader.response_headers, headers)
        with response('HTTP/1.1 500 ERROR\nX-Yandex-Http-Code: 0\n\n') as r:  # real 500
            self._downloader._parse_response(r)
            headers = {'Status': '500', 'X-Yandex-Http-Code': '0'}
            self.assertEqual(self._downloader.response_headers, headers)


class TestFileStubDownloader(unittest.TestCase):
    def setUp(self):
        self.downloader = FileStubDownloader()
        self.tmpdir = "./temp"
        if os.path.exists(self.tmpdir):
            shutil.rmtree(self.tmpdir, True)
        os.mkdir(self.tmpdir)

    def tearDown(self):
        shutil.rmtree(self.tmpdir, True)

    def test_fake_download(self):
        src_file = os.path.join(self.tmpdir, 'something.xml')
        with open(src_file, 'w') as f:
            f.write('hello world\n')

        dst_file = os.path.join(self.tmpdir, 'downloaded.xml')
        args = ARGS(
            url='file://' + os.path.abspath(src_file),
            destination=dst_file,
            header_answer_destination=dst_file + '.download_status',
            verbose=1,
        )

        self.downloader.download(args)

        with open(dst_file, 'r') as f:
            res = f.read()
        self.assertEqual(res, 'hello world\n')

        with open(dst_file + '.download_status', 'r') as f:
            status = f.read()
        self.assertTrue('Status: 200' in status)


if '__main__' == __name__:
    unittest.main()
