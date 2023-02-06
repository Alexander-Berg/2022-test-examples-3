#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;
use XML::LibXML;
use Data::Dumper;

use utf8;
use open ':std' => ':utf8';

# wget -qO - 'http://8805.beta.direct.yandex.ru/FakeBlackbox?method=userinfo&login=sbx-lensgonTSp3H'

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

my %UID;

my $url = base_url()."/FakeBlackbox";

my @tests = (
    {
        name => 'userinfo by login',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => \@logins,
        preprocess => sub { return {login => $_[0], method => 'userinfo'} },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $doc;
            lives_ok { $doc = XML::LibXML->new()->parse_string($resp->content) } "$name: parse xml";
            my $error = XML::LibXML::XPathContext->new($doc)->find("/doc/error")->string_value();
            is($error, 'OK', "$name: status for $data");
            my $login = XML::LibXML::XPathContext->new($doc)->find("/doc/login")->string_value();
            is($login, $data, "$name: login for $data");
            $UID{$data} = XML::LibXML::XPathContext->new($doc)->find("/doc/uid")->string_value();
        },
    },
    {
        name => 'userinfo by uids',
        read_only => 1,
        url => $url, 
        method => 'GET',
        code => 200,
        data => \@logins,
        preprocess => sub { return {uid => $UID{$_[0]}, method => 'userinfo'} },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $doc;
            lives_ok { $doc = XML::LibXML->new()->parse_string($resp->content) } "$name: parse xml";
            my $error = XML::LibXML::XPathContext->new($doc)->find("/doc/error")->string_value();
            is($error, 'OK', "$name: status for $data");
            my $login = XML::LibXML::XPathContext->new($doc)->find("/doc/login")->string_value();
            isnt($login, '', "$name: login for $data");
        },
    },
);

run_tests(\@tests);
