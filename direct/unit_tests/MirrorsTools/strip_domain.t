#!/usr/bin/perl

use warnings;
use strict;

use Yandex::MirrorsTools::Hostings qw/strip_domain/;
use Test::More tests => 11;


is(strip_domain("5ballov.ru"), "5ballov.ru");
is(strip_domain("www.5ballov.ru"), "5ballov.ru");
is(strip_domain("forum.com.ru"), "forum.com.ru");
is(strip_domain("yapanama.mail333.com"), "yapanama.mail333.com");
is(strip_domain("www.club-plaza.relax.by"), "club-plaza.relax.by");
is(strip_domain("autoschool.yenisite.ru"), "yenisite.ru");
is(strip_domain("xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai"), "xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai");
is(strip_domain("3dlevel.xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai"), "xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai");
is(strip_domain("4thlevel.ucoz.com.ru"), "ucoz.com.ru");
is(strip_domain("4thlevel.ucoz.ru.com"), "ucoz.ru.com");
is(strip_domain("4thlevel.ucoz.domain.com"), "domain.com");

