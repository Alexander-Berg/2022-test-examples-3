#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Yandex::Test::UTF8Builder;
use Test::More;

use TextTools qw/process_text_template/;

*f = \&process_text_template;

is(f(undef), undef);
is(f("string without template"), "string without template");
is(f("string with #template# and without params"), "string with #template# and without params");
is(f("hello, #who#", who => "world"), "hello, world");
is(f("hello, #who with space#", "who with space" => "world"), "hello, #who with space#");
is(f("hello, #who_with_space#", "who_with_space" => "world"), "hello, world");

done_testing;
