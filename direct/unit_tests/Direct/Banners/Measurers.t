use Direct::Modern;
use open ':std' => 'utf8';

use List::MoreUtils qw/each_array/;
use Test::More;
use Settings;
use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::Banner::Measurer');
    use_ok('Direct::Banners::Measurers');
}

sub mk_measurer { Direct::Model::Banner::Measurer->new(@_, has_integration => 0) }

subtest get_measurers => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_adgroup('cpm_banner');
    my $text_db_obj = Direct::Test::DBObjects->new()->with_adgroup('text');

    subtest 'measurers' => sub {
       my $measurer = mk_measurer(
           measurer_system => 'moat',
           params => '{"level3": "6612"}',
           adgroup_type => 'cpm_banner',
       );
       my $banner = $db_obj->create_banner('cpm_banner', {measurers => [$measurer]});
       my $got_measurer = @{Direct::Banners::Measurers->get_by(banner_id => [$banner->id])->items}[0];
       $measurer->banner_id($banner->id);
       $measurer->creative_id($banner->creative->creative_id);
       $measurer->adgroup_id($banner->adgroup_id);
       $measurer->campaign_id($banner->campaign_id);
       $measurer->client_id($banner->client_id);
       cmp_model_with $got_measurer, $measurer;
    };

    subtest get_several_measurers => sub {

       my $measurer1 = mk_measurer(
           measurer_system => 'moat',
           params => '{}',
           adgroup_type => 'cpm_banner',
       );
       my $measurer2 = mk_measurer(
           measurer_system => 'adloox',
           params => '{"ssid": "sds7HEYE51gfeA", "base_url": "p.adloox.com"}',
           adgroup_type => 'cpm_banner',
       );
       my $banner1 = $db_obj->create_banner('cpm_banner', {measurers => [$measurer1, $measurer2]});

       $measurer1->banner_id($banner1->id);
       $measurer1->creative_id($banner1->creative->creative_id);
       $measurer1->adgroup_id($banner1->adgroup_id);
       $measurer1->campaign_id($banner1->campaign_id);
       $measurer1->client_id($banner1->client_id);

       $measurer2->banner_id($banner1->id);
       $measurer2->creative_id($banner1->creative->creative_id);
       $measurer2->adgroup_id($banner1->adgroup_id);
       $measurer2->campaign_id($banner1->campaign_id);
       $measurer2->client_id($banner1->client_id);

       my $measurer3 = mk_measurer(
           measurer_system => 'admetrica',
           params => '{"type": "ya", "creative-id": 123}',
           adgroup_type => 'base',
       );
       my $banner2 = $text_db_obj->create_banner('text', {measurers => [$measurer3]});
       $measurer3->banner_id($banner2->id);
       $measurer3->creative_id(undef);
       $measurer3->adgroup_id($banner2->adgroup_id);
       $measurer3->campaign_id($banner2->campaign_id);
       $measurer3->client_id($banner2->client_id);

       my $measurers = Direct::Banners::Measurers->get_by(banner_id => [$banner1->id, $banner2->id])->items;
       my @got_measurers = sort { $a->banner_id . $a->measurer_system cmp $b->banner_id . $b->measurer_system } @$measurers;
       my @exp_measurers = sort { $a->banner_id . $a->measurer_system cmp $b->banner_id . $b->measurer_system } $measurer1, $measurer2, $measurer3;
       my $iterator = each_array(@got_measurers, @exp_measurers);
       while (my ($got, $exp) = $iterator->()) {
           cmp_model_with $got, $exp;
       }
    };
};

done_testing;
