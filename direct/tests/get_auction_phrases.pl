#!/usr/bin/env perl

use strict;
use warnings;
use utf8;
use my_inc;

use Test::More;
use Test::Deep;

use Yandex::DBTools;
use Settings;

use PhrasesAuction;
use BannersCommon;

sub check_auction_phrases {

    my ($phrases, $banners_phrases) = @_;
    
    my @auction_phrases = sort {$a->{id} <=> $b->{id}} @$phrases;
    my @banners_phrases = sort {$a->{id} <=> $b->{id}} map {@{$_->{phrases}}} @$banners_phrases;
    
    is(scalar(@auction_phrases), scalar(@banners_phrases));
    while (@auction_phrases) {
        my $acuction_url = shift(@auction_phrases) || {};
        my $banner_url = shift(@banners_phrases) || {};
        is($acuction_url->{id}, $banner_url->{id});
        is($acuction_url->{bs_url}, $banner_url->{bs_url});
    } 
}

my $adgroups = get_hash_sql(PPC(shard => 'all'), ["
    SELECT p.pid, b.bid
    FROM
        phrases p
        JOIN campaigns c USING(cid)
        JOIN banners b USING(pid)
    WHERE
        c.statusActive = 'Yes'
    LIMIT 20 OFFSET ", int rand 1000]); 

my $phrases = PhrasesAuction::get_auction_phrases(pid => [keys %$adgroups]);

my ($banners) = BannersCommon::get_banners({
    bid => [values %$adgroups]
}, {
    get_auction => 1, no_pokazometer_data => 1,
    get_add_camp_options => 1
});

check_auction_phrases($phrases, $banners);

done_testing;
