use strict;
use warnings;

use Test::Deep;
use Test::More;
use VarsSchema;

my @tests = (
    [
        'simple nested hash',
        {a => {b => 1, c => 1}},
        {
            type => 'object',
            description => '',
            properties => {
                a => {
                    type => 'object',
                    description => '',
                    properties => {
                        b => {type => 'number', description => ''},
                        c => {type => 'number', description => ''},
                    },
                },
            },
        },
    ],
    [
        'one element array',
        {a => [{b => 1, c => 1}]},
        {
            type => 'object',
            description => '',
            properties => {
                a => {
                    type => 'array',
                    description => '',
                    items => {
                        type => 'object',
                        description => '',
                        properties => {
                            b => {type => 'number', description => ''},
                            c => {type => 'number', description => ''},
                        },
                        required => [qw/b c/],
                    },
                },
            },
        },
    ],
    [
        'multi element array',
        {a => [{b => 1, c => 1}, {b => 1, d => 1}]},
        {
            type => 'object',
            description => '',
            properties => {
                a => {
                    type => 'array',
                    description => '',
                    items => {
                        type => 'object',
                        description => '',
                        properties => {
                            b => {type => 'number', description => ''},
                            c => {type => 'number', description => ''},
                            d => {type => 'number', description => ''},
                        },
                        required => [qw/b/],
                    },
                },
            },
        },
    ],
    [
        'blinking property',
        [[{a => 1}], [{a => 1, b => 1}]],
        {
            type => 'array',
            description => '',
            items => {
                type => 'array',
                description => '',
                items => {
                    type => 'object',
                    description => '',
                    properties => {
                        a => {type => 'number', description => ''},
                        b => {type => 'number', description => ''},
                    },
                    required => [qw/a/],
                },
            },
        },
    ],
    [
        'nested blinking property',
        [{a => [{a => 1}, {a => 1, b => 1}]}],
        {
            type => 'array',
            description => '',
            items => {
                type => 'object',
                description => '',
                properties => {
                    a => {
                        type => 'array',
                        description => '',
                        items => {
                            type => 'object',
                            description => '',
                            properties => {
                                a => {type => 'number', description => ''},
                                b => {type => 'number', description => ''},
                            },
                            required => [qw/a/],
                        },
                    },
                },
                required => [qw/a/],
            },
        },
    ],
    [
        'numeric keys',
        [{1 => 1, 2 => 2}, {1 => 1, 2 => 2}],
        {
            type => 'array',
            description => '',
            items => {
                type => 'object',
                description => '',
                properties => {
                    NNNNN => {
                        type => 'number',
                        description => '',
                    }
                },
                required => [],
            },
        },
    ],
);

for my $t (@tests) {
   cmp_deeply(VarsSchema::get_schema($t->[1]), $t->[2], $t->[0]); 
}

done_testing;
