#!/usr/bin/perl

use warnings;
use strict;

use Test::More tests => 15;

use Yandex::MirrorsTools::Hostings qw/splice_domain/;

is(splice_domain("www.5ballov.ru"), "5ballov.ru");
is(splice_domain("asdfadsf.5ballov.ru"), "5ballov.ru");
is(splice_domain("asdf.asdfadsf.5ballov.ru"), "5ballov.ru");
is(splice_domain("5ballov.ru"), "5ballov.ru");
is(splice_domain("ru"), "ru");
is(splice_domain("qwe.nnov.ru"), "nnov.ru");
is(splice_domain("qwer.qwe.nnov.ru"), "nnov.ru");
is(splice_domain("globalmarket.com.ua"), "globalmarket.com.ua");
is(splice_domain("qwe.globalmarket.com.ua"), "globalmarket.com.ua");
is(splice_domain("qwer.qwe.globalmarket.com.ua"), "globalmarket.com.ua");

is(splice_domain("bar.co.uk"), "bar.co.uk");
is(splice_domain("foo1.bar2.co.uk"), "bar2.co.uk");

is(splice_domain("bar.co.uk", 2), "bar.co.uk");
is(splice_domain("foo1.bar2.co.uk", 2), "foo1.bar2.co.uk");
is(splice_domain("foo2.foo1.bar2.co.uk", 2), "foo1.bar2.co.uk");
