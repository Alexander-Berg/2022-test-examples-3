#!/usr/bin/perl

#    $Id$

use warnings;
use strict;
use Test::More tests => 25;

use TextTools;

use utf8;
use open ':std' => ':utf8';

*vl= \&TextTools::validate_login;

# symbols
is(vl("abc"), "1", "abc");
is(vl("абв"), "0", "russian letters");
is(vl("а" x 35), "0", "long string");

# digits
is(vl("abc0"), "1", "abc0");
is(vl("abc1"), "1", "abc1");
is(vl("123abc"), "0", "123abc");
is(vl("12345"), "0", "full digits");

# minus
is(vl("a-bc"), "1", "abc");
is(vl("a--bc"), "0", "double minus");
is(vl("-abc"), "0", "begin with minus");
is(vl("abc-"), "0", "minus on finish");

# dot
is(vl("a.b.c"), "1", "login with dotes");
is(vl("a..c"), "0", "double dotes");
is(vl(".abc"), "0", "begin with dot");
is(vl("abc."), "0", "finish dot");

# bad symbols
is(vl("ab\@cd.ru"), "0", "email as login");
is(vl("_abc"), "0", "underline on start");
is(vl("a_bc"), "0", "underline");
is(vl("a^bc"), "0", "a^bc");
is(vl("a&bc"), "0", "ab&c");

# Проверяем логины простым способом
is(vl("abcdef", lite => 1), "1", "abcdef");
is(vl("12345", lite => 1), "1", "login = 12345");
is(vl("a" x 35, lite => 1), "1", "long login");
is(vl("a^b_c&d", lite => 1), "0", "bad symbols");
is(vl("абвгде", lite => 1), "0", "russian symbols in lite check");


