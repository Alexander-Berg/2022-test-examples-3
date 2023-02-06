import requests
import urllib3
import yaml
import time
import json
import logging
from startrek_client import Startrek

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
yaconfig = yaml.load(open("config.yaml"))
logger = logging.basicConfig(level=logging.INFO)

QUEUE_REPO = [
	['DARIA', '/Daria/liza']
]


def getBugsNum(queue):
	client = Startrek(useragent="curl/7.53.1", token=yaconfig["AUTH_ST"])

	issues = client.issues.find(
		'Queue: %s Tags:InProd (Priority: !Minor) AND (Priority: !Trivial)  Resolution: empty()' % queue
	)

	print("Total bugs count is: %s" % len(issues))
	return len(issues)


def getCodeMBytes(repo):
	total_mbytes_of_code = 0
	total_lines_of_code = 0
	r = requests.get("https://github.yandex-team.ru/api/v3/repos%s/stats/contributors" % repo).json()
	for contributor in r:
		for week in contributor['weeks']:
			total_lines_of_code = total_lines_of_code + int(week['a'])
			total_lines_of_code = total_lines_of_code - int(week['d'])
	print(total_lines_of_code)
	# for key in r.keys():
	# 	total_mbytes_of_code = total_mbytes_of_code + int(r[key])
	#
	# total_mbytes_of_code = float(total_mbytes_of_code) / (1024*1024)
	# print("Total MBytes of code is: %s" % total_mbytes_of_code)
	# return total_mbytes_of_code


def count_bugs_per_mbyte(bugs, mbytes):
	return float(bugs) / mbytes


def send_to_stat(count_per_bug, queue):
	queue = queue.lower()
	data = [
		{
			"fielddate": time.strftime("%Y-%m-01"), "bugs_per_mb_%s" % queue: count_per_bug
		}
	]
	print (data, 'OAuth %s' % yaconfig["AUTH_STAT"])
	r = requests.post(
		'https://upload.stat.yandex-team.ru/_api/report/data',
		headers={'Authorization': 'OAuth %s' % yaconfig["AUTH_STAT"]},
		data={
			'name': 'Mail/Others/BugsPerKB',
			'scale': 'm',
			'data': json.dumps({'values': data}),
		},
	)
	print (r.text)


for q_r in QUEUE_REPO:
	bugs_in_queue = getBugsNum(q_r[0])
	kbytes_in_repo = getCodeMBytes(q_r[1])
	# send_to_stat(count_bugs_per_mbyte(bugs_in_queue, kbytes_in_repo), q_r[0])
