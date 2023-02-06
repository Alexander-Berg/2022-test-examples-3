#!/usr/bin/env python3

import smtpd
import asyncore
import sys
from time import sleep


class CustomSMTPServer(smtpd.SMTPServer):
    def process_message(self, peer, mailfrom, rcpttos, data):
        print("{} {} {}".format(peer, mailfrom, rcpttos))
        return None


if len(sys.argv) != 2:
    print("usage {} <port>".format(sys.argv[0]))
    sys.exit(1)
server = CustomSMTPServer(("::", int(sys.argv[1])), None)
asyncore.loop()
