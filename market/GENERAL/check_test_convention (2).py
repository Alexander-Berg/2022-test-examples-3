import os
import re
import sys

rootDir = sys.argv[1] if len(sys.argv) > 1 else '.'
has_invalid_tests = False
for dirName, subdirList, fileList in os.walk(rootDir):
    for fname in fileList:
        if not fname.endswith('.java'):
            continue
        has_test = False
        with open(os.path.join(dirName, fname)) as f:
            for line in f.readlines():
                if re.search("@Test[\s(]", line):
                    has_test = True
        if has_test and not (fname.endswith('Test.java') or fname.endswith('Testing.java')):
            print(os.path.join(dirName, fname))
            has_invalid_tests = True
if has_invalid_tests:
    raise Exception('There are tests not following naming convention!')
