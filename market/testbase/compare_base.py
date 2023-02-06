from __future__ import print_function

import json
import types
import sys

class Logger(object):
    def __init__(self, name1, name2):
        self.name1 = name1
        self.name2 = name2
        self.path = []

    def add_path(self, name):
        if type(name) == types.TupleType:
            self.path.append(name[0])
        else:
            self.path.append(name)

    def pop_path(self):
        self.path.pop()

    def print_path(self):
        ret = "root"
        for s in self.path:
            if s != "__chapters__":
                ret = ret + "->" + s
        return ret

    def log(self, *objects):
        print(self.print_path().encode("utf-8"))
        print("\t", end="")
        for i in objects:
            print(repr(i).decode('unicode-escape').encode("utf-8"), end="")
        print("\n")

class NoNameException(Exception):
    def __init__(self):
        pass

def key_from_item(it):
    if type(it) == types.StringType or type(it) == types.UnicodeType:
        return (it, 0)

    if type(it) == types.DictType:
        if "__name__" in it:
            return (it["__name__"], 1)
        if "__attrm__" in it:
            it = it["__attrm__"]
            if type(it) == types.DictType and "__name__" in it:
                return (it["__name__"], 2)
        if "__chaptername__" in it:
            return (it["__chaptername__"], 3)

    raise NoNameException()

def list2dict(lst):
    ret = {}
    for it in lst:
        k = key_from_item(it)
        ret[k[0]] = it
    return ret

def compare_list(l1, l2, logger):
    d1 = {}
    d2 = {}
    try:
        d1 = list2dict(l1)
    except NoNameException:
        logger.log("can't convert list from ", logger.name1, " to dict")

    try:
        d2 = list2dict(l2)
    except NoNameException:
        logger.log("can't convert list from ", logger.name2, " to dict")

    return compare_dict(d1, d2, logger)


def extra_keys(d1, d2):
    ret = []
    for k in d1.keys():
        if k not in d2:
            ret.append(k)
    return ret;


def compare_dict(d1, d2, logger):
    e1 = extra_keys(d1, d2)
    e2 = extra_keys(d2, d1)

    if e1 or e2:
        msg1 = ("there are extra keys in ", logger.name1, ": ", e1)
        msg2 = ("there are extra keys in ", logger.name2, ": ", e2)
        if e1 and e2:
            logger.log(*(msg1 + ("\n\t", ) + msg2))
        elif e1:
            logger.log(*msg1)
        else:
            logger.log(*msg2)


    for k in d1.keys():
        if k not in d2:
            continue
        logger.add_path(k)
        compare(d1[k], d2[k], logger)
        logger.pop_path()
    return True


def compare_int(s1, s2, logger):
    if type(s1) == types.DictType:
#        print (logger.print_path(), "compare as dicts")
        return compare_dict(s1, s2, logger)

    if type(s1) == types.ListType:
#        print (logger.print_path(), "compare as lists")
        return compare_list(s1, s2, logger)

#    print (logger.print_path(), "compare as strings")
    return s1 == s2

def compare(s1, s2, logger):
    if type(s1) != type(s2):
        logger.log("types are not equal (", type(s1), " and ", type(s2), ")")
        return

    if not compare_int(s1, s2, logger):
        if type(s1) == types.StringType or type(s1) == types.UnicodeType:
            logger.log("strings \"", s1, "\" and \"", s2, "\" are not equal")
        else:
            logger.log("structs are not equal")

def loadjson(filename):
    import gzip

    json_file = None
    if filename.endswith(".gz"):
        json_file = gzip.open(filename)
    else:
        json_file = open(filename)
    return json.load(json_file)


def main():
    if len (sys.argv) != 3:
        print(sys.argv[0], "file1 file2\ncompares two guruassistant data files", file=sys.stderr)
        return 1

    filename1 = sys.argv[1]
    filename2 = sys.argv[2]

    logger = Logger(filename1, filename2)

    data1 = loadjson(filename1)
    data2 = loadjson(filename2)

    compare(data1, data2, logger)
    return 0

main()
