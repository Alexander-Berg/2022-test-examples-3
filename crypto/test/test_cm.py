import time
import json
import requests

from crypta.cm.services.common.data.python.id import TId


def test_rt_cm_duid_uploader(rt_cm_duid_uploader, cm_client, rt_duid_msg_generator):
    all_msgs = []
    for i in range(6):
        all_msgs += rt_duid_msg_generator.write(i+1)

    time.sleep(20)

    test_results = []
    for msg in all_msgs:
        response = cm_client.identify(TId("duid", msg["domain_cookie"]))

        if response.status_code == requests.codes.ok:  # hack timestamp
            ids = json.loads(response.text)
            assert 1 == len(ids)
            msg_to_dump = ids[0]
            msg_to_dump["match_ts"] = 1619091706
            test_results.append((response.status_code, msg, msg_to_dump))
        else:
            test_results.append((response.status_code, msg, response.text))

    return test_results
