#! /usr/bin/perl 

# $Id$

use strict;
use warnings;
use utf8;

use Test::Intapi;

use JSON;

# curl -s -d 'country=RU&firstname=Test&password=TestPassword&language=ru&lastname=Test&login=sbx-lensgonTSp3H' 'http://9708.beta2.direct.yandex.ru/FakePassport/1/bundle/account/register/by_middleman/?consumer=slova'
# {"uid":"1000011565","status":"ok"}

my @logins = qw/
    adlabsdirect     
    advancetby       
    Adventum2009     
    advertisingPPC   
    advmesto         
    aleksandr-lis    
    aleksandrcheyda  
    alex-ziyangirov  
    alex-ziyangirov3 
    alexey1478       
/;

my $url = base_url()."/FakePassport";

my @tests = (
    {
        name => 'existing login',
        read_only => 1,
        url => "$url/1/bundle/account/register/by_middleman/?consumer=slova",
        method => 'POST',
        code => 200,
        data => \@logins,
        preprocess => sub { return {login => $_[0]} },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $doc;
            lives_ok { $doc = decode_json($resp->content) } "$name: parse response";
            is($doc->{status}, 'error', "$name: response status is error");
            is_deeply($doc->{errors}, ['login.notavailable'], "$name: the only error is login.notavailable");
        },
    },
);

run_tests(\@tests);
