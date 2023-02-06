import unittest
import sys

testmodules = [
    'tests.test_MailGenmapHandler',
    'tests.test_config'
    ]

suite = unittest.TestSuite()

for t in testmodules:
    try:
        # If the module defines a suite() function, call it to get the suite.
        mod = __import__(t, globals(), locals(), ['suite'])
        suitefn = getattr(mod, 'suite')
        suite.addTest(suitefn())
    except (ImportError, AttributeError):
        # else, just load all the test cases from the module.
        suite.addTest(unittest.defaultTestLoader.loadTestsFromName(t))

unittest.TextTestRunner().run(suite)

if not unittest.TextTestRunner().run(suite).wasSuccessful():
    print("Tests error. Exiting.")
    sys.exit(1)
else:
    print("Test was successful.")