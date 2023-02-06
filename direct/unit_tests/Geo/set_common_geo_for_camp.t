#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::CreateDBObjects;

use Settings;

use Direct::Test::DBObjects;
use Direct::AdGroups2;

BEGIN {
    use_ok('Geo');
}

Direct::Test::DBObjects->create_tables;
my $db_obj = Direct::Test::DBObjects->new()->with_campaign('text'); 

sub _set_geo {
    my ($geo_changes, $mode) = @_;
    $mode //= 'merge';
    return Geo::set_common_geo(
            $db_obj->campaign->id,
            $geo_changes,
            merge_geo => $mode eq 'merge' ? 1 : 0,
            ClientID => $db_obj->user->client_id
    );
}

sub _refresh_group {
    my ($adgroup) = @_;
    return Direct::AdGroups2->get_by(adgroup_id => $adgroup->id)->items()->[0];
}

my $adgroup =$db_obj->create_adgroup( text => {campaign_id => $db_obj->campaign->id, geo => '113'});

my $geo_changes = {
  113 => {is_negative => 0 },
  225 => {is_negative => 0 },
  213 => {is_negative => 1 },
  20674 => {is_negative => 0},
};


_set_geo($geo_changes);
is _refresh_group($adgroup)->geo,
'113,225,-213,20674,977', #при сохранении в базу Россия превращается в "Россия+Крым"
'simple merge';

_set_geo({225 => {is_negative => 0 }}, 'replace');
is _refresh_group($adgroup)->geo,
'225,977',
'Russia, replace';

_set_geo({225 => {is_negative => 1 }});
is _refresh_group($adgroup)->geo,
'225,977',
'Russia -Russia, empty geo declined';

_set_geo({225 => {is_negative => 0 }, 1 => {is_negative => 1}}, 'replace');
is _refresh_group($adgroup)->geo,
'225,-1,977',
'Russia, Moscow district, replace';

_set_geo({1 => {is_negative => 1}});
is _refresh_group($adgroup)->geo,
'225,-1,977',
'Russia, Moscow district, merge';

_set_geo({225 => {is_negative => 0 }, 213 => {is_negative => 1}}, 'replace');
is _refresh_group($adgroup)->geo,
'225,-213,977',
'Russia -Moscow setted';

_set_geo({ 216 => { is_negative => 0}, 20674 => {is_negative => 0},21624 => {is_negative =>0}});
is _refresh_group($adgroup)->geo,
'216,225,-213,20674,21624,977',
'Russia -Moscow + (Troitsk, Scherbinka, Zelenograd)';

_set_geo({225 => {is_negative => 0 }, 3 => {is_negative => 1}}, 'replace');
is _refresh_group($adgroup)->geo,
'225,-3,977',
'Russia -Central_regions';

_set_geo({ 225 => { is_negative => 0}});
is _refresh_group($adgroup)->geo,
'225,977',
'Whole Russia';

done_testing;
