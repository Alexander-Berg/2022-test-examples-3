#!/usr/bin/env /usr/bin/bash
import os.path
import re
import json
import requests
import argparse
import yaml

# default values
lunapark_link = "https://lunapark.yandex-team.ru"

parser = argparse.ArgumentParser()
parser.add_argument('-w', '--wmi', type=str, help="FQDN of the shooting's aggregator", default=lunapark_link)
parser.add_argument('-s', '--sid', type=int, help="Shooting ID", default=0)
parser.add_argument('-r', '--rid', type=int, help="Regression ID", default=0)
parser.add_argument('-q', '--rname', type=str, help="Regression name", default="")
parser.add_argument('-f', '--rfile', type=str, help="Reference file", default="")
parser.add_argument('-t', '--threshold', type=int, help="Permissible threshold in %", default=5)
parser.add_argument('-m', '--memlimit', type=int, help="Memory limit in Gb", default=8)
parser.add_argument('-i', '--ignore', type=str, help="Ignore tests", default="memory")
args = parser.parse_args()

# Global variables
LUNA = args.wmi
SID = args.sid
RID = args.rid
RNAME = args.rname
RFILE = args.rfile
THRESHOLD = args.threshold
MEMLIMIT = args.memlimit
IGNORETEST = args.ignore.split()
MONTEST = {'CPU', 'memory', 'netmb'}


class LunaError(Exception):
    def __init__(self, text):
        self.txt = text

class LunaTest(object):
    def __init__(self):
        self.wmi = LUNA
        self.threshold = float(1) + float(THRESHOLD)/100
        self.complist = []
        self.ignore_monitoring = MONTEST == MONTEST.intersection(set(IGNORETEST))

# Check the presence of the shooting number and get a sign of compliance to form a comparison list
        if SID == 0:
            raise LunaError("Shooting is not set")
        else:
            self.sid = SID
            self.job_summary()

# Check availability of conditions for formation of the list of comparison. If they are available we get a list for comparison
        if RFILE == "" and RID == 0:
            raise LunaError("No comparison data specified")
        if RID > 0:
            self.complist += self.job_list(RID)
        if RFILE != "":
            self.complist += self.ref_list(RFILE)
        if len(self.complist) == 0:
            raise LunaError("Comparing list is empty")

# Initializing tracking dictionaries
        self.test = {}
        self.reason = {}

        self.job_percentiles()
        self.job_monitoring()

# Data of the shooting
    def job_summary(self):
        try:
            self.scheduler, self.shootype, self.sname = get_scheduler(self.wmi, self.sid)
            self.imbalance_rps = float(api_job(self.wmi, self.sid, 'summary')[0]['imbalance_rps'])
        except Exception as ex:
            raise LunaError("Impossible to get shooting parameters. Error:{}".format(ex))

    def job_percentiles(self):
        jobpercentiles = api_job(self.wmi, self.sid, 'percentiles')
        self.percentiles = parse_percentiles(jobpercentiles)

    def job_monitoring(self):
        if self.sname == None or self.ignore_monitoring:
            self.monitoring = None
        else:
            jobmonitoring = api_job(self.wmi, self.sid, 'monitoring')
            self.monitoring = parse_monitoring(jobmonitoring, self.sname)

    def job_http(self):
        self.http = {}
        jobhttp = api_job(self.wmi, self.sid, 'http')

        if type(jobhttp) == list and len(jobhttp) > 0:

            for item in jobhttp:
                http_code = item[u'http']
                self.http[http_code] = item[u'percent']
        else:
            self.test[u'http_answers'] = False
            self.reason[u'http_answers'] = "Has no http answers data!"

    def job_net(self):
        self.net = {}
        jobnet = api_job(self.wmi, self.sid, 'net')

        if type(jobnet) == list and len(jobnet) > 0:

            for item in jobnet:
                net_code = item[u'net']
                self.net[net_code] = item[u'percent']
        else:
            self.test[u'net_answers'] = False
            self.reason[u'net_answers'] = "Has no net answers data!"

