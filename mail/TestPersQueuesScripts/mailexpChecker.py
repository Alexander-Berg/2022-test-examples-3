import urllib3
from retrying import retry
from startrek_client import Startrek

import parameters

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
client = Startrek(useragent="curl/7.53.1", token=parameters.myToken)


def find_left_bugs():
	issues = client.issues.find(
		'Queue: MAILEXP Resolution: empty() Created: < today()-30d Type: Bug Followers: !"cosmopanda"'
	)
	return issues


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def add_to_followers(issues, follower_nick):
	client.bulkchange.update(
		issues,
		followers={'add': [follower_nick]}
	)


follower_to_add = 'cosmopanda'
issues = find_left_bugs()
add_to_followers(issues, follower_to_add)
