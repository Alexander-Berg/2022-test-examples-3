#!/usr/bin/python
import sys


class Token(object):
    def __init__(self, url, uname, interactive, fh):
        self.interactive = interactive
        self.uname = uname
        self.url = url
        self.fh = fh

    def __call__(self, key_file=None):
        pass


if __name__ == '__main__':
    print("Start fake sandbox api with parameters: %s" % " ".join(sys.argv[1:]))

    print ("\n\nhttps://sandbox.yandex-team.ru/task/12345/view\n")
    print ("Resource download link: http://proxy.sandbox.yandex-team.ru/5678\n")
