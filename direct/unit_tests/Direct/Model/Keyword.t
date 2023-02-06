#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use Yandex::DBShards qw/get_new_id/;
use Yandex::DBTools;

use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::Keyword');
    use_ok('Direct::Model::Keyword::Manager');
}

sub mk_keyword { Direct::Model::Keyword->new(@_) }

subtest 'Keyword Model' => sub {
    lives_ok { mk_keyword() };
    lives_ok { mk_keyword({}) };
    dies_ok { mk_keyword("unknown" => "args") };

    # Just construct
    lives_ok {
        mk_keyword(text => "тестовая фраза", price => 11.0);
    };

    # to_hash
    is_deeply mk_keyword()->to_hash, {};
    is_deeply mk_keyword(text => "фраза 1")->to_hash, {text => "фраза 1"};

    # to_template_hash
    is_deeply mk_keyword(autobudget_priority => 1)->to_template_hash, {autobudgetPriority => 1};

    subtest 'last_change' => sub {
        my $keyword = mk_keyword();
        $keyword->last_change('now');
        ok $keyword->has_last_change;
        ok $keyword->get_db_column_value(bids => 'modtime', extended => 1)->{val__dont_quote} =~ /^now/i;
    };
};

subtest 'Keyword Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_campaign('text')->with_adgroup();

    subtest 'Create keyword' => sub {
        my $keyword = mk_keyword(
            id => get_new_id('phid'),
            adgroup_id => $db_obj->adgroup->id, campaign_id => $db_obj->campaign->id,
            text => "тестовая фраза", normalized_text => "тест фраза",
            price => '0.11', price_context => '1.18',
            href_param1 => 'param1', href_param2 => 'param2', bs_history => 'history1',
        );
        $keyword->last_change('now');
        Direct::Model::Keyword::Manager->new(items => [$keyword])->create();
        cmp_model_with $keyword, $db_obj->get_keyword($keyword->id), exclude => [qw/last_change/];
        is $keyword->is_changed, 0, 'Test resetting model state';
    };

    subtest 'Update keyword' => sub {
        my $keyword = $db_obj->create_keyword({bs_history => 'history2'});

        $keyword->text("другая фраза 2");
        $keyword->words_count(3);
        $keyword->price('1.11');
        $keyword->price_context('2.44');
        $keyword->autobudget_priority(5);
        $keyword->status_moderate('Yes');
        $keyword->is_suspended(1);
        $keyword->href_param1("param1.1");
        $keyword->bs_history(undef);

        Direct::Model::Keyword::Manager->new(items => [$keyword])->update();
        cmp_model_with $keyword, $db_obj->get_keyword($keyword->id), exclude => [qw/last_change _mirror_last_change/];
        ok !get_one_field_sql(PPC(shard => $db_obj->shard), "SELECT 1 FROM bids_phraseid_history WHERE cid = ? AND id = ?", $keyword->campaign_id, $keyword->id);
        is $keyword->is_changed, 0, 'Test resetting model state';
    };

    subtest 'Moderate template banner' => sub {
        my $banner = $db_obj->create_banner('text', { title => '#template#' });

        my $keyword = mk_keyword(
            id => get_new_id('phid'),
            adgroup_id => $db_obj->adgroup->id, campaign_id => $db_obj->campaign->id,
            text => "тестовая фраза", normalized_text => "тест фраза",
            price => '0.11', price_context => '1.18',
            href_param1 => 'param1', href_param2 => 'param2', bs_history => 'history1',
        );
        $keyword->do_moderate_template_banners(1);
        $keyword->last_change('now');
        do_update_table(PPC(bid => $banner->id), 'banners', { statusModerate => 'Yes'}, where => { bid =>, $banner->id });
        my $creative = {
              'ClientID' => $db_obj->get_user($db_obj->campaign->user_id)->client_id,
              'alt_text' => undef,
              'business_type' => 'retail',
              'creative_group_id' => undef,
              'creative_id' => 1,
              'stock_creative_id' => 1,
              'creative_type' => 'video_addition',
              'group_create_time' => undef,
              'group_name' => undef,
              'height' => undef,
              'href' => undef,
              'layout_id' => undef,
              'moderate_info' => undef,
              'moderate_send_time' => '2017-04-04 15:33:21',
              'moderate_try_count' => '0',
              'name' => 'testVideoAdditionName',
              'preview_url' => 'https://cdn-austrian.economicblogs.org/wp-content/uploads/2016/09/AdobeStock_58349892-300x300.jpeg',
              'statusModerate' => 'New',
              'sum_geo' => undef,
              'template_id' => undef,
              'theme_id' => undef,
              'width' => undef
        };
        do_insert_into_table(PPC(bid => $banner->id), perf_creatives => $creative);
        do_insert_into_table(PPC(bid => $banner->id), 'banners_performance', {
            cid => $db_obj->campaign->id,
            pid => $db_obj->adgroup->id,
            bid => $banner->id,
            creative_id => 1,
            statusModerate => 'New',
        });
        Direct::Model::Keyword::Manager->new(items => [$keyword])->create();
        is(get_one_field_sql(PPC(bid => $banner->id), 'select statusModerate from banners_performance where bid = ?', $banner->id), 'Ready',
            'video addition sent');
    };

    # subtest 'Flag: bs_sync_banners' => sub {
    #     my $banner = $db_obj->create_banner('performance', {status_bs_synced => 'Yes', last_change => '_in_past'});

    #     my $perf_filter = $db_obj->create_perf_filter();
    #     $perf_filter->do_bs_sync_banners(1);
    #     Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->update();

    #     my $banner2 = $db_obj->get_banner($banner->id);
    #     is $banner2->status_bs_synced, 'No';
    #     is $banner2->last_change, $banner->last_change;
    # };

    # subtest 'Flag: bs_sync_adgroup' => sub {
    #     my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, {status_bs_synced => 'Yes', last_change => '_in_past'});

    #     my $perf_filter = $db_obj->create_perf_filter();
    #     $perf_filter->do_bs_sync_adgroup(1);
    #     Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->update();

    #     my $adgroup2 = $db_obj->get_adgroup($adgroup->id);
    #     is $adgroup2->status_bs_synced, 'No';
    #     is $adgroup2->last_change, $adgroup->last_change;
    # };

    # subtest 'Flag: update_adgroup_last_change' => sub {
    #     my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, {last_change => '_in_past'});

    #     my $perf_filter = $db_obj->get_perf_filters(adgroup_id => $adgroup->id, with_additional => 1)->[0];
    #     $perf_filter->do_update_adgroup_last_change(1);
    #     Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->update();

    #     ok $db_obj->get_adgroup($adgroup->id)->last_change gt $adgroup->last_change;
    # };
};

done_testing;
