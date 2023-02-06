#!/usr/bin/env perl

use Direct::Modern;

use my_inc '../..';

use Test::Intapi;

use Direct::AdGroups2;

use JSON;

$Test::Intapi::USE_DONE_TESTING = 1;

my $dyn_CampaignId = 13634364;
my $dyn_AdGroupId = 799669313;

my @tests = (
    make_test(
        {}, 'response format', sub {
            is(ref $_, "HASH");
        },
    ),
    make_test(
        {1 => [1]}, 'response to invalid format', sub {
            cmp_deeply($_, {success => 0});
        },
    ),
    make_test(
        {1 => {1 => 'empty'}}, 'nonexistent items' => sub {
            cmp_deeply($_, {success => 1, error_items => {1 => [1]}});
        },
    ),
    make_test(
        {'campaignId1' => {1 => 'empty'}}, 'invalid CampaignId' => sub {
            cmp_deeply($_, {success => 1, error_items => {'campaignId1' => [1]}});
        },
    ),
    make_test(
        {$dyn_CampaignId => {$dyn_AdGroupId => 'invalid'}}, 'invalid status' => sub {
            cmp_deeply($_, {success => 1, error_items => {$dyn_CampaignId => [$dyn_AdGroupId]}});
        },
    ),
    make_test(
        {$dyn_CampaignId => {$dyn_AdGroupId => 'empty'}}, 'valid query (set to empty)' => sub {
            cmp_deeply($_, {success => 1});
            is(Direct::AdGroups2->get($dyn_AdGroupId)->items->[0]->status_bl_generated, "No");
        },
    ),
    make_test(
        {$dyn_CampaignId => {$dyn_AdGroupId => 'non-empty'}}, 'valid query (set to non-empty)' => sub {
            cmp_deeply($_, {success => 1});
            is(Direct::AdGroups2->get($dyn_AdGroupId)->items->[0]->status_bl_generated, "Yes");
        },
    ),
    make_test(
        {$dyn_CampaignId => {$dyn_AdGroupId => 'empty', 'badId' => 'empty'}, 1 => {2 => 'empty'}}, 'partial valid query' => sub {
            cmp_deeply($_, {success => 1, error_items => {$dyn_CampaignId => ['badId'], 1 => [2]}});
            is(Direct::AdGroups2->get($dyn_AdGroupId)->items->[0]->status_bl_generated, "No");
        },
    ),
    make_test(
        {1 => {$dyn_AdGroupId => 'non-empty'}}, 'other CampaignId' => sub {
            cmp_deeply($_, {success => 1, error_items => {1 => [$dyn_AdGroupId]}});
        },
    ),
);

sub make_test {
    my ($data, $name, $check_sub) = @_;
    state $test_num = 0;
    $test_num++;
    return {
        name => $name // "test #${test_num}",
        read_only => 0,
        url => '/jsonrpc/BannerLand',
        method => 'POST',
        data => [$data],
        preprocess => sub {
            return to_json {
                method => 'setStatusForAdGroups',
                params => $_[0],
            };
        },
        check => sub {
            my ($data_in, $resp, $name) = @_;
            my $r = decode_json($resp->content);
            local $_ = $r->{result};
            $check_sub->($resp);
        },
    };
};

run_tests(\@tests);

done_testing;
