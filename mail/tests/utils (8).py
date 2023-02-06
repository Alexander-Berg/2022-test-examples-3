import json
import os

def get_conf(rel_path):
    # type: (str) -> Dict
    try:
        import yatest.common
        path = yatest.common.source_path(os.path.join("mail/logbroker-client-common", rel_path))
    except:
        path = rel_path
    return json.loads(open(path).read())
