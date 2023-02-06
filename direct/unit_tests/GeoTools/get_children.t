#!/usr/bin/perl
#
#

use strict;
use warnings;
use utf8;
use open ':std' => ':utf8';


use Yandex::Test::UTF8Builder;
use GeoTools;


use Test::More;


my @regions = (
    225,    # Россия
    213     # Москва
);
my @expected_children = (
    1,      # Москва и область
    3,      # Центр
    213,    # Москва
    10849,  # Северодвинск 
    10772,  # Орел и область
);


my @result = GeoTools::get_geo_children( 
    join(',', @regions),
    {host => 'direct.yandex.ru'},
);

cmp_ok @result, '>', 0, 'get ' . @result . ' childs';

my %ids = map {$_ => 1} @result;
cmp_ok @result, '==', scalar keys %ids, 'no duplicates';

cmp_ok @regions, '==', grep( { $ids{$_} } @regions ), 
   'expected parents in result';
cmp_ok @expected_children, '==', grep( { $ids{$_} } @expected_children ), 
   'expected children in result';



done_testing();




