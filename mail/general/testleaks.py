#!/usr/bin/python

# Utility script for testing Python binding
from testcommon import *

if __name__ == '__main__':

    bad_response_1 = "asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf asdf"
    bad_response_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hi><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/><test param='some shit'/></hi>"
    good_resp = '<?xml version="1.0" encoding="utf-8"?> \
<doc>\
<uid hosted="0" domid="" domain="">11806301</uid>\
<karma confirmed="0">0</karma>\
<regname>junit-test</regname>\
<address-list>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@YA.ru</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@narod.ru</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.by</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.com</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.kz</address>\
<address validated="1" default="1" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.ru</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.ua</address>\
<address validated="0" default="0" rpop="0" native="0" born-date="2010-11-08 14:18:34">%a8%b8@yandex.ru</address>\
<address validated="0" default="0" rpop="0" native="0" born-date="2010-11-08 14:14:05">$%{}@yandex.ru</address>\
<address validated="0" default="0" rpop="0" native="0" born-date="2010-11-08 14:14:55">eeeee@xn--b1aonadt8a9a.xn--h1ae7bxa</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@YA.ru</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@narod.ru</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.by</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.com</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.kz</address>\
<address validated="1" default="1" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.ru</address>\
<address validated="1" default="0" rpop="0" native="1" born-date="2010-10-04 00:00:00">junit-test@yandex.ua</address>\
</address-list>\
</doc>'

    for i in range(1,1000000):
        try:
          resp = bb.Response(bad_response_2)
        except:
          pass

    print "Done"

    while True:
        pass
