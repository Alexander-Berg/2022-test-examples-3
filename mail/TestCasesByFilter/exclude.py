# -*- coding: utf-8 -*-
import os

casesToExclude=[]
scriptPath = os.path.dirname(os.path.abspath(__file__) + '/SupportQueueMonitoring')

f = open(os.path.dirname(scriptPath) + "/exclude.txt","r")
for line in f:
    casesToExclude.append(line[:-1])

print(casesToExclude)