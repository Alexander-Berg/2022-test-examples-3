#!/usr/bin/python3.6
import os
import sys
import json
import codecs

DELETE_FROM_URI = ",use-new-report-init"

HEADERS = {
    'x-real-remote-ip',
    'x-region-affected-exps',
    'x-region-city-id',
    'x-region-home',
    'x-region-id',
    'x-region-interest',
    'x-region-location',
    'x-region-precision',
    'x-region-suspected',
    'x-region-suspected-city',
    'x-region-suspected-location',
    'x-region-suspected-precision',
    'x-source-port-y',
    'x-start-time',
    'x-yandex-expboxes',
    'x-yandex-expsplitparams',
}


DELETE_KEYS = {
    '__debug_info',
    'version',
    '__compressed__',
    '_src_setup',
    '__who__',
    'x-yandex-randomuid',
    'x-yandex-expflags',
    'x-req-id',
    'x-yandex-expboxes-crypted',
    'x-metabalancer-fqdn',
    'disable_recommendation',
    'timeout',
    'waitall',
    'nocache',
    'test-id',
    'HEADERS',
    "debug",
    "dump_source_requests",
    "dump_source_responses",
    "graphrwr",
    "is_internal",
    "is_suspected_robot",
    "report_graphrwr",
    "reqid",
    "ruid",
    "srcask",
    'use-new-report-init',
    'time_epoch',
    'is_itp',
    'is_itp_fake_cookie',
    'is_itp_maybe',
    'is_same_site_supported',
    'yt',
    'ids',
    'slots',
    'module_version',
    'alias_json_version_for_abt',
    'REPORT',
    'request_time',
    'start_time',
    'sdch_group',
    'r',
    'RESTRICTIONS',
    'TESTID',
    'disable_flag_sections',
    'images_wizard_page_size',
    'flags_json_version',
    'is_prestable',
    'headers',
    'port',
    'id',
    'location',
    'name',
    'name_l10n',
    'path',
    'span',
    'has_user_num_doc',
    'text_props',
    'cookies',
    'cookie',
    'lr',
    'is_html_dump',
    'is_prefetch',
    'WEB',
    'WEB_MISSPELL',
    'car_brand',
    'images_wizard_vcomm_threshold',
    'etext',
    'itags_geo',
    'enable_misspell',
    'enable_reask',
    'HTTP_RESPONSE',
    'dump_source_request',
    'dump_source_response',
    'yandexcom',
    'search_app_lang',
    'is_full_stack_log_for_bans',
    'family',

}.union(HEADERS)

DELETE_TYPES = {
    '__debug_info',
    'true',
    'blackbox_multisession',
    'blackbox',
    'region',
    'log_access',
}


DELETE_LIST_ITEMS = {
    'template-is-lowload',
    'scheme_Local/TurboAppManualUpper/AddTurboLabel=no',
    'scheme_Local/TurboAppManualUpper/Enabled=1',
    'scheme_Local/TurboAppManualUpper/ReplaceUrl=\"\"',
    'rearr=scheme_Local/ImgCommercial/AllExceptMarketIsSimilar=1',
    'rearr=scheme_Local/ImgCommercial/MaxOffersCountForOneDocument=10',
    'rearr=scheme_Local/ImgCommercial/ShowSingleSourcePerDocument=1',
    'use-new-report-init',
}


def uniform_simple(item):
    if isinstance(item, bool):
        item = str(int(item))

    if isinstance(item, int):
        item = str(item)

    return item


def sort_arrays(tree):
    if isinstance(tree, list):
        if tree and not isinstance(tree[0], dict):
            tree.sort()
        delete_items = []
        for item in tree:
            if isinstance(item, str) and item in DELETE_LIST_ITEMS:
                delete_items.append(item)
            item = uniform_simple(item)
            sort_arrays(item)
        for item in delete_items:
            tree.remove(item)
    elif isinstance(tree, dict):
        deleted_keys = set()
        for key in tree.keys():
            tree[key] = uniform_simple(tree[key])
            if key == 'uri':
                uri = tree[key]
                i = uri.find(DELETE_FROM_URI)
                tree[key] = (uri[:i].strip('&') + '&' + uri[i + len(DELETE_FROM_URI):].strip('&')).strip('&') if (i + 1) else uri
            if key in DELETE_KEYS:
                deleted_keys.add(key)
            else:
                sort_arrays(tree[key])
            if not tree[key]:
                deleted_keys.add(key)
        for key in deleted_keys:
            del(tree[key])


def format_response(json_string):
    js = json.loads(json_string)
    for item in js['answers']:
        if 'version' in item and item['version'] == 'INIT.report 2.0':
            js['answers'].remove(item)
            break
    items_to_remove = []
    for item in js['answers']:
        if 'type' in item and item['type'] in DELETE_TYPES:
            items_to_remove.append(item)
    for item in items_to_remove:
        js['answers'].remove(item)

    if 'named-answers' in js:
        del(js['named-answers'])

    sort_arrays(js)
    if 'answers' in js:
        js['answers'].sort(key=lambda x: x['type'] if 'type' in x else '0')

    return json.dumps(js, indent=2, sort_keys=True)


def read_single_scheme_responses(filename):
    responses = {}
    f = codecs.open(filename, 'r', encoding='utf-8', errors='ignore')
    for line in f:
        [reqid, json_string] = line.strip().split('\t')

        responses[reqid] = format_response(json_string)
    return responses


def read_responses():
    first_scheme_responses = read_single_scheme_responses(sys.argv[1])
    second_scheme_responses = read_single_scheme_responses(sys.argv[2])

    i = 0
    j = 0
    for reqid in first_scheme_responses:
        if reqid in second_scheme_responses:
            j += 1
            if first_scheme_responses[reqid] != second_scheme_responses[reqid]:
                with open(os.path.join(sys.argv[3], str(i)+'_left'), 'w+') as out_left:
                    out_left.write(reqid + '\n')
                    out_left.write(first_scheme_responses[reqid])
                with open(os.path.join(sys.argv[3], str(i)+'_right'), 'w+') as out_right:
                    out_right.write(second_scheme_responses[reqid])
                i += 1
    print("Diff in " + str(i) + "/" + str(j) + " requeests; left total: " + str(len(first_scheme_responses)) + "; right total:" + str(len(second_scheme_responses)))


if __name__ == '__main__':
    read_responses()
