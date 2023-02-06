#!/usr/bin/perl

=head1 DESCRIPTION

    Тест проверяет что виртуальный ключ в таблице ppc.banner_permalinks работает корректно.
    Ключ должен разрешать создать только одну manual-запись на один bid, и 
    неорганиченное количество auto-записей.

=cut

use Direct::Modern;
use Test::More tests => 13;
use Test::Exception;
use Yandex::DBTools;
use Yandex::DBSchema qw/create_table_by_schema/;
use Yandex::DBUnitTest qw/:all/;
use Settings;

use open ':std' => ':utf8';

local $SIG{__WARN__} = sub {};

my $table_name = 'banner_permalinks';
create_table_by_schema( UT, "ppc.$table_name" );

my $permalink = 10;
my $chain_id = 10;

test_ok('Можно создать manual запись с пермалинком',
        [[ $permalink, 0, 'manual' ]]);
test_ok('Можно создать manual запись с пермалинком и чейном',
        [[ $permalink, $chain_id, 'manual' ]]);
test_ok('Можно создать manual запись с чейном',
        [[ 0, $chain_id, 'manual' ]]);
test_ok('Можно создать auto запись с пермалинком', 
        [[ $permalink, 0, 'auto' ]]);
test_ok('Можно создать auto запись с пермалинком и чейном', 
        [[ $permalink, $chain_id, 'auto' ]]);
test_ok('Можно создать auto запись с чейном', 
        [[ 0, $chain_id, 'auto' ]]);
test_ok('Можно создать на один баннер две авто-записи с разными чейнами',
        [[ 0, $chain_id, 'auto' ],  
         [ 0, $chain_id+1, 'auto' ]]);
test_ok('Можно создать на один баннер две авто-записи с разными пермалинками',
        [[ $permalink,   0, 'auto' ], 
         [ $permalink+1, 0, 'auto' ]]);
test_ok('Можно создать на один баннер две авто-записи с совпадающими пермалинком и чейном',
        [[ $permalink, 0, 'auto' ], 
         [ 0, $permalink, 'auto' ]]);

test_dies('Нельзя создать на баннер две auto записи с одинаковым пермалинком',
        [[ $permalink,   0, 'auto' ],   
         [ $permalink,   0, 'auto' ]]);
test_dies('Нельзя создать на баннер две записи с одинаковым пермалинком, одну manual и одну auto',
        [[ $permalink,   0, 'auto' ],   
         [ $permalink,   0, 'manual' ]]);
test_dies('Нельзя создать на баннер две manual записи',
        [[ $permalink,   0, 'manual' ], 
         [ $permalink+1, 0, 'manual' ]]);
test_dies('Нельзя создать на баннер запись с нулевыми пермалинком и чейном одновременно',
        [[ 0, 0, 'auto' ]]);

do_sql(UT, "drop table $table_name");

done_testing();

sub test_ok {
    my ($name, $values) = @_;
    lives_ok { do_test($values) } $name;
}

sub test_dies {
    my ($name, $values) = @_;
    dies_ok { do_test($values) } $name;
}

sub do_test {
    my ($values) = @_;
    my $bid = get_one_field_sql(UT, "SELECT MAX(bid) FROM $table_name") + 1;
    for my $value (@$values) {
        my $data = {
            bid => $bid,
            permalink => $value->[0],
            chain_id => $value->[1],
            permalink_assign_type => $value->[2],
        };
        do_insert_into_table(UT, $table_name, $data);
    }
}
