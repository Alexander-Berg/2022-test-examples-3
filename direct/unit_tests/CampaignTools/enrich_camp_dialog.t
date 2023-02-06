use strict;
use warnings;
use utf8;
no warnings "redefine";

use JSON;

use Yandex::I18n;
use Yandex::Test::UTF8Builder;
use Test::Deep;
use Test::More;
use Test::Warn;
use Yandex::DBUnitTest qw/:all/;

use Settings;

BEGIN {use_ok('CampaignTools', 'enrich_camp_dialog');}

*enrich_camp_dialog = \&CampaignTools::enrich_camp_dialog;


my %db = (
    shard_client_id => {
        original_db => PPCDICT,
            rows => [
                {ClientID => 1, shard => 1},
            ],
    },
    client_dialogs =>{
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
            {ClientID => 1, client_dialog_id => 1, skill_id => '702bc852-7e82-456b-b44e-3f87df8812d8',
             bot_guid => 'bbbbbbb-df56-43a4-a2b0-e363bee00711', is_active => 1, name => 'Stored dialog'},
        ]},
    },
);

init_test_dataset(\%db);

my $client_id = 1;
my $name_over_255 = 'N' x 256;

my %id2response = (
    'cf78e17c-df56-43a4-a2b0-e363bee00799' => {
        is_success => 0,
        content    => '{ "error": { "message": "Service error" } }',
    },
    '367e66ec-6c89-4d47-ac8e-fba4e7e625e4' => {
        is_success => 0,
        headers    => { Status => 404, Reason => 'Not Found', },
    },
    '77edb670-ec01-11e8-b568-0800200c9a66' => {
        is_success => 1,
        content    => '[{"error": {"code": 404, "message": "skill not found"}}]',
    },
    '0d731b50-ec01-11e8-b568-0800200c9a66' => {
        is_success => 1,
        content    => '[{"botGuid": "cf78e17c-df56-43a4-a2b0-e363bee00711", "name": "Test Name", "onAir": false, "id": "0d731b50-ec01-11e8-b568-0800200c9a66"}]',
    },
    'd04bffb5-ed2e-467e-9419-42d34a9ec3bc' => {
        is_success => 1,
        content    => '[{"botGuid": "cf78e17c-df56-43a4-a2b0-e363bee00700", "name": "Test Name", "onAir": true, "id": "d04bffb5-ed2e-467e-9419-42d34a9ec3bc"}]',
    },
    'a5c8c930-edab-11e8-b568-0800200c9a66' => {
        is_success => 1,
        content    => '[{"botGuid": "accc97c0-edab-11e8-b568-0800200c9a66", "name": "' . $name_over_255 . '", "onAir": true, "id": "a5c8c930-edab-11e8-b568-0800200c9a66"}]',
    },
);
local *Yandex::TVM2::get_ticket = sub {'fake_ticket'};
local *Yandex::HTTP::http_parallel_request = sub {
    my ($method, $data, %O) = @_;
    my $result;
    foreach my $rec_id (keys %$data) {
        my $dialog_id = decode_json($data->{$rec_id}->{body})->{skillIds}->[0];
        $result->{$rec_id} = $id2response{$dialog_id} // $id2response{'cf78e17c-df56-43a4-a2b0-e363bee00799'};
    }
    return $result;
};

cmp_deeply(enrich_camp_dialog($client_id, {}), { error => iget('Некорректный идентификатор чата') });
cmp_deeply(enrich_camp_dialog($client_id, { id => '' }), { id => '', error => iget('Некорректный идентификатор чата') });
cmp_deeply(enrich_camp_dialog($client_id, { id => 'xxx' }), { id => 'xxx', error => iget('Некорректный идентификатор чата') });

warning_like {
    cmp_deeply(
        enrich_camp_dialog($client_id, { id => 'cf78e17c-df56-43a4-a2b0-e363bee00799' }),
        { id => 'cf78e17c-df56-43a4-a2b0-e363bee00799', error => iget('Не удалось сохранить идентификатор чата, пожалуйста попробуйте позже') }
    );
    cmp_deeply(
        enrich_camp_dialog($client_id, { id => '367e66ec-6c89-4d47-ac8e-fba4e7e625e4' }),
        { id => '367e66ec-6c89-4d47-ac8e-fba4e7e625e4', error => iget('Не удалось сохранить идентификатор чата, пожалуйста попробуйте позже') }
    );
} [
    qr/paskills request failed/,
    qr/paskills request failed/,
];
cmp_deeply(
    enrich_camp_dialog($client_id, { id => '77edb670-ec01-11e8-b568-0800200c9a66' }),
    { id => '77edb670-ec01-11e8-b568-0800200c9a66', error => iget('Указан некорректный идентификатор чата') }
);

cmp_deeply(
    enrich_camp_dialog($client_id, { id => '0d731b50-ec01-11e8-b568-0800200c9a66' }),
    { id => '0d731b50-ec01-11e8-b568-0800200c9a66', error => iget('Указанный идентификатор чата не активен') }
);

cmp_deeply(
    enrich_camp_dialog($client_id, { id => 'd04bffb5-ed2e-467e-9419-42d34a9ec3bc' }),
    {
        id        => 'd04bffb5-ed2e-467e-9419-42d34a9ec3bc',
        is_active => 1,
        bot_guid  => 'cf78e17c-df56-43a4-a2b0-e363bee00700',
        name      => 'Test Name',

    }
);
cmp_deeply(
    enrich_camp_dialog($client_id, { id => 'a5c8c930-edab-11e8-b568-0800200c9a66' }),
    {
        id        => 'a5c8c930-edab-11e8-b568-0800200c9a66',
        is_active => 1,
        bot_guid  => 'accc97c0-edab-11e8-b568-0800200c9a66',
        name      => 'N' x 252 . '...',

    }
);
cmp_deeply (
    enrich_camp_dialog($client_id, { id => '702bc852-7e82-456b-b44e-3f87df8812d8' }),
    {
        id        => '702bc852-7e82-456b-b44e-3f87df8812d8',
        is_active => 1,
        bot_guid  => 'bbbbbbb-df56-43a4-a2b0-e363bee00711',
        name      => 'Stored dialog',
        client_dialog_id => 1,
    }
);

done_testing;
