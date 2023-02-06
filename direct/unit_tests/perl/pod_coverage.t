#!/usr/bin/perl
use strict;
use warnings;
use utf8;

=pod

=head1 NAME

    pod_coverage.t

=head1 SYNOPSIS

    ./unit_tests/runtests.pl unit_tests/perl/pod_coverage.t

=head1 DESCRIPTION
    Покрытие кода POD документацией (пока только методов)

    Пример запуска:

    TODO: хорошо бы еще проверять SYNOPSIS в скритах

    Тест короткий (7 секунд), большого смысла распараллеливать нет, также при
    распаралеливании перестает работать CountParents -- когда файл родителя и
    наследника попадают в разные чанки, то покрытие файла наследника меняется

=cut

use my_inc '../../';

use Pod::Coverage::CountParents;
use File::Spec;
use Test::More;

use Test::ListFiles;

use Settings;

=head2 %agreed_coverage

    ( Имя модуля => кол-во не покрытых методов (по умолчанию 0) )

    Приемлимое покрытие для модулей. Служит, во-первых, для указания модулей, для
    которых не 100%-ое покрытие является правильным, а во-вторых для закрепления
    статуса не до конца покрытых модулей, чтобы кол-во недокументированного кода
    в них не росло

    Модули из этого списка нужно проверить и либо задокументировать и удалить
    из списка; либо перенести в %WHITELIST

    217 - модулей с неполной документацией;
    121 - полностью задокументированных;

=cut

