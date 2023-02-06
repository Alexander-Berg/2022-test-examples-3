#!/usr/bin/perl

# $Id$

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use GeoTools;

use utf8;

*ggp = sub {
    my ($region, $opts) = (@_);
    return get_geo_projection($region, $opts);
};

# Проекции на список регионов (для информера на Морде)
is(ggp(213, {geo_list => [0, 1, 3], tree => 'ru'}), 1);
is(ggp(213, {geo_list => [0, 3, 1], tree => 'ru'}), 1);
is(ggp(213, {geo_list => [1, 0, 3], tree => 'ru'}), 1);

is(ggp(213, {geo_list => [0, 138, 215], tree => 'ru'}), 0, 'проекция на список регионов, в котором есть 0 (Все регионы)');

is(ggp(213, {geo_list => [1, 0, 3, 213], tree => 'ru'}), 213);

is(ggp(213, {geo_list => [187, 418], tree => 'ru'}), undef, 'проекция на список регионов, в котором нет 0 (Все регионы)');


# Проекции по типу региона (для подсказки в визитке)
is(ggp(213, {type => $geo_regions::CITY, tree => 'ru'}),    213, "city for Moscow"); 
is(ggp(213, {type => $geo_regions::COUNTRY, tree => 'ru'}), 225, "country for Moscow"); 

is(ggp(21775, {type => $geo_regions::CITY, tree => 'ru'}),  21775, "ищем город для Днепродзержинска"); 
is(ggp(21775, {type => $geo_regions::COUNTRY, tree => 'ru'}),  187, "ищем страну для Днепродзержинска");

is(ggp(20538, {type => $geo_regions::COUNTRY, tree => 'ru'}), 187, "ищем страну для Харькова и области"); 

# транслокальность

is(ggp(146, {type => $geo_regions::COUNTRY, host => 'direct.yandex.ru'}), 225, "транслокальность: страна для Симферополя - Россия");
is(ggp(146, {type => $geo_regions::COUNTRY, host => 'direct.yandex.kz'}), 225, "транслокальность: страна для Симферополя - Россия");
is(ggp(146, {type => $geo_regions::COUNTRY, host => 'direct.yandex.ua'}), 187, "транслокальность: страна для Симферополя - Украина");

is(ggp(977, {geo_list => [225, 187],  host => 'direct.yandex.ua'}), 187, "транслокальность: проекция Крыма для Украины");
is(ggp(977, {geo_list => [225, 187],  host => 'direct.yandex.ru'}), 225, "транслокальность: проекция Крыма для России");
is(ggp(977, {geo_list => [225, 187],  host => 'direct.yandex.kz'}), 225, "транслокальность: проекция Крыма для Казахстана");

done_testing();
