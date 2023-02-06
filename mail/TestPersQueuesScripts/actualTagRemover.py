# -*- coding: utf-8 -*-
import urllib3
import os
from startrek_client import Startrek
from set_secret import set_secret

set_secret.set_secrets()

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def remove_tag_actual():
    issuesKeys = []
    excludeKeys = []

    client = Startrek(useragent="curl/7.53.1", token=os.environ['STARTRECK_TOKEN'])
    issues_with_old_actual = client.issues.find(
        'Queue: DARIA Status: Open Tags:actual Tags: changed(to:actual date: < today() - 1y)'
    )

    issues_with_renewed_actual = client.issues.find(
        'Queue: DARIA Status: Open Tags:actual Tags: changed(to:actual date: > today() - 1y)'
    )

    for issue in issues_with_old_actual:
        issuesKeys.append(issue.key)
    for issue in issues_with_renewed_actual:
        excludeKeys.append(issue.key)

    print(issuesKeys)
    print(excludeKeys)

    issuesKeys = list(set(issuesKeys) - set(excludeKeys))
    issuesKeys.sort()

    print(issuesKeys)

    bulkchange = client.bulkchange.update(
        issuesKeys,
        tags={'remove': ['actual']}
    )

    print(len(issuesKeys))
    print(bulkchange.status)
    bulkchange = bulkchange.wait()
    print(bulkchange.status)


remove_tag_actual()
