# -*- coding: utf-8 -*-

import urllib3
import os
from startrek_client import Startrek
from retrying import retry
from set_secret import set_secret

set_secret.set_secrets()

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

QUEUES = ['DARIA', 'QUINN', 'MAYA', 'CHEMODAN', 'DOCVIEWER', 'DISCSW', 'MOBDISK', 'MOBILEMAIL', 'PASSP']


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def mark_as_inprod():
	for queue in QUEUES:
		print("")
		print(queue)

		issues = []
		issuesKeys = []

		client = Startrek(useragent="curl/7.53.1", token=os.environ['STARTRECK_TOKEN'])

		issues = client.issues.find(
			'queue: ' + queue + ' Tags: !InProd Type: Bug (Stage: FromTestToProd OR Stage: Production OR Deployed: '
								'"Да")(Resolution:!Duplicate AND Resolution: !Invalid)  Tags:!do_not_mark_inprod'
		)
		for issue in issues:
			issuesKeys.append(issue.key)
		issuesKeys.sort()

		print(issuesKeys)
		print(len(issuesKeys))

		bulkchange = client.bulkchange.update(
			issuesKeys,
			tags={'add': ['inProd']}
		)

		print(bulkchange.status)
		bulkchange = bulkchange.wait()
		print(bulkchange.status)


mark_as_inprod()
