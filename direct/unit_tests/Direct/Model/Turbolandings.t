#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use Direct::Test::DBObjects;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::Sitelink');
    use_ok('Direct::Model::SitelinksSet');
    use_ok('Direct::Model::SitelinksSet::Manager');
    use_ok('Direct::Model::TurboLanding');
    use_ok('Direct::Model::TurboLanding::Banner');
    use_ok('Direct::Model::BannerText::Manager');
    use_ok('Direct::Model::TurboLanding::Sitelink');
}

sub mk_sitelinks_set { Direct::Model::SitelinksSet->new(@_) }
sub mk_sitelink { Direct::Model::Sitelink->new(@_) }
sub mk_turbolanding {Direct::Model::TurboLanding->new(@_)}
sub mk_banner_turbolanding {Direct::Model::TurboLanding::Banner->new(@_)}
sub mk_sitelink_turbolanding {Direct::Model::TurboLanding::Sitelink->new(@_)}
sub _serialize_array{ join ', ', sort @_ }

my %landing = (
    id => 12345,
    href => 'https://yandex.ru/turbo?jkghkjg',
    name => 'testTL',
    metrika_counters_json => '[{"id": 333, "goals": [12345]}, {"id": 12345, "goals":[45678]}]',
    client_id => 777,
);


subtest 'Turbolanding Model' => sub {
    lives_ok { mk_banner_turbolanding() };
    dies_ok { mk_banner_turbolanding(unknown => 'args') };
    
    lives_ok { mk_sitelink_turbolanding() };
    dies_ok { mk_sitelink_turbolanding(unknown => 'args') };

    is mk_banner_turbolanding(
        %landing,    
        bid  => 111,
        cid  => 222,
        status_moderate => 'No',
    )->metrica_counters()->[0]->{id}, 333, 'New banner turbolanding with metrica counters';
    dies_ok { mk_banner_turbolanding(id => 1)->href } 'New banner turbolanding without href';

    is mk_sitelink_turbolanding(
        %landing,    
        sl_id  => 444,
    )->sl_id, 444, 'New sitelink turbolanding';
    dies_ok { mk_sitelink_turbolanding(id => 1)->href } 'New sitelink turbolanding without href';


    subtest 'Recalc metrica_counters_json' => sub {
        my $turbolanding = mk_turbolanding(%landing);
        isnt $turbolanding->clone(metrica_counters => [{id => 7777, goals => [222]}])->metrika_counters_json, $turbolanding->metrika_counters_json;
    };

    subtest 'To template hash' => sub {
        my $turbolanding = mk_banner_turbolanding(
            %landing,
            bid  => 111,
            cid  => 222,
            status_moderate => 'Ready',
        );
        is _serialize_array(keys %{$turbolanding->to_template_hash}), 'bid, href, id, name, status_moderate';
        
        $turbolanding = mk_sitelink_turbolanding(
        %landing,    
            sl_id  => 444,
        );
        is _serialize_array(keys %{$turbolanding->to_template_hash}), 'href, id, name';
    };
    
    subtest 'Id at from_db_hash' => sub {
        my $cache;
        my $turbolanding = Direct::Model::TurboLanding::Banner->from_db_hash({id => 42}, \$cache);
        is $turbolanding->id, 42;
        $turbolanding = Direct::Model::TurboLanding::Banner->from_db_hash({tl_id => 24}, \$cache);
        is $turbolanding->id, 24;
        dies_ok{$turbolanding = Direct::Model::TurboLanding::Banner->from_db_hash({xx_id => 88}, \$cache)};
    };
};

subtest 'Sitelink Model' => sub {

    my $sitelink;
    lives_ok {
        $sitelink = mk_sitelink(turbolanding => {id => 22});
    } 'New sitelink with turbolanding';
    
    is $sitelink->tl_id, 22;
    
    my $cache;
    $sitelink = Direct::Model::Sitelink->from_db_hash({tl_id => 24}, \$cache);
    is $sitelink->tl_id, 24;
    
};


subtest 'SitelinksSet Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_user();

    subtest 'Create sitelinks set' => sub {
        my $sitelinks_set1 = mk_sitelinks_set(
            client_id => $db_obj->user->client_id,
            links => [
                mk_sitelink(title => "title1", description => undef, href => "ya.ru/1", turbolanding => {id => 333}),
                mk_sitelink(title => "title2", description => undef, href => "ya.ru/2"),
                mk_sitelink(title => "title3", description => undef, href => "ya.ru/3"),
            ],
        );
        my $sitelinks_set2 = $sitelinks_set1->clone;

        $sitelinks_set2->links->[0]->tl_id(444);
        Direct::Model::SitelinksSet::Manager->new(items => [$sitelinks_set1, $sitelinks_set2])->save();

        for my $sitelinks_set ($sitelinks_set1, $sitelinks_set2) {
            cmp_model_with $sitelinks_set, $db_obj->get_sitelinks_set($sitelinks_set->id);
            is $sitelinks_set->is_changed, 0, 'Test resetting model state';
        }

        isnt $sitelinks_set1->links->[0]->id, $sitelinks_set2->links->[0]->id;
        is $sitelinks_set1->links->[$_]->id, $sitelinks_set2->links->[$_]->id for 1..2;
    };

};


done_testing;
