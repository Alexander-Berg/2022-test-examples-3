#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import sys
import logging
import time
import re
import json
import argparse
import shutil
from lxml import etree
from urlparse import urlparse
from freezegun import freeze_time

import market.pylibrary.yenv as yenv

from generator import suggests_dumper

log = logging.getLogger('tests')

MOCKED_TIME = '2016-12-31 03:00'


def read_file_content(filename):
    f = open(filename, 'r')
    text = f.read()
    f.close()
    return text


def get_suggests_files(workdir):
    file2count = dict()
    for f in filter(lambda f: f.endswith('xml'), os.listdir(workdir)):
        filepath = os.path.join(workdir, f)
        content = read_file_content(filepath)
        # в tsv в записи канонического саджеста второе поле пусто, можно проверять по двум табам
        suggests_count = content.count('<name>') if f.endswith('xml') else content.count('\t\t')
        file2count[f] = suggests_count
    return file2count


def get_tag_value(text):
    if text.lstrip().startswith('</'):
        return ''
    begin = text.find('>')
    end = text.rfind('<')
    if begin == -1 or end == -1:
        return ''
    return text[begin + 1: end]


def test_img(params, must_exist=False):
    present = False
    if 'img' in params:
        img = params['img']
        if img is not None:
            present = True
            if not img.startswith('//avatars.mds.yandex.net/get-mpic') or '&size=' in img:
                raise Exception(
                    'img {} should have format like '.format(img) +
                    '//avatars.mds.yandex.net/get-mpic/0000000/img_id0000000000000000000/orig#200#194')

    if must_exist and not present:
        raise Exception('No image in rich_props %s' % str(params))


def get_rich_props(suggests_text, mobile=False, count=None):
    tag = 'rich_props_mob' if mobile else 'rich_props'
    rich_props_template = '<{tag}>.*</{tag}>'.format(tag=tag)
    all_rich_props = re.findall(rich_props_template, suggests_text)

    if count is not None and len(all_rich_props) != count:
        raise Exception('Expected {} {} but found {}'.format(count, tag, len(all_rich_props)))

    # correct format is <![CDATA[[{json}]]]>
    cdata_prefix = '<![CDATA[['
    cdata_postfix = ']]]>'
    for object_rich_props in all_rich_props:
        tag_value = get_tag_value(object_rich_props)
        if not tag_value.startswith(cdata_prefix) or not tag_value.endswith(cdata_postfix):
            raise Exception('rich props tag has wrong format')
        json_str = tag_value[len(cdata_prefix): -len(cdata_postfix)]
        yield json.loads(json_str)


def get_objects(suggests_text, suggest_type=None):
    obj_template = r'<object type="{}">.*?</object>'.format(suggest_type if suggest_type else '.*?')
    all_obj = re.findall(obj_template, suggests_text)
    for obj in all_obj:
        yield obj


def test_rich_props_params(params):
    test_img(params)


def test_rich_props(suggests_text, suggest_count):
    for params in get_rich_props(suggests_text, count=suggest_count):
        test_rich_props_params(params)


def test_rich_props_mob(suggests_text):
    for params in get_rich_props(suggests_text, mobile=True):
        test_rich_props_params(params)


def test_required_params(suggests_text):
    log.info(' ==== TEST: suggest, suggest_type, suggest_text ====')

    n_urls = len(re.findall(r'<url>', suggests_text))
    if len(re.findall(r'<url>.*suggest=1', suggests_text)) != n_urls:
        raise Exception('suggest=1 not in all suggests')
    if len(re.findall(r'<url>.*suggest_type', suggests_text)) != n_urls:
        raise Exception('suggest_type not in all suggests')
    if len(re.findall(r'<url>.*suggest_text', suggests_text)) != n_urls:
        raise Exception('suggest_text not in all suggests')

    log.info('correct format of urls')
    bad_urls = re.findall(r'<url>[^\?]*\&.*</url>', suggests_text)
    if bad_urls:
        raise Exception('bad format of url: %s' % bad_urls[0])

    # в листовых категорийных саджестах должны быть и ниды и хиды
    category_urls = re.findall('<url>/catalog/[0-9]*/.*</url>', suggests_text)
    for category_url in category_urls:
        if 'recipe' in category_url:
            continue
        begin_nid_pos = 14
        end_nid_pos = category_url.find('/', begin_nid_pos + 1)
        if end_nid_pos == -1:
            raise Exception('no nid param in category url')
        nid = category_url[begin_nid_pos: end_nid_pos]

        hid_match = re.search('hid=[0-9]*[$&]', category_url)
        if not hid_match or hid_match.start() == -1 or hid_match.end() == -1:
            raise Exception('no hid param in category url')
        hid = category_url[hid_match.start() + 4: hid_match.end() - 1]

        if hid == nid:  # косяк в коде
            raise Exception('hid and nid must have different values')
    return n_urls


