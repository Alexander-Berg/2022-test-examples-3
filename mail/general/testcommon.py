import sys
import libxml2

cur_path = sys.path[0]
if cur_path:
    cur_path += "/"

sys.path.insert(0, cur_path + ".libs")
sys.path.insert(0, cur_path + "../build/python")

import blackbox2 as bb

# Get XML child text for given xpath
def xpath_get ( ctxt, path ) :
    res = ctxt.xpathEval(path)
    if len(res) == 1:
        return res[0].getContent()
    return ""

def xpath_get_all ( ctxt, path ) :
    res = ctxt.xpathEval(path)
    return [u.getContent() for u in res]

# Check if XML node with given xpath exists
def xpath_exists ( ctxt, path ):
    res = ctxt.xpathEval(path)
    return len(res) > 0


# Compare uri with reference, currently just string compare
def match_uri ( uri, ref ) :
    if uri != ref :
        print "Request uri do not match!\n", \
            "Request  : '", uri , "'\n", \
            "Reference: '", ref , "'\n"
    return uri == ref

# Compare post data with reference, currently just string compare
def match_data ( data , ref ) :
    if data != ref :
        print "Login request bodies do not match!\n", \
            "Request  : '", data , "'\n", \
            "Reference: '", ref , "'\n"
    return data == ref

