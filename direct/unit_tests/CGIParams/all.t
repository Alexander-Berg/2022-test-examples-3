#!/usr/bin/perl

BEGIN {
    # нам нужны реальные инстансы redis, поэтому используем разработческую конфигурацию
    $ENV{SETTINGS_LOCAL_SUFFIX} = 'DevTest';
}

use strict;
use warnings;

use Test::More;
use Test::Deep;

use utf8;

if ($ENV{TEST_WITH_REDIS}) {
    require CGIParams;

    our $form_data;
    {
        no warnings qw/redefine once/;
        *CGIParams::parse_form = sub {return %$form_data};
    }

    my @data = (
        {method => 'POST', additional_key => rand(), form_data => {asdf => '2134', vasdfew=>"dsafdfaewr", cmd => 'showCamp'}},
        {method => 'GET', additional_key => rand(), form_data => {asdf => '2134', vasdfew=>"москва", cmd => 'showCamps'}},
        );

    for my $d (@data) {
        local $form_data = $d->{form_data};
        $d->{key} = CGIParams::save_cgi_params(%$d);
        ok(scalar($d->{key} =~ /^\d+$/));
    }
} else {
    # тесты с рекальным redis в sandbox запускаться не умеют
    ok(1);
}

done_testing;
