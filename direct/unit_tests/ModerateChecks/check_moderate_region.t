#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 9;

use ModerateChecks;

use utf8;
use open ':std' => ':utf8';

my $moscow_and_region = 1;
my $piter = 2;
my $kaz = 159;
my $russia = 225;
my $astana = 29406;

*cmr = sub { return check_moderate_region(@_) };

# Смена Казахстана на Казахстан
is(cmr($kaz,$kaz), undef, "No moderation on change Kz to Kz");

# Смена России на Казахсатн
is(cmr($astana,$russia), 1, "Moderation on change Rus to Astana");

# Смена России на Казахсатн
is(cmr($kaz,$russia), 1, "Moderation on change Rus to Kz");

# Смена Казахстана на Россию
is(cmr($russia,$kaz), undef, "No moderation on change Kz to Rus");

# Смена Астаны на Казахстан
is(cmr($kaz, $astana), undef, "No moderation on change Astana to Kz");

# Смена Москвы на Питер
is(cmr($piter, $moscow_and_region), undef, "No moderation on change piter to moscow");

# Смена Россия на Россия + Казахстан
is(cmr("$russia, $kaz", $russia), undef, "No moderation on change Russia to Russia+Kz");

# Смена Россия + Казахстан на Казахстан
is(cmr($kaz, "$russia, $kaz"), 1, "Moderation on change Russia+Kz to Kz");

# Смена Казахстан+Астана на Казахстан
is(cmr($astana, "$astana, $kaz"), undef, "Moderation on change Astana+Kz to Kz");
