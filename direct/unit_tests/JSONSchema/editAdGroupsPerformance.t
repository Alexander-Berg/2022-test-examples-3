#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use JSON::XS;
use JSSchema::Validator;

BEGIN {
    use_ok 'JSSchema::Validator';
}

sub get_validator {
    my $type = shift // 'output';
    return JSSchema::Validator->new(for => $type);
}

my $CMD = 'editAdGroupsPerformance';

my $correct_output_data = <<OUTPUT
{
    "campaign": {
        "cid" : 1234567,
        "uid" : 1111111,
        "multipliers_meta" : {
            "retargeting_multiplier": {
                "pct_max" : 15,
                "pct_min" : 0,
                "max_conditions" : 7
            },
            "demography_multiplier":{
                "pct_max" : 31,
                "pct_min" : -24,
                "max_conditions" : 6
            },
            "mobile_multiplier" : {
                "pct_max" : 2,
                "pct_min" : 1
            }
        },
        "has_archived_banners_are_opened_for_edit" : true,
        "sum" : 100500,
        "status_moderate" : "Yes",
        "archived": "No",
        "groups" : []
    },
    "feeds": [
        {
            "feed_id" : 2345,
            "name" : "feed_01",
            "offers_count" : 77,
            "last_refreshed" : "2016-03-15",
            "categories" : [
                {
                    "category_id" : 22222,
                    "name" : "category_22222",
                    "path" : [3,0,2,9,1],
                    "is_deleted" : false
                },
                {
                    "category_id" : 44444,
                    "name" : "category_44444",
                    "path" : [2,7,0],
                    "is_deleted" : true
                },
                {
                    "category_id" : 88888,
                    "name" : "category_88888",
                    "path" : [3,0,2,9,1],
                    "is_deleted" : false
                }
            ]
        }
    ],
    "tags_allowed" : true,
    "is_groups_copy_action": false,
    "all_retargeting_conditions": {
        "0" : {
            "condition_name" : "condition_0",
            "condition" : [{"goals":[{"goal_id":1892464,"time":14}],"type":"or"},{"goals":[{"goal_id":1892467,"time":14}],"type":"not"}],
            "condition_desc" : "condition_0 description",
            "ret_cond_id" : 91,
            "is_accessible" : true
        },
        "77" : {
            "condition_name" : "condition_77",
            "condition" : [{"goals":[{"goal_id":11111,"time":14}],"type":"and"},{"goals":[{"goal_id":1892467,"time":14}],"type":"not"}],
            "condition_desc" : "condition_77 description",
            "ret_cond_id" : 19,
            "is_accessible" : false
        }
    }
}

OUTPUT
;

my $correct_input_data = <<INPUT
    {
        "cid": "12345678"
    }
INPUT
;

ok get_validator('input')->validate( $CMD => decode_json($correct_input_data) ), 'correct_input_data';


my $validator = get_validator('output');
my $data = decode_json($correct_output_data);


ok $validator->validate($CMD => $data), 'correct output data';

$data->{campaign}->{cid} = 'xxxxxxxxxxx';
is_deeply
    $validator->validate($CMD => $data)->errors,
    {'/campaign/cid' => {
        keyword => 'type',
        message => 'instance type doesn\'t match schema type',
        data => 'xxxxxxxxxxx',
        data_type => 'string',
    }}, 'wrong data';


is_deeply 
    $validator->bite_by_schema($CMD => { %$data, alien => '-----<+>----------------<+>-----' }),
    $data,
    'bite_by_schema';

done_testing;
