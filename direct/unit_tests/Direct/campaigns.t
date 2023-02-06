use my_inc "../..";
use Direct::Modern;

use Test::More;

use Test::Subtest;
use Test::CreateDBObjects;


BEGIN {
    use_ok 'Campaign';
    use_ok 'Direct::Campaigns';
}


{
    no warnings 'redefine';
    my $original_new = \&Yandex::Log::new;
    *Yandex::Log::new = sub { my $self = shift; my %O = @_; $O{use_syslog} = 0; return $original_new->($self, %O) };
}


subtest_ "get_by()" => sub {
    subtest_ "return Campaign subclasses" => sub {
        my $cid = create('campaign');
        my $camp = Direct::Campaigns->get_by(campaign_id => $cid)->items->[0];
        isa_ok $camp, 'Direct::Model::Campaign';
    };
    subtest_ "loads performance campaigns" => sub {
        my $cid = create('campaign', type => 'performance');
        my $camp = Direct::Campaigns->get_by(campaign_id => $cid)->items->[0];
        isa_ok $camp, 'Direct::Model::CampaignPerformance';
    };
    subtest_ "loads internal_distrib campaigns" => sub {
        my $cid = create('campaign', type => 'internal_distrib', currency => 'RUB');
        my $camp = Direct::Campaigns->get_by(campaign_id => $cid)->items->[0];
        isa_ok $camp, 'Direct::Model::CampaignInternalDistrib';
        # TODO: проверить сосотояния остальных полей
    };
};

create_tables;
run_subtests;
