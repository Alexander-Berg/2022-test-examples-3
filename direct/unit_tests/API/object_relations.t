#!/usr/bin/perl

use Direct::Modern;

use Test::More tests => 64;
use Test::Deep;
use Test::Exception;

my @CAMPAIGN_TYPES = qw/text mobile_content dynamic performance/;
my @ADGROUP_TYPES = qw/base mobile_content dynamic performance/;
my @BANNER_TYPES = qw/text mobile_content dynamic performance/;

BEGIN { use_ok('API::ObjectRelations') }

*is_eligible_container_item = \&API::ObjectRelations::is_eligible_container_item;

throws_ok { is_eligible_container_item() } qr/container class/, 'call without args';
throws_ok { is_eligible_container_item('campaign') } qr/container type/, 'call without container type';
throws_ok { is_eligible_container_item('campaign', 'text') } qr/item class/, 'call without item class';
throws_ok { is_eligible_container_item('campaign', 'text', 'adgroup') } qr/item type/, 'call without item type';
throws_ok { is_eligible_container_item('campaing', 'text', 'adgroup', 'base') } qr/campaing/, 'call with wrong container class';
throws_ok { is_eligible_container_item('campaign', 'text', 'abgroup', 'base') } qr/abgroup/, 'call with wrong item class';
foreach my $campaign_type (@CAMPAIGN_TYPES) {
    foreach my $adgroup_type (@ADGROUP_TYPES) {
        my $is_eligible = ($campaign_type eq $adgroup_type || $campaign_type eq 'text' && $adgroup_type eq 'base') ? 1 : 0;
        is(
            is_eligible_container_item('campaign', $campaign_type, 'adgroup', $adgroup_type), $is_eligible,
            ($is_eligible ? "" : "no ")."$adgroup_type adgroup in $campaign_type campaign"
        );
    }
}

foreach my $adgroup_type (@ADGROUP_TYPES) {
    foreach my $banner_type (@BANNER_TYPES) {
        my $is_eligible = $adgroup_type eq $banner_type || $adgroup_type eq 'base' && $banner_type eq 'text' ? 1 : 0;
        is(
            is_eligible_container_item('adgroup', $adgroup_type, 'banner', $banner_type), $is_eligible,
            ($is_eligible ? "" : "no ")."$banner_type banner in $adgroup_type adgroup"
        );
    }

    foreach my $item_class (qw/keyword retargeting/) {
        my $is_eligible = $adgroup_type =~ /^(?:base|mobile_content)$/ ? 1 : 0;
        is(
            is_eligible_container_item('adgroup', $adgroup_type, $item_class), $is_eligible,
            ($is_eligible ? "" : "no ")."$item_class in $adgroup_type adgroup"
        );
    }

    my $is_eligible = $adgroup_type eq 'dynamic' ? 1 : 0;
    is(
        is_eligible_container_item('adgroup', $adgroup_type, 'dynamic_condition'), $is_eligible,
        ($is_eligible ? "" : "no ")."dynamic_condition in $adgroup_type adgroup"
    );
}

foreach my $banner_type (@BANNER_TYPES) {
    foreach my $item_class (qw/vcard sitelink/) {
        my $is_eligible = $banner_type =~ /^(?:text|mobile_content)$/ ? 1 : 0;
        is(
            is_eligible_container_item('banner', $banner_type, $item_class), $is_eligible,
            "$item_class in $banner_type banner"
        );
    }

    my $is_eligible = $banner_type eq 'text' ? 1 : 0;
    is(
        is_eligible_container_item('banner', $banner_type, 'image'), $is_eligible,
        "image in $banner_type banner"
    );
}

ok(is_eligible_container_item('banner', 'text', 'image', 'new_type'), 'new_type image in text banner');

