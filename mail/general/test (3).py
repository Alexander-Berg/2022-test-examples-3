#! /usr/bin/python

import sys

sys.path.insert(0, "../build/python")

import blackbox2

for i in xrange(0,10000000):
    options = blackbox2.Options();
    options.add( blackbox2.Option("aa", "bb") );

    result = blackbox2.InfoRequest("1212", "127.0.0.1", options);

    #    print(result);

    req_body = "<?xml version=\"1.0\" encoding=\"utf-8\"?><doc><status id=\"0\">VALID</status><error>OK</error><uid hosted=\"0\" domid=\"\" domain=\"\">20903734</uid><karma confirmed=\"0\">75</karma></doc>";

    response = blackbox2.Response(req_body);

    karmainfo = blackbox2.KarmaInfo(response);
#    print(karmainfo.karma())

sys.path.remove("../build/python")