def test_main_suggest_file(filepath, test_rich):
    log.info('check ' + filepath)
    suggests_text = read_file_content(filepath)
    suggest_count = test_required_params(suggests_text)
    if test_rich:
        test_rich_props(suggests_text, suggest_count)
        test_rich_props_mob(suggests_text)


def assert_eq(result, expected_result, param_name):
    if expected_result != result:
        msg = '{param} is equal {result}, but expected {expected_result}'
        raise Exception(msg.format(param=param_name, result=result, expected_result=expected_result))


def check_suggests_count(filename, file2count, expected_result):
    assert_eq(file2count[filename], expected_result, 'quantity of suggests in {}'.format(filename))


def test_symlinks(generation_dir):
    """
    Check all symlinks types
    """
    white_rich_dir = os.path.join(generation_dir, 'rich_suggests')
    get_white_link = lambda filename: os.readlink(os.path.join(white_rich_dir, filename))

    assert_eq(
        get_white_link('suggests_types.txt'),
        os.path.join(generation_dir, 'suggests_types.txt'),
        'file with usual types for white')
    assert_eq(
        get_white_link('suggests_types_exp1.txt'),
        os.path.join(generation_dir, 'suggests_types_exp1.txt'),
        'file with exp types for white')

    assert_eq(
        get_white_link('suggests_shop.xml'),
        os.path.join(generation_dir, 'suggests_shop.xml'),
        'usual file for white')
    assert_eq(
        get_white_link('suggests_shop_exp1.xml'),
        os.path.join(generation_dir, 'suggests_shop_exp1.xml'),
        'exp file for white')

    blue_simple_dir = os.path.join(generation_dir, 'blue')
    assert_eq(
        os.readlink(os.path.join(blue_simple_dir, 'suggest.xml')),
        os.path.join(generation_dir, 'suggest_blue.xml'),
        'suggest.xml for blue')

    blue_rich_dir = os.path.join(blue_simple_dir, 'rich_suggests')
    get_blue_link = lambda filename: os.readlink(os.path.join(blue_rich_dir, filename))

    assert_eq(
        get_blue_link('suggests_types.txt'),
        os.path.join(generation_dir, 'blue_suggests_types.txt'),
        'file with usual types for blue')
    assert_eq(
        get_blue_link('suggests_types_exp3.txt'),
        os.path.join(generation_dir, 'blue_suggests_types_exp3.txt'),
        'file with exp types for blue')

    assert_eq(
        get_blue_link('suggests_model.xml'),
        os.path.join(generation_dir, 'suggests_model_blue.xml'),
        'usual file for blue')
    assert_eq(
        get_blue_link('suggests_model_exp3.xml'),
        os.path.join(generation_dir, 'suggests_model_exp3_blue.xml'),
        'exp file for blue')

    tsv_rich_dir = os.path.join(generation_dir, 'tsv', 'rich_suggests')
    get_tsv_link = lambda filename: os.readlink(os.path.join(tsv_rich_dir, filename))

    assert_eq(
        get_tsv_link('suggests_types.txt'),
        os.path.join(generation_dir, 'all_tsv_suggests_types.txt'),
        'file with usual types for tsv'
    )
    assert_eq(
        get_tsv_link('search_ready.txt'),
        os.path.join(generation_dir, 'search_ready.txt'),
        'file with usual types for tsv'
    )
    assert_eq(
        get_tsv_link('search_groups.txt'),
        os.path.join(generation_dir, 'search_groups.txt'),
        'file with usual types for tsv'
    )
    assert_eq(
        get_tsv_link('model_ready.txt'),
        os.path.join(generation_dir, 'model_ready.txt'),
        'file with usual types for tsv'
    )
    assert_eq(
        get_tsv_link('model_groups.txt'),
        os.path.join(generation_dir, 'model_groups.txt'),
        'file with usual types for tsv'
    )


