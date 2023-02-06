#!/usr/bin/perl

=head1 

    тест на /FakeYaMoney

    $Id$

=cut

use warnings;
use strict;
use Test::Intapi;
use Data::Dumper;
use HTTP::Headers;

use my_inc '../..';
use Settings;

my $ok_token = '12345678901234.1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF';
my $wrong_token = '12345678901235.1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF';

my $headers_ok = HTTP::Headers->new;
$headers_ok->header('Authorization' => "Bearer $ok_token");

my $headers_wrong = HTTP::Headers->new;
$headers_wrong->header('Authorization' => "Bearer $wrong_token");

my $url = base_url()."/FakeYaMoney";

my @tests = (
    {
        name => 'token-validate-wrong',
        read_only => 1,
        url => $url."/token-validate", 
        method => 'POST',
        code => 401,
        check_num => 0,
        data => [[]],
    },
);

run_tests(\@tests);
