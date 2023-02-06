#!/usr/bin/perl

=head1 

    простой тест на /FakeBalance

    $Id$

=cut

use warnings;
use strict;
use Test::Intapi;
use Data::Dumper;

use my_inc '../..';
use Settings;

my @logins = qw(
    yndx-sandbox-manager 
    at-direct-api-test   
    at-direct-ag-full    
);

my $url = base_url()."/FakeBalance";

my @tests = (
    {
        name => "simple correct requests",
        read_only => 1, 
        proxy => $url,
        method => "Balance.FindClient",
        data => \@logins,
        preprocess => sub { return [{Login => $_[0]}] },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;

            my ($err, $err_str, $content) = $resp->paramsall();
            ok(!$err, "Balance error: FindClient return '$err', '$err_str'");
            isnt($content->[0]->{EMAIL}, "", "email");
            ok($content->[0]->{EMAIL} =~ /\w+\@\w+/, "email-2: $content->[0]->{EMAIL}");
        },
    }, 
    {
        name => "wrong method",
        read_only => 1, 
        proxy => $url,
        method => "WrongMethod",
        fault => 1,
        data => \@logins,
        preprocess => sub { return [{Login => $_[0]}] },
        check_num => 2, 
        check => sub {
            my ($data, $resp, $name) = @_;

            isnt($resp->fault->{faultString}, '', "$name: faultString");
            isnt($resp->fault->{faultCode}, '', "$name: faultCode");
        },
    },
);

run_xmlrpc_tests(\@tests);
