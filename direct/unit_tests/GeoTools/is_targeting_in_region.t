#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

use GeoTools;

use utf8;
use open ':std' => ':utf8';

*itr = sub {
    my ($geo, $region, $opt) = @_;
    return is_targeting_in_region($geo, $region, $opt);
};

# Входит ли Казахстан в Казахстан
is(itr(159, 159, {tree => 'ua'}), 1);

# Входит ли Алматинская область в Казахстан
is(itr(29406, 159, {tree => 'ua'}), 1);

# Входит ли Казахстан в Алматинскую область
is(itr(159, 29406, {tree => 'ua'}), 0);

# второй параметр undef (или 0)
is(itr(159, 0, {tree => 'ua'}), 1);

# первый параметр undef (или 0)
is(itr(0, 159, {tree => 'ua'}), 0);

# транслокальность
# входит ли Крым в Россию
is(itr(977, 225, {tree => 'ua'}), 0);
is(itr(977, 225, {tree => 'ru'}), 1);
is(itr(977, 225, {host => 'direct.yandex.ua'}), 0);
is(itr(977, 225, {host => 'direct.yandex.ru'}), 1);
is(itr(977, 225, {host => 'direct.yandex.com.tr'}), 1);

# входит ли Крым в Украину
is(itr(977, 187, {tree => 'ua'}), 1);
is(itr(977, 187, {tree => 'ru'}), 0);
is(itr(977, 187, {host => 'direct.yandex.ua'}), 1);
is(itr(977, 187, {host => 'direct.yandex.ru'}), 0);
is(itr(977, 187, {host => 'direct.yandex.com.tr'}), 0);

# АПИ дерево (Крым никуда не входит кроме корневого региона)
is(itr(977, 187, {tree => 'api'}), 0);
is(itr(977, 225, {tree => 'api'}), 0);
is(itr(977, 0,   {tree => 'api'}), 1);

done_testing();
