from __future__ import print_function

import json
import sys
import types

def parseopt():
    from optparse import OptionParser
    usage = "usage: %prog -a assistant_base_file -g guru_base_file\ncompares the bases"
    parser = OptionParser(usage=usage)
    parser.add_option('-a', dest='abase', help='read assistant data from FILE', metavar='FILE')
    parser.add_option('-g', dest='gurubase', help='read guru data from FILE', metavar='FILE')
    (options, args) = parser.parse_args()

    for option in (options.gurubase, options.abase):
        if not option:
            parser.print_help(sys.stderr)
            sys.exit(1)

    return options

def loadjson(filename):
    json_file = open(filename)
    return json.load(json_file)

def readgurubase(filename):
    data = loadjson(filename)

    ret = {}
    for item in data:
        filt = []
        for f in item["Properties"]["Ids"].items():
            filt.append("gfilter=" + f[0] + ":-" + f[1])
        if filt:
            names = []
            for n in item["Properties"]["Names"].items():
                names.append(n[0] + ": " + n[1])
            ret[frozenset(filt)] = (item["VendorName"] + ": " + item["ModelName"], names)

    return ret


def key_from_item(it):
    if type(it) == types.StringType or type(it) == types.UnicodeType:
        return it

    if type(it) == types.DictType:
        for k in ("__name__", "__", "__attrm__"):
            if k in it:
                return key_from_item(it[k])

    return ""

def updatepath(path, item):
    name = key_from_item(item)
    if not name:
        return path;

    if not path:
        return name

    return path + "->" + name

def addfilter(filt, path, res):
    path = updatepath(path, filt)
    arr = []
    for (k,v) in filt["__filter__"].items():
        for i in v:
            arr.append(k + "=" + i)

    names = []
    for (k,v) in filt.items():
        if len(k) > 1 and k[0] == "_" and k[1] == "_":
            continue
        names.append(k + ": " + v)

    res[frozenset(arr)] = (path, names)

def addfilters(item, path, res):
    for filt in item:
        addfilter(filt, path, res)

def parseass(item, path, res):
    path = updatepath(path, item)

    if type(item) == types.ListType:
        for it in item:
            parseass(it, path, res)
        return

    if type(item) == types.DictType:
        if "__filters__" in item:
            addfilters(item["__filters__"], path, res)
        if "__chapters__" in item:
            parseass(item["__chapters__"], path, res)

def loadassistantbase(filename):
    data = loadjson(filename)

    ret = {}
    parseass(data["__data__"], "", ret)

    return ret

def compare(base1, base2):
    res = []
    for (k,v) in base1.items():
        if k not in base2:
            res.append((v, k))

    return sorted(res)

def printdiff(d):
    for it in d:
        print ("\t", it[0][0].encode("utf-8"))
        for k in it[0][1]:
            print ("\t\t", k.encode("utf-8"))
        print()



def main():
    options = parseopt()

    gurubase = readgurubase(options.gurubase)
    abase = loadassistantbase(options.abase)

    d1 = compare(gurubase, abase)
    if d1:
        print ("Extra keys in guru:")
        printdiff(d1)

    d2 = compare(abase, gurubase)
    if d2:
        print ("Extra keys in assistant:")
        printdiff(d2)

main()

