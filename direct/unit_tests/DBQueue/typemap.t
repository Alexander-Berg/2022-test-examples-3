#!/usr/bin/perl
use Direct::Modern;

use Test::Deep;
use Test::Exception;
use Test::More;

use Yandex::DBQueue::Typemap;
use Yandex::DBSchema 'create_table_by_schema';
use Yandex::DBTools;
use Yandex::DBUnitTest ':all';
use Yandex::Log;

use Settings;

$Yandex::DBQueue::Typemap::SOURCE_FILE_PATH = "$Settings::ROOT/unit_tests/DBQueue/dbqueue-types.yaml";

my $log = Yandex::Log->new( use_syslog => 0, no_log => 1 );

create_table_by_schema( UT, 'ppc.dbqueue_job_types' );

my $job_types = [qw/type1 type2 type3/];

my $typemap = Yandex::DBQueue::Typemap->new(UT) or die 'failed to create typemap';

# список типов правильный
is_deeply($typemap->get_job_types, $job_types, "job types 1");

## type_to_id
is $typemap->type_to_id('type1'), 1, "type_to_id";
dies_ok { $typemap->type_to_id('bogus_type') } 'type_to_id (bogus type)';

## id_to_type
is $typemap->id_to_type(2), 'type2', "id_to_type";
dies_ok { $typemap->id_to_type(65536) } 'id_to_type (bogus id)';

## fill_job_types
dies_ok { $typemap->fill_job_types } 'fill_job_types requires log';

# заполняем таблицу полностью
do_sql( UT, 'DELETE FROM dbqueue_job_types' );
is $typemap->fill_job_types( log => $log ), 3, 'fill_job_types filled all types';

# список типов по-прежнему тот же самый
is_deeply($typemap->get_job_types, $job_types, "job types 2");

# заполняем таблицу полностью, удаляем один из типов, проверяем, что повторный вызов его допишет
do_sql( UT, 'DELETE FROM dbqueue_job_types' );
$typemap->fill_job_types( log => $log );
do_sql( UT, 'DELETE FROM dbqueue_job_types WHERE job_type = ?', 'type3' );
is $typemap->fill_job_types( log => $log ), 1, 'fill_job_types filled type3';

# список типов по-прежнему тот же самый
is_deeply($typemap->get_job_types, $job_types, "job types 3");

# заполняем таблицу полностью, ещё раз заполняем таблицу полностью
do_sql( UT, 'DELETE FROM dbqueue_job_types' );
$typemap->fill_job_types( log => $log );
lives_ok { $typemap->fill_job_types( log => $log ) } 'fill_job_types: can call twice';

# пишем в таблицу битые данные: type1 => 65536
do_sql( UT, 'DELETE FROM dbqueue_job_types' );
do_sql( UT, 'INSERT INTO dbqueue_job_types (job_type, job_type_id) VALUES (?, ?)', 'type1', 65536 );
dies_ok { $typemap->fill_job_types( log => $log ) } 'fill_job_types: inconsistent ID';

# список типов по-прежнему тот же самый
is_deeply($typemap->get_job_types, $job_types, "job types 4");

# пишем в таблицу битые данные: bogus_type => 1
do_sql( UT, 'DELETE FROM dbqueue_job_types' );
do_sql( UT, 'INSERT INTO dbqueue_job_types (job_type, job_type_id) VALUES (?, ?)', 'bogus_type', 1 );
dies_ok { $typemap->fill_job_types( log => $log ) } 'fill_job_types: inconsistent type';

# список типов отдаст и типы из файла, и (некорректный) тип из БД
is_deeply($typemap->get_job_types, [qw/bogus_type/, @$job_types], "job types 5");

done_testing;
