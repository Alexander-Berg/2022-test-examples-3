#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;
use Encode;
use Data::Dumper;
use List::MoreUtils;

use utf8;
use open ':std' => ':utf8';


my $url = "/RequestBlockNumberDetailed";


my @existing_campaigns = (
    172,
    261, 
    263,
    295,
);

sub check_backtoback {
    my ($data, $responses, $name) = @_;
    my @values;
    foreach my $r (@$responses) {
        lives_ok { push @values, decode_json($r->content) } "$name: decode json";
    }
    cmp_deeply(@values, 'compare block date');
}


my @tests = (
    {
        name => 'incorrect json in POST',
        read_only => 1,
        url => $url, 
        method => 'POST', 
        data => \@existing_campaigns,
        code => 200,
        preprocess => sub { return encode('utf8', to_json {CampaignCode => $_[0] })."{}[]()" },
        check_num => 4, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{status}, 'error', "$name: status");
            is($v->{ResultCode}, 0, "$name: ResultCode");
        },
    },
    {
        name => 'incorrect POST data: missing CampaignCode',
        read_only => 1,
        url => $url, 
        method => 'POST', 
        data => \@existing_campaigns,
        code => 200,
        preprocess => sub { return encode('utf8', to_json {Campaign => $_[0] }) },
        check_num => 4, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{status}, 'error', "$name: status");
            is($v->{ResultCode}, 0, "$name: ResultCode");
        },
    },
    {
        name => 'incorrect GET data: missing CampaignCode',
        read_only => 1,
        url => $url, 
        method => 'GET', 
        data => \@existing_campaigns,
        code => 200,
        preprocess => sub { return {Campaign => $_[0]} },
        check_num => 4, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{status}, 'error', "$name: status");
            is($v->{ResultCode}, 0, "$name: ResultCode");
        },
    },
    {
        name => 'existing campaigns: GET cid',
        read_only => 1,
        url => $url, 
        method => 'GET', 
        data => \@existing_campaigns,
        preprocess => sub { return {cid =>$_[0]} },
        check_num => 6, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{status}, 'ok', "$name: status");
            is($v->{ResultCode}, 1, "$name: ResultCode");
            ok( $v->{BlockNumber} > 0, "$name: BlockNumber" );
            ok( List::MoreUtils::all { $_ } (map { exists $v->{$_} } qw/RequestNumber ModerateRequest BlockNumber ResultCode 
                    InactiveRequest InactiveBlock DraftBlock RejectedRequest status RejectedBlock ModerateBlock 
                    ActiveBlock DraftRequest ActiveRequest ArchiveBlock ArchiveRequest/), 'all required fields present');
        },
    },
    {
        name => 'existing campaigns: GET CampaignCode',
        read_only => 1,
        url => $url, 
        method => 'GET', 
        data => \@existing_campaigns,
        preprocess => sub { return {CampaignCode =>$_[0]} },
        check_num => 6, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{status}, 'ok', "$name: status");
            is($v->{ResultCode}, 1, "$name: ResultCode");
            ok( $v->{BlockNumber} > 0, "$name: BlockNumber" );
            ok( List::MoreUtils::all { $_ } (map { exists $v->{$_} } qw/RequestNumber ModerateRequest BlockNumber ResultCode 
                    InactiveRequest InactiveBlock DraftBlock RejectedRequest status RejectedBlock ModerateBlock 
                    ActiveBlock DraftRequest ActiveRequest ArchiveBlock ArchiveRequest/), 'all required fields present');
        },
    },
    {
        name => 'existing campaigns: POST CampaignCode',
        read_only => 1,
        url => $url, 
        method => 'POST', 
        data => \@existing_campaigns,
        preprocess => sub { return encode('utf8', to_json {CampaignCode =>$_[0]}) },
        check_num => 6, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v; 
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{status}, 'ok', "$name: status");
            is($v->{ResultCode}, 1, "$name: ResultCode");
            ok( $v->{BlockNumber} > 0, "$name: BlockNumber" );
            ok( List::MoreUtils::all { $_ } (map { exists $v->{$_} } qw/RequestNumber ModerateRequest BlockNumber ResultCode 
                    InactiveRequest InactiveBlock DraftBlock RejectedRequest status RejectedBlock ModerateBlock 
                    ActiveBlock DraftRequest ActiveRequest ArchiveBlock ArchiveRequest/), 'all required fields present');
        },
    },
    # back to back testing
    {
        name => 'existing campaigns: GET cid',
        read_only => 1,
        is_backtoback => 1,
        url => $url, 
        method => 'GET', 
        data => \@existing_campaigns,
        preprocess => sub { return {cid =>$_[0]} },
        check_num => 3, 
        check => \&check_backtoback,
    },
    {
        name => 'existing campaigns(back-to-back): GET CampaignCode',
        read_only => 1,
        is_backtoback => 1,
        url => $url, 
        method => 'GET', 
        data => \@existing_campaigns,
        preprocess => sub { return {CampaignCode =>$_[0]} },
        check_num => 3, 
        check => \&check_backtoback,
    },
    {
        name => 'existing campaigns(back-to-back): POST CampaignCode',
        read_only => 1,
        is_backtoback => 1,
        url => $url, 
        method => 'POST', 
        data => \@existing_campaigns,
        preprocess => sub { return encode('utf8', to_json {CampaignCode =>$_[0]}) },
        check_num => 3, 
        check => \&check_backtoback, 
    },
);

run_tests(\@tests);