# Generate shooting's list for comparing
# Get list from regression component
    def job_list(self, rid):
        complist = []
        templist = api_regress(self.wmi, rid, 'joblist')

        if type(templist) == type([]) and len(templist) > 0:

            for item in templist:
                number = item[u'n']

                if get_scheduler(self.wmi, number)[0] == self.scheduler:
                    complist.append(number)
                else:
                    continue

        return sorted(complist)[-10:]

# Get list from file
    def ref_list(self, rfile):

        if os.path.exists(rfile):
            _, extension = os.path.splitext(rfile)

            if extension == '.yaml' or extension == '.yml' or extension == '.YAML' or extension == '.YML':
                complist = self.list_from_yaml(rfile)
            else:
                complist = self.list_from_file(rfile)
            return complist

        else:
            raise LunaError("{} is not found".format(rfile))

# For YAML files
    def list_from_yaml(self, rfile):
        complist = []

        with open(rfile) as rfile:
            try:
                reference = yaml.safe_load(rfile)

                for shoot in reference[self.shootype]:

                    if get_scheduler(self.wmi, shoot)[0] == self.scheduler:
                        complist.append(shoot)
                    else:
                        continue
                return complist

            except Exception as ex:
                raise LunaError("Error during reading yaml file. {}".format(ex))

# For other files
    def list_from_file(self, rfile):
        complist = []

        with open(rfile) as rfile:

            for line in rfile:

                if get_scheduler(self.wmi, line)[0] == self.scheduler:
                    complist.append(line)
                else:
                    continue

        return complist

# Reference data for comparing
    def data_for_comparing(self):
        self.ref_percentiles = {}
        self.ref_monitoring = {}

        for shoot in self.complist:
            self.ref_percentiles[shoot] = parse_percentiles(api_job(self.wmi, int(shoot), 'percentiles'))

            if self.sname == None or self.ignore_monitoring:
                self.ref_monitoring[shoot] = None
            else:
                self.ref_monitoring[shoot] = parse_monitoring(api_job(self.wmi, int(shoot), 'monitoring'), self.sname)

# Add shooting to regression
    def add_to_regression(self, rname):
        if rname == "":
            pass
        else:
            try:
                _ = add_job(self.wmi, self.sid, rname)
            except Exception as ex:
                pass

# Link to luna comparing sheet
    def luna_compare(self, num):
        lunatemplate = 'https://lunapark.yandex-team.ru/compare/#jobs=%s&tab=test_data&mainjob=%s&helper=all&cases=&plotGroup=additional&metricGroup=&target='
        compared = sorted(self.complist)[-num:]
        compared.insert(0, self.sid)
        return lunatemplate%(",".join(str(i).replace("\n","").replace(" ","") for i in compared), self.sid)

