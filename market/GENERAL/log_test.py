# -*- coding: utf-8 -*-

import os
import logging
import sys
import datetime
from dotenv import load_dotenv
from structlog import wrap_logger
from structlog.stdlib import add_logger_name, add_log_level, add_log_level_number
from structlog.processors import JSONRenderer, StackInfoRenderer, UnicodeDecoder

# if not os.path.isdir('logs'):
#     os.makedirs("logs", exist_ok=True)

load_dotenv()

logging.basicConfig(
    stream=sys.stdout,
    filename="./logs/app.log.json",
    format="%(message)s",
    # TODO довольно странная конфигурация, надо разобраться
    level=logging.INFO if os.getenv("LOG_LEVEL") == "INFO" else logging.ERROR
)


def add_timestamp(_, __, event_dict):
    event_dict["_time"] = datetime.datetime.utcnow().isoformat()
    return event_dict


log = wrap_logger(
    logging.getLogger(__name__),
    processors=[
        add_timestamp,
        add_logger_name,
        add_log_level,
        add_log_level_number,
        StackInfoRenderer(),
        UnicodeDecoder(),
        JSONRenderer()
    ]
)

d = {
    "sfd": 3,
    "ss": ["32"],
    "aaa": {
        "qq": {"sdf": "asdsd"},
        "set": {"aaaa"}
    }
}

log.info("hello world", param1="dsdf", param2="dfsd")
log.info("hello world", param3=[1, 2, 3])
log.info("hello world", param3=d)
log.error('hwlo')

print("ok")
