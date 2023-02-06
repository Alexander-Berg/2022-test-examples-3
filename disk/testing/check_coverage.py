#!/usr/bin/python
# -*- coding: utf-8 -*-
import os
import requests
import json
import sys

from coverage import Coverage


COVERAGE_FILE = "coverage_data"
COVERAGE_FILE_FROM_STABLE = "stable_coverage_data"
DEFAULT_ACCEPTABLE_COVERAGE = 65.0


def coverage_result(data_file=COVERAGE_FILE):
    cov = Coverage(data_file=data_file)
    cov.load()
    coverage_result = cov.report(file=open(os.devnull, "w"))
    return coverage_result


def stable_branch_coverage():
    user = os.environ["TEAMCITY_USER"]
    password = os.environ["TEAMCITY_PASSWORD"]

    results_url = "https://teamcity.yandex-team.ru/httpAuth/app/rest/builds/"\
        "buildType:Mpfs_Test/artifacts/content/%s" % COVERAGE_FILE

    response = requests.get(results_url, auth=(user, password), stream=True)
    if response.status_code == requests.codes.OK:
        with open(COVERAGE_FILE_FROM_STABLE, "w") as cov_file:
            cov_file.write(response.content)
        coverage = coverage_result(COVERAGE_FILE_FROM_STABLE)
    else:
        coverage = DEFAULT_ACCEPTABLE_COVERAGE
    return coverage


def teamcity_build_status(text):
    print("##teamcity[buildStatus text='%s']" % text)


if __name__ == "__main__":
    exit_code = 0

    current_coverage = coverage_result()
    stable_coverage = stable_branch_coverage()

    if current_coverage < stable_coverage:
        exit_code = 1
        teamcity_build_status("%.2f%% < %.2f%%" % (current_coverage, stable_coverage))
    else:
        teamcity_build_status("%.2f%%" % current_coverage)

    sys.exit(exit_code)
