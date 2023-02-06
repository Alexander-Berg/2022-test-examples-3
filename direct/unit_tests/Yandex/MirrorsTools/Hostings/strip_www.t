#!/usr/bin/perl

use warnings;
use strict;
use utf8;

use Test::More;

use Yandex::MirrorsTools::Hostings qw/strip_www/;

is(strip_www("www.5ballov.ru"), "5ballov.ru");
is(strip_www("5ballov.ru"), "5ballov.ru");
is(strip_www("www.ru"), "www.ru");
is(strip_www("www.nnov.ru"), "www.nnov.ru");
is(strip_www("www.qwe.nnov.ru"), "qwe.nnov.ru");

is(strip_www("www.yandex"), "www.yandex");
is(strip_www("www.zyandex.ru"), "zyandex.ru");
is(strip_www("www.leningrad.spb.ru"), "leningrad.spb.ru");
is(strip_www("www.abcde.com"),  "abcde.com");
is(strip_www("www.003.ru"),  "003.ru");
is(strip_www("www.ффф"),  "ффф");
is(strip_www("www..."),  "..");
is(strip_www("www.xxxa"),  "www.xxxa");
is(strip_www("www.aaa.bbbb"),  "aaa.bbbb");
is(strip_www("bbbb.cc"),  "bbbb.cc");
is(strip_www("www.а-вагонка.рф"),  "а-вагонка.рф");
is(strip_www("www.biz.io"),  "www.biz.io");
is(strip_www("www.biz.дев"),  "biz.дев");

done_testing();
