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
}

sub mk_sitelinks_set { Direct::Model::SitelinksSet->new(@_) }
sub mk_sitelink { Direct::Model::Sitelink->new(@_) }

subtest 'Sitelink Model' => sub {
    lives_ok { mk_sitelink() };
    dies_ok { mk_sitelink("unknown" => "args") };

    is mk_sitelink(title => "title1", href => "ya.ru/1", hash => 1)->hash, 1, 'New sitelink with defined hash';
    dies_ok { mk_sitelink(title => "title1")->hash } 'New sitelink without href and hash';
    ok mk_sitelink(title => "title1", description => undef, href => "ya.ru/1")->hash > 0, 'New sitelink with auto hash';

    subtest 'Auto recalc hash' => sub {
        my $sitelink = mk_sitelink(title => "title1", description => undef, href => "ya.ru/1");
        isnt $sitelink->clone(title => "title2")->hash, $sitelink->hash;
        isnt $sitelink->clone(description => "desc1")->hash, $sitelink->hash;
        isnt $sitelink->clone(href => "ya.ru/2")->hash, $sitelink->hash;
    };

    lives_ok { mk_sitelink(title => "title1", description => "desc2", href => "ya.ru/3")->get_uhash };
};

subtest 'SitelinksSet Model' => sub {
    lives_ok { mk_sitelinks_set() };
    dies_ok { mk_sitelinks_set("unknown" => "args") };

    lives_ok {
        mk_sitelinks_set(links => [mk_sitelink(), mk_sitelink(), mk_sitelink()])
    } 'New SitelinksSet with links';

    dies_ok {
        mk_sitelinks_set(links => [mk_sitelink(), mk_sitelink(), mk_sitelink()])->calculate_hash
    } 'Cannot calculate hash without links ids';

    lives_ok {
        mk_sitelinks_set(links => [mk_sitelink(id => 1), mk_sitelink(id => 2), mk_sitelink(id => 3)])->calculate_hash
    } 'Can calculate hash with links ids';

    dies_ok {
        my $sitelinks_set = mk_sitelinks_set(links => [mk_sitelink(id => 1), mk_sitelink(id => 2), mk_sitelink(id => 3)]);
        $sitelinks_set->links->[0]->title("title12");
        $sitelinks_set->calculate_hash;
    } 'Cannot calculate hash for unsaved links';
};

subtest 'SitelinksSet Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_user();

    subtest 'Create sitelinks set' => sub {
        my $sitelinks_set1 = mk_sitelinks_set(
            client_id => $db_obj->user->client_id,
            links => [
                mk_sitelink(title => "title1", description => undef, href => "ya.ru/1"),
                mk_sitelink(title => "title2", description => undef, href => "ya.ru/2"),
                mk_sitelink(title => "title3", description => undef, href => "ya.ru/3"),
            ],
        );
        my $sitelinks_set2 = $sitelinks_set1->clone;
        $sitelinks_set2->links->[0]->title("title1.2");

        Direct::Model::SitelinksSet::Manager->new(items => [$sitelinks_set1, $sitelinks_set2])->save();

        for my $sitelinks_set ($sitelinks_set1, $sitelinks_set2) {
            cmp_model_with $sitelinks_set, $db_obj->get_sitelinks_set($sitelinks_set->id);
            is $sitelinks_set->is_changed, 0, 'Test resetting model state';
        }

        isnt $sitelinks_set1->links->[0]->id, $sitelinks_set2->links->[0]->id;
        is $sitelinks_set1->links->[$_]->id, $sitelinks_set2->links->[$_]->id for 1..2;
    };

    subtest 'Create duplicate sitelinks sets' => sub {
        my $sitelinks_set1 = mk_sitelinks_set(
            client_id => $db_obj->user->client_id,
            links => [
                mk_sitelink(title => "title1", description => "desc1", href => "ya.ru/1"),
                mk_sitelink(title => "title2", description => "desc2", href => "ya.ru/2"),
                mk_sitelink(title => "title3", description => "desc3", href => "ya.ru/3"),
            ],
        );
        my $sitelinks_set2 = $sitelinks_set1->clone;

        Direct::Model::SitelinksSet::Manager->new(items => [$sitelinks_set1, $sitelinks_set2])->save();

        cmp_model_with $sitelinks_set1, $sitelinks_set2;
    };

    subtest 'Change sitelinks set (test immutability)' => sub {
        my $orig_sitelinks_set = $db_obj->create_sitelinks_set();

        my $new_sitelinks_set = $orig_sitelinks_set->clone;
        $new_sitelinks_set->links->[0]->title("Something new");

        Direct::Model::SitelinksSet::Manager->new(items => [$new_sitelinks_set])->save();

        cmp_model_with $orig_sitelinks_set, $db_obj->get_sitelinks_set($orig_sitelinks_set->id);
        isnt $orig_sitelinks_set->id, $new_sitelinks_set->id;
    };
};

done_testing;
