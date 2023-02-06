#!/usr/bin/perl

=head1 

    Копия теста на /soap

    Очень простой тест на /soap-utf8 
    Никакая содержательная работа не проверяется, 
    только "echo" -- запрос дошел туда, и такой же вернулся обратно

    $Id$

=cut

use warnings;
use strict;
use Test::Intapi;

use Data::Dumper;

use my_inc '../..';
use Settings;

my @data = (
    'text',
    "long\ntext",
    [ 1, 2, 3, ],
    { a => 1, b => 2 },
    [{ a => 1, b => 2 }, {arr => [{}, 5, 't']}],
);

my $url = base_url()."/soap-utf8";

my @tests = (
    {
        name => 'echo',
        read_only => 1,
        proxy => $url, 
        uri => "ServiceSOAP",
        method => 'soapEcho', 
        data => \@data,
        preprocess => sub { return [$_[0]]; },
        check_num => 1, 
        check => sub {
            my ($data, $resp, $res, $name) = @_;

            is_deeply($res, $data, 'result');
        },
    },
);

run_soap_tests(\@tests);
