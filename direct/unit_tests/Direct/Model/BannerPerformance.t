#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;
use Test::CreateDBObjects;

use Settings;

use Yandex::DBTools;
use Yandex::DBShards;

use POSIX qw/strftime/;

use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::BannerPerformance');
    use_ok('Direct::Model::BannerPerformance::Manager');
}

sub mk_banner { Direct::Model::BannerPerformance->new(@_) }

subtest 'BannerPerformance Model' => sub {
    lives_ok { mk_banner() };
    lives_ok { mk_banner({}) };
    dies_ok { mk_banner("unknown" => "args") };

    # AdGroup constraint
    lives_ok { mk_banner(adgroup => bless({}, 'Direct::Model::AdGroupPerformance')) };
    dies_ok { mk_banner(adgroup => bless({}, 'Direct::Model::AdGroup')) };

    # Base properties
    is mk_banner()->banner_type, 'performance';

    ok mk_banner()->has_title, 'has default title';
    dies_ok { mk_banner()->title("title") } 'title is ro';
    ok !mk_banner()->is_title_supported;

    ok mk_banner()->has_body, 'has default body';
    dies_ok { mk_banner()->body("body") } 'body is ro';
    ok !mk_banner()->is_body_supported;

    dies_ok { mk_banner(is_mobile => 1) } 'cannot be mobile';

    dies_ok { mk_banner(href => "http://ya.ru") } 'cannot have href';
    dies_ok { mk_banner(domain => "ya.ru") } 'cannot have domain';
    ok !mk_banner()->is_href_supported;

    dies_ok { mk_banner(image_hash => "123") } 'cannot have image (1)';
    dies_ok { mk_banner(image => bless({}, 'Direct::Model::BannerImage')) } 'cannot have image (2)';
    ok !mk_banner()->is_image_supported;

    dies_ok { mk_banner(vcard_id => 1) } 'cannot have vcard (1)';
    dies_ok { mk_banner(vcard => bless({}, 'Direct::Model::VCard')) } 'cannot have vcard (2)';
    ok !mk_banner()->is_vcard_supported;

    dies_ok { mk_banner(sitelinks_set_id => 1) } 'cannot have sitelinks (1)';
    dies_ok { mk_banner(sitelinks_set => bless({}, 'Direct::Model::SitelinksSet')) } 'cannot have sitelinks (2)';
    ok !mk_banner()->is_sitelinks_set_supported;

    lives_ok { mk_banner(creative_id => 1) };
    lives_ok { mk_banner(creative => bless({}, 'Direct::Model::Creative')) };
    dies_ok { mk_banner(creative => bless({}, 'Direct::Model::Unknown')) };

    # manager_class
    is(Direct::Model::BannerPerformance->manager_class, 'Direct::Model::BannerPerformance::Manager');
    lives_ok { mk_banner()->manager_class->new(items => []) };

    # TODO: to_hash
    # TODO: to_template_hash
};

