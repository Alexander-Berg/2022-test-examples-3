import logging
import json
import re
import sys

import yatest.common

from yamarec1.udfs.statistics import (
    parse_metrika_log_record, MetrikaLogRecordParser, recommendation_type_keywords, old_special_goals,
    special_pricedrop_goals, additional_context_keys, extract_handler_info,
)
import yamarec1.config.loader

from functions_for_generation import (
    hit_functions, mobile_functions, type_id, default_mobile_batch, default_desktop_batch, type_id_unhashed,
    hit_to_event_mapping,
)
from templates import bs_types, mobile_types

logger = logging.getLogger(__name__)

if yatest.common.context.test_stderr:
    logger.addHandler(logging.StreamHandler(stream=sys.stderr))


def insert_entity_into_pattern(entity, pattern, goal):
    params = json.loads(pattern)
    if "garsons" not in params and goal in special_pricedrop_goals:
        params['reqId'] = entity['batch']
        params['productId'] = type_id['model']
        return json.dumps(params)

    if goal in old_special_goals:
        params['reqId'] = entity['batch']
        params['snippet']['skuId'] = type_id['sku']
        return json.dumps(params)

    if 'reqId' in params:
        params['reqId'] = entity['batch']

    if entity['type'] == 'model':
        entity['type'] = 'product'

    if "block" in params and isinstance(params["block"], dict) and "elements" in params["block"] or "elements" in params:
        if "elements" in params:
            block = params
        else:
            block = params["block"]
        for element in block["elements"]:
            if not isinstance(element, dict):
                continue

            content = element.get("content", {})
            if not isinstance(content, dict):
                continue

            if 'subEntity' in content:
                content['tags'] = [entity['tags']]
                content['entity'] = 'taggedEntity'
                content = content['subEntity']

            if 'type' in content:
                content['entity'] = 'page'
                content['type'] = entity['type']
            else:
                content['entity'] = entity['type']

            if "id" in content:
                content['id'] = entity['id']
            elif "wareId" in content:
                content['wareId'] = entity['id']
            elif "wareid" in content:
                content['wareid'] = entity['id']

            return json.dumps(params)
    else:
        handler_info, handler_key = extract_handler_info(params)

        if handler_info is None:
            return

        if entity['tags'].find(':') == -1:
            handler_info[handler_key] = entity['tags']
        else:
            handler_info[handler_key] = entity['tags'][: entity['tags'].find(':')]

        if "params" in handler_info and isinstance(handler_info["params"], dict) and "tag" in handler_info["params"]:
            params_tag_index = entity['tags'].find(':')
            if isinstance(handler_info["params"]["tag"], list):
                if params_tag_index != -1:
                    handler_info["params"]["tag"] = entity['tags'][params_tag_index + 1:].split('_')
                else:
                    handler_info["params"]["tag"] = []
            else:
                if params_tag_index != -1:
                    handler_info["params"]["tag"] = entity['tags'][params_tag_index + 1:]
                else:
                    handler_info["params"]["tag"] = ''

        # putting all recommendation types from entity into the handler_info for tests
        recommendation_type_values = entity["recommendation_type"].split(':')

        position = 0
        for keyword in recommendation_type_keywords:
            if keyword in handler_info:
                handler_info[keyword] = recommendation_type_values[position]
                position += 1
            elif "params" in handler_info and isinstance(handler_info["params"], dict) and keyword in handler_info["params"]:
                handler_info["params"][keyword] = recommendation_type_values[position]
                position += 1
            elif keyword in params:
                params[keyword] = recommendation_type_values[position]
                position += 1
            if position == len(recommendation_type_values):
                break  # all types present in the pattern have been put into the handler_info

        # seems needless for tests
        if "cmsPageId" in params:
            params['cmsPageId'] = 0

        if "productId" in params:
            params["productId"] = entity['id'] + '_' + 'ababsb'
        elif "skuId" in params:
            params["skuId"] = entity['id']
        elif "wareId" in params:
            params["wareId"] = entity['id']
        elif "vendorId" in params:
            params["vendorId"] = entity['id']
        elif "reviewId" in params:
            params["reviewId"] = entity['id']
        elif "navnodeId" in params:
            params["navnodeId"] = type_id_unhashed.get(entity['type'], '0')
        elif "categoryId" in params:
            params["categoryId"] = type_id_unhashed.get(entity['type'], '0')
        elif "hubId" in params:
            params["hubId"] = entity['id']
        elif "entrypointId" in params:
            params["entrypointId"] = entity['id']
        elif "articleId" in params:
            params["articleId"] = entity['id']
        elif "shopId" in params:
            params["shopId"] = entity['id']
        elif "hid" in params:
            params["hid"] = type_id_unhashed.get(entity['type'], '0')
        return json.dumps(params)


def generate_events(action, place_name, action_name):
    if 'records' not in action:
        return []
    records = action.records
    events = []
    for tuple_params in records:
        function_name, record_type = tuple_params[:2]
        events.append(hit_to_event_mapping[function_name](action_name, place_name, record_type))
    return events


def find_max_goal_except_special(pattern, text):
    all_matches = re.findall(pattern, text)
    allowed = ['' if matched_str in old_special_goals else matched_str for matched_str in all_matches]
    return max(allowed, key=len)


def find_max_recommendation_type(pattern, text):
    all_matches = re.findall(pattern, text)
    return max(all_matches, key=len)


def replace_words(pat):
    word = re.search('\\([a-z]*\\|', pat)
    if word is None or word.group() == '':
        return pat
    return re.sub('\\(.*\\)', word.group()[1:-1], pat)


parser = MetrikaLogRecordParser()


