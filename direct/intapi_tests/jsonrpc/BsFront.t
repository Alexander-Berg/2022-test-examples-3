#! /usr/bin/perl 

use Direct::Modern;
use open ':std' => ':utf8';
use Test::Intapi;
use Test::More;
use Test::Deep;
use List::MoreUtils qw/any/;
use JSON;

my @tests = (
    # 50437873 - тимлид менеджеров 
    test_auth(2 => {operator_uid => 50437873, client_login => 'api-serv-rub'}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        ok(any {/create|edit/} @{$r->{result}->{availableActions}});
    }),
    test_auth(1 => {operator_uid => "dsfdfsdf", client_login => "gsdfdsfdfsg", creative_id => -12}, sub {
        my $r = shift;
        ok($r->{error}->{message} =~ /incorrect .+ operator_uid/);
    }),
    # holodilnikru editing exists creative
#    test_auth(2 => {operator_uid => 6138950, creative_id => 5}, sub {
#        my $r = shift;
#        ok(@{$r->{result}->{availableTemplates}} > 0);
#        ok(any {/create|edit/} @{$r->{result}->{availableActions}})
#    }),
    # holodilnikru creating new creative
    test_auth(2 => {operator_uid => 6138950}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        ok(any {/create|edit/} @{$r->{result}->{availableActions}})
    }),
    # manager create new creative for available client
    test_auth(2 => {operator_uid => 20502235, client_login => 'abm-stroy001'}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        ok(any {/create|edit/} @{$r->{result}->{availableActions}})
    }),
    # abm-stroy001 not a owner creative 5 
    test_auth(1 => {operator_uid => 20502235, client_login => 'abm-stroy001', creative_id => 5}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableActions}} == 0);
    }),
    test_auth(1 => {operator_uid => 20502235, client_login => 'holodilnikru'}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableActions}} == 0)
    }),
    
    # agency
    test_auth(2 => {operator_uid => 11787588, client_login => 'A-AD-Felix'}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        ok(any {/create|edit/} @{$r->{result}->{availableActions}});
    }),
    test_auth(1 => {operator_uid => 11787588, client_login => 'holodilnikru'}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableActions}} == 0)
    }),
    
    # agency|manager create creatives for yourself
    test_auth(1 => {operator_uid => 11787588}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableActions}} == 0)
    }),
    test_auth(1 => {operator_uid => 20502235}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableActions}} == 0)
    }),
    
    # super
    test_auth(2 => {operator_uid => 50244268, client_login => 'at-client-curr-rus'}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        cmp_deeply(
            [sort @{$r->{result}->{availableActions}}], 
            [sort qw/creative_create creative_edit creative_delete creative_get/]
        );
    }),
    
    test_auth(2 => {operator_uid => 50244268}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} == 0);
        ok(@{$r->{result}->{availableActions}} == 0);
    }),
    
    test_auth(2 => {operator_uid => 101260409, client_login => "at-direct-api-test"}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        ok(@{$r->{result}->{availableActions}} > 1);
    }),
    
    # supermedia
    test_auth(2 => {operator_uid => 23916145, client_login => 'at-client-curr-rus'}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        cmp_deeply($r->{result}->{availableActions}, ['creative_get']);
    }),
    
    test_change_notify(1 => {operator_uid => 6138950, creatives => [{id => -8}]}, sub {
        my $r = shift;
        ok($r->{error}->{message} =~ /incorrect .+ creatives/);
    }),

    # uid 195960142 - alisecar is main represetative
    test_auth(2 => {operator_uid => 195960142, client_login => "alisecar"}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        ok(@{$r->{result}->{availableActions}} > 1);
    }),
    
    # uid 195960142 - alisecar1 is represetative of alisecar
    test_auth(2 => {operator_uid => 351732966, client_login => "alisecar1"}, sub {
        my $r = shift;
        ok(@{$r->{result}->{availableTemplates}} > 0);
        ok(@{$r->{result}->{availableActions}} > 1);
    }),
);

run_tests(\@tests);

sub test_auth {
    
    my ($check_num, $params, $check) = @_;
    
    return {
        url => "/jsonrpc/BsFront", 
        method => 'POST',
        read_only => 1,
        data => [$params],
        preprocess => sub {
            return to_json {
                method => "auth", 
                params => $_[0]
            };
        },
        check_num => $check_num, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $r = decode_json($resp->content);
            $check->($r);
        },
    };    
}

sub test_change_notify {
    
    my ($check_num, $params, $check) = @_;
    
        return {
        url => "/jsonrpc/BsFront", 
        method => 'POST',
        read_only => 1,
        data => [$params],
        preprocess => sub {
            return to_json {
                method => "change_notify", 
                params => $_[0]
            };
        },
        check_num => $check_num,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $r = decode_json($resp->content);
            $check->($r);
        },
    };    
}