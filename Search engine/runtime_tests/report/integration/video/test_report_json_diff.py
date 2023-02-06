#!/usr/bin/env python2
# -*- coding: utf-8 -*-

import argparse
import subprocess
import imp
import json
import re
import os

ignored_fields = set([
                     ".tmpl_data.rdat",
                     ".tmpl_data.search_props",
                     ".tmpl_data.search.context.data.Grouping",
                     ".tmpl_data.search.context.data.SearcherProp",
                     ".tmpl_data.search.context.data.SearchProperties",
                     ".tmpl_data.reqdata.headers",
                     ".tmpl_data.highlighter",
                     ".tmpl_data.links.abuse",
                     ".tmpl_data.reqdata.action_key",
                     ".tmpl_data.reqdata.etext",
                     ".tmpl_data.reqdata.hostname",
                     ".tmpl_data.reqdata.ruid",
                     ".tmpl_data.reqdata.flags.yappy-beta",
                     ".tmpl_data.reqparam.icookie",
                     ".tmpl_data.search.request_wizards.Headers",
                     ".tmpl_data.reqdata.time",
                     ".tmpl_data.reqdata.user_time",
                     ".tmpl_data.reqparam.rearr.[0]..scheme_Local/Ugc/UserId",
                     ".tmpl_data.search.context.context.rearr.[0]..scheme_Local/Ugc/UserId",
                     ".tmpl_data.reqparam.wizard.[0].. rtt",
                     ".tmpl_data.reqparam.wizard.[0].. rttvar",
                     ".tmpl_data.search.context.context.wizard.[0].. rtt",
                     ".tmpl_data.search.context.context.wizard.[0].. rttvar",
                     ".tmpl_data.search.context.context.icookie",
                     ".tmpl_data.search.context.data.BalancingInfo.Elapsed",
                     ".tmpl_data.search.context.data.BalancingInfo.WaitInQueue",
                     ".tmpl_data.search.context.data.Head.SegmentId",
                     ".tmpl_data.search.context.data.TotalDocCount",
                     ".tmpl_data.searchdata.numdocs",
                     ".tmpl_data.searchdata.numitems",
                     ".tmpl_data.app_host.user_connection.rtt",
                     ".reqid",
                     ".tmpl_data.search.context.data.DebugInfo.BaseSearchCount",
                     ".tmpl_data.unanswer_data.base_search_count",
                     ".tmpl_data.navi.links",
                     ".tmpl_data.reqdata.deajaxed_url",
                     ".tmpl_data.reqdata.http_host",
                     ".tmpl_data.reqdata.unparsed_uri",
                     ".tmpl_data.reqdata.url",
                     ".tmpl_data.sorter.links.date",
                     ".tmpl_data.sorter.links.relev",
                     ".tmpl_data.search.context.data.DebugInfo",
                     ".tmpl_data.search.request_wizards",
                     ".tmpl_data.unanswer_data",
                     ".tmpl_data.reqdata.experiments"
                 ])

ignored_fields_regex = [re.compile(item) for item in [
                     "^\.tmpl_data.*\.reqid$",
                     ".*parent-reqid$",
                     ".*metahosts.*",
                     ".*yandexuid.*",
                     "^\.tmpl_data\.searchdata\.clips\.\[\d*\]\._markers.*",
                     "^\.tmpl_data\.searchdata\.clips\.\[\d*\]\.basehost$",
                     "^\.tmpl_data\.searchdata\.clips.*group_relevance$",
                     "^\.tmpl_data\.searchdata\.clips.*relevance$",
                     "^\.tmpl_data\.searchdata\.clips.*rvb",
                     "^.clips\.\[\d*\]\.relevance$",
                     "^\.tmpl_data\.navi\.links\.pages.*\.url",
                     ".*clips\.\[\d+\]\.detail_url$",
                     ".*clips\.\[\d+\]\.preview_url$",
                     "^\.tmpl_data\.reqdata\.flags\.its_location"
                 ]]

parsed_fields = set([
                    ".tmpl_data.reqparam.rearr.[0]",
                    ".tmpl_data.search.context.context.rearr.[0]",
                    ".tmpl_data.reqparam.wizard.[0]",
                    ".tmpl_data.search.context.context.wizard.[0]",
                    ".tmpl_data.reqparam.relev.[0]",
                    ".tmpl_data.search.context.context.relev.[0]"
                ])

class ParseJsonSerp(object):
    def __init__(self):
        self.present_fields = set()

    def parse_field(self, obj, keys):
        for item in obj.split(";"):
            data = item.split("=")
            if len(data) == 2:
                new_keys = keys + ".." + data[0]
                if new_keys in ignored_fields:
                    self.present_fields.add(keys)
                    continue
                yield new_keys, data[1]

    def recursive_iter(self, obj, keys=""):
        if keys in ignored_fields:
            self.present_fields.add(keys)
            return
        for expr in ignored_fields_regex:
            if expr.match(keys):
                return
        if keys in parsed_fields:
            for item in self.parse_field(obj, keys):
                yield item
        elif isinstance(obj, dict):
            for k, v in obj.items():
                for item in self.recursive_iter(v, keys + "." + k):
                    yield item
        elif any(isinstance(obj, t) for t in (list, tuple)):
            for idx, item in enumerate(obj):
                for item in self.recursive_iter(item, keys + ".[" + str(idx) + "]"):
                    yield item
        else:
            yield keys, obj

