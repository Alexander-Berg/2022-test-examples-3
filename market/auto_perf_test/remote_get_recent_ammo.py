#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import re
import sys

from constants import *


def remote_get_recent_ammo():
    ammo_list = []
    for name in os.listdir(DEFAULT_AMMO_DIR[REPORT_MAIN]):
        if re.match(AMMO_DATE_RE, name) and len(os.listdir(os.path.join(DEFAULT_AMMO_DIR[REPORT_MAIN], name))):
            ammo_list.append(name)
    sys.stdout.write(max(ammo_list))


def main():
    remote_get_recent_ammo()


if __name__ == "__main__":
    main()
