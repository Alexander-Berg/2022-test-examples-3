use strict;
use warnings;

use Storable qw/dclone/;
use Test::Deep;
use Test::Exception;
use Test::More;
use VarsSchema;

throws_ok {VarsSchema::generalize([{type => 'a'}])} qr/unknown type/, 'unknown type';
throws_ok {VarsSchema::generalize([{type => 'string'}, {type => 'array'}])} qr/mixed types/, 'mixed types';
lives_and {is VarsSchema::generalize([{type => 'string'}, {type => 'number'}])->{type}, 'string'} 'number is converted to string';

my $s1 = {
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
                    c => {type => 'string', description => ''},
                },
                required => [qw/b c/],
            },
        },
    },
    required => [qw/a/],
};

my $s2 = dclone $s1;
delete $s2->{properties}->{a}->{items}->{properties}->{c};
$s2->{properties}->{a}->{items}->{properties}->{d} = {type => 'string', description => ''};
$s2->{properties}->{a}->{items}->{required} = [qw/b d/];

my $s3 = dclone $s1;
$s3->{properties}->{a}->{items}->{properties}->{d} = {type => 'string', description => ''};
$s3->{properties}->{a}->{items}->{required} = [qw/b/];

my @tests = (
    [
        'trivial',
        [$s1, $s1],
        $s1
    ],
    [
        'merge items properties',
        [$s1, $s2],
        $s3
    ],
);

for my $t (@tests) {
   cmp_deeply(VarsSchema::generalize($t->[1]), $t->[2], $t->[0]); 
}

done_testing;