if __name__ == '__main__':
    parser = argparse.ArgumentParser('Report testing script: diff between two serps')
    parser.add_argument('-p', '--prod_server', required=True)
    parser.add_argument('-b', '--beta_server', required=True)
    parser.add_argument('--prod_cgi', default = "")
    parser.add_argument('--beta_cgi', default = "")
    args = parser.parse_args()

    retval = 0
    out_str = ""
    FNULL = open(os.devnull, 'w')
    for request in open("./video_requests.txt"):
            out_str_req = ""
            retval_req = 0
            answrs = []
            out_str_req += "---------------------------------------------\nREQUESTS:\n"
            try:
                for server_data in [(args.prod_server, args.prod_cgi), (args.beta_server, args.beta_cgi)]:
                    full_request = "https://" + server_data[0] + request.strip("\n") + server_data[1] + "&waitall=da&timeout=1000000000"
                    out_str_req += full_request.encode("utf-8") + "\n"
                    proc = subprocess.Popen(["curl", full_request], stdout=subprocess.PIPE, stderr=FNULL)
                    (out, err) = proc.communicate()
                    json_data = json.loads(out)
                    answrs.append(json_data)
            except Exception as e:
                print e
                continue

            ans_sets = []
            present_fields = []
            for ans in answrs:
                ans_set = set()
                parser = ParseJsonSerp()
                for keys, item in parser.recursive_iter(ans):
                    if any(isinstance(item, t) for t in (int, float, long)):
                        item = str(item)
                    if item is None:
                        item = "None"
                    ans_set.add(keys + u":" + item)
                ans_sets.append(ans_set)
                present_fields.append(parser.present_fields)
            diff_fields_new = (present_fields[1] - present_fields[0])
            if len(diff_fields_new) > 0:
                retval_req = 1
                out_str_req += "-----------------------------------------------\nFIELDS FOUND IN NEW SERP ONLY:\n"
                for field in diff_fields_new:
                    out_str_req +=  field.encode("utf-8") + "\n"

            diff_fields_old = (present_fields[0] - present_fields[1])
            if len(diff_fields_old) > 0:
                retval_req = 1
                out_str_req += "-----------------------------------------------\nFIELDS FOUND IN OLD SERP ONLY:\n"
                for field in diff_fields_old:
                    out_str_req += field.encode("utf-8") + "\n"

            diff = (ans_sets[1] - ans_sets[0]) | (ans_sets[0] - ans_sets[1])
            if len(diff) > 0:
                retval_req = 1
                diff_change = set()

                prev_item = ""
                prev_key = ""
                prev_value = ""
                out_str_req_diff = ""
                retval_req_diff = 0
                out_str_req_diff += "------------------------------------------------\nITEMS FOUND IN BOTH SERPS (DIFF):\n"
                for item in sorted(diff):
                    pos = item.find(":")
                    if pos != -1:
                        key = item[:pos]
                        value = item[pos+1:]
                    else:
                        key = ""
                        value = ""
                    if prev_key == key and key != "":
                        retval_req_diff = 1
                        out_str_req_diff += key.encode("utf-8") + "\n"
                        if item in ans_sets[0]:
                            out_str_req_diff += "prod: " + value.encode("utf-8") + "\n"
                            out_str_req_diff += "beta: " + prev_value.encode("utf-8") + "\n"
                        else:
                            out_str_req_diff += "prod: " + prev_value.encode("utf-8") + "\n"
                            out_str_req_diff += "beta: " + value.encode("utf-8") + "\n"
                        diff_change.add(item)
                        diff_change.add(prev_item)
                    prev_item = item
                    prev_key =  key
                    prev_value = value
                if retval_req_diff:
                    out_str_req += out_str_req_diff

                diff_unique = diff - diff_change
                out_str_req_old_items = ""
                retval_req_old_items = 0
                out_str_req_old_items += "----------------------------------------------------\nOLD ITEMS:\n"
                for item in sorted(diff_unique):
                    if item in ans_sets[0]:
                        retval_req_old_items = 1
                        out_str_req_old_items += item.encode("utf-8") + "\n"
                if retval_req_old_items:
                    out_str_req += out_str_req_old_items

                out_str_req_new_items = ""
                retval_req_new_items = 0
                out_str_req_new_items += "----------------------------------------------------\nNEW ITEMS:\n"
                for item in sorted(diff_unique):
                    if item in ans_sets[1]:
                        retval_req_new_items = 1
                        out_str_req_new_items += item.encode("utf-8") + "\n"
                if retval_req_new_items:
                    out_str_req += out_str_req_new_items
            if retval_req:
                out_str += out_str_req
                retval = 1
    if retval == 1:
        print out_str
    f = open("./output", "w")
    print >> f, out_str
    f.close()
