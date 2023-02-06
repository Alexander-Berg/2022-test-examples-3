#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use utf8;


BEGIN {
    use_ok 'FilterSchema';
}

my $validator = FilterSchema->new(filter_type=>'auto_AutoRu');

ok $validator->compiled_schema, 'compilation';

my $rules = [
    {field => 'wheel', relation => 'ilike', value => ['левый']},
    {field => 'color', relation => 'ilike', value => ['red']},
    {field => 'metallic', relation => 'ilike', value => ['да']},
    {field => 'year', relation => '==', value => ['1980']}
];
my $vr = $validator->reset->check($rules);
ok $vr->is_valid, 'correct data' ;
# diag($vr->get_first_error_description);

    
done_testing;
