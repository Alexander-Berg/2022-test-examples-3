#! /usr/bin/perl 

=head1 

   $Id$ 

=cut

use strict;
use warnings;

use Test::Intapi;

use JSON;
use Data::Dumper;

use utf8;
use open ':std' => ':utf8';

my @orders = ( 
    [],
    [36614],
    [36614, 36613],
    [36614, 36612, 36611],
);


my $url = base_url()."/FakeBSInfo";

my @tests = (
    {
        name => 'simple request',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => \@orders,
        preprocess => sub { return {'order-id' => join(" ", @{$_[0]})} },
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            my @lines = split "\n", $resp->content;
            is(scalar @lines, scalar @$data, "$name: number of lines");
        },
    },
);

run_tests(\@tests);
