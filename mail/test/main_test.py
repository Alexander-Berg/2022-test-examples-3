from ymod_python_sys import terminate_application
import py_unistat
import time
import json
import sys

unistat = py_unistat.find_module("unistat")

if unistat is not None:
    abc_present = unistat.is_metric_present("abc")
    xyz_present = unistat.is_metric_present("xyz")

    print(abc_present)
    print(xyz_present)

    config = json.dumps({
        "name": "xyz",
        "aggregation": "absolute_average",
        "host_aggregation": "average"
    })

    unistat.add_metric(config)

    xyz_present = unistat.is_metric_present("xyz")
    print(xyz_present)

    unistat.push("xyz", 5.0)

    print(unistat.get_values_in_json_str())

    unistat.push("xyz", 15.0)

    print(unistat.get_values_in_json_str(True))
    print(unistat.get_values_in_json_str())

    sys.stdout.flush()

else:

    print("error")

time.sleep(1.0)
terminate_application()
