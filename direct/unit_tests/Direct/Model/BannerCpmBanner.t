#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use List::MoreUtils qw/each_array/;

use Test::More tests => 6;
use Test::Exception;

use Settings;

use Yandex::DBShards;

use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

use constant AUDIENCE_URL => 'https://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%';
use constant AUDIT_ADFOX_URL => 'https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=b';
use constant AUDIT_TNS_URL => 'https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%25aw_random%25';


BEGIN {
    use_ok('Direct::Model::BannerCpmBanner');
    use_ok('Direct::Model::BannerCpmBanner::Manager');
    use_ok('Direct::Model::Pixel');
    use_ok('Direct::Model::BannerCreative');
}

sub mk_banner { Direct::Model::BannerCpmBanner->new(@_) }
sub mk_cpm_banner {
    my %params = @_;

    return Direct::Model::BannerCpmBanner->new(
        href => 'http://ya.ru',
        creative => Direct::Model::BannerCreative->new(
            creative_id => $params{creative}->id,
            campaign_id => $params{campaign_id},
            adgroup_id => $params{adgroup_id},
            banner_id => $params{id},
            creative => delete $params{creative},
            status_moderate => 'New',
            extracted_text => undef,
        ),
        status_moderate => 'New',
        %params,
    );
}

sub mk_pixel {
    my ($kind, $url, %params) = @_;
    my $pixel = Direct::Model::Pixel->new(
        url => $url,
        kind => $kind,
	%params
    );
}

subtest 'BannerCpmBanner Model' => sub {
    lives_ok { mk_banner() };
    lives_ok { mk_banner({}) };
    dies_ok { mk_banner(unknown => "args") };

    # Base properties
    is mk_banner()->banner_type, 'cpm_banner';

    # manager_class
    is(Direct::Model::BannerCpmBanner->manager_class, 'Direct::Model::BannerCpmBanner::Manager');
    lives_ok { mk_banner()->manager_class->new(items => []) };

    ok !mk_banner({status_moderate => 'No'})->is_changed, 'state does not change if volatile field set in constructor but not set by accessor';
    my $banner = mk_banner({status_moderate => 'Ready'});
    $banner->status_moderate('Ready');
    ok $banner->is_status_moderate_changed(), 'volatile field state changes if set to same value as initial';
};


subtest 'BannerCpmBanner Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_adgroup('cpm_banner');
    my $client_id = $db_obj->user->client_id;

    my $creative = $db_obj->create_canvas_creative();
    subtest 'Create cpm_banner' => sub {
        my $banner = mk_cpm_banner(
            id => get_new_id('bid', cid => $db_obj->campaign->id),
            adgroup_id => $db_obj->adgroup->id,
            campaign_id => $db_obj->campaign->id,
            creative => $creative,
        );
        Direct::Model::BannerCpmBanner::Manager->new(items => [$banner])->create();
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [qw/last_change/];
        ok !$banner->is_changed, 'Test resetting model state';
    };

    subtest 'Update cpm_banner' => sub {
        my $banner = $db_obj->create_banner('cpm_banner');

        $banner->status_moderate('Yes');
        $banner->status_bs_synced('Yes');

        Direct::Model::BannerCpmBanner::Manager->new(items => [$banner])->update();
        cmp_model_with $banner, $db_obj->get_banner($banner->id, with_pixels => 1), exclude => [qw/last_change/];
        ok !$banner->is_changed, 'Test resetting model state';
    };


    subtest 'Pixels' => sub {
        my @pixels = map { mk_pixel(@$_) } ([ audience => AUDIENCE_URL, adgroup_type => 'cpm_banner' ], [ audit => AUDIT_ADFOX_URL, adgroup_type => 'cpm_banner' ]);
        my $banner = $db_obj->create_banner('cpm_banner', { pixels => \@pixels });
        my @banner_pixels = sort { $a->kind cmp $b->kind } @{$db_obj->get_banner($banner->id, with_pixels => 1)->pixels};
        my $arr_iterator = each_array(@pixels, @banner_pixels);
        while (my ($source_pixel, $banner_pixel) = $arr_iterator->()) {
            $source_pixel->banner_id($banner->id);
            $source_pixel->campaign_id($banner->campaign_id);
            $source_pixel->provider; # ленивое определение провайдера
            cmp_model_with $banner_pixel, $source_pixel;
        }


        @pixels = mk_pixel(audit => AUDIT_TNS_URL, adgroup_type => 'cpm_banner');
        $banner->pixels(\@pixels);
        $banner->do_pixels_change(1);
        Direct::Model::BannerCpmBanner::Manager->new(items => [$banner])->update();
        @banner_pixels = sort { $a->kind cmp $b->kind } @{$db_obj->get_banner($banner->id, with_pixels => 1)->pixels};
        $arr_iterator = each_array(@pixels, @banner_pixels);
        while (my ($source_pixel, $banner_pixel) = $arr_iterator->()) {
            $source_pixel->banner_id($banner->id);
            $source_pixel->campaign_id($banner->campaign_id);
            $source_pixel->provider; # ленивое определение провайдера
            cmp_model_with $banner_pixel, $source_pixel;
        }

        $banner->pixels([]);
        $banner->do_pixels_change(1);
        Direct::Model::BannerCpmBanner::Manager->new(items => [$banner])->update();
        ok !@{$db_obj->get_banner($banner->id, with_pixels => 1)->pixels};
    };
};