# Tests
# 0 - test failed; 1 - test passed; 2 - improved perfomance

    def check_imbalance(self):
        imbalance_rps = []

        for item in self.complist:
            imbalance_rps.append(api_job(self.wmi, item, 'summary')[0]['imbalance_rps'])
        mediana = get_median(imbalance_rps)

        if mediana == None:
            self.test[u'check_imbalance'] = 0
            self.reason[u'check_imbalance'] = 'Regression list is empty'
        elif self.imbalance_rps == 0:
            self.test[u'check_imbalance'] = 1
        else:
            deviation = mediana / self.imbalance_rps

            if deviation > self.threshold:
                self.test[u'check_imbalance'] = 0
                self.reason[u'check_imbalance'] = 'Shooting is unbalanced faster in %f times'%(deviation)
            elif (float(1)/deviation) > self.threshold:
                self.test[u'check_imbalance'] = 2
                self.reason[u'check_imbalance'] = 'Shooting is unbalanced slowly in %f times'%(float(1)/deviation)
            else:
                self.test[u'check_imbalance'] = 1

    def test_http(self):
        self.job_http()
        if u'http_answers' not in self.test.keys():
            try:
                deviation = float(100) - self.http[200]

                if deviation == float(0):
                    self.test[u'http_answers'] = 1
                else:
                    self.test[u'http_answers'] = 0
                    self.reason[u'http_answers'] = '%f percents wrong http answers'%(deviation)

            except Exception:
                self.test[u'http_answers'] = 0
                self.reason[u'http_answers'] = 'Has no successfull http answers'

    def test_net(self):
        self.job_net()
        if u'net_answers' not in self.test.keys():
            try:
                deviation = float(100) - self.net[0]

                if deviation == float(0):
                    self.test[u'net_answers'] = 1
                else:
                    self.test[u'net_answers'] = 0
                    self.reason[u'net_answers'] = '%f percents wrong network answers'%(deviation)

            except Exception:
                self.test[u'http_answers'] = 0
                self.reason[u'http_answers'] = 'Has no successfull net answers'

    def test_quantile(self, quantile, band):
        arrayq = []

        for item in self.complist:
            arrayq.append(self.ref_percentiles[item][quantile])
        mediana = get_median(arrayq)

        if mediana == None:
            self.test[u'q' + quantile] = 0
            self.reason[u'q' + quantile] = 'Regression list is empty'
        elif self.percentiles[quantile] == 0:
            self.test[u'q' + quantile] = 0
            self.reason[u'q' + quantile] = 'Shooting\'s data is absent'
        else:
            deviation = self.percentiles[quantile] / mediana

            if deviation > float(band):
                self.test[u'q' + quantile] = 0
                self.reason[u'q' + quantile] = 'Longer by %f percents'%((deviation - float(1))*100)

            elif (float(1)/deviation) > float(band):
                self.test[u'q' + quantile] = 2
                self.reason[u'q' + quantile] = 'Faster by %f percents'%(((float(1)/deviation) - float(1))*100)
            else:
                self.test[u'q' + quantile] = 1

    def test_monitoring(self, metric, measure, band):

        if self.sname == None:
            self.test[metric] = 1
            self.reason[metric] = "Monitoring is not configured"
        else:

            try:
                indexes = {u'avg':0, u'min':1, u'max':2}
                index = indexes[measure]
                signals = {
                    u'cpu_usage':u'custom:portoinst-cpu_usage_cores_tmmv',
                    u'cpu_wait':u'custom:portoinst-cpu_wait_cores_tmmv',
                    u'io_read':u'custom:portoinst-io_read_fs_bytes_tmmv',
                    u'io_write':u'custom:portoinst-io_write_fs_bytes_tmmv',
                    u'net_mb_summ':u'custom:portoinst-net_mb_summ',
                    u'memory_usage':u'custom:portoinst-memory_usage_gb_tmmv'
                }
                signal = signals[metric]
                arraym = []

                for item in self.complist:
                    arraym.append(self.ref_monitoring[item][signal][index])
                mediana = get_median(arraym)

                if mediana == None:
                    self.test[metric] = 0
                    self.reason[metric] = 'Regression list is empty'
                elif self.monitoring[signal][index] == 0:
                    self.test[metric] = 0
                    self.reason[metric] = 'Shooting\'s data is absent'
                else:
                    deviation = self.monitoring[signal][index] / mediana

                    if deviation > float(band):
                        self.test[metric] = 0
                        self.reason[metric] = 'Worse by %f percents'%((deviation - float(1))*100)
                    elif (float(1)/deviation) > float(band):
                        self.test[metric] = 2
                        self.reason[metric] = 'Better by %f percents'%(((float(1)/deviation) - float(1))*100)
                    else:
                        self.test[metric] = 1

            except Exception as ex:
                self.test[metric] = 0
                self.reason[metric] = "Monitoring test was received error: {}".format(ex)

    def test_memory(self, limit):
        band = self.threshold
        metric = u'memory_usage'
        if self.sname == None:
            self.test[metric] = 1
            self.reason[metric] = "Monitoring is not configured"
        elif self.monitoring[u'custom:portoinst-memory_usage_gb_tmmv'][2] >= limit:
            self.test[metric] = 0
            self.reason[metric] = "Memory usage is more than limit"
        else:
            average_increase = []
            memory_increased =  self.monitoring[u'custom:portoinst-memory_usage_gb_tmmv'][2] - self.monitoring[u'custom:portoinst-memory_usage_gb_tmmv'][1]
            try:
                for item in self.complist:
                    average_increase.append(self.ref_monitoring[item][u'custom:portoinst-memory_usage_gb_tmmv'][2] - self.ref_monitoring[item][u'custom:portoinst-memory_usage_gb_tmmv'][1])
                mediana = get_median(average_increase)
                if mediana == None:
                    self.test[metric] = 0
                    self.reason[metric] = 'Regression list is empty'
                elif memory_increased == 0:
                    self.test[metric] = 0
                    self.reason[metric] = 'Shooting\'s data is absent'
                else:
                    deviation = memory_increased / mediana

                    if deviation > float(band):
                        self.test[metric] = 0
                        self.reason[metric] = 'Worse by %f percents'%((deviation - float(1))*100)
                    elif (float(1)/deviation) > float(band):
                        self.test[metric] = 2
                        self.reason[metric] = 'Better by %f percents'%(((float(1)/deviation) - float(1))*100)
                    else:
                        self.test[metric] = 1
            except Exception as ex:
                self.test[metric] = 0
                self.reason[metric] = "Monitoring test was received error: {}".format(ex)

    def test_Q50(self):
        self.test_quantile(u'50', self.threshold)

    def test_Q75(self):
        self.test_quantile(u'75', self.threshold)

    def test_Q80(self):
        self.test_quantile(u'80', self.threshold)

    def test_Q85(self):
        self.test_quantile(u'85', self.threshold)

    def test_Q90(self):
        self.test_quantile(u'90', self.threshold)

    def test_Q95(self):
        self.test_quantile(u'95', self.threshold)

    def test_Q98(self):
        self.test_quantile(u'98', self.threshold)

    def test_CPU(self):
        self.test_monitoring(u'cpu_usage', u'max', self.threshold)

    def test_netmb(self):
        self.test_monitoring(u'net_mb_summ', u'avg', self.threshold)

