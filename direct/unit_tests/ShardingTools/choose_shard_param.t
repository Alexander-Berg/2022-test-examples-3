#!/usr/bin/perl

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;
use Test::Exception;
use Yandex::DBTools;
use Yandex::DBShards;
use Yandex::DBUnitTest;

use Settings;
use ShardingTools;

use utf8;

$Yandex::DBTools::QUOTE_DB = Yandex::DBUnitTest::UT;

my @tests_dont_die = (
[
    {uid => 1, cid => 2, bid => 3, pid => 4},
    [qw/bid cid pid/],
    {},
    {bid => 3},
    'Get the first element',
], [
    {uid => 1, cid => 2},
    [qw/bid cid pid/],
    {},
    {cid => 2},
    'Get the second element',
], [
    {uid => 1, cid => 2},
    [qw/bid cid pid/],
    {allow_shard_all => 1},
    {cid => 2},
    'Get the element with allow_shard_all',
], [
    {bid => 1, pid => 2},
    [qw/uid cid/],
    {allow_shard_all => 1},
    {shard => 'all'},
    'Get shard=>all with allow_shard_all',
],

[
    {'c.uid' => 1, 'p.cid' => [2,3]},
    [qw/bid pid uid cid/],
    {},
    {uid => 1},
    'Get the element - key with table alias',
], [
    {'c.uid' => 1, 'p.cid' => [2,3]},
    [qw/cid uid/],
    {},
    {cid => [2,3]},
    'Get the element - key with table alias',
], [
    {'c.uid' => 1, 'campaigns.cid' => [2,3]},
    [qw/cid uid/],
    {},
    {cid => [2,3]},
    'Get the element - key with table name',
], [
    {'banner_images.bid' => 1},
    [qw/bid/],
    {},
    {bid => 1},
    'Get the element - key with table_name',
], [
    {'`campaigns`.`cid`' => 1},
    [qw/cid/],
    {},
    {cid => 1},
    'Get the element - key with table name (quoted)',
], [
    {'`banner_images`.`bid`' => 2},
    [qw/bid/],
    {},
    {bid => 2},
    'Get the element - key with table_name (quoted)',
],

[
    { '`users`.`ClientID`' => 1, '`users_options`.`uid`' => 2, '`camp_options`.`cid`' => 3, '`campaigns`.`OrderID`' => 4, '`phrases`.`pid`' => 5, '`banner_images`.`bid`' => 6, '`media_banners`.`mbid`' => 7, '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {ClientID => 1},
    'Get elements by default keys - ClientID',
], [
    { '`users_options`.`uid`' => 2, '`camp_options`.`cid`' => 3, '`campaigns`.`OrderID`' => 4, '`phrases`.`pid`' => 5, '`banner_images`.`bid`' => 6, '`media_banners`.`mbid`' => 7, '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {uid => 2},
    'Get elements by default keys - uid',
], [
    { '`camp_options`.`cid`' => 3, '`campaigns`.`OrderID`' => 4, '`phrases`.`pid`' => 5, '`banner_images`.`bid`' => 6, '`media_banners`.`mbid`' => 7, '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {cid => 3},
    'Get elements by default keys - cid',
], [
    { '`campaigns`.`OrderID`' => 4, '`phrases`.`pid`' => 5, '`banner_images`.`bid`' => 6, '`media_banners`.`mbid`' => 7, '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {OrderID => 4},
    'Get elements by default keys - OrderID',
], [
    { '`phrases`.`pid`' => 5, '`banner_images`.`bid`' => 6, '`media_banners`.`mbid`' => 7, '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {pid => 5},
    'Get elements by default keys - pid',
], [
    { '`banner_images`.`bid`' => 6, '`media_banners`.`mbid`' => 7, '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {bid => 6},
    'Get elements by default keys - bid',
], [
    { '`media_banners`.`mbid`' => 7, '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {mbid => 7},
    'Get elements by default keys - mbid',
], [
    { '`banners`.`BannerID`' => 8, '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {BannerID => 8},
    'Get elements by default keys - BannerID',
], [
    { '`tag_group`.`tag_id`' => 9, '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {tag_id => 9},
    'Get elements by default keys - tag_id',
], [
    { '`sitelinks_set_to_link`.`sitelinks_set_id`' => 10 },
    undef,
    {},
    {sitelinks_set_id => 10},
    'Get elements by default keys - sitelinks_set_id',
], [
    { '`users`.`FIO`' => 1, '`users_options`.`options`' => 2, '`camp_options`.`create_time`' => 3, '`campaigns`.`sum`' => 4, '`banner_images`.`image_hash`' => 6, '`media_banners`.`md5_flash`' => 7, '`banners`.`LastChange`' => 8, '`tag_campaign_list`.`tag_name`' => 9, '`sitelinks_set_to_link`.`order_num`' => 10 },
    undef,
    {allow_shard_all => 1},
    {shard => 'all'},
    'Get shard=>all with allow_shard_all by default keys',
], 
    [
        { pid => 1, bid => 2, 'g.statusModerate' => 'Yes', 'b.statusModerate' => 'Yes' },
        [qw/pid bid/],
        {},
        { pid => 1 },
        "dont die with duplicate non-sharded keys",
    ],

    [{ 'c.cid' => 1, sql_quote_identifier('b.bid') => 2 }, [qw/cid bid/], {}, { cid => 1 }, "choose shard with table name"],
    [{ 'c.cid' => 1, sql_quote_identifier('b.bid') => 2 }, [qw/bid cid/], {}, { bid => 2 }, "choose shard with quoted identifier"],
);
my @tests_die = (
	[{bid => 1, pid => 2}, [qw/uid cid/], {}, 'undefined value => die'],
    [{pid => 15, 'g.pid' => [16,15]}, ['pid'], {}, 'duplicate keys => die'],
    [{'`sitelinks_set_to_link`.`order_num`' => 11 }, undef, {}, 'undefined value with default keys => die'],
    [{ '`users`.`FIO`' => 1, '`users_options`.`options`' => 2, '`camp_options`.`create_time`' => 3, '`campaigns`.`sum`' => 4, '`banner_images`.`image_hash`' => 6, '`media_banners`.`md5_flash`' => 7, '`banners`.`LastChange`' => 8, '`tag_campaign_list`.`tag_name`' => 9, '`sitelinks_set_to_link`.`order_num`' => 10 }, undef, {}, 'undefined value with default keys => die'],
);


for my $test(@tests_dont_die) {
	my %shard = choose_shard_param($test->[0], $test->[1], %{$test->[2]});
	cmp_deeply(\%shard, $test->[3], $test->[4]);
}

for my $test(@tests_die) {
	dies_ok { choose_shard_param($test->[0], $test->[1], %{$test->[2]}) } $test->[3]; 
}

my ($where, @shard);

$where = {
    bid => 1,
    pid => 2,
};

@shard = choose_shard_param($where, [qw/pid bid/], set_shard_ids => 1);
cmp_deeply(\@shard, [pid => 2], 'set_shard_ids: check shard params');
cmp_deeply($where, { pid => SHARD_IDS, bid => 1 }, 'set_shard_ids: check where params');

$where = {
    'b.bid' => 1,
    '`p`.`pid`' => 2,
};
@shard = choose_shard_param($where, [qw/bid pid/], set_shard_ids => 1);
cmp_deeply(\@shard, [bid => 1], 'set_shard_ids: check shard params');
cmp_deeply($where, { 'b.bid' => SHARD_IDS, '`p`.`pid`' => 2 }, 'set_shard_ids: check where params');

$where = {
    'b.bid' => 1,
    '`p`.`pid`' => 2,
};
@shard = choose_shard_param($where, [qw/pid bid/], set_shard_ids => 1);
cmp_deeply(\@shard, [pid => 2], 'set_shard_ids: check shard params');
cmp_deeply($where, { 'b.bid' => 1, '`p`.`pid`' => SHARD_IDS }, 'set_shard_ids: check where params');

$where = {
    'b.bid' => 1,
    '`p`.`pid`' => [1..10],
};
@shard = choose_shard_param($where, [qw/pid bid/], set_shard_ids => 1);
cmp_deeply(\@shard, [pid => [1..10]], 'set_shard_ids: check shard params (with array of values)');
cmp_deeply($where, { 'b.bid' => 1, '`p`.`pid`' => SHARD_IDS }, 'set_shard_ids: check where params (with array of values)');

$where = {
    'b.bid' => [1,2],
    '`p`.`pid`' => [1..10],
};
@shard = choose_shard_param($where, [qw/cid/], allow_shard_all => 1, set_shard_ids => 1);
cmp_deeply(\@shard, [shard => 'all'], 'set_shard_ids: check shard params (with allow_shard_all)');
cmp_deeply($where, { 'b.bid' => [1,2], '`p`.`pid`' => [1..10] }, 'set_shard_ids: check where params (with allow_shard_all)');

done_testing();
