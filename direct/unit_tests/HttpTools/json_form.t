#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Deep;

use JSON;

use Tools;
use HttpTools;

my %json = (
    json_cids => [(45..53)],
    json_strategy => {name => '', search => {name => 'min_price'}, net => {name => 'default'}}
);
my %params = (
    cid => 647212,
    cmd => 'showCamp',
    ulogin => 'hrustyashko',
    strategy => 'different_places'
);
my %form = (
    %params,
    map {
        ($_ => encode_json $json{$_})
    } keys %json
);

cmp_deeply({HttpTools::parse_json_form(\%form)}, {%params, %json});
is(Tools::validate_structure({}, []), 'type_mismatch');
is(Tools::validate_structure({}, {k => []}), 'not equal');
is(Tools::validate_structure({}, {k => []}, only_type => 1), '');
is(Tools::validate_structure([{},5,8,undef,[]], [{},5,8,-5,[]]), '');
is(Tools::validate_structure([(1..5)], [1,2,3,4,5]), '');

done_testing;
