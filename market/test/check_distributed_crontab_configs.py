#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
from __future__ import absolute_import

# требуется дополнительно установленный пакет jsonschema==3.2.0
from jsonschema import validate
# требуется дополнительно установленный пакет croniter==0.3.34
from croniter import croniter
# требуется дополнительно установленный пакет json5==0.9.5
import json5
import os

# описание формата на вики https://nda.ya.ru/t/umexkF-y3Y8c5K
schema = {
    "$schema": "http://json-schema.org/schema#",
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "cronExpression": {
                "type": "string",
                # было бы неплохо добавить pattern для https://nda.ya.ru/t/cLb5rvRo3Y98Zn
            },
            "period": {
                "type": "string",
                "pattern": "^PT\\d+[DHMS]$"
            },
            "command": {
                "type": "string"
            },
            "taskName": {
                "type": "string",
                # паттерн в коде базинги: https://nda.ya.ru/t/bRg109Ik3Y8c5Z
                "pattern": "^[a-zA-Z0-9._-]+$"
            },
            "timeoutSeconds": {
                "type": "integer",
                "minimum": 1
            },
            "retryCount": {
                "type": "integer",
                "minimum": 1
            },
            "useDateRange": {
                "type": "boolean"
            },
            "environment": {
                "type": "string",
                "pattern": "^(production|testing|development)$"
            },
            "logOutput": {
                "type": "boolean"
            },
            "enableCustomEnvVars": {
                "type": "boolean"
            }
        },
        "allOf": [
            {
                "required": ["command", "taskName"]
            },
            {
                "oneOf": [
                    { "required": ["cronExpression"] },
                    { "required": ["period"] }
                ]
            }
        ],
        "additionalProperties": False
    }
}

dir_with_configs = os.path.dirname(os.path.abspath(__file__)) + "/../distributed-crontab"
file_name_by_task_name_and_env_dict = {}
for file_name in os.listdir(dir_with_configs):
    print("validating {}...".format(file_name))
    full_file_name = dir_with_configs + "/" + file_name
    with open(full_file_name, "r") as f:
        json_data = json5.load(f)
    validate(schema=schema, instance=json_data)
    for config in json_data:
        if "cronExpression" in config:
            cron_expression = config["cronExpression"]
            if not croniter.is_valid(cron_expression):
                raise RuntimeError("cron expression {} is not valid".format(cron_expression))
        task_name = config["taskName"]
        environment = config["environment"] if "environment" in config else "production"
        if (task_name, environment) in file_name_by_task_name_and_env_dict:
            raise RuntimeError("task name {} is not uniq in env {}, it presents both in {} and {} at least"
                               .format(task_name,
                                       environment,
                                       file_name_by_task_name_and_env_dict[(task_name, environment)],
                                       file_name))
        file_name_by_task_name_and_env_dict[(task_name, environment)] = file_name
    print("ok")
