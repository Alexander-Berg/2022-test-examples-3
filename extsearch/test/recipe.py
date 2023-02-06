import os
import urllib


def test():
    sockaddr = os.environ['RECIPE_APP_SOCKADDR']
    url = 'http://{}/ping'.format(sockaddr)
    assert urllib.urlopen(url).read() == 'pong'