# Test analyze
    def get_result(self):
        self.passed = True

        for key in sorted(self.test.keys()):
            if self.test[key] == 0:
                self.passed = False
                print('Test %s | %s | %s'%(key, "failed", self.reason[key]))
            elif self.test[key] == 2:
                print('Test %s | %s | %s'%(key, "improved", self.reason[key]))
            else:
                print('Test %s | %s'%(key, "passed"))

        if len(self.complist) > 5:
            for count in (3, len(self.complist)):
                print('Compare %i | %s'%(count, self.luna_compare(count)))
        else:
            print('Compare %i | %s'%(len(self.complist), self.luna_compare(len(self.complist))))

        if self.passed == True:
            self.add_to_regression(RNAME)
            print('passed')
        else:
            print('failed')
# End of Class


def parse_percentiles(perclist):
    percentiles = {}

    if type(perclist) == type([]) and len(perclist) > 7:
        for item in perclist[:7]:
            quantile = item[u'percentile']
            percentiles[quantile] = float(item[u'ms'])
    else:
        raise LunaError('Wrong percentiles!')

    return percentiles


def parse_monitoring(monlist, service):
    monitoring = {}

    if type(monlist) == type([]) and len(monlist) > 0 :
        for item in monlist:
            if item[u'host'] == service:
                metric = item[u'metric']
                monitoring[metric] = (item[u'avg'], item[u'min'], item[u'max'])
            else:
                continue
    else:
        pass

    return monitoring

