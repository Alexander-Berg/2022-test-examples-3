#!/usr/bin/env python

import sys, optparse

if __name__ == "__main__":
    parser = optparse.OptionParser()
    parser.add_option('--i1', dest = 'oldIndexInfo')
    parser.add_option('--i2', dest = 'newIndexInfo')
    options, args = parser.parse_args()
    if (len(args) != 2):
        parser.error("need 2 args - old answers and new answers")
    if (bool(options.oldIndexInfo) ^ bool(options.newIndexInfo)):
        parser.error("need 2 indexinfo files or zero")

    ansOld = open(args[0])
    ansNew = open(args[1])
    if options.oldIndexInfo:
        indexinfoWithTSOld = set(open(options.oldIndexInfo).read().strip().split("\n"))
        indexinfoWithTSNew = set(open(options.newIndexInfo).read().strip().split("\n"))
        indexinfoIntersec = set([s.split()[1] for s in indexinfoWithTSOld.intersection(indexinfoWithTSNew)])
    diffoutput = open(args[0].replace(".ans", "") + "-vs-" + args[1].replace(".ans", "") + ".diff", "w")

    for line in ansOld:
        setOld = set(line.split()[2:])
        setNew = set(ansNew.readline().split()[2:])
        diffpos = setNew.difference(setOld)
        diffneg = setOld.difference(setNew)
        if options.oldIndexInfo:
            diffpos = indexinfoIntersec.intersection(diffpos)
            diffneg = indexinfoIntersec.intersection(diffneg)
        if (len(diffpos) + len(diffneg)):
            print >> diffoutput, line.split()[0] + "\t" + "\t".join(["+"] + list(diffpos) + ["-"] + list(diffneg))

    diffoutput.close()