def test_types(generation_dir):
    """
    Проверяем файлы саджестов в suggest_types всех видов
    """
    read_all = lambda fn: [n.strip() for n in read_file_content(os.path.join(generation_dir, fn)).split()]

    def check_in(fn, fs):
        if fn not in fs:
            raise Exception('{} is not in {}'.format(fn, fs))

    def check_not_in(fn, fs):
        if fn in fs:
            raise Exception('{} is in {}'.format(fn, fs))

    all_white_files = read_all(os.path.join(generation_dir, 'all_suggests_types.txt'))
    check_in('suggests_model.xml', all_white_files)
    check_in('suggests_shop.xml', all_white_files)
    check_in('suggests_shop_exp1.xml', all_white_files)

    white_files_no_exp = read_all(os.path.join(generation_dir, 'suggests_types.txt'))
    check_in('suggests_model.xml', white_files_no_exp)
    check_in('suggests_shop.xml', white_files_no_exp)
    check_not_in('suggests_shop_exp1.xml', white_files_no_exp)

    white_files_exp = read_all(os.path.join(generation_dir, 'suggests_types_exp1.txt'))
    check_in('suggests_model.xml', white_files_exp)
    check_not_in('suggests_shop.xml', white_files_exp)
    check_in('suggests_shop_exp1.xml', white_files_exp)

    white_files_exp4 = read_all(os.path.join(generation_dir, 'suggests_types_exp4.txt'))
    check_in('suggests_category_exp4.xml', white_files_exp4)

    all_blue_files = read_all(os.path.join(generation_dir, 'all_blue_suggests_types.txt'))
    check_in('suggests_category.xml', all_blue_files)
    check_in('suggests_model.xml', all_blue_files)
    check_not_in('suggests_model_blue.xml', all_blue_files)
    check_in('suggests_model_exp3.xml', all_blue_files)

    blue_files_no_exp = read_all(os.path.join(generation_dir, 'blue_suggests_types.txt'))
    check_in('suggests_category.xml', blue_files_no_exp)
    check_in('suggests_model.xml', blue_files_no_exp)
    check_not_in('suggests_model_blue.xml', blue_files_no_exp)
    check_not_in('suggests_model_exp3.xml', blue_files_no_exp)
    check_not_in('suggests_model_exp3_blue.xml', blue_files_no_exp)
    check_not_in('suggests_model_blue_exp3.xml', blue_files_no_exp)

    blue_files_exp = read_all(os.path.join(generation_dir, 'blue_suggests_types_exp3.txt'))
    check_in('suggests_category.xml', blue_files_exp)
    check_not_in('suggests_model.xml', blue_files_exp)
    check_not_in('suggests_model_blue.xml', blue_files_exp)
    check_in('suggests_model_exp3.xml', blue_files_exp)
    check_not_in('suggests_model_exp3_blue.xml', blue_files_exp)
    check_not_in('suggests_model_blue_exp3.xml', blue_files_exp)


def get_all_objects_by_id_and_type(filepath, entity_id, entity_type):
    objects = etree.parse(filepath)
    point = '%s in %s' % (entity_id, filepath)
    results = []
    for obj in objects.findall('object'):
        if entity_type != obj.get('type'):
            continue

        url_elem = obj.find('url')

        if entity_id not in url_elem.text:
            continue

        results += [obj]

    if not results:
        raise Exception('No entity for %s', point)

    return results


def check_aliases_count(filepath, entity_id, cnt):
    point = '%s in %s' % (entity_id, filepath)
    model = get_all_objects_by_id_and_type(filepath, entity_id, 'model')[0]

    aliases_elem = model.find('aliases')
    if aliases_elem is not None:
        assert_eq(len(aliases_elem), cnt, 'N of aliases for %s' % point)
    else:
        assert_eq(0, cnt, 'N of aliases for %s' % point)


def test_all_aliases_count(generation_dir):
    """
    Проверяем число алиасов у моделей и ску
    1759295190 -- модель, у которой 8 алиасов в тестовой выгрузке,
    100126173307 -- ску, для которого 1759295190 -- родительская модель
    В обратном эксперименте exp3 алиасы НЕ прокидываются в ску
    """

    check_aliases_count(os.path.join(generation_dir, 'suggest.xml'), '1759295190', 8)
    check_aliases_count(os.path.join(generation_dir, 'suggests_model.xml'), '1759295190', 8)

    check_aliases_count(os.path.join(generation_dir, 'suggest_blue.xml'), '100126173307', 8)
    check_aliases_count(os.path.join(generation_dir, 'suggests_model_blue.xml'), '100126173307', 8)

    # No aliases in reverse experiment
    check_aliases_count(os.path.join(generation_dir, 'suggests_model_exp3_blue.xml'), '100126173307', 0)


