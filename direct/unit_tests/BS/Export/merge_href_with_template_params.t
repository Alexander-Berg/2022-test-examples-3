#!/usr/bin/perl

# $Id$

use Direct::Modern;
use Test::More tests => 10;

BEGIN { use_ok( 'BS::Export' ); }

*mergeparams = \&BS::Export::merge_href_with_template_params;

# тесты со строкой без параметров
is(mergeparams('http://ya.ru/yandsearch', 'utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}')
    , 'http://ya.ru/yandsearch?utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has no params by itself; all template params are added to the href');

is(mergeparams('http://ya.ru/yandsearch?', 'utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}')
    , 'http://ya.ru/yandsearch?utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has no params by itself, ends with ? symbol; all template params are added to the href');

# тесты с дублями параметров в шаблоне
is(mergeparams('http://ya.ru/yandsearch', 'utm_medium=cpc&utm_medium=smm&utm_source=yandex_direct&c={CampaignID}&p1={param1}')
    , 'http://ya.ru/yandsearch?utm_medium=cpc&utm_medium=smm&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has no params by itself; template has duplicate param names; all template params are added to the href');

is(mergeparams('http://ya.ru/yandsearch', 'utm_medium=smm&utm_source=yandex_direct&c={CampaignID}&p1={param1}&utm_medium=cpc')
    , 'http://ya.ru/yandsearch?utm_medium=smm&utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has no params by itself; template has separated duplicate param names; all template params are added to the href');

# тесты с непересекающимися параметрами в ссылке и шаблоне 
is(mergeparams('http://ya.ru/yandsearch?text=test&utm_content=moskva', 'utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}')
    , 'http://ya.ru/yandsearch?text=test&utm_content=moskva&utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has params that are non-conflicting with template params; all template params are added to the href');

is(mergeparams('http://ya.ru/yandsearch?text=test&utm_content=moskva&', 'utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}')
    , 'http://ya.ru/yandsearch?text=test&utm_content=moskva&utm_medium=cpc&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has params that are non-conflicting with template params, ends with & symbol; all template params are added to the href');

# тесты с пересекающимися параметрами в ссылке и шаблоне 
is(mergeparams('http://ya.ru/yandsearch?utm_medium=cpc&text=test&utm_content=moskva', 'utm_medium=smm&utm_source=yandex_direct&c={CampaignID}&p1={param1}')
    , 'http://ya.ru/yandsearch?utm_medium=smm&text=test&utm_content=moskva&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has params that are conflicting with template params; new template params are added to the href, conflicting params in href are replaced with template values');

is(mergeparams('http://ya.ru/yandsearch?utm_medium=cpc&utm_medium=social_cpc&utm_content=moskva&utm_content=peterburg&text=test', 'utm_medium=retargeting&utm_medium=price&utm_source=yandex_direct&c={CampaignID}&p1={param1}')
    , 'http://ya.ru/yandsearch?utm_medium=retargeting&utm_medium=price&utm_content=moskva&utm_content=peterburg&text=test&utm_source=yandex_direct&c={CampaignID}&p1={param1}'
    , 'Href has duplicate params that are conflicting with duplicate template params; new template params are added to the href, conflicting params in href are replaced with template values');
#тесты с кириллицей в ссылке и шаблоне
is(mergeparams('http://ya.ru/yandsearch?ключ=значение', 'другой_ключ=другое_знвчение')
    , 'http://ya.ru/yandsearch?ключ=значение&другой_ключ=другое_знвчение'
    , 'Href has params with cyrillic characters, template params have cyrillic characters as well; all params are added and correctly decoded');
