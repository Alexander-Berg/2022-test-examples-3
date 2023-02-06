import re
import urllib2

__author__ = 'aokhotin'


class SerpDataHtml(object):
    '''
        takes SERP
        returns dict with list of urls and reqid
    '''

    def __init__(self, data):
        self.urls = self._get_urls(data)
        self.reqid = self._get_reqid(data)

    def _get_reqid(self, data):
        try:
            reqid = re.search(re.compile(r'(?<=\"reqid\":\").*?(?=\")', re.S), data).group()
        except:
            reqid = 'UNKNOWN_ID'
        return reqid

    def _get_urls(self, data):
        regex = re.compile(
            r'''(?:
                    <a\s+class=" 				# Search for HTML link tag
                    (?:
                        [^"]*?       			# With any list of classes,
                        \s(?:serp-item__title-link|organic__url) # organic url class
                    )
                    (?:
                        [^"]*?"					# and any list of classes followed.
                    )
                    [^>]*?						# Then skip all attributes before 'href',
                )
                href="(
                    [^"]+						# and get link from href.
                )"
                [^>]*?							# Skip all text to the end of tag
                >(
                    .*?							# and get the tag body including all nested tags. NOTE: USE ONLY LAZY REGEX HERE
                )
                </a>							# stop at close tag.
            ''', re.MULTILINE | re.VERBOSE)
        links = regex.findall(data)
        urls = [urllib2.unquote(url.replace('//h.yandex.net/?', '')).strip().lower() for url, _ in links]
        if urls:
            return urls

        # old style serp
        def safe_re_search(pattern, text):
            if not text:
                # logging.warning('attempted search on empty text') TODO: what we should to do in this case
                return ''
            res = re.search(pattern, text)
            if res:
                return res.group()
            else:
                # logging.error('counldn\'t find {0} pattern in text:'.format(pattern)) TODO: what we should to do in this case
                # logging.error(text) TODO: what we should to do in this case
                return ''

        # get list items - whole html code for each result item
        # only numbered items are accounted for
        pattern_li = re.compile(r'<li class="b-serp-item.*?/li>', re.S)
        result_items = filter(lambda item: 'b-serp-item__number' in item,
                              re.findall(pattern_li, data))

        # get h2 code of each item
        pattern_h2 = re.compile(r'<h2.*?/h2>', re.S)
        result_items = [safe_re_search(pattern_h2, item) for item in result_items]
        # get urls
        pattern_href = re.compile(r'href=".*?"', re.S)
        url_items = [safe_re_search(pattern_href, item) for item in result_items]
        url_items = map(lambda x: x.replace('href=', '').strip('\"'),
                        url_items)
        return [url.lower() for url in url_items]
