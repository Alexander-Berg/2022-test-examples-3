#!/usr/bin/perl

use strict;
use warnings;

use utf8;
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

my $complex_type_and_references = <<END
{
    "checkedStruct": "#/schema/struct",

    "schema" : {
        "struct": {
            "type": "#/types/restrictedInteger",
            "constraints": [
                { "name":"minValue", "parameters":6, "error": "less than six" }
            ],
        } 
    },
    "types": {
        "restrictedInteger": {
            "type":"integer",
            "errorOnWrongType" : "Wrong value" ,
            "constraints" : [
                { "name":"maxValue", "parameters":9, "error": "greater than nine" }
            ],
        }
    }
}
END
;

my $object_type = <<END
{
    "checkedStruct": "#/schema/struct",

    "schema" : {
        "struct": {
            "type": "#/types/restrictedObject",
            "fields": {
                "foo" : { 
                    "type": "#/types/restrictedInteger",
                    "errorOnWrongType": "Invalid foo value",
                },
                "bar" : {
                    "type": "#/types/arrayOfSmallStrings",
                    "items" : {
                        "type" : "#/types/smallString",
                        "constraints" : [
                            {"name": "minLength", "parameters": 2, "error": "length < 2"}
                        ],
                    }
                },
                "qux" : {
                    "type": {
                        "enum" : ["a","b","c"]
                    },
                    "errorOnWrongType": "abc only",
                }
            }
        } 
    },
    "types": {
        "restrictedInteger": {
            "type":"integer",
            "errorOnWrongType" : "Wrong value" ,
            "constraints" : [
                { "name":"maxValue", "parameters":9, "error": "greater than nine" }
            ],
        },
        "smallString": {
            "type": "string",
            "constraints" : [
                { "name":"maxLength", "parameters":5, "error": "string too big" }
            ]
        },
        "arrayOfSmallStrings" : {
            "type" : "array",
            "items" : { "type": "#/types/smallString"}
        },
        "restrictedObject" : {
            "type" : "object",
            "errorOnWrongType" : "Wrong object"
        }
    }
}
END
;

my $type_by_condition = <<END
{
    "checkedStruct" : {
        "type": "object",
        "fields": {
            "name" : {"type": "string"},
            "value" : {
                "typeByCondition":[
                    {
                        "condition" : { "field": "name", "in": ["foo", "bar"] },
                        "type" : "string",
                        "errorOnWrongType": "wrong string",
                        "constraints": [
                            {"name": "minLength", "parameters" : "1", "error": "empty value not allowed"},
                            {"name": "withoutSpaces", "error": "value must not contain any whitespace"}
                        ]
                    },
                    {
                        "condition" : { "field": "name", "in": ["qux"] },
                        "type" : "integer",
                        "errorOnWrongType": "wrong integer",
                        "constraints": [
                            {"name": "minValue", "parameters" : 7, "error": "less than seven"}
                        ]
                    },
                ],
                "default":{"type":{"enum": [11,22,33]}, "errorOnWrongType":"11,22,33 for names except 'foo', 'bar', 'qux'" },
                "constraints": [
                    {"name": "maxLength", "parameters": 3, "error":"too long"},
                    {"name": "minLength", "parameters": 2, "error":"too short"}
                ]
            }
        }
    }
}
END
;

my $item_any_of = <<END
{
    "checkedStruct": {
        "type" : "array",
        "items" : { "type":"#/myObjects"},
        "errorOnWrongType" : "Array of myObjects required",
        "constraints" : [
            {"name": "itemAnyOf", "parameters":{ "variants": "#/myVariants", "mapBy" : "name", "allowOnlyDefined" : true}, "error": "Wrong item"}
        ]
    },

    "myObjects":{
        "type": "object",
        "fields": {
            "name": {"type": "string"},
            "weight": {"type": "integer"},
            "color" : {"type": "string"}
        },
        "allowOptionalFields":1
    },

    "myVariants":{
        "orange": {
            "type" : "#/myOrange",
            "errorOnWrongType": "broken orange"
        },
        "apple": {
            "type": "#/myApple",
        },
        "brick": {
            "type":"object",
            "errorOnWrongType" : "Invalid brick",
            "fields": {
                "name": {"type": "string"},
                "weight": {
                    "type": "integer",
                    "constraints":[
                        {"name":"minValue", "parameters": 100, "error":"too lightweight brick"}
                    ]
                },
                "color":{
                    "typeByCondition": [
                        {
                            "condition": {"field": "weight", "in": [100,200,300]},
                            "type":{"enum":["Red"]},
                            "errorOnWrongType": "color must be red"
                        },
                        {
                            "condition": {"field": "weight", "in": [500,600,700]},
                            "type":{"enum":["White"]},
                            "errorOnWrongType": "color must be white"
                        },
                    ],
                    "default": {"type" : {"enum":[]}, "errorOnWrongType":"Invalid weight of brick"}
                },

                "smoothness":{"type":"integer", "errorOnWrongType": "invalid smoothness"}
            }
        }
    },
    "myApple": {
        "type":"object",
        "fields": {
            "name": {"type": "string"},
            "weight": {
                "type": "integer",
                "constraints":[
                    {"name":"minValue", "parameters": 10, "error":"too lightweight apple"}
                ]
            },
            "color":{"type":{"enum":["Red", "Green"]}, "errorOnWrongType": "invalid apple color"}
        }
    },
    "myOrange": {
        "type":"object",
        "fields": {
            "name": {"type": "string"},
            "weight": {
                "type": "integer",
                "constraints":[
                    {"name":"minValue", "parameters": 15, "error":"too lightweight orange"}
                ]
            },
            "color":{"type":{"enum":["Orange", "Green", "Yellow"]}, "errorOnWrongType": "invalid orange color"}
        }
    }
}

