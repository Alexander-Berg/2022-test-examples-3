"""
parse output of 'printguruass' and check if every answer has 'filter' section
"""

from __future__ import print_function
import sys

def nextanswer():
    ret = []
    flRead = False
    line = sys.stdin.readline()
    while line:
        line = line.strip()
        if not line:
            if ret:
                return ret
        elif flRead:
            ret.append(line)
        elif line[0] == '[':
            flRead = True
            ret.append(line)
        line = sys.stdin.readline()
    assert not ret
    return []

ans = nextanswer()
while ans:
    if len(ans) == 1:
        print("Unknown single line:")
        print("\t", ans[0], sep='')
        print()
    if len(ans) == 2: # request and error string
        print("Error in request:")
        print("\t", ans[0], sep='')
        print("\t", ans[1], sep='')
        print()
    else: #request, path, answer
        if not ans[1].endswith("->filters"):
            print("No filters for request:")
            print("\t", ans[0], sep='')
            print("\t", ans[1], sep='')
            print()
    ans = nextanswer()
