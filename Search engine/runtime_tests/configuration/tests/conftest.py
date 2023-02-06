import json
from collections import defaultdict

import os
import pytest
import re


def pytest_addoption(parser):
    parser.addoption('-C', '--config', required=True, help='path to config')
    parser.addoption('-A', '--upper_dir', default=None, help='path to arkanavt')


@pytest.fixture(scope='session')
def config_file(request):
    return request.config.getoption('config')


class Config:
    def __repr__(self):
        try:
            return "<Config : {}>".format(str(self.location))
        except:
            return "<Config : {}>".format(str(self.config_path))

    def __init__(self, config_path):
        self.name = ''
        self.config_path = config_path
        self.options = dict()
        self.SearchSources = list()
        self.dns_list = list()
        self.comments = defaultdict(lambda: [])
        self.tags = list()

        if not os.path.exists(config_path):
            pytest.fail(msg='No Configuration Found', pytrace=False)

        if os.path.getsize(config_path) == 0:
            pytest.fail(msg='Configuration file exist but empty', pytrace=False)

        self.fp = open(config_path)
        self.sources = list()

    def parse_source_search(self):
        source = dict()
        # skip source comments
        while True:
            line = next(self.get_line())
            if not line.strip().startswith("#"):
                break
        source['ServerDescr'] = line[len('ServerDescr'):].strip()

        line = next(self.get_line())
        source['CgiSearchPrefix'] = line[len('CgiSearchPrefix'):].strip()

        line = next(self.get_line())
        source['Options'] = line[len('Options'):].strip()

        re_host = re.compile(r"^(.*:)//([A-Za-z0-9\-\.]+)(:[0-9]+)?(.*)$")
        for s in source['CgiSearchPrefix'].split():
            host = re.findall(re_host, s)[0][1]
            self.sources.append(host)

        self.SearchSources.append(source)

    def parse_dns_cache(self):
        line = next(self.get_line())
        dns_list = line[len('DNSCache'):].split()
        for dns in dns_list:
            host_name, host_ip = dns.strip().split(('='))
            self.dns_list.append((host_name, host_ip))

    def parse_tags(self):
        while True:
            line = next(self.get_line())
            if not line.startswith('#'):
                break
            else:
                tag = line[1:].strip()
                self.tags.append(tag)

    def parse_search_templates(self):
        line = next(self.get_line())
        while line and line != '</SearchPageTemplate>':
            line = next(self.get_line())

    def parse_option(self, line):
        option_name = line.split(' ', 1)[0]
        option_data = line.split(' ', 1)[1] if len(line.split(' ', 1)) > 1 else ''
        self.options[option_name] = option_data

    def parse_comment(self, line):
        sep = line.find(':')
        if sep <= 0: return
        self.comments[line[1:sep].strip()].append(line[sep + 1:].strip())

    def parse_collection(self):
        for line in self.get_line():
            if line.startswith('<SearchSource>'):
                self.parse_source_search()
            elif line.startswith('<DNSCache>'):
                self.parse_dns_cache()
            elif line.startswith('<SearchPageTemplate>'):
                self.parse_search_templates()
            elif line.startswith('# Tags:'):
                self.parse_tags()
            elif line.startswith('#'):
                self.parse_comment(line)
            else:
                self.parse_option(line)
        self.fp.close()

    def get_line(self):
        for line in self.fp:
            line = line.strip()
            if not line:
                continue
            yield line

    @property
    def location(self):
        source_data = self.comments['SOURCE DATA']
        locations = filter(lambda x: x.startswith("LOCATION"), source_data)
        location = locations[0]
        location = location.split('=', 1)[1]
        fallbacks = location.split(',')
        return fallbacks[0].split("_")

    @property
    def project_type(self):
        return self.location[1]


@pytest.fixture(scope='session')
def configuration(request, config_file):
    try:
        # request.json config only
        return json.load(open(config_file))
    except:
        pass
    __tracebackhide__ = True
    # upper|noapache config only
    cfg = Config(config_file)
    cfg.parse_collection()
    __tracebackhide__ = False
    if cfg.project_type == "NEWS":
        pytest.skip("Skipping NEWS configurations")
    return cfg


def pytest_runtest_makereport(item, call, __multicall__):
    rep = __multicall__.execute()
    try:
        config = Config(item.config.getoption('config'))
        config.parse_collection()
        rep.nodeid = "{}::{}".format("_".join(config.location), rep.nodeid)
        return rep
    except:
        pass
    try:
        config = json.load(open(config_file))
        rep.nodeid = "{}::{}".format("_".join(config["_METAINFO_"]["configuration"]), rep.nodeid)
        return rep
    except:
        pass
    return rep
