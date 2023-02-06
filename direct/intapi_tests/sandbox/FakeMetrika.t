#! /usr/bin/perl 

=head1 

    тест фейковвых ручек метрики (/FakeMetrika/...)

=cut

use strict;
use warnings;

use Test::Intapi;

use utf8;
use open ':std' => ':utf8';
warn base_url()."/FakeMetrika/direct/get_order_stat";
my @tests = (
    {
        name => 'bad request',
        read_only => 1,
        url => base_url()."/FakeMetrika/direct/unsupported", 
        method => 'GET',
        code => 500,
        data => [{}],
        check_num => 0,
        check => sub {
            my ($data, $resp, $name) = @_;
        },
    },
);

run_tests(\@tests);
