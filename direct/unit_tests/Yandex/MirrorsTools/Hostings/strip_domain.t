#!/usr/bin/perl

use warnings;
use strict;

use Test::More tests => 10;

use Yandex::MirrorsTools::Hostings qw/strip_domain/;

is(strip_domain("www.5ballov.ru"), "5ballov.ru");
is(strip_domain("asdfadsf.5ballov.ru"), "5ballov.ru");
is(strip_domain("asdf.asdfadsf.5ballov.ru"), "5ballov.ru");
is(strip_domain("5ballov.ru"), "5ballov.ru");
is(strip_domain("ru"), "ru");
is(strip_domain("qwe.nnov.ru"), "qwe.nnov.ru");
is(strip_domain("qwer.qwe.nnov.ru"), "qwe.nnov.ru");
is(strip_domain("globalmarket.com.ua"), "globalmarket.com.ua");
is(strip_domain("qwe.globalmarket.com.ua"), "qwe.globalmarket.com.ua");
is(strip_domain("qwer.qwe.globalmarket.com.ua"), "qwe.globalmarket.com.ua");
