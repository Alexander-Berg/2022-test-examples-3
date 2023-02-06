#! /usr/bin/perl 

=head1 

    тест фейковой статистики (/FakeBS/export/direct_orderstat.cgi)

=cut

use strict;
use warnings;

use Test::Intapi;

use JSON;
use Data::Dumper;

use utf8;
use open ':std' => ':utf8';

my @good_params = ( 
    {
        orderid => 1,
        start => '2010-06-07',
        stop => '2010-06-07',
    },
    {
        orderid => 100,
        start => '2011-02-21',
        stop => '2011-02-21',
    },
    {
        orderid => 1000,
        start => '2009-11-21',
        stop => '2009-11-24',
    },
    # вообще-то следующий запрос некорректный, без номера заказа
    # но на практике -- работает
    {
        orderid => '',
        start => '2010-06-07',
        stop => '2010-06-07',
    },
);

my @bad_params = ( 
    {
        orderid => 100,
        start => '2011-02-21',
        stop => '',
    },
    {
        orderid => 1000,
        start => '',
        stop => '2009-11-24',
    },
);

my $url = base_url()."/FakeBS/export/direct_orderstat.cgi";

my @tests = (
    {
        name => 'simple request',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => \@good_params,
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            ok($resp->content =~ /#End$/s, 'format');
        },
    },
    {
        name => 'bad request',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 500,
        data => \@bad_params,
        check_num => 0,
        check => sub {
            my ($data, $resp, $name) = @_;
        },
    },
);

run_tests(\@tests);