def check_aliases_from_whitelist_are_unique(generation_dir, white_list, all_suggests_files):
    forbidden_aliases = {}

    for record in white_list:
        forbidden_aliases[record.name if record.alias is None else record.alias] = record.url

    for filename in all_suggests_files:
        filepath = os.path.join(generation_dir, filename)
        objects = etree.parse(filepath)

        for obj in objects.findall('object'):
            aliases = obj.find('aliases')
            name = obj.find('name')
            url = urlparse(obj.find('url').text).path

            if name.text in forbidden_aliases:
                if forbidden_aliases[name.text] != url:
                    raise Exception("name {} from {} coincides with record from whitelis; url {} differs from expected {}".format(name.text, filename, url, forbidden_aliases[name.text]))

            if aliases is None:
                continue

            for alias in aliases.findall('alias'):
                if alias.text in forbidden_aliases:
                    if url != forbidden_aliases[alias.text]:
                        raise Exception("alias {} from {} coincides with record from whitelis; url {} differs from expected {}".format(alias.text, filename, url, forbidden_aliases[alias.text]))


def make_config(path_to_arcadia, path_to_build):
    tpl = open(os.path.join(path_to_arcadia, 'market/guru-models-dumper/test/etc/config.tpl')).read()
    path = os.path.join(path_to_build, 'suggests_generator.cfg')
    open(path, 'w').write(
        tpl.format(
            data=os.path.join(path_to_arcadia, 'market/guru-models-dumper/py_test/data')
        )
    )
    return path


def make_default_config():
    with open('etc/config.tpl') as f:
        tpl = f.read()
    path = 'etc/suggests_generator.cfg'
    with open(path, 'w') as f:
        f.write(
            tpl.format(
                data=os.path.realpath('../py_test/data')
            )
        )

    return path


def parse_args():
    parser = argparse.ArgumentParser('Run old test and (when called manually) generate test generation')
    parser.add_argument('-a', '--arcadia-path', type=str, help='path to arcadia, requires --build-path (-b)')
    parser.add_argument('-b', '--build-path', type=str, help='build path, requires --arcadia-path (-a)')
    parser.add_argument('--keep-previous', action='store_true', help='is specified will save previous generation in 20161231_060000.prev, only non-CI usage')
    parser.add_argument('--skip-tests', action='store_true', help='skipping testing and generating generations (when script called manually')

    parsed = parser.parse_args(sys.argv[1:])

    if (parsed.arcadia_path is not None) ^ (parsed.build_path is not None):
        parser.error('--arcadia-path (-a) and --build-path (-b) requires each other')

    return parsed


@freeze_time(MOCKED_TIME)
def main():
    yenv.set_environment_type(yenv.DEVELOPMENT)
    args = parse_args()

    if args.arcadia_path is not None:
        path_to_arcadia = args.arcadia_path
        path_to_build = args.build_path
        path_to_config = make_config(path_to_arcadia, path_to_build)
    else:
        path_to_config = os.path.realpath(make_default_config())

    dumper = suggests_dumper.Dumper(path_to_config)
    if args.keep_previous:
        # дампер использует для генерации имени поколения time.localtime()
        generation = os.path.realpath(os.path.join('generations', time.strftime('%Y%m%d_%H%M%S', time.localtime())))
        prev_generaion = generation + '.prev'
        if os.path.exists(prev_generaion):
            log.info('deleting too old previous generation {}'.format(prev_generaion))
            shutil.rmtree(prev_generaion)
        if not os.path.exists(generation):
            log.info('{} does not exist, skipping move'.format(generation))
        else:
            log.info('move from {} to {}'.format(generation, prev_generaion))
            shutil.move(generation, prev_generaion)
    else:
        generation = os.path.realpath(os.path.join('generations', time.strftime('%Y%m%d_%H%M%S', time.localtime())))
        log.info('--keep-previous not specified, deleting {} if exists'.format(generation))
        if os.path.exists(generation):
            log.info('deleting {}'.format(generation))
            shutil.rmtree(generation)
        else:
            log.info('{} does not exist, skiiping delete'.format(generation))

    generation_dir = dumper.run(yt_idle=True)

    if args.skip_tests:
        log.info('skipping tests because of --skip-tests')
    else:
        log.info('check quantity suggests files and object in them')

        test_main_suggest_file(os.path.join(generation_dir, 'suggest.xml'), test_rich=False)
        test_main_suggest_file(os.path.join(generation_dir, 'suggest_blue.xml'), test_rich=True)

        test_symlinks(generation_dir)
        test_types(generation_dir)

        test_all_aliases_count(generation_dir)

        log.info('unittests result is successful')


if __name__ == '__main__':
    main()
