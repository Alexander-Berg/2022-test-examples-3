import logging
from flask import Flask
import json
import signal
import sys
import traceback
import time

import instance
import webapi

logging.basicConfig(format="%(asctime)s %(message)s", datefmt="%Y-%m-%d %H:%M:%S %Z", level=logging.DEBUG)

def main():
    _cmStatus = webapi.CmStatus()
    _inst = instance.InstanceCtl()

    app = Flask(__name__)

    @app.route("/status")
    def status():
        return _cmStatus.status()[0]

    @app.route("/unistat")
    def unistat():
        stat = [ ]
        stat.append(["running_ammv", 1])
        last_finished = _cmStatus.status()[1]
        if last_finished:
            stat.append(["last_finished_axxx", int(time.time()) - int(last_finished)])
        return json.dumps(stat)

    def sigterm(signum, frame):
        _cmStatus.stop()
        sys.exit(0)

    signal.signal(signal.SIGTERM, sigterm)

    _cmStatus.start()
    _inst.start()

    try:
        app.run(debug=False, host="::", port=8080)
    except Exception as error:
        logging.critical(traceback.format_exc())
    finally:
        sigterm(None, None)

if __name__ == '__main__':
    sys.exit(main())
