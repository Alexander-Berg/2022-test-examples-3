#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;

use Client;
$Yandex::DBShards::STRICT_SHARD_DBNAMES = 0;

local $Yandex::DBUnitTest::SHARDED_DB_RE = qr/^ppc$/;   # Считаем, что PPC шардирована
local $Yandex::DBShards::STRICT_SHARD_DBNAMES = 1;

my %rows = (
    1 => [ {
            ClientID => 1,
            camp_count_limit => 7000,
            unarc_camp_count_limit => 4000,
            banner_count_limit => 0,
            feed_count_limit => 0,
            video_blacklist_size_limit => 100,
            general_blacklist_size_limit => 200,
        }, ],
    2 => [ {ClientID => 2, camp_count_limit => 0, unarc_camp_count_limit => 6813, banner_count_limit => 9821, feed_count_limit => 123 },
           {ClientID => 3, camp_count_limit => 0, unarc_camp_count_limit => 0, banner_count_limit => 0, feed_count_limit => 0, feed_max_file_size => 6434444, keyword_count_limit => 300} ],
);

my %db = (
    client_limits => {
        original_db => PPC(shard => 'all'),
        like => 'client_limits',
        rows => \%rows,
    },
    shard_client_id => {
        original_db => PPCDICT,
        like => 'shard_client_id',
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 2},
            {ClientID => 3, shard => 2},
            {ClientID => 99999, shard => 2},
        ],
    },
);

my %clients = (
    1 => {camp_count_limit => 7000, unarc_camp_count_limit => 4000,
          banner_count_limit => $Settings::DEFAULT_BANNER_COUNT_LIMIT,
          feed_max_file_size => $Settings::DEFAULT_FEED_MAX_FILE_SIZE,
          feed_count_limit => $Settings::DEFAULT_FEED_COUNT_LIMIT,
          keyword_count_limit => $Settings::DEFAULT_KEYWORD_COUNT_LIMIT,
          video_blacklist_size_limit => 100,
          general_blacklist_size_limit => 200,
        },
    2 => {camp_count_limit => $Settings::DEFAULT_CAMP_COUNT_LIMIT, 
          unarc_camp_count_limit => 6813, banner_count_limit => 9821,
          feed_max_file_size => $Settings::DEFAULT_FEED_MAX_FILE_SIZE,
          feed_count_limit => 123,
          keyword_count_limit => $Settings::DEFAULT_KEYWORD_COUNT_LIMIT,
          video_blacklist_size_limit => $Settings::DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT,
          general_blacklist_size_limit => $Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT,
      },
    3 => {camp_count_limit => $Settings::DEFAULT_CAMP_COUNT_LIMIT,
          unarc_camp_count_limit => $Settings::DEFAULT_UNARC_CAMP_COUNT_LIMIT,
          banner_count_limit => $Settings::DEFAULT_BANNER_COUNT_LIMIT,
          feed_max_file_size => 6434444,
          feed_count_limit => $Settings::DEFAULT_FEED_COUNT_LIMIT,
          keyword_count_limit => 300,
          video_blacklist_size_limit => $Settings::DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT,
          general_blacklist_size_limit => $Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT,
      },
    99999 => {camp_count_limit => $Settings::DEFAULT_CAMP_COUNT_LIMIT,
              unarc_camp_count_limit => $Settings::DEFAULT_UNARC_CAMP_COUNT_LIMIT,
              banner_count_limit => $Settings::DEFAULT_BANNER_COUNT_LIMIT,
              feed_max_file_size => $Settings::DEFAULT_FEED_MAX_FILE_SIZE,
              feed_count_limit => $Settings::DEFAULT_FEED_COUNT_LIMIT,
              keyword_count_limit => $Settings::DEFAULT_KEYWORD_COUNT_LIMIT,
              video_blacklist_size_limit => $Settings::DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT,
              general_blacklist_size_limit => $Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT,
        },
);

my @limits = (
    [{camp_count_limit => undef, unarc_camp_count_limit => 'jddijf', banner_count_limit => '0sdf2'}, 
        ['неверно указано ограничение на общее число кампаний',
          'неверно указано ограничение на число объявлений в каждой кампании; необходимый диапазон от 1000 до 15000',
          'неверно указано ограничение на число незаархивированных кампаний']],
          
    [{camp_count_limit => 0, unarc_camp_count_limit => '-45', banner_count_limit => '9999999999999'},
        ['неверно указано ограничение на число незаархивированных кампаний',
          'неверно указано ограничение на число объявлений в каждой кампании; необходимый диапазон от 1000 до 15000']],
    [{camp_count_limit => 0, unarc_camp_count_limit => 5672, banner_count_limit => 7881},
        ['ограничение на число незаархивированных кампаний не должно превышать ограничение на общее число кампаний']],
    [{camp_count_limit => 7812, unarc_camp_count_limit => 711, banner_count_limit => 8913},
        []]                  
);




plan tests => @limits + keys %clients;

init_test_dataset(\%db);
foreach my $id (keys %clients) {
    cmp_deeply(Client::get_client_limits($id), $clients{$id})
}

foreach my $l (@limits) {
    cmp_deeply([sort {$a cmp $b} Client::validate_client_limits($l->[0])],
                [sort @{$l->[1]}])
}
