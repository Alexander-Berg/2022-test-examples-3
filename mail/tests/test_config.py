import unittest, re
from yaml import load


mongo_uri = re.compile(r'^(mongodb:(?:\/{2})?)((\w+?):(\{\w+?\})@|:?@?)([\w\-\.]+?):(\d+)')

class ConfigTestCase(unittest.TestCase):
    def test_port(self):
        from server import load_config
        #print "port is: {0}".format(LoadConfig("config_prod.yaml")['server']['httpServerPort'])
        self.assertTrue(load_config("config_prod.yaml")['server']['httpServerPort'] == "21050")

    def test_mongouri(self):
        from server import load_config
        #print load_config("config_prod.yaml")['database']['connectUri'] % {"_SECRET_PASSWORD_HERE_":"123"}
        self.assertTrue(mongo_uri.match(load_config("config_prod.yaml")['database']['connectUri'] % {"_SECRET_PASSWORD_HERE_":"123"}))


if __name__ == "__main__":
    unittest.main()