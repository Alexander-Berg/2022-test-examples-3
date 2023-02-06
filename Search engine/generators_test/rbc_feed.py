#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import re
import sys
import json
import argparse
from xml.dom import minidom

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def modify_companies(data):
    xml_doc = minidom.parseString(data)
    companies = xml_doc.getElementsByTagName('company')
    pattern = re.compile('"([^"]+)"')
    for company_xml in companies:
        src_companny_name = misc.get_text(company_xml, 'name')
        result = pattern.search(src_companny_name)
        if result:
            company_name = result.group(1)
        else:
            company_name = src_companny_name
        misc.replace_text(company_xml, 'name', company_name)
        alt_company_name = xml_doc.createElement('name-other')
        alt_company_name.setAttribute('lang', 'ru')
        alt_company_name_text = xml_doc.createTextNode(src_companny_name)
        alt_company_name.appendChild(alt_company_name_text)
        company_xml.appendChild(alt_company_name)
        info_page = xml_doc.createElement('info-page')
        info_page_text = xml_doc.createTextNode(misc.get_text(company_xml, 'url'))
        info_page.appendChild(info_page_text)
        company_xml.appendChild(info_page)
        company_xml.removeChild(company_xml.getElementsByTagName('url')[0])
    return xml_doc

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates RBC feed')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    params = json.loads(args.parameters)
    xml_data = misc.download(params.get('input_table'))
    feed = modify_companies(xml_data)
    yt_client.write_file(params.get('output_path'), feed.toxml())