my %agreed_coverage = (

    'APICommon' => 8,
    'APIMethods' => 1,
    'APIUnits' => 12,
    'Agency' => 1,
    'AutobudgetAlerts' => 14,
    'BannerImages' => 2,
    'BannerImages::Pool' => 1,
    'CRM' => 3,
    'CairoGraph' => 15,
    'Campaign' => 8,
    'Captcha' => 6,
    'CheckShardMetabaseId' => 2,
    'Common' => 10,
    'Currencies' => 3,
    'DBStat' => 14,
    'DevTools' => 1,
    'DirectContext' => 1,
    'DoCmd' => 97,
    'DoCmdAJAX' => 2,
    'DoCmdAPI' => 2,
    'DoCmdAdGroup' => 2,
    'DoCmdAdmin' => 25,
    'DoCmdAnalytics' => 2,
    'DoCmdApiCertification' => 6,
    'DoCmdBanner' => 1,
    'DoCmdDev' => 6,
    'DoCmdFakeAdm' => 5,
    'DoCmdMediaplan' => 32,
    'DoCmdPDD' => 1,
    'DoCmdReports' => 19,
    'DoCmdStaff' => 11,
    'DoCmdVCards' => 1,
    'DoCmdWallet' => 4,
    'DoCmdXls' => 10,
    'EnvTools' => 1,
    'FakeAdminTools' => 4,
    'FeedBackCommander' => 1,
    'Forecast' => 3,
    'ForecastXLS' => 3,
    'HashingTools' => 9,
    'Intapi' => 3,
    'IpTools' => 3,
    'LWPRedirect' => 1,
    'LockObject' => 4,
    'LockTools' => 2,
    'LogsCommander' => 1,
    'MTools' => 20,
    'MailNotification' => 1,
    'MailService' => 4,
    'Mcb' => 3,
    'Mediaplan' => 11,
    'ModerationQueue' => 1,
    'MoneyTransfer' => 1,
    'Monitor' => 1,
    'Notification' => 29,
    'PhraseText' => 1,
    'Primitives' => 9,
    'PrimitivesIds' => 1,
    'Property' => 4,
    'RedirectCheckQueue' => 5,
    'SOAPAPI' => 1,
    'SearchObjects' => 1,
    'ServicedClient' => 2,
    'StoredVars' => 5,
    'TTTools' => 6,
    'Tag' => 2,
    'TeXEscape' => 1,
    'TextTools' => 3,
    'TimeTarget' => 1,
    'Tools' => 1,
    'HttpTools' => 6,
    'URLDomain' => 5,
    'User' => 3,
    'XLSCampImport' => 17,
    'XLSCampaign' => 2,
    'XLSMediaplan' => 3,
    'XLSParse' => 3,
    'XLSVocabulary' => 3,
    'YaCatalogApi' => 2,
    'YandexOffice' => 1,
    'mediastatXLS' => 4,
    'search' => 5,
    'ArrayProcessor' => 2,
    'API::Errors' => 2,
    'API::Filter' => 6,
    'API::Limits' => 5,
    'API::Soapize' => 21,
    'API::ValidateRights' => 6,
    'API::ValidateTools' => 2,
    'Apache::AuthAdwords' => 11,
    'Apache::DebugMergeStatic' => 9,
    'Apache::DebugWSDL' => 9,
    'Apache::LogParser' => 4,
    'Apache::MaskSecrets' => 1,
    'BS::ExportMaster' => 3,
    'Client::NDSDiscountSchedule' => 2,
    'Direct::AdGroups' => 8,
    'Direct::Errors' => 5,
    'Direct::PredefineVars' => 2,
    'Direct::ReShard' => 6,
    'Direct::ResponseHelper' => 9,
    'Direct::StaticFilesHash' => 1,
    'DoCmd::Base' => 9,
    'DoCmd::FormCheck' => 14,
    'DoCmd::Public' => 4,
    'DoCmdXls::History' => 1,
    'Export::AgencyCheckForMetrika' => 1,
    'Export::UserRole' => 3,
    'Export::Widget' => 2,
    'Forecast::Autobudget' => 5,
    'Forecast::Budget' => 5,
    'Intapi::Alive' => 1,
    'Intapi::AutobudgetAlerts' => 3,
    'Intapi::CampaignsForMetrica' => 2,
    'Intapi::CampaignsSumsForBS' => 1,
    'Intapi::CLegacy::ClientInfo' => 1,
    'Intapi::CLegacy::News' => 2,
    'Intapi::CLegacy::RBAC' => 1,
    'Intapi::DostupUserManagement' => 5,
    'Intapi::ExportCmdLog' => 1,
    'Intapi::FakeAdmin' => 6,
    'Intapi::FileStorage' => 1,
    'Intapi::Moderation' => 1,
    'Intapi::TestDataGenerator' => 3,
    'Intapi::TestUsers' => 2,
    'Lang::Guess' => 1,
    'Lang::Unglue' => 2,
    'Model::AdGroup' => 5,
    'Model::Banner' => 7,
    'Model::Campaign' => 2,
    'Model::Mappers' => 3,
    'Model::MediaAdGroup' => 1,
    'Model::VCard' => 5,
    'Models::AdGroup' => 3,
    'Models::Campaign' => 1,
    'Models::Phrase' => 1,
    'Moderate::Quick' => 2,
    'ModerateChecks::HotPhrases' => 1,
    'PSGIApp::FeedBackCommander' => 1,
    'PSGIApp::Intapi' => 1,
    'PSGIApp::JSONRPC' => 1,
    'PSGIApp::LogJS' => 1,
    'PSGIApp::LogsCommander' => 1,
    'PSGIApp::Public' => 2,
    'PSGIApp::Search' => 1,
    'RBAC2::DirectChecks' => 12,
    'RBACElementary' => 1,
    'Reports::Checks' => 5,
    'Reports::ClientPotential' => 15,
    'Reports::Queue' => 1,
    'Sandbox::FakeBSInfo' => 1,
    'Sandbox::FakeBSSOAP' => 1,
    'Sandbox::FakeMetrika' => 1,
    'Sandbox::FakeBalanceXMLRPC' => 1,
    'Sandbox::FakeBlackbox' => 1,
    'Sandbox::FakePassport' => 1,
    'Sandbox::FakeYaMoney' => 1,
    'Settings' => 2,
    'SettingsALL' => 1,
    'Test::Intapi' => 6,
    'Yandex::CallerStack' => 3,
    'API::Authorization' => 12,
    'API::Services' => 20,
    'API::Service::Reports' => 1,
    'API::Methods::AccountManagement' => 2,
    'API::Methods::AdImage' => 7,
    'API::Methods::Clients' => 2,
    'API::Methods::Customer' => 1,
    'API::Methods::Finance' => 3,
    'API::Methods::Keyword' => 1,
    'API::Methods::Mediaplan' => 4,
    'API::Methods::Prices' => 4,
    'API::Methods::Retargeting' => 6,
    'API::Methods::Sandbox' => 1,
    'API::Methods::Staff' => 1,
    'Direct::AdGroups::AddItem' => 1,
    'Direct::AdGroups::AddList' => 3,
    'Direct::AdGroups::DeleteItem' => 2,
    'Direct::AdGroups::DeleteList' => 1,
    'Direct::AdGroups::Statuses' => 6,
    'Direct::AdGroups::UpdateItem' => 3,
    'Direct::AdGroups::UpdateList' => 21,
    'Direct::Data::List' => 2,
    'Direct::Monitor::Base' => 1,
    'Direct::Monitor::Daily' => 13,
    'Direct::Validation::MinusWords' => 1,
    'Direct::YT::banners_href_substituted_length_yt' => 1,
    'Model::Mapper::AdGroups' => 10,
    'Model::Mapper::AdImages' => 1,
    'Model::Mapper::Banners' => 10,
    'Model::Mapper::Campaigns' => 2,
    'Model::Mapper::Keywords' => 8,
    'Model::Mapper::MediaAdGroups' => 1,
    'Model::Mapper::MinusWords' => 3,
    'Model::Mapper::Tags' => 2,
    'Moderate::JSONRPC::Client' => 2,
    'PSGIApp::API::JSON' => 1,
    'PSGIApp::API::SOAP' => 1,
    'PSGIApp::Export::Widget' => 1,
    'Stat::CustomizedArray::Base' => 5,
    'API::Service::Base' => 22,
    'Direct::Data::Item::Update' => 1,
    'Direct::Data::List::Util' => 2,
    'Campaign::Copy' => 2,
);

