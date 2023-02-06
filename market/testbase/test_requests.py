from __future__ import print_function
# -*- coding: utf-8 -*-
import json
import subprocess

def loadjson(filename):
    json_file = open(filename)
    return json.load(json_file)

def splitans(ans):
    reqpos = ans.index('\n')
    infopos = ans.index('\n', reqpos + 1)
    req = ans[:reqpos]
    jsn = json.loads(ans[infopos + 1:])

    d = {}
    if "Result" in jsn:
        res = jsn["Result"]["Results"]
        d = dict((r["Name"], r) for r in res)
    else:
        print("ERROR: no results for request: ", req.encode("utf-8"))

    return (req[1:-1], d)

def printjsn(j):
    return json.dumps(j, indent=2, separators=(',',': '), ensure_ascii=False, sort_keys=True).encode("utf-8")

def testanswers(input, req2ans):
    for r in input:
        rq = r["req"]
        ans = req2ans[rq]
        tests = r["tires"]
        for t in tests:
            nm = t["Name"]
            if nm not in ans:
                print("ERROR: no tire '", nm.encode("utf-8"), "' for '", rq.encode("utf-8"), "' request", sep='')
                continue
            tire = ans[nm]
            for k, v in t.items():
                if k not in tire:
                    if v:
                        print("ERROR: no field '", k.encode("utf-8"), "' for tire '",
                            nm.encode("utf-8"), "' for '", rq.encode("utf-8"), "' request",
                            "; expected value is '", unicode(v).encode("utf-8"), "'", sep='')
                    continue
                if v != tire[k]:
                    print("ERROR: expected value '", unicode(v).encode("utf-8"), "' for field '", k.encode("utf-8"),
                        "' got '", unicode(tire[k]).encode("utf-8"), "' for tire '", nm.encode("utf-8"),
                        "' for '", rq.encode("utf-8"), "' request", sep='')
                    pass

def callguruass(input, command):
    reqs = map(lambda i: i["req"], input)

    pga = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    res = pga.communicate('\n'.join(reqs).encode("utf-8"))[0].decode("utf-8").split('\n\n')

    return dict(splitans(r) for r in res if r)

def parseopt():
    import sys
    from optparse import OptionParser
    parser = OptionParser()
    parser.add_option('--autobase', dest='datadir', help='read wheels data from directory', metavar='DIR')
    parser.add_option('--exec', dest='executable', help='guruassistant binary', metavar='FILE')
    parser.add_option('--requests', dest='requests', help='requests file', metavar='FILE')
    (options, args) = parser.parse_args()

    if not options.datadir or not options.executable or not options.requests:
        parser.print_help(sys.stderr)
        sys.exit(1)
    return options

def main():
    options = parseopt()
    input = loadjson(options.requests)

    req2ans = callguruass(input, (options.executable, '-d', options.datadir))
    testanswers(input, req2ans)

main()
