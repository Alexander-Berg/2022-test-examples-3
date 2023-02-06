# !/usr/bin/env python3

import logging  # for debug
import json as js

def get_sum(responds, field, type):
    if(type == 'int'):
        type = int
    elif(type =='float'):
        type = float
    return sum(map(type, responds.get(field)))

