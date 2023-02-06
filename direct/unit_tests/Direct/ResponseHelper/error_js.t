#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 3;

use Settings;
use Direct::ResponseHelper;

use utf8;
use open ':std' => ':utf8';

my $errstr = 'Ошибка на русском языке';

eval { error_js($errstr) };
my $e = $@;
ok($e && ref $e && $e->isa('Direct::HttpResponse'), 'error_js dies ok');

is($e->{type}, 'application/x-javascript; charset=utf-8', 'good type');
is($e->{body}, qq#{"error":"$errstr"}#, 'good body');

