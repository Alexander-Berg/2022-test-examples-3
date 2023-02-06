#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::CreateDBObjects;

use Settings;

use Direct::Test::DBObjects;

sub _sorted {
    my $ref = shift;
    if (ref $ref eq 'HASH') {
        _sorted($ref->{$_}) foreach keys %$ref
    } elsif (ref $ref eq 'ARRAY'){
        @$ref = sort @$ref;
    }
    return $ref;
}

BEGIN {
    use_ok('Geo');
}

# note: пометка (dub) означает, что регион поглощается вышестоящим
my @GEO = (
    '225,977,-52',          # Россия, Крым, -УФО
    '225,236,-59,-73,-52',  # Россия (без Крыма), Наб. Челны (dub), -СибФО, -ДВФО, -УФО 
    '187,977,1,236,-20536', # Украина, Крым (dub), Москва+МО, Наб. Челны, -Донецкая область
);

my (@GIVEN, @adgroups);

Direct::Test::DBObjects->create_tables;
my $db_obj = Direct::Test::DBObjects->new()->with_campaign('text'); 

for my $i (0 .. $#GEO) {
    my $adgroup =$db_obj->create_adgroup( text => {campaign_id => $db_obj->campaign->id, geo => $GEO[$i]});
    $adgroups[$i] = $adgroup;
    $GIVEN[$i] = Geo::get_union_geo_for_camp($db_obj->campaign->id, $db_obj->user->client_id);
}

is_deeply $GIVEN[0],
{
    geo => '225,-52', #Крым(977) в российском геодереве покрывается Россией, остальное без изменений
    extended => {
        225 => {all => 1},
        52  => {negative => {all => 1}},
    },
    #dictionary с именами групп, для фронта
    pid2group_name => { map {$_->id => $_->adgroup_name} ($adgroups[0]) },
}, 'one group';

is_deeply $GIVEN[1],
{
    geo => '225,-52', #Набережные челны тоже поглотились Россией, из минус-регионов остается только общий для всех
    extended => {
        #регионы, выбранные не для всех групп метятся как "partly", в kind - как интерпретировать adgroup_ids
        #в adgroup_ids - список id-групп для которых включен регион
        977 => {negative => {partly => { adgroup_ids => [$adgroups[1]->id]}}},
        225 => {all => 1},
        73  => {negative => {partly => {adgroup_ids => [$adgroups[1]->id]}}},
        59  => {negative => {partly => {adgroup_ids => [$adgroups[1]->id]}}},
        52  => {negative => {all => 1}},
    },
    pid2group_name => { map {$_->id => $_->adgroup_name} @adgroups[0..1] },
}, 'two groups with same minus-region';    

is_deeply _sorted($GIVEN[2]),
_sorted ({
    geo => '187,225', #Минус-регионов, заданных для всех групп не осталось, в плюс регионах - Россия и Украина
    extended => {
        977   => {negative => {partly => {adgroup_ids => [$adgroups[1]->id]}}, partly => {adgroup_ids => [$adgroups[2]->id]} },
        20536 => {negative => {partly => { adgroup_ids => [$adgroups[2]->id]}}},
        225   => {partly => {adgroup_ids => [$adgroups[0]->id, $adgroups[1]->id]}},
        236   => {partly => {adgroup_ids => [$adgroups[2]->id]}},
        187   => {partly => {adgroup_ids => [$adgroups[2]->id]}},
        73    => {negative => {partly => {adgroup_ids => [$adgroups[1]->id]}}},
        59    => {negative => {partly => {adgroup_ids => [$adgroups[1]->id]}}},
        52    => {negative => {partly => {adgroup_ids => [$adgroups[0]->id, $adgroups[1]->id]}}},
        1     => {partly => {adgroup_ids => [$adgroups[2]->id]}},
    },
    pid2group_name => { map {$_->id => $_->adgroup_name} @adgroups[0..2] },
}), 'three groups, +ukraine';


done_testing;
