import pytest
import sys

from crm.space.test.tests.test_issue import TestBaseSuiteIssueCreate, TestBaseSuiteIssueGet
from crm.space.test.tests.test_lift import TestLiftNextPeriod
from crm.space.test.tests.test_take_letter import TestCreateIssueByMail


def main():
    sys.exit(
        pytest.main(
            ["-qq"],
            plugins=[
                TestBaseSuiteIssueCreate(),
                TestBaseSuiteIssueGet(),
                TestLiftNextPeriod(),
                TestCreateIssueByMail()
            ]
        )
    )
