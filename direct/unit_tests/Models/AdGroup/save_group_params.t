#!/usr/bin/perl
use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;
use Test::Deep;

use Settings;

use utf8;

use Yandex::DBUnitTest qw/:all/;
use Yandex::HashUtils;

sub load_modules: Tests(startup => 1) {
    use_ok 'Models::AdGroup';
}

sub init_database: Tests(startup) {
    my %db = (
        phrases => {
            original_db => PPC(shard => 'all'),
            like => 'phrases',
        },
        group_params => {
            original_db => PPC(shard => 'all'),
            like => 'group_params',
        },
        shard_client_id => {
            original_db => PPCDICT,
            rows => [
                {
                    ClientID => 1, shard => 1 },
                {
                    ClientID => 2, shard => 2 },
            ],
        },
        shard_inc_pid => {
            original_db => PPCDICT,
            rows => [
                (map { {pid => $_, ClientID => 1} } 1..5),
                (map { {pid => 100 + $_, ClientID => 1} } 1..5),
                (map { {pid => 200 + $_, ClientID => 2} } 1..5),
            ],
        },
    );

    init_test_dataset(\%db);
}

sub save {
    shift;
    &Models::AdGroup::save_group_params;
}

sub get {
    shift;
    my $pid2params = &Models::AdGroup::get_groups_params;
    $pid2params->{$_} = hash_cut $pid2params->{$_}, qw/pid has_phraseid_href/ for keys %$pid2params;
    return $pid2params;
}

sub insert_group_params_with_href: Test {
    my $c = shift;
    $c->save({
        pid => 1,
        banners => [{href => '{phrase_id}'}],
    });
    cmp_deeply(
        $c->get(1)->{1},
        {pid => 1, has_phraseid_href => 1},
    );
}

sub dont_insert_without_any_params: Test {
    my $c = shift;
    $c->save({pid => 2});
    cmp_deeply($c->get(2), {});
}

sub mass_crosshard_update_test: Test {
    my $c = shift;
    $c->save([
        {pid => 101, banners => [{href => '{phrase_id}'}]},
        {pid => 102, banners => [{href => '{phrase_id}'}]},
        {pid => 103, banners => [{href => '{phrase_id}'}]},
        {pid => 104},
        {pid => 201},
        {pid => 202, banners => [{href => '{phrase_id}'}]},
    ]);
    cmp_deeply(
        $c->get([qw/101 102 103 104 201 202/]),
        {
            101 => {pid => 101, has_phraseid_href => 1},
            102 => {pid => 102, has_phraseid_href => 1},
            103 => {pid => 103, has_phraseid_href => 1},
            202 => {pid => 202, has_phraseid_href => 1},
        },
    )
}

__PACKAGE__->runtests();
