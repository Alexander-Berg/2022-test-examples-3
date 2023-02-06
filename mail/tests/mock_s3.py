class MockS3(object):
    def __init__(self):
        self.dictionaries = {}

    def add_dictionary(self, name, content):
        self.dictionaries[name] = content.copy()

    def add_key(self, name, key, value):
        self.dictionaries[name][key] = value

    def load_dictionary(self, name):
        return self.dictionaries[name]