# Lunapark request
def api_job (host, id, method):

    methods = {
        'summary':'summary.json',
        'times':'dist/times.json',
        'percentiles':'dist/percentiles.json',
        'http':'dist/http.json',
        'net':'dist/net.json',
        'cases':'dist/cases.json',
        'monitoring':'monitoring.json'
    }

    try:
        response = requests.get('%s/api/job/%s/%s'%(host, id, methods[method]), verify=False)
        return response.json()
    except Exception as ex:
        raise LunaError('Wrong API job request. %s'%(ex))
    finally:
        requests.session().close()


def get_scheduler(host, sid):

    scheduler = None
    shootype = None
    servicename = None

    URL = "{}/api/job/{}/configinfo.txt".format(host, str(sid).replace("\n","").replace(" ",""))
    response = requests.get(URL, verify=False)

    if response.status_code == 200:
        config = yaml.safe_load(response.content)

        try:
            panels = config['yasm']['panels'].keys()
            yasm = True
        except Exception:
            yasm = False

        try:
            if config['phantom']['enabled'] == True:
                scheduler = config['phantom']['load_profile']['schedule'].replace(" ", "")
                shootype = scheduler.split('(')[0]

                if yasm == True:

                    for panel in panels:

                        if re.compile(panel).search(config['phantom']['address']) != None:
                            servicename = panel

            elif config['pandora']['enabled'] == True:

                if type(config['pandora']['config_content']['pools'][0]['rps']) == list:
                    scheduler = config['pandora']['config_content']['pools'][0]['rps'][0]
                else:
                    scheduler = config['pandora']['config_content']['pools'][0]['rps']
                shootype = scheduler['type']

                if yasm == True:

                    for panel in panels:

                        if re.compile(panel).search(config['pandora']['config_content']['pools'][0]['gun']['target']) != None:
                            servicename = panel
        except Exception:
            pass
    
    return (scheduler, shootype, servicename)


# Add shooting to regression
def add_job(host, id, rname):
    payload = {'regression':1, 'component':rname}

    try:
        response = requests.post('%s/api/job/%s/edit.json'%(host, id), data=json.dumps(payload), verify=False)
    except Exception as ex:
        raise LunaError('Job is not add to regression. %s'%(ex))
    finally:
        requests.session().close()


# Regression data
def api_regress (host, id, method):
    methods = {
        'components':'componentlist.json',
        'kpilist':'kpilist.json',
        'joblist':'joblist.json'
    }

    try:
        response = requests.get('%s/api/regress/%s/%s'%(host, id, methods[method]), verify=False)
        return response.json()
    except Exception, ex:
        raise LunaError('Wrong API regression request. %s'%(ex))
    finally:
        requests.session().close()


# Calculate of the median value
def get_median(array):
    lenght = len(array)

    if int(lenght) > 5:
        return sum(sorted(array)[1:-1])/float(lenght - 2)
    elif int(lenght) > 0:
        return sum(sorted(array))/float(lenght)
    else:
        return None


# Constructor of the test cases
if __name__ == "__main__":
    try:
        regtest = LunaTest()
# For line shootings check only the value of RPS imbalance
        if regtest.shootype == "line":
            regtest.check_imbalance()
# For const shootings check the numerical parameters
        else:
            regtest.data_for_comparing()
            if 'http' not in IGNORETEST:
                regtest.test_http()
            if 'net' not in IGNORETEST:
                regtest.test_net()
            if 'Q50' not in IGNORETEST:
                regtest.test_Q50()
            if 'Q75' not in IGNORETEST:
                regtest.test_Q75()
            if 'Q80' not in IGNORETEST:
                regtest.test_Q80()
            if 'Q85' not in IGNORETEST:
                regtest.test_Q85()
            if 'Q90' not in IGNORETEST:
                regtest.test_Q90()
            if 'CPU' not in IGNORETEST:
                regtest.test_CPU()
            if 'netmb' not in IGNORETEST:
                regtest.test_netmb()
            if 'memory' not in IGNORETEST:
                regtest.test_memory(MEMLIMIT)
# Print the result of the comparing
        regtest.get_result()
    except LunaError as luna:
        print("Regression test failed.\n{}\nfailed".format(luna.txt))
