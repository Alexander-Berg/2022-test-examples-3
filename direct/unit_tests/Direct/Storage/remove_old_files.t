#!/usr/bin/perl

use Direct::Modern;
use Test::More;
use Test::Yandex::MDS;
use Direct::Storage;

use Yandex::DBTools;
use Settings;
use Yandex::DBUnitTest qw/:all/;

use Carp;
$SIG{__WARN__} = \&Carp::cluck;
$SIG{__DIE__} = \&Carp::confess;

my %db = (
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1, ClientID => 1 },
        ],
    },
    inc_mds_id => {
        original_db => PPCDICT,
        rows => [],
    },
    mds_metadata => {
        original_db => PPC(shard => 'all'),
    },
    mds_custom_names => {
        original_db => PPC(shard => 'all'),
    },
);

my %db2 = (
    # еще одна переменная из-за конфликта имен
    mds_metadata => {
        original_db => PPCDICT,
    },
    mds_custom_names => {
        original_db => PPCDICT,
    },
);

init_test_dataset(\%db);
init_test_dataset(\%db2);


my $server = Test::Yandex::MDS::mock_server(namespace => 'direct-files', authorization => 'hi' );

my $host = $server->uri;
$host =~ s!^.+?://!!;
$host =~ s!/+$!!;
do_sql(PPC(shard => 'all'), "alter table mds_metadata change storage_host storage_host enum(?) not null", $host);
do_sql(PPCDICT, "alter table mds_metadata change storage_host storage_host enum(?) not null", $host);

my $storage = Direct::Storage->new(
    get_host => $host,
    put_host => $host,
    authorization => 'hi',
    namespace => 'direct-files',
);

{
my $feed = $storage->save("perf_feeds", "content", ClientID => 1);
my $feed2 = $storage->save("perf_feeds", "content2", ClientID => 1);

do_update_table(PPC(shard => 1), 'mds_metadata', { 
        create_time__dont_quote => "create_time - interval 10 second"
    },
    where => {
        id => $feed->_id 
});

my $removed = $storage->remove_old_files(1, 'perf_feeds', 0);

ok(@$removed == 1, "removed old file");
}

{
my $file = $storage->save('common_file_export', "content", filename => 'test filename');
my $file2 = $storage->save('common_file_export', "content2", filename => 'test filename2');
do_update_table(PPCDICT, 'mds_metadata', { 
        create_time__dont_quote => "create_time - interval 10 second"
    },
    where => {
        id => $file->_id 
});

my $removed = $storage->remove_old_files(1, 'common_file_export', 0);

ok(@$removed == 1, "removed old file with custom name");

}


done_testing();
