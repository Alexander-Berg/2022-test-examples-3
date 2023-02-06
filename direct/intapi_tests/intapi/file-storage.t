#! /usr/bin/perl 

use strict;
use warnings;

use MIME::Base64;
use Encode qw/decode_utf8 encode_utf8/;
use Test::Intapi;

use utf8;
use open ':std' => ':utf8';


my $url = base_url()."/file-storage";

my $data = [
{
    content => join("" , <DATA>),
    uid => 12345,
    id => '',
},
];

my @tests = (
    {
        name => 'no action',
        read_only=> 1,
        url => $url, 
        method => 'POST',
        code => 500,
        data => [{
            owner => 13, 
            content => 'CoNtEnT',
            type => 'documents',
        }],
        check_num => 1, 
        check => sub {
            my ($data, $resp, $name) = @_;

            is( decode_utf8($resp->content), 'Не указано имя команды', "$name: error message" );
        },
    },
    {
        name => 'unknown action',
        read_only=> 1,
        url => $url, 
        method => 'POST',
        code => 500,
        data => [{
            action => 'something_strange',
            owner => 13, 
            content => 'CoNtEnT',
            type => 'documents',
        }],
        check_num => 1, 
        check => sub {
            my ($data, $resp, $name) = @_;

            is( decode_utf8($resp->content), 'Неизвестная команда', "$name: error message" );
        },
    },
    {
        name => 'no id',
        read_only=> 1,
        url => $url, 
        method => 'POST',
        code => 500,
        data => [qw/get_file remove_file/],
        preprocess => sub { return {action => $_[0], owner => 13, type => 'documents'}; },
        check_num => 1, 
        check => sub {
            my ($data, $resp, $name) = @_;

            is( decode_utf8($resp->content), 'Не указан параметр id', "$name: error message" );
        },
    },
    {
        name => 'put doc',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => $data,
        preprocess => sub { return {action => 'put_file', owner => $_[0]->{uid}, content => encode_base64($_[0]->{content}), type => 'documents'}; },
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;

            isnt( $resp->content, undef, "$name: response defined" );
            # записываем id, чтобы в следующем тесте запросить его и сравнить контент документа
            $data->{id} = $resp->content; 
        },
    },
    {
        name => 'get doc',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => $data,
        preprocess => sub { return {action => 'get_file', id => $_[0]->{id}, type => 'documents'}; },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;

            ok( $resp, "$name: response defined" );
            my $doc = decode_base64($resp->decoded_content());
            is($doc, $data->{content}, "$name: content is the same")
        },
    },
    {
        name => 'remove doc',
        read_only => 0,
        url => $url,
        method => 'POST',
        data => $data,
        preprocess => sub { return {action => 'remove_file', id => $_[0]->{id}, type => 'documents'}; },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;

            ok( $resp, "$name: response defined" );
            my $doc = decode_base64($resp->decoded_content());
            is($doc, '', "$name: empty response")
        },
    },
    {
        name => 'get removed doc',
        read_only => 0,
        url => $url,
        method => 'POST',
        code => 424,
        data => $data,
        preprocess => sub { return {action => 'get_file', id => $_[0]->{id}, type => 'documents'}; },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;

            ok( $resp, "$name: response defined" );
            is(decode_utf8($resp->content), 'Файл не найден', "$name: message")
        },
    },

);


run_tests(\@tests);


__DATA__
10000000	3964
10000002	3756
10000003	1939,2505
10000006	2090,3334
10000010	2906
10000011	4100