def get_appropriate_tag(tag_pattern, empty_params, goal, debug=False):
    all_tags = sorted(re.findall('[\\w\\-:]*', tag_pattern), key=len)[::-1]
    if debug:
        logger.debug('get_appropriate_tag()')
    for tag in all_tags:
        if tag.startswith(':'):
            tag = tag[1:]
        entity = {
            'type': 'navnode',
            'id': type_id['navnode'],
            'tags': tag,
            'batch': '1',
            'recommendation_type': '',
        }
        if debug:
            logger.debug('tag: {}'.format(tag))
            logger.debug('entity: {}'.format(entity))
        try:
            filled_in_pattern = {
                'params': insert_entity_into_pattern(entity, empty_params, goal),
                'goal': goal,
                'hit_id': '1'}
            if debug:
                logger.debug('filled_in_pattern: {}'.format(filled_in_pattern))
            for entity in parser._parse_metrika_params(filled_in_pattern):
                if debug:
                    logger.debug('entity from _parse_metrika_params(): {}'.format(entity))
                if re.search(tag_pattern, entity['tags']) is not None:
                    return tag
        except:
            continue
    return ''


def generate_records(action_name, action, debug=False):
    records = []
    record_tags = []
    record_goals = []
    record_params = []
    if 'records' not in action:
        return records, record_tags, record_goals, record_params
    goal = find_max_goal_except_special('[\\w\\-:]*', replace_words(action['goal_pattern']))
    if 'tag_pattern' not in action:
        tag_pattern = ''
    else:
        tag_pattern = replace_words(action['tag_pattern'])
    if 'recommendation_type_pattern' not in action:
        recommendation_type = ''
    else:
        recommendation_type = find_max_recommendation_type('[\\w\\-:]*', replace_words(action['recommendation_type_pattern']))

    if debug:
        logger.debug('The config within generate_records')
        logger.debug(goal)
        logger.debug(tag_pattern)
        logger.debug(recommendation_type)

    for tuple_params in action.records:
        function_name, record_type, template_index = tuple_params[:3]
        function = hit_functions[function_name]
        empty_params = mobile_types[template_index] if function_name in mobile_functions else bs_types[template_index]
        default_batch = default_mobile_batch if function_name in mobile_functions else default_desktop_batch
        entity = {
            'type': record_type,
            'id': type_id[record_type],
            'tags': tuple_params[4] if len(tuple_params) > 4 and tuple_params[4] is not None else get_appropriate_tag(tag_pattern, empty_params, goal, debug),
            'batch': default_batch,
            'recommendation_type': recommendation_type,
        }
        if debug:
            logger.debug('entity')
            logger.debug(entity)
        if record_type == "navnode":
            entity["unhashed_navnode_id"] = type_id_unhashed[record_type]
            # other additional keys are not checked in these unit tests
        local_goal = tuple_params[3] if len(tuple_params) > 3 and tuple_params[3] is not None else goal
        params = insert_entity_into_pattern(entity, empty_params, local_goal)
        record_tags.append(entity['tags'])
        record_goals.append(local_goal)
        record_params.append(tuple_params)
        if debug:
            logger.debug('record')
            logger.debug(action_name)
            logger.debug(local_goal)
            logger.debug(params)
        records.append(function(action_name, local_goal, params))
    return records, record_tags, record_goals, record_params


def form_event_from_parsed_data(parsed_data, action):
    if len(parsed_data) == 0:
        return 0
    # unwrap
    assert len(parsed_data) == 1
    parsed_data = parsed_data[0]
    # this test does not check additional_context_keys
    context = {key: value for key, value in parsed_data[3].items() if key not in additional_context_keys}
    return parsed_data[0], action, parsed_data[1], parsed_data[2], context


def prepare_array(arr):
    return json.loads(json.dumps(arr))


def test_generated_by_statistics_config():
    path = "resfs/file/market/yamarec/yamarec/yamarec1/statistics_config/"
    settings = yamarec1.config.loader.load_from_resource(path)

    message_template = "In place %s, action %s cannot match record with given params %s, extracted (or provided explicitly) goal is %s, extracted (or provided explicitly) tag is %s"

    for place_name in settings.places:
        place = settings.places[place_name]
        for action_name in ['preview', 'click']:
            if action_name not in place:
                continue
            if 'recommendation_type_pattern' not in place:
                continue
            records, record_tags, record_goals, record_params = generate_records(action_name, place[action_name])
            events = generate_events(place[action_name], place_name, action_name)
            parsed_records = [form_event_from_parsed_data(parse_metrika_log_record(record), action_name) for record in records]
            for x, y, tag, goal, param in zip(prepare_array(parsed_records), prepare_array(events), record_tags, record_goals, record_params):
                try:
                    assert x == y, message_template % (place_name, action_name, param, goal, tag)
                except AssertionError:
                    logger.debug('The assertaton has failed for {}, {}'.format(action_name, place[action_name]))
                    logger.debug('The place config:')
                    for place_config_field in ['goal_pattern', 'tag_pattern', 'recommendation_type_pattern', 'records']:
                        logger.debug(place[action_name][place_config_field])
                    logger.debug('generate_records()')
                    records, _, _, _ = generate_records(action_name, place[action_name], debug=True)
                    logger.debug('The records are')
                    for k, rec in enumerate(records):
                        logger.debug('#{}: {}'.format(k, rec))
                    logger.debug('generate_events()')
                    logger.debug(events)
                    logger.debug('parsed records')
                    logger.debug(parsed_records)
                    logger.debug('x, y, tag, goal, param')
                    logger.debug(x)
                    logger.debug(y)
                    logger.debug(tag)
                    logger.debug(goal)
                    logger.debug(param)
                    raise
            assert len(parsed_records) == len(events), "Probably some records were not parsed"
