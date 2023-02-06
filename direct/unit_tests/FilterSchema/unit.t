#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use JSON::XS;

BEGIN {
    use_ok 'FilterSchema';
}

sub get_checker {
    return FilterSchema->new(schema => JSON::XS->new->relaxed(1)->utf8(1)->decode( shift ));
}


my $simple_schema = <<END
{
    "checkedStruct": {
        "type" : "array",
        "items" : { "type": "integer", "errorOnWrongType" : "Error_3"},
        "constraints": [
             {"name": "itemsCountMin", "parameters": 1,  "error": "Error_1" },
             {"name": "itemsCountMax", "parameters": 4, "error": "Error_2" },
        ],
        "errorOnWrongType" : "Error_0"
    }
}

END
;

my $checker = get_checker($simple_schema);


is_deeply $checker->_merge_constraints(
    [ {name=> 1, error=>1111},
      {name=> 2, error=>2222},
      {name=> 3, error=>3333}, ],

    [ {name=> 1, error=>1001},
      {name=> 4, error=>4444},
      {name=> 5, error=>5555}, 
      {name=> 6, error=>6666},
    ], ),
    [ {name=> 1, error=>1001},
      {name=> 2, error=>2222},
      {name=> 3, error=>3333},
      {name=> 4, error=>4444},
      {name=> 5, error=>5555}, 
      {name=> 6, error=>6666},
    ], '_merge_constraints';

is_deeply $checker->_merge(
    { aa=>{bb=>[1,2,3]}, cc=>'dd', ee=>{ff => 1, gg => 2}}, 
    { aa=>{bb=>[5,6]}, zz=>'xx', cc=>'mm', ee=>{ff=>7, pp=>0}}
),
    { aa=>{bb=>[5,6]}, zz=>'xx', cc=>'mm', ee=>{ff => 7, gg => 2, pp=>0}},
    '_merge'
;

    done_testing;
