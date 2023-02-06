import os


class RenameFile(object):
    def __init__(self, target_filename):
        self.target_filename = target_filename

    def __call__(self, table):
        os.rename(table, self.target_filename)
        return self.target_filename
