#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;
use Test::Deep;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'TimeTarget' ); }

use utf8;
use open ':std' => ':utf8';

*hh = \&TimeTarget::hours_hash;

cmp_deeply(hh('1A'),    {1 => {0 => 100} });
cmp_deeply(hh('1A8'),   {1 => {0 => 100}, 8 => {} });
cmp_deeply(hh('1ABCD'), {1 => {0 => 100,  1 => 100, 2 => 100, 3 => 100} });
cmp_deeply(hh('1A2B8'), {1 => {0 => 100}, 2 => {1 => 100}, 8 => {} });

# с коэффициентами
cmp_deeply(hh('1Ab'),    {1 => {0 => 10}});
cmp_deeply(hh('1Aj8'),   {1 => {0 => 90}, 8 => {}});
cmp_deeply(hh('1AdBdCdDd'), {1 => {0 => 30,  1 => 30, 2 => 30, 3 => 30}});
cmp_deeply(hh('1BbCcDdEeFfGgHhIiJj'), {1 => {1 => 10,  2 => 20, 3 => 30, 4 => 40, 5 => 50, 6 => 60, 7 => 70, 8 => 80, 9 => 90}});
cmp_deeply(hh('1Ab2Bc8'), {1 => {0 => 10}, 2 => {1 => 20}, 8 => {}});
# > 100%
cmp_deeply(hh('1BlCmDnEoFpGqHrIsJtKuL'), {1 => {1 => 110, 2 => 120, 3 => 130, 4 => 140, 5 => 150, 6 => 160, 7 => 170, 8 => 180, 9 => 190, 10 => 200, 11 => 100}});

cmp_deeply(hh('123456-ABC--FGHIJKLMNOPQRSTUVWX'), {7 => {3 => 100, 4 => 100}});

cmp_deeply(hh('123-56-ABC--FGHI--LMNOPQRSTUVWX'), {4 => {3 => 100, 4 => 100, 9 => 100, 10 => 100}, 7 => {3 => 100, 4 => 100, 9 => 100, 10 => 100}});

# Со строкой настроек
cmp_deeply(hh('1ABCD;'), {1 => {0 => 100,  1 => 100, 2 => 100, 3 => 100} });
cmp_deeply(hh('1Ab2Bc8;p:'), {1 => {0 => 10}, 2 => {1 => 20}, 8 => {}});

done_testing();
