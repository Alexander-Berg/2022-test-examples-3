# !/usr/bin/env python3

import logging  # for debug
import json as js

def get_sum(responds, field, type):
    if(type == 'int'):
        type = int
    elif(type =='float'):
        type = float
    return sum(map(type, responds.get(field)))

def get_avg(responds, field):
    l = responds.get(field)
    to_remove = ['None', None]
    for rm in to_remove:
        for i in range(l.count(rm)):
            l.remove(rm)
    l = map(float, l)
    if not len(l):
    	return -1
    return sum(l) / len(l)
