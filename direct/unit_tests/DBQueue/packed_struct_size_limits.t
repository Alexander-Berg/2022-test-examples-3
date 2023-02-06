#!/usr/bin/perl
use Direct::Modern;

use JSON;
use Test::Exception;
use Test::More;

use Yandex::Compress;
use Yandex::DBQueue;
use Yandex::DBSchema 'create_table_by_schema';
use Yandex::DBTools;
use Yandex::DBUnitTest ':all';

use Settings;

$Yandex::DBQueue::Typemap::SOURCE_FILE_PATH = "$Settings::ROOT/unit_tests/DBQueue/dbqueue-types.yaml";
$Yandex::DBQueue::LOG = Yandex::Log->new( use_syslog => 0, no_log => 1 );

my $big_struct = { 'key' => 'unencumbered' x 500_000 };

# на сжатие каждый раз надо 120 мс; сжатая структура занимает 11680 байт
my $big_struct_len = length( mysql_compress( to_json( $big_struct ) ) );

# весь тест работает с одним заданием -- иногда стирает всё содержимое таблиц, задание пересоздаёт
my $job_hash = {
    job_id => 1,
    ClientID => 1,
    args => $big_struct,
    priority => 0,
};

create_table_by_schema( UT, 'ppc.dbqueue_job_types' );
create_table_by_schema( UT, 'ppc.dbqueue_jobs' );
create_table_by_schema( UT, 'ppc.dbqueue_job_archive' );

my $queue = Yandex::DBQueue->new( UT, 'type1' );
my $job;

sub wipe_jobs {
    do_sql( UT, 'DELETE FROM dbqueue_jobs' );
    do_sql( UT, 'DELETE FROM dbqueue_job_archive' );
}

plan tests => 6;

## ставим новое задание
# максимальная длина на 1 меньше $big_struct_len -- должно падать
$Yandex::DBQueue::Job::MAX_PACKED_ARGS_LENGTH = $big_struct_len - 1;
wipe_jobs();
dies_ok { $queue->insert_job($job_hash) } 'cannot insert job with args too big';

# максимальная длина в точности $big_struct_len -- должно работать
$Yandex::DBQueue::Job::MAX_PACKED_ARGS_LENGTH = $big_struct_len;
wipe_jobs();
lives_ok { $queue->insert_job($job_hash) } 'can insert job with args at exactly the limit';

# максимальная длина на 1 больше $big_struct_len -- должно работать
$Yandex::DBQueue::Job::MAX_PACKED_ARGS_LENGTH = $big_struct_len + 1;
wipe_jobs();
lives_ok { $queue->insert_job($job_hash) } 'can insert job with args smaller than the limit';

## выбираем задание, сохраняем результат
$Yandex::DBQueue::Job::MAX_PACKED_ARGS_LENGTH = $big_struct_len * 2;

# максимальная длина на 1 меньше $big_struct_len -- должно падать
$Yandex::DBQueue::Job::MAX_PACKED_RESULT_LENGTH = $big_struct_len - 1;
wipe_jobs();
$queue->insert_job($job_hash);
$job = $queue->grab_jobs();
dies_ok { $job->mark_finished($big_struct) } 'cannot finish job with result too big';

# максимальная длина в точности $big_struct_len -- должно работать
$Yandex::DBQueue::Job::MAX_PACKED_RESULT_LENGTH = $big_struct_len;
wipe_jobs();
$queue->insert_job($job_hash);
$job = $queue->grab_jobs();
lives_ok { $job->mark_finished($big_struct) } 'can finish job with result at exactly the limit';

# максимальная длина на 1 больше $big_struct_len -- должно работать
$Yandex::DBQueue::Job::MAX_PACKED_RESULT_LENGTH = $big_struct_len + 1;
wipe_jobs();
$queue->insert_job($job_hash);
$job = $queue->grab_jobs();
lives_ok { $job->mark_finished($big_struct) } 'can finish job with result smaller than the limit';
