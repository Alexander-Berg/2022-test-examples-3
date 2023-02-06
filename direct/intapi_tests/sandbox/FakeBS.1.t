#!/usr/bin/perl

=head1 

    Тест фейковой отправки данных в БК

    $Id$

=cut

use warnings;
use strict;
use Test::Intapi;

use Data::Dumper;

use my_inc '../..';
use Settings;

my @test_prices = (1, "text", {a => 1});

my $url = base_url()."/FakeBS";

my @tests = (
    {
        name => 'simple query',
        read_only => 1,
        proxy => $url, 
        uri => "YaBSSOAPExport",
        method => 'UpdatePrices', 
        data => \@test_prices,
        preprocess => sub { return [$_[0]]; },
        check_num => 1, 
        check => sub {
            my ($data, $resp, $res, $name) = @_;

            is($res, 1, "$name: result");
        },
    },
);

run_soap_tests(\@tests);
