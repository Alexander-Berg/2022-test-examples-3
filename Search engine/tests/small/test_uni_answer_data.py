import codecs
import string
import re
import json
from collections import defaultdict


VALID_COMBINATION_ATTRS = set(['Expr', 'PostExpr', 'Anatomy', 'Priority', 'RightColumn'])
VALID_ANATOMY_BLOCK_ATTRS = set(['Expr', 'Block', 'Pos', 'Meta', 'Optional', 'ViewTabIndex', 'ViewGroup', 'Action', 'blndrViewType', 'MakeVideoFact'])
VALID_ANATOMY_BLOCK_ACTIONS = set(['move', 'copy', 'remove'])
VALID_BLOCK_ATTRS = set(['Expr', 'Intent', 'SerpDataType', 'SerpDataPattern', 'Hosts', 'Markers', 'Grouping'])


def check_json_correctness(framework_str):
    def get_words_dict(text):
        word2count = defaultdict(int)
        for word in re.split(r'[^\w]', text):
            if word:
                word2count[word] += 1
        return word2count

    basic_dict = get_words_dict(framework_str)

    framework_jsn = json.loads(framework_str)
    result_str = json.dumps(framework_jsn, ensure_ascii=False)
    result_dict = get_words_dict(result_str)

    keys = set().union(basic_dict.keys() + result_dict.keys())
    diffs = []
    for key in keys:
        if key not in basic_dict:
            diffs.append('new word after loads/dumps: %s' % key)
        elif key not in result_dict:
            diffs.append('word disappeared: %s' % key)
        elif basic_dict[key] != result_dict[key]:
            diffs.append('word value changed: word=%s diff=%d' % (key, basic_dict[key] - result_dict[key]))

    if diffs:
        raise Exception('Different words count before/after json.loads/json.dumps:\n' + '\n'.join(diffs))


def parse_flags_from_expr(place, expr, flag2used):
    for token in re.split('[' + string.whitespace + '\(\)=]', expr):
        if token.startswith('flag.'):
            flag = token[5:]
            if flag not in flag2used:
                raise Exception('unknown flag in %s: %s' % (place, flag))
            flag2used[flag] = True


def check_unused(name, item2used):
    for (item, used) in item2used.iteritems():
        if not used:
            raise Exception('unused %s: %s' % (name, item))


def check_valid_attrs(place, attrs, valid_attrs):
    for attr in attrs:
        if attr not in valid_attrs:
            raise Exception('unknown attr in %s: %s' % (place, attr))


def test_framework(file_path):
    with codecs.open(file_path, encoding='utf8') as framework_file:
        framework_str = framework_file.read()

    check_json_correctness(framework_str)
    framework_jsn = json.loads(framework_str)

    flag2used = {flag: False for flag in framework_jsn['Flags']}
    block2used = {block: False for block in framework_jsn['Blocks']}
    grouping2used = {grouping: False for grouping in framework_jsn['InputGroupings']}

    for (combination_name, combination) in framework_jsn['Combinations'].iteritems():
        check_valid_attrs('combination \'%s\'' % combination_name, combination, VALID_COMBINATION_ATTRS)
        if 'Expr' in combination:
            parse_flags_from_expr('combination \'%s\'' % combination_name, combination['Expr'], flag2used)
        if 'PostExpr' in combination:
            parse_flags_from_expr('postexpr in combination \'%s\'' % combination_name, combination['PostExpr'], flag2used)
        positions = set()
        for (anatomy_block_name, anatomy_block) in combination['Anatomy'].iteritems():
            check_valid_attrs('anatomy block \'%s\' in combination \'%s\'' % (anatomy_block_name, combination_name), anatomy_block, VALID_ANATOMY_BLOCK_ATTRS)
            if 'Action' in anatomy_block:
                action = anatomy_block["Action"]
                if action not in VALID_ANATOMY_BLOCK_ACTIONS:
                    raise Exception('unknown action in block \'%s\' in combination \'%s\': %s' % (anatomy_block_name, combination_name, action))
            if 'Expr' in anatomy_block:
                parse_flags_from_expr('anatomy block \'%s\' in combination \'%s\'' % (anatomy_block_name, combination_name), anatomy_block['Expr'], flag2used)
            if 'Block' not in anatomy_block:
                raise Exception('undefined Block in anatomy block \'%s\' in combination \'%s\'' % (anatomy_block_name, combination_name))
            block = anatomy_block['Block']
            if block not in framework_jsn['Blocks']:
                raise Exception('unknown block in combination \'%s\': %s' % (combination_name, block))
            block2used[block] = True

            if 'Pos' not in anatomy_block:
                raise Exception('undefined Pos in anatomy block \'%s\' in combination \'%s\'' % (anatomy_block_name, combination_name))
            pos = anatomy_block['Pos']
            if pos in positions and pos != '*':
                raise Exception('same position (%s) in block \'%s\' in combination \'%s\'' % (unicode(pos), anatomy_block_name, combination_name))
            if not (pos == '*' or type(pos) == int):
                raise Exception('invalid position (%s) in block \'%s\' in combination \'%s\'' % (unicode(pos), anatomy_block_name, combination_name))
            positions.add(pos)

    for (block_name, block) in framework_jsn['Blocks'].iteritems():
        check_valid_attrs('block \'%s\'' % block_name, block, VALID_BLOCK_ATTRS)
        if 'Expr' in block:
            parse_flags_from_expr('block ' + block_name, block['Expr'], flag2used)
        if 'Grouping' in block:
            grouping = block['Grouping']
            if grouping not in grouping2used:
                raise Exception('unknown grouping in block \'%s\': %s' % (block_name, grouping))
            grouping2used[grouping] = True

    check_unused('flag', flag2used)
    check_unused('block', block2used)
    check_unused('grouping', grouping2used)
