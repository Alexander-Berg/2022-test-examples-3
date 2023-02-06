# -*- coding: utf-8 -*-
from rtcc.core.dataobjects import ConfigResult
from rtcc.core.dataobjects import ConfigurationId
from rtcc.core.dataobjects import ConfigurationInfo
from rtcc.core.dataobjects import ERROR
from rtcc.core.dataobjects import FAILED
from rtcc.core.dataobjects import GeneratorResult
from rtcc.core.dataobjects import GeneratorResultList
from rtcc.core.dataobjects import TestInfo as CoreTestInfo
from rtcc.core.dataobjects import TestInfoList as CoreTestInfoList
from rtcc.core.dataobjects import WARNING
from rtcc.core.session import Session
from rtcc.view.reports.errorswarningreport import ErrorsWarningReport

SESSION = Session()


def test_none():
    assert ErrorsWarningReport().view() == "None"


def test_multiply():
    errors = [CoreTestInfo("item1", "ErrorOne", None, status=ERROR),
              CoreTestInfo("item2", "ErrorOne", None, status=ERROR)]

    warns = [CoreTestInfo("item3", "WarnOne", None, status=WARNING)]
    fails = [CoreTestInfo("item4", "FailOne", None, status=FAILED),
             CoreTestInfo("item5", "FailTwo", None, status=FAILED)]
    result = ConfigResult(ConfigurationInfo(ConfigurationId("contour", "stand", "location"), "revision"),
                          GeneratorResultList([GeneratorResult("generator",
                                                               results=CoreTestInfoList(errors + fails + warns))]))
    view = ErrorsWarningReport()
    view.add(result)
    assert view.view() == "Failures:\n" \
                          "  * FailTwo: contour/stand/location\n" \
                          "  * FailOne: contour/stand/location\n" \
                          "Warnings:\n" \
                          "  * WarnOne: contour/stand/location\n" \
                          "Errors:\n" \
                          "  * ErrorOne: contour/stand/location, contour/stand/location"
