#!/usr/bin/perl
use Direct::Modern;

use Test::More;

use Yandex::DBQueue;
use Yandex::DBSchema 'create_table_by_schema';
use Yandex::DBTools;
use Yandex::DBUnitTest ':all';

use Settings;

$Yandex::DBQueue::Typemap::SOURCE_FILE_PATH = "$Settings::ROOT/unit_tests/DBQueue/dbqueue-types.yaml";
$Yandex::DBQueue::LOG = Yandex::Log->new( use_syslog => 0, no_log => 1 );

create_table_by_schema( UT, 'ppc.dbqueue_job_types' );
create_table_by_schema( UT, 'ppc.dbqueue_jobs' );
create_table_by_schema( UT, 'ppc.dbqueue_job_archive' );

my $queue = Yandex::DBQueue->new( UT, 'type1' );

sub _insert
{
    state $id = 0;
    my $job_hash = {
        ClientID => 1,
        args => {},
        priority => 0,
    };
    $job_hash->{job_id} = ++$id;
    $queue->insert_job($job_hash);
}

{
do_sql(UT, "delete from dbqueue_jobs");
_insert();
_insert();

my @jobs = $queue->grab_jobs(limit => 1);
is(scalar @jobs, 1, "grab 1 when 2 avaible");
is(ref $jobs[0], 'Yandex::DBQueue::Job', "list context");
}

{
do_sql(UT, "delete from dbqueue_jobs");
_insert();
_insert();
_insert();

my @jobs = $queue->grab_jobs(limit => 2);
is(scalar @jobs, 2, "grab 2 when 3 avaible");
}

{
do_sql(UT, "delete from dbqueue_jobs");
_insert() for (1 .. 5);

my @jobs = $queue->grab_jobs(limit => 10);
is(scalar @jobs, 5, "grab 5 when 5 avaible and asked for 10");
}

{
do_sql(UT, "delete from dbqueue_jobs");
_insert() for (1 .. 5);

my $job = $queue->grab_jobs(limit => 10);
is(ref $job, 'Yandex::DBQueue::Job', "scalar context");
}


done_testing();
