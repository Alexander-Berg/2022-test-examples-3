#!/usr/bin/perl
use strict;
use warnings;

=pod

Этот тест проверяет, есть ли в коде подключения модулей, которые на самом деле не используются.
Пример: модуль POSIX экспортирует в пространство имён модуля, в котором он подключается,
очень много функций, расходуя память. Если ни одна из этих функций на самом деле не вызывается,
импорт стоит удалить.

Что используется для проверки: https://metacpan.org/pod/Test::UsedModules
+ патч, см. https://github.com/moznion/Test-UsedModules/pull/2

# с непатченной версией Test::UsedModules список импортируемых методов нужно писать в одну строчку, 
# иначе проверка работает неправильно, подробности в $DRT/contrib/Test-UsedModules/runme.sh
use Module qw/method1 method2 method3/;

# в $DRT/contrib/Test-UsedModules есть патч remove_newlines.diff, 
# после применения которого можно писать импортируемые методы в несколько строк:
use Module qw/
  method1 
  method2 
  method3
/;

=cut

use my_inc;

use PPI::Cache;
use PPI::Document;

use Digest::CRC qw( crc32 );
use Path::Tiny;
use Perl::Critic::Utils qw( all_perl_files );
use Test::More;
use Test::UsedModules;

# предварительная загрузка тяжёлых модулей
require DoCmd;
require API;

use Settings;

my %SKIP_FILE = map { $_ => 1 } (
    # это тяжёлый автогенерируемый файл, его не может разобрать старая версия PPI, которая у нас используется;
    # https://rt.cpan.org/Public/Bug/Display.html?id=81616
    # кажется, проверять его на каждый коммит не обязательно, так что не проверяем
    "$Settings::ROOT/protected/geo_regions.pm",
);

# кеш для PPI
umask 0;
my $CACHE_ROOT = "/var/cache/ppc";
if (-d $CACHE_ROOT && -w $CACHE_ROOT) {
    my $ppi_cache_dir = "$CACHE_ROOT/ppi-cache-$>";
    path($ppi_cache_dir)->mkpath() unless -e $ppi_cache_dir;
    PPI::Document->set_cache( PPI::Cache->new(path => $ppi_cache_dir) );
}

# Этот список нужен для модулей, которые при подключении делают что-то, что Test::UsedModules
# не может определить.
# Ниже есть ещё один список FILES_WHITELIST, иногда лучше пользоваться им.
my @modules_whitelist = (
    # проверенная часть: используются без явного вызова функций, нужно оставить
    'my_inc',
    'lib::abs',
    'FindBin',

    'Direct::Modern',

    'Attribute::Handlers',

    'API::WSDL::ErrorsTranslation',
    'Direct::Errors::Messages',

    'SettingsALL',
    'Settings',
    'ScriptHelper',

    'Net::INET6Glue::INET_is_INET6',

    'nginx',
    'EV',
    'SOAP::Lite',
    qr/^DoCmd/,
    'Direct::Monitor::Daily',
    qr/Direct::YT::monitor_stats::.*/,

    'Sandbox::Balance',

    'Direct::YT::account_score_yt',

    'AnyEvent',
    'common::sense',

    'Campaign::Creator::Types', # задает Mouse ограничения
    'Yandex::ORM::Types',
    'Direct::Model::Keyword::BsData',

    'Test::More',
    'Yandex::Test::UTF8Builder',
    
    'URI::QueryParam',

    # в юнит-тесте unit_tests/Direct/errors.t проверяется ошибка подгрузки этого модуля
    'TestErrors::Module7',

    # непроверенная часть: нужно проверить и либо удалить импорты, либо перенести в проверенные
    'SandboxClient',
    'Apache2::Connection',
    'Apache2::Const',
    'Apache2::ServerRec',
    'Apache2::ServerUtil',
    'API::Authorization::MouseIpType',
    'API::Error::ToExceptionNotification',
    'API::Methods::Keyword::ActionResults',
    'API::Service::ResultSet::Base',
    'API::Service::ResultSet::Indexed',
    'API::App::Types',
    'API::App::Request::MassMethods',
    'API::App::Request::History',
    'API::App::Request::Tools',
    'API::Reports::ReportTypeConstraints',
    'Carp',
    'Crypt::SSLeay',
    'Cwd',
    'Date::Calc',
    'DateTime::Format::MySQL',
    'Digest::MD5',
    qr/^Direct::Validation::/,
    'Email::Date::Format',
    'Fcntl',
    'FileHandle',
    qr/^HierarchicalMultipliers::/,
    'HTTP::Request::Common',
    'HTTP::Request',
    'HTTP::Response',
    'Image::ExifTool',
    'Intapi',
    'IO::File',
    'IO::Handle',
    'IO::Scalar',
    'MRStreaming',
    'Plack::NormalizeResponse',
    'Plack::Response',
    'Plack::UTF8Request',
    'Plack::Util::Accessor',
    'Spreadsheet::WriteExcel',
    'Stat::SearchQuery::Queue',
    'Template',
    'Time::Local',
    'Try::Tiny',
    'Yandex::HighlightWords',
    'Yandex::I18n',
    'Yandex::MirrorsTools::Hostings',
    'Yandex::Queryrec',
    'Yandex::ReportsXLS',
    'Yandex::SOAP::UTF8Serializer',
    'DDP',
);

