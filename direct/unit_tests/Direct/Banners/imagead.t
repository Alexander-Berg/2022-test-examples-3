#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::MockModule;
use Settings;

BEGIN {
    use_ok('Direct::Model::BannerImageAd');
    use_ok('Direct::Model::BannerCreative');
    use_ok('Direct::Banners::ImageAd');
}

sub mk_banner_with_creative {
    my ($init_status_banner, $init_status_creative) = @_;
    $init_status_banner //= 'Ready';
    $init_status_creative //= 'Yes';

    my $adgroup = Direct::Model::AdGroupText->new( id => 1, status_moderate => 'Yes' );
    $adgroup->has_show_conditions( 1 );

    my %args = (
        adgroup_id           => $adgroup->id,
        adgroup              => $adgroup,
        href                 => undef,
        domain               => undef,
        status_moderate      => $init_status_banner,
        status_moderate     => $init_status_banner,
        status_post_moderate => 'No',
    );
    my $banner = Direct::Model::BannerImageAd->new( %args, old => Direct::Model::BannerImageAd->new( %args ) );
    my $creative = Direct::Model::BannerCreative->new( status_moderate => $init_status_creative, creative_id => 1 );
    $banner->creative( $creative );
    $banner->old->creative( Direct::Model::BannerCreative->new( status_moderate => 'Yes', creative_id => 10 ) );
    $banner->old->status_moderate( 'Yes' );
    return $banner;
}

subtest 'Prepare to update image ad 1' => sub {
        my $banner = mk_banner_with_creative();
        $banner->creative->status_moderate( 'New' );
        Direct::Banners::ImageAd->new( [ $banner ] )->prepare_update();
        is_deeply($banner->get_state_hash, { changes => { }, flags => { }, });
        is_deeply($banner->creative->get_state_hash, { changes => { status_moderate => 1 }, flags => { }, });
        ok($banner->creative->status_moderate eq 'Ready', 'Creative status_moderate is Ready');
    };

subtest 'Prepare to update image ad 2' => sub {
        my $banner = mk_banner_with_creative();
        Direct::Banners::ImageAd->new( [ $banner ] )->prepare_update();
        is_deeply($banner->get_state_hash, { changes => { }, flags => { }, });
        is_deeply($banner->creative->get_state_hash, { changes => { }, flags => { }, });
    };

subtest 'Prepare to update image ad 3' => sub {
        my $banner = mk_banner_with_creative();
        $banner->creative->creative_id( 2 );
        $banner->creative->status_moderate( 'New' );
        Direct::Banners::ImageAd->new( [ $banner ] )->prepare_update();
        is_deeply($banner->get_state_hash, { changes => { status_bs_synced => 1, status_moderate => 1 }, flags => { moderate_adgroup => 1 }, });
        is_deeply($banner->creative->get_state_hash, { changes => { creative_id => 1, status_moderate => 1 }, flags => { }, });
        ok($banner->creative->status_moderate eq 'Ready', 'Creative status_moderate is Ready');
    };

subtest 'Prepare to update image ad 4' => sub {
        my $banner = mk_banner_with_creative('New', 'New');
        $banner->creative->creative_id( 2 );
        Direct::Banners::ImageAd->new( [ $banner ] )->prepare_update();
        is_deeply($banner->creative->get_state_hash, { changes => { creative_id => 1, status_moderate => 1 }, flags => { }, });
        ok($banner->creative->status_moderate eq 'New', 'Creative status_moderate is New');
    };

subtest 'Prepare to update image ad 5' => sub {
        my $banner = mk_banner_with_creative('Yes', 'Yes');
        $banner->creative->creative_id( 2 );
        Direct::Banners::ImageAd->new( [ $banner ] )->prepare_update();
        ok($banner->status_moderate eq 'Ready', 'Banner status_moderate is Yes');
        is_deeply($banner->creative->get_state_hash, { changes => { creative_id => 1, status_moderate => 1 }, flags => { }, });
        ok($banner->creative->status_moderate eq 'Ready', 'Creative status_moderate is New');
    };

subtest 'Prepare to update image ad 6' => sub {
        my $module = Test::MockModule->new( 'RedirectCheckQueue' );
        $module->mock( is_known_redirect => sub { return 0} );
        my $banner = mk_banner_with_creative('Yes');
        $banner->href( 'http://ya.ru/' );
        Direct::Banners::ImageAd->new( [ $banner ] )->prepare_update();
        ok($banner->status_moderate eq 'Ready', 'Banner status_moderate is Ready');
        is_deeply($banner->creative->get_state_hash, { changes => { }, flags => { }, });
        ok($banner->creative->status_moderate eq 'Yes', 'Creative status_moderate is Yes');
    };

done_testing;