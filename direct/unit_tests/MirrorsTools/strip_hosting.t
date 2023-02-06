#!/usr/bin/perl

use warnings;
use strict;

use Yandex::MirrorsTools::Hostings qw/strip_hosting/;
use Test::More tests => 10;


is(strip_hosting("5ballov.ru"), "5ballov.ru");
is(strip_hosting("www.5ballov.ru"), "www.5ballov.ru");
is(strip_hosting("forum.com.ru"), "forum.com.ru");
is(strip_hosting("yapanama.mail333.com"), "yapanama");
is(strip_hosting("www.club-plaza.relax.by"), "www.club-plaza");
is(strip_hosting("autoschool.yenisite.ru"), "autoschool.yenisite.ru");
is(strip_hosting("xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai"), "xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai");
is(strip_hosting("3dlevel.xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai"), "3dlevel.xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai");
is(strip_hosting("4thlevel.ucoz.com"), "4thlevel");
is(strip_hosting("4thlevel.ru.narod.ru"), "4thlevel.ru");

