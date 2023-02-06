#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'TimeTarget' ); }

use utf8;
use open ':std' => ':utf8';

is(TimeTarget::letter2hour('A'), 0);
is(TimeTarget::letter2hour('c'), 2);
is(TimeTarget::letter2hour('X'), 23);

is(TimeTarget::hour2letter(0), 'A');
is(TimeTarget::hour2letter(2), 'C');
is(TimeTarget::hour2letter(23), 'X');

is(TimeTarget::letter2coef('a'),   0); # для БК
is(TimeTarget::letter2coef('b'),  10);
is(TimeTarget::letter2coef('c'),  20);
is(TimeTarget::letter2coef('d'),  30);
is(TimeTarget::letter2coef('e'),  40);
is(TimeTarget::letter2coef('f'),  50);
is(TimeTarget::letter2coef('g'),  60);
is(TimeTarget::letter2coef('h'),  70);
is(TimeTarget::letter2coef('i'),  80);
is(TimeTarget::letter2coef('j'),  90);
is(TimeTarget::letter2coef('k'), 100); # для БК
is(TimeTarget::letter2coef('l'), 110);
is(TimeTarget::letter2coef('m'), 120);
is(TimeTarget::letter2coef('n'), 130);
is(TimeTarget::letter2coef('o'), 140);
is(TimeTarget::letter2coef('p'), 150);
is(TimeTarget::letter2coef('q'), 160);
is(TimeTarget::letter2coef('r'), 170);
is(TimeTarget::letter2coef('s'), 180);
is(TimeTarget::letter2coef('t'), 190);
is(TimeTarget::letter2coef('u'), 200);

is(TimeTarget::coef2letter(0),   'a'); # для БК
is(TimeTarget::coef2letter(10),  'b');
is(TimeTarget::coef2letter(20),  'c');
is(TimeTarget::coef2letter(30),  'd');
is(TimeTarget::coef2letter(40),  'e');
is(TimeTarget::coef2letter(50),  'f');
is(TimeTarget::coef2letter(60),  'g');
is(TimeTarget::coef2letter(70),  'h');
is(TimeTarget::coef2letter(80),  'i');
is(TimeTarget::coef2letter(90),  'j');
is(TimeTarget::coef2letter(100), 'k'); # для БК
is(TimeTarget::coef2letter(110), 'l');
is(TimeTarget::coef2letter(120), 'm');
is(TimeTarget::coef2letter(130), 'n');
is(TimeTarget::coef2letter(140), 'o');
is(TimeTarget::coef2letter(150), 'p');
is(TimeTarget::coef2letter(160), 'q');
is(TimeTarget::coef2letter(170), 'r');
is(TimeTarget::coef2letter(180), 's');
is(TimeTarget::coef2letter(190), 't');
is(TimeTarget::coef2letter(200), 'u');

done_testing();