END
;

ok get_checker($simple_schema)->check([ 9,8,7 ])->is_valid, 'is_valid';

is_deeply [map {$_->description} @{get_checker($simple_schema)->check({})->get_errors}], ['Error_0' ], 'errors 1';
is_deeply [map {$_->description} @{get_checker($simple_schema)->check([ 9,8,"x",7])->get_errors}], ['Error_3' ], 'errors 2';

is_deeply [map {$_->description} @{get_checker($simple_schema)->check([ 9,8,7,6,5])->get_errors}], ['Error_2' ], 'constraints';

is_deeply [
    [map {$_->description} @{get_checker($complex_type_and_references)->check(4)->get_errors}],
    [map {$_->description} @{get_checker($complex_type_and_references)->check(10)->get_errors}],
    [map {$_->description} @{get_checker($complex_type_and_references)->check({1=>2})->get_errors}],
],
[
    ['less than six'],
    ['greater than nine'],
    ['Wrong value'],
], 'type and references';

ok get_checker($object_type)->check({'foo' => 3, 'bar'=>['xx','ii'], 'qux' => 'a'})->is_valid, 'valid object';
is_deeply 
    [
        [map {$_->description} @{get_checker($object_type)->check( {'foo' => 3, 'bar'=>['x','ii'], 'qux' => 'a'} )->get_errors}],
        [map {$_->description} @{get_checker($object_type)->check( {'foo' => "x", 'bar'=>['ooooo','ii'], 'qux' => 'b'} )->get_errors}],
        [map {$_->description} @{get_checker($object_type)->check( {'foo' => 8, 'bar'=>['jj','ii'], 'qux' => 'zz'} )->get_errors}],
        [map {$_->description} @{get_checker($object_type)->check( {'foo' => 8, 'bar'=>['abcdefghijk','ii'], 'qux' => 'c'} )->get_errors}],
        [map {$_->description} @{get_checker($object_type)->check( {'foo' => 8, 'qux' => 'c'} )->get_errors}],
        [map {$_->description} @{get_checker($object_type)->check( {'foo' => 7, 'bar'=>['jj','ii'], 'qux' => 'c', "excess" => "333"} )->get_errors}],
    ],    
    [
        ['length < 2'],
        ['Invalid foo value'],
        ['abc only'],
        ['string too big'],
        ['Wrong object'],
        ['Wrong object'],
    ],
'object';

my $object_type_with_optional_fields = get_checker($object_type);
$object_type_with_optional_fields->schema->{types}->{restrictedObject}->{allowOptionalFields} = 1;
    
ok $object_type_with_optional_fields->check( {'foo' => 7, 'bar'=>['jj','ii'], 'qux' => 'c', "excess" => "333"} )->is_valid, 'object with optional fields';

ok get_checker($type_by_condition)->check({'name' => '3', 'value' => 11 })->is_valid, 'type by condition - correct data';

is_deeply 
    [
        [map {$_->description} @{get_checker($type_by_condition)->check( {'name' => 'foo', 'value' => 'g h' } )->get_errors}],
        [map {$_->description} @{get_checker($type_by_condition)->check( {'name' => 'bar', 'value' => '8' } )->get_errors}],
        [map {$_->description} @{get_checker($type_by_condition)->check( {'name' => 'qux', 'value' => 6 } )->get_errors}],
        [map {$_->description} @{get_checker($type_by_condition)->check( {'name' => '1', 'value' => '1' } )->get_errors}],
        [map {$_->description} @{get_checker($type_by_condition)->check( {'name' => 'foo', 'value' => {} } )->get_errors}],
    ],    
    [
        ['value must not contain any whitespace'],
        ['too short'],
        ['less than seven', 'too short'],
        ["11,22,33 for names except 'foo', 'bar', 'qux'"],
        ['wrong string'],
    ],
'type by condition - errors';


ok get_checker($item_any_of)->check([{'name' => 'orange', 'weight' => 18, 'color'=>'Yellow' }])->is_valid, 'item_any_of - correct data';

is_deeply 
    [
        map {$_->description} @{get_checker($item_any_of)->check( [
            {'name' => 'stone', 'weight' => 18, 'color'=>'Red' },
            {'name' => 'orange', 'weight' => 18, 'color'=>'Red' },
            {'name' => 'apple', 'weight' => 18, 'color'=>'White' },
            {'name' => 'apple', 'weight' => 9, 'color'=>'Green' },
            {'name' => 'brick', 'weight' => 150, 'color'=>'Red', 'smoothness'=>4 },
            {'name' => 'brick', 'weight' => 200, 'color'=>'White', 'smoothness'=>4 },
            {'name' => 'brick', 'weight' => 200, 'color'=>'Red', 'smoothness'=>"h" },
            {'name' => 'brick', 'weight' => 500, 'color'=>'Red', 'smoothness'=>7 },
            {'name' => 'brick', 'weight' => 500, 'color'=>'Red'},

        ] )->get_errors}
    ]
    ,    
    [
        'Wrong item',
        'invalid orange color',
        'invalid apple color',
        'too lightweight apple',
        'Invalid weight of brick',
        'color must be red',
        'invalid smoothness',
        'color must be white',
        'Invalid brick'
    ],
'item_any_of - errors';


done_testing();
