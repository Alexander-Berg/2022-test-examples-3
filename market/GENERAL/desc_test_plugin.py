try:
    from nose.plugins import Plugin
except:
    print('Extended test descriptions are disabled because nose installation was not found')

    class Plugin(object):
        pass


class TestDescription(Plugin):
    name = 'desc'

    def describeTest(self, test):
        test_name = test.test._testMethodName
        test_desc = test.test._testMethodDoc.strip() if test.test._testMethodDoc else None
        if test_desc:
            desc = '{test_name}\n\t{test_desc}'.format(test_name=test_name, test_desc=test_desc)
        else:
            desc = test_name
        return desc