subtest 'BannerPerformance Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_adgroup('performance');

    subtest 'Create performance banner' => sub {
        my $banner = mk_banner(
            id => get_new_id('bid', cid => $db_obj->campaign->id),
            adgroup_id => $db_obj->adgroup->id,
            campaign_id => $db_obj->campaign->id,
            creative_id => $db_obj->create_perf_creative()->id,
        );
        Direct::Model::BannerPerformance::Manager->new(items => [$banner])->create();
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [qw/last_change/];
        ok !$banner->is_changed, 'Test resetting model state';
    };

    subtest 'Update performance banner' => sub {
        my $banner = $db_obj->create_banner();

        $banner->creative_id($db_obj->create_perf_creative()->id);
        $banner->status_moderate('Yes');
        $banner->status_bs_synced('Yes');

        Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();

        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [qw/creative last_change/];
        ok !$banner->is_changed, 'Test resetting model state';
    };

    subtest 'Delete performance banner' => sub {
        my $banner = $db_obj->create_banner();

        Direct::Model::BannerPerformance::Manager->new(items => [$banner])->delete();

        my $db = PPC(cid => $db_obj->campaign->id);
        ok !get_one_field_sql($db, "SELECT 1 FROM banners WHERE bid = ?", $banner->id);
        ok !get_one_field_sql($db, "SELECT 1 FROM banners_performance WHERE bid = ?", $banner->id);
        ok get_one_field_sql($db, "SELECT bid FROM deleted_banners where bid = ?", $banner->id);
        ok !get_shard(bid => $banner->id);
    };

    subtest 'Flag: set_geo_for_new_creative' => sub {
        my $banner = $db_obj->create_banner();
        $banner->creative->rejection_reason_ids([]);
        ok !defined $db_obj->get_perf_creative($banner->creative_id)->sum_geo;

        $db_obj->adgroup->geo(225);
        Direct::Model::AdGroupPerformance::Manager->new(items => [$db_obj->adgroup])->update();
        $banner->do_set_geo_for_new_creative({translocal_opt => {tree => 'api'}});
        Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();
        is $db_obj->get_perf_creative($banner->creative_id)->sum_geo, "225";

        $db_obj->adgroup->geo(120);
        Direct::Model::AdGroupPerformance::Manager->new(items => [$db_obj->adgroup])->update();
        $banner->do_set_geo_for_new_creative({translocal_opt => {tree => 'api'}});
        Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();
        is $db_obj->get_perf_creative($banner->creative_id)->sum_geo, "120", 'sum_geo is mutable for new creatives';
    
        my $another_banner = $db_obj->create_banner('performance', {creative_id => $banner->creative->id}, with_new_adgroup => 1);
        my $another_adgroup = $db_obj->get_adgroup($another_banner->adgroup_id);
        $db_obj->adgroup->geo(225);
        $another_adgroup->geo(120);
        Direct::Model::AdGroupPerformance::Manager->new(items => [$db_obj->adgroup, $another_adgroup])->update();
        $_->do_set_geo_for_new_creative({translocal_opt => {tree => 'api'}}) foreach ($banner, $another_banner);
        Direct::Model::BannerPerformance::Manager->new(items => [$banner, $another_banner])->update();
        is $db_obj->get_perf_creative($banner->creative_id)->sum_geo, "120,225", 'joined sum_geo';

        do_sql(PPC(creative_id => [$banner->creative_id]), [q/UPDATE perf_creatives SET statusModerate='Yes', sum_geo=120/, WHERE => {creative_id => SHARD_IDS}]);
        $db_obj->adgroup->geo(225);
        Direct::Model::AdGroupPerformance::Manager->new(items => [$db_obj->adgroup])->update();
        $banner->do_set_geo_for_new_creative({translocal_opt => {tree => 'api'}});
        Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();
        is $db_obj->get_perf_creative($banner->creative_id)->sum_geo, 120, 'sum_geo is immutable for moderated creatives';

        subtest 'The world' => sub {
            my $banner = $db_obj->create_banner();
            $db_obj->adgroup->geo("225,977");
            Direct::Model::AdGroupPerformance::Manager->new(items => [$db_obj->adgroup])->update();
            $banner->do_set_geo_for_new_creative({translocal_opt => {tree => 'api'}});
            Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();
            ok @{[split /,/, $db_obj->get_perf_creative($banner->creative_id)->sum_geo]} > 100;
        };
    };

    subtest 'Flag: moderate_creative' => sub {
        my $banner = $db_obj->create_banner();
        $banner->creative->rejection_reason_ids([]);

        # With `New`/`Error` statuses
        for my $status_moderate (qw/New Error/) {
            $db_obj->update_perf_creative($banner->creative, {
                status_moderate => $status_moderate,
                moderate_send_time => '_in_past',
                moderate_try_count => 3,
            });

            $banner->do_moderate_creative(1);
            Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();
            is $db_obj->get_perf_creative($banner->creative_id)->status_moderate, 'Ready';
            is $db_obj->get_perf_creative($banner->creative_id)->moderate_try_count, 0, 'Reset moderate_try_count';
            ok $db_obj->get_perf_creative($banner->creative_id)->moderate_send_time gt strftime('%Y-%m-%d %H:%M:%S', localtime);
        }

        # With other statuses
        for my $status_moderate (qw/Sending Sent Yes No/) {
            $db_obj->update_perf_creative($banner->creative, {status_moderate => $status_moderate});

            $banner->do_moderate_creative(1);
            Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();
            is $db_obj->get_perf_creative($banner->creative_id)->status_moderate, $status_moderate;
        }
    };
    
    subtest 'Flag: set_adgroup_bl_status' => sub {
        my $banner = $db_obj->create_banner();
        $banner->creative->rejection_reason_ids([]);
        $banner->do_set_adgroup_bl_status('Processing');
        Direct::Model::BannerPerformance::Manager->new(items => [$banner])->update();
        my $group = $db_obj->with_adgroup('performance', { id => $banner->adgroup_id })->adgroup;
        is $group->status_bl_generated, 'Processing';
    };
};

done_testing;
