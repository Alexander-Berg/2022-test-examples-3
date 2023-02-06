#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use Yandex::DBShards;

use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::BannerText');
    use_ok('Direct::Model::BannerText::Manager');
    use_ok('Direct::Model::TurboLanding::Banner');
}

sub mk_banner { Direct::Model::BannerText->new(@_) }
sub mk_txt_banner {
    return Direct::Model::BannerText->new(
        title => 'banner title',
        body => 'banner_body',
        href => 'http://ya.ru',
        domain => 'ya.ru',
        is_mobile => 0,
        @_,
    );
}

subtest 'BannerText Model' => sub {
    lives_ok { mk_banner() };
    lives_ok { mk_banner({}) };
    dies_ok { mk_banner("unknown" => "args") };

    # Base properties
    is mk_banner()->banner_type, 'text';


    ok mk_banner()->is_display_href_supported;

    is mk_banner({href => 'https://ya.ru/test={coef_goal_context_id}'})->has_coef_goal_context_id_param, 1;
    is mk_banner({href => 'https://ya.ru/test={not-coef_goal_context_id}'})->has_coef_goal_context_id_param, '';

    # manager_class
    is(Direct::Model::BannerText->manager_class, 'Direct::Model::BannerText::Manager');
    lives_ok { mk_banner()->manager_class->new(items => []) };

    ok !mk_banner({status_moderate => 'No'})->is_changed, 'state does not change if volatile field set in constructor but not set by accessor';
    my $banner = mk_banner({status_moderate => 'Ready'});
    $banner->status_moderate('Ready');
    ok $banner->is_status_moderate_changed(), 'volatile field state changes if set to same value as initial';

    # TODO: to_hash
    # TODO: to_template_hash
};


subtest 'BannerText Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_adgroup('text');

    subtest 'Create text banner' => sub {
        my $banner = mk_banner(
            id => get_new_id('bid', cid => $db_obj->campaign->id),
            adgroup_id => $db_obj->adgroup->id,
            campaign_id => $db_obj->campaign->id,
            title => 'Title',
            body => 'Body',
        );
        Direct::Model::BannerText::Manager->new(items => [$banner])->create();
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [qw/last_change/];
        ok !$banner->is_changed, 'Test resetting model state';
    };

    subtest 'Update text banner' => sub {
        my $banner = $db_obj->create_banner();

        $banner->status_moderate('Yes');
        $banner->status_bs_synced('Yes');

        Direct::Model::BannerText::Manager->new(items => [$banner])->update();
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [qw/last_change/];
        ok !$banner->is_changed, 'Test resetting model state';
    };


    subtest 'Display href' => sub {
        my $banner = $db_obj->create_banner();

        $banner->display_href('display/href');
        Direct::Model::BannerText::Manager->new(items => [$banner])->update();
        $banner->display_href_status_moderate('Ready');
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [qw/last_change/];

        $banner->display_href(undef);
        Direct::Model::BannerText::Manager->new(items => [$banner])->update();
        $banner->display_href_status_moderate(undef);
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [qw/last_change/];

    };

    subtest 'coef_goal_context_id in href' => sub {
        my $banner = $db_obj->create_banner();

        $banner->href("https://ya.ru/test={coef_goal_context_id}");
        Direct::Model::BannerText::Manager->new( items => [ $banner ] )->update();
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [ qw/last_change/ ];
    };

    subtest 'statuModerate force updated' => sub {
        my $banner = $db_obj->create_banner('text', {status_moderate => 'Ready'});

        # racing here
        my $banner_clone = $banner->clone();
        $banner_clone->status_moderate('Yes');
        Direct::Model::BannerText::Manager->new( items => [ $banner_clone ] )->update();

        $banner->status_moderate('Ready');
        Direct::Model::BannerText::Manager->new( items => [ $banner ] )->update();
        cmp_model_with $banner, $db_obj->get_banner($banner->id), exclude => [ qw/last_change/ ];
    };

    subtest 'display href multiupdate', sub {
        my $adgroup = $db_obj->adgroup;
        my $uid = $db_obj->campaign->user_id;
        my $cid = $adgroup->campaign_id;
        my $pid = $adgroup->id;

        my $tests = [
            ['hello',  undef,   undef,  'hello'], # создаем 4 баннера с указанными display_href
            ['world', 'hello',  undef,  'hello'], # меняем display_href у 4 баннеров
            [ undef,  'hello', 'hello', 'world'], # --//--
        ];
        my $created = 0;
        my $ids = [];

        for my $row (keys @$tests) {
            my $t = $tests->[$row];
            if ($created == 0) {
                $ids = get_new_id_multi(bid => scalar(@$t), uid => $uid);
                my @banners;
                for my $i (keys @$t) {
                    my $banner = mk_txt_banner(id => $ids->[$i], campaign_id => $cid, adgroup_id => $pid, display_href => $t->[$i]);
                    push @banners, $banner;
                }
                Direct::Model::BannerText::Manager->new(items => \@banners)->create();
                $created = 1;
            } else {
                my $banners = Direct::Banners->get_by(banner_id => $ids)->items;
                for my $i (keys @$t) {
                    my $banner = $banners->[$i];
                    $banner->old($banner->clone);
                    $banner->display_href($t->[$i]);
                }
                Direct::Model::BannerText::Manager->new(items => $banners)->update();
            }

            my $banners = Direct::Banners->get_by(banner_id => $ids)->items;
            for my $i (keys @$t) {
                my $expected_display_href = $t->[$i];
                my $banner = Direct::Banners->get_by(banner_id => $ids->[$i])->items->[0];
                my $db_display_href = $banners->[$i]->has_display_href ? $banners->[$i]->display_href : undef;
                is($db_display_href, $expected_display_href, "row $row, column $i");
            }
        }
    };

    subtest 'BannerTurboLanding' => sub {
        my $banner = $db_obj->create_banner();

        my $turbolanding = $db_obj->create_turbolanding();
        $banner->turbolanding(Direct::Model::TurboLanding::Banner->from_db_hash({
                    tl_id => $turbolanding->id,
                    bid   => $banner->id,
                    href  => $turbolanding->href,
                    name  => $turbolanding->name,
                    cid   =>  $db_obj->campaign->id,
                    ClientID => $turbolanding->client_id,
                    is_disabled => 0,
                    metrika_counters_json => $turbolanding->metrika_counters_json,
        }, \my $cache));
        Direct::Model::BannerText::Manager->new(items => [$banner])->update();
        cmp_model_with $banner, $db_obj->get_banner($banner->id, with_turbolanding => 1), exclude => [qw/last_change/];

        $banner->do_delete_turbolanding(1);
        Direct::Model::BannerText::Manager->new(items => [$banner])->update();
        ok !$db_obj->get_banner($banner->id)->has_turbolanding, 'turbolanding deleted' ;

    };
};

done_testing;
