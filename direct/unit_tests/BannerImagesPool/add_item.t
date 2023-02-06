#!/usr/bin/perl

use warnings;
use strict;
use utf8;

use Test::More;

use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools qw/do_sql/;

use Settings;
use BannerImages::Pool;
use Test::JavaIntapiMocks::GenerateObjectIds;

*add = sub {
    BannerImages::Pool::add_items(@_)->[0]->{imp_id};
};
sub init_database {
    my %O = @_;

    init_test_dataset({
        shard_client_id => {
            original_db => PPCDICT,
            rows => [
                { ClientID => 1, shard => 1 },
                { ClientID => 2, shard => 2 },
                { ClientID => 3, shard => 3 },
                { ClientID => 4, shard => 4 },
            ],
        },
        shard_inc_banner_images_pool_id => {
            original_db => PPCDICT,
            rows => [
                { banner_images_pool_id => 49, ClientID => 0 },
            ],
        },
        banner_images_pool => {
            original_db => PPC(shard => 'all'),
            rows => {
                3 => [
                    { imp_id => 10, ClientID => 3, name => 'my_picture.png', image_hash => 'kL7tuwxubi0ohbXuhVZXQw' },
                ],
            },
        },
    });
    if ($O{with_auto_increment}) {
        do_sql(PPC(shard => 'all'), 'ALTER TABLE banner_images_pool MODIFY `imp_id` bigint(20) unsigned AUTO_INCREMENT NOT NULL');
        note('adding AUTO_INCREMENT to PRIMARY KEY');
    }
}

sub test_cases{
    my %O = @_;
    $O{prefix} //= '';
    
    init_database(with_auto_increment => $O{with_auto_increment});
    is(add([{ClientID => 4, name => 'new_picture.png', image_hash => 'C0KiRx-jJZTYKwiKATWJcw'}]),
       50, "$O{prefix}added new item"
    );
    check_test_dataset({
        shard_inc_banner_images_pool_id => {
            original_db => PPCDICT,
            rows => [
                { banner_images_pool_id => 49, ClientID => 0 },
                { banner_images_pool_id => 50, ClientID => 4 },
            ],
        },
        banner_images_pool => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [ 
                    { imp_id => 10, ClientID => 3, name => 'my_picture.png', image_hash => 'kL7tuwxubi0ohbXuhVZXQw' },
                ],
                4 => [ 
                    { imp_id => 50, ClientID => 4, name => 'new_picture.png', image_hash => 'C0KiRx-jJZTYKwiKATWJcw' },
                ],
            },
        },
    }, "$O{prefix}check database data");
################################################################################
    init_database(with_auto_increment => $O{with_auto_increment});
    ok(add([{ClientID => 3, name => 'my_picture.png', image_hash => 'kL7tuwxubi0ohbXuhVZXQw'}]) > 0, "$O{prefix}added same item");
    check_test_dataset({
        shard_inc_banner_images_pool_id => {
            original_db => PPCDICT,
            rows => [
                { banner_images_pool_id => 49, ClientID => 0 },
                { banner_images_pool_id => 50, ClientID => 3 },
            ],
        },
        banner_images_pool => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [ 
                    { imp_id => 10, ClientID => 3, name => 'my_picture.png', image_hash => 'kL7tuwxubi0ohbXuhVZXQw' },
                ],
                4 => [],
            },
        },
    }, "$O{prefix}check database data");
################################################################################
    init_database(with_auto_increment => $O{with_auto_increment});
    ok(add([{ClientID => 3, name => 'new_picture_name.png', image_hash => 'kL7tuwxubi0ohbXuhVZXQw'}]) > 0, "$O{prefix}added different item with same hash"); 
    check_test_dataset({
        shard_inc_banner_images_pool_id => {
            original_db => PPCDICT,
            rows => [
                { banner_images_pool_id => 49, ClientID => 0 },
                { banner_images_pool_id => 50, ClientID => 3 },
            ],
        },
        banner_images_pool => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [],
                2 => [],
                3 => [ 
                    { imp_id => 10, ClientID => 3, name => 'new_picture_name.png', image_hash => 'kL7tuwxubi0ohbXuhVZXQw' },
                ],
                4 => [],
            },
        },
    }, "$O{prefix}check database data");
}

test_cases(with_auto_increment => 1, prefix => '[with AI] ');
test_cases(with_auto_increment => 0, prefix => '[by table schema] ');
done_testing();
