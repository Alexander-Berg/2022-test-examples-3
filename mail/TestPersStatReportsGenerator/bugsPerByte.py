import yaml
import urllib3
import requests
from startrek_client import Startrek

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
yaconfig = yaml.load(open("config.yaml"))

def getBugsNum(queue):
	client = Startrek(useragent="curl/7.53.1", token=yaconfig["AUTH_ST"])

	issues = client.issues.find(
		'Queue: %s Tags:InProd (Priority: !Minor) AND (Priority: !Trivial)  Resolution: empty()' % queue
	)

	print len(issues)

def getCodeBytes(repo):
	r = requests.get("https://github.yandex-team.ru/api/v3/repos/Daria/liza/languages").json()
	print(r)

getCodeBytes("")