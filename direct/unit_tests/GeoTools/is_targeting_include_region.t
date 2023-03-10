#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

use GeoTools;

use utf8;
use open ':std' => ':utf8';

*itr = sub { return is_targeting_include_region(@_) };

# включает ли таргетинг Украина - Украину
is(itr(187, 187, {tree => 'ua'}), 1);

# включает ли таргетинг СНГ - Украину
is(itr(166, 187, {tree => 'ua'}), 1);

# включает ли таргетинг СНГ (с исключение Украины) - Украину
is(itr('166,-187', 187, {tree => 'ua'}), 0);

# включает ли таргетинг СНГ (с исключение Киевской области) - Украину
is(itr('166,-20544', 187, {tree => 'ua'}), 1);

# включает ли таргетинг Киевская область - Украину
is(itr(20544, 187, {tree => 'ua'}), 1);

# включает ли таргетинг Киевская область - Казахстан
is(itr(20544, 159, {tree => 'ua'}), 0);

# включает ли таргетинг Украина - Казахстан
is(itr(187, 159, {tree => 'ua'}), 0);

# включает ли таргетинг Украина - Крьім (транслокальное дерево для Украины)
is(itr(187, 977, {tree => 'ua'}), 1);

# включает ли таргетинг Украина - Крьім (транслокальное дерево для России)
is(itr(187, 977, {tree => 'ru'}), 0);

# включает ли таргетинг Украина - Крьім (транслокальное дерево для Украины)
is(itr(187, 977, {host => 'direct.yandex.ua'}), 1);

# включает ли таргетинг Украина - Крьім (транслокальное дерево для России)
is(itr(187, 977, {host => 'direct.yandex.ru'}), 0);

# включает ли таргетинг Украина - Крьім (транслокальное дерево для Казахстана)
is(itr(187, 977, {host => 'direct.yandex.kz'}), 0);

# включает ли таргетинг Россия - Крьім (транслокальное дерево для Казахстана)
is(itr(225, 977, {host => 'direct.yandex.kz'}), 1);

# включает ли таргетинг Украина - Крьім (транслокальное дерево для Турции)
is(itr(187, 977, {host => 'direct.yandex.com.tr'}), 0);

# включает ли таргетинг Россия - Крьім (транслокальное дерево для Турции)
is(itr(225, 977, {host => 'direct.yandex.com.tr'}), 1);

# включает ли таргетинг Россия - Крьім (транслокальное дерево для Украины)
is(itr(225, 977, {tree => 'ua'}), 0);

# включает ли таргетинг Россия - Крьім (транслокальное дерево для России)
is(itr(225, 977, {tree => 'ru'}), 1);

# второй параметр undef (или 0)
is(itr(187,0, {tree => 'ua'}), 1);

# первый параметр undef (или 0)
is(itr(0,187, {tree => 'ua'}), 1);

# включает ли таргетинг Россия/Украина/Мир - Крьім (транслокальное дерево для api)
is(itr(225, 977, {tree => 'api'}), 0);
is(itr(187, 977, {tree => 'api'}), 0);
is(itr(0, 977, {tree => 'api'}), 1);

done_testing();