=head2 %WHITELIST

    Модули котороые проверять не надо

=cut

my $WHITELIST_RE = join q{|}, (
    'DoCmd', # контроллеры, большого смысла документировать которые нет
    'ScriptHelper', # документирован, плюс под pod_coverage кидает варнинг про INIT
    'E',
    # контроллеры API5
    'API::Service::AdGroups',
    'API::Service::Ads',
    'API::Service::Bids',
    'API::Service::Keywords',
    'API::Service::VCards',
    # Модели
    'Direct::Model::.+',
    # Модуль для тестирования (в процессе разработки)
    'Direct::Test::DBObjects',
    # подневные статистики (документация в ::base)
    'Direct::YT::monitor_stats::(?!base).*',
    # при проверке без БД портится план
    (defined $ENV{UNIT_OPT_DB} && !$ENV{UNIT_OPT_DB} ? 'Test::CreateDBObjects' : ()),
    # его подгрузка затирает DBConfig от юнит-тестовой базы
    'SettingsDockerDB',
);

my @MY_INC = @my_inc::MY_INC;

# [ { dir => $dir, file => $file, module => $module }, ... ]
my @tests;

foreach my $dir (@MY_INC) {
    foreach my $file ( Test::ListFiles->list_repository($dir) ) {
        next unless -f $file && $file =~ /\.pm$/;

        my $module = package_by_file($dir, $file);

        next if $module =~ /^$WHITELIST_RE$/o;

        push @tests, { dir => $dir, file => $file, module => $module };
    }
}

plan tests => scalar(@tests);

for my $test (sort { $a->{module} cmp $b->{module} } @tests) {
    my $dir = $test->{dir};
    my $file = $test->{file};
    my $module = $test->{module};

    my $coverage = Pod::Coverage::CountParents->new( package => $module );
    my @uncovered = $coverage->uncovered;

    my $expected = exists $agreed_coverage{$module}
        ? $agreed_coverage{$module}
        : 0;

    is( scalar(@uncovered), $expected,
        "$module has ".scalar(@uncovered)." uncovered methods: ".join(',', @uncovered));
}


sub package_by_file {
    my $dir = shift;
    my $file_path = shift;

    my $file_name = File::Spec->abs2rel($file_path, $dir);

    my @parts = File::Spec->splitdir( $file_name );
    $parts[-1] =~ s/\.pm$// if @parts;

    for ( @parts ) {
        if ( /^([a-zA-Z0-9_\.\-]+)$/ && ($_ eq $1) ) {
            $_ = $1;  # Untaint the original
        } else {
            die qq{Invalid and untaintable filename "$file_path"!};
        }
    }

    return join( "::", @parts );
}