# Сюда нужно добавлять какие-то модули, которые подключают другие модули только для того, чтобы
# сделать более удобный интерфейс. Пример: E.pm подключает несколько других модулей и реэкспортирует
# их функции в пакет, из которого он вызывается.
my %FILES_WHITELIST = map { $_ => 1 } (
    'protected/E.pm',
    'protected/Direct/Errors/Messages.pm',
    # Mouse::Role подключется чтобы получить роль, сама же роль является просто меткой и не содержит свойств/методов
    'protected/Direct/Model/Role/OnlyBannersResources.pm',
    # в Client используется функция из User без импорта, т.к. User импортирует Client
    'unit_tests/Client/get_client_currencies.t',
    # юнит-тест на компилируемость шаблонов подключает некторорые модули (e.g. TTTools) для того, чтобы они были доступны в RAWPERL в шаблонах
    'unit_tests/templates/compile_PARALLEL4.t',
);

$Test::UsedModules::MODULES_WHITELIST ||= [];
my $whitelist_re = join '|', @modules_whitelist;
push @$Test::UsedModules::MODULES_WHITELIST, qr/$whitelist_re/;

my ($par_id, $par_level) = (0, 1);
if (@ARGV == 1 && $ARGV[0] =~ /^(\d+)[:\/](\d+)$/) {
    ($par_id, $par_level) = ($1, $2);
} elsif (@ARGV) {
    die "Usage: $0 [par_id/par_level]";
}
$par_id %= $par_level;

my @files = all_perl_files(
    "$Settings::ROOT/api/lib",
    "$Settings::ROOT/api/services",
    "$Settings::ROOT/api/t",
    "$Settings::ROOT/deploy",
    "$Settings::ROOT/protected",
    "$Settings::ROOT/unit_tests",
);

@files = grep { $_ !~ m!/deploy/archive/! } @files;
@files = grep { !$SKIP_FILE{$_} } @files;
@files = grep { !$FILES_WHITELIST{ path($_)->relative($Settings::ROOT) } } @files;
@files = grep { crc32($_) % $par_level == $par_id } @files;

if (defined $ENV{UNIT_OPT_DB} && !$ENV{UNIT_OPT_DB}) {
    plan skip_all => "doesn't works properly on modules with Yandex::DBUnitTests and --no-db option";
} else {
    plan tests => scalar @files;
    used_modules_ok($_) foreach @files;
}

