#!/usr/bin/perl

=pod

    $Id$
    Проверка на отсутствие в скриптах отладочных штук:
      * SettingsALL

    А так же прочих deprecated конструкций:
      * $r->header_in()
      * $r->header_out()
      * use Data::Printer or DDP
      * use E
      * is_production() -- есть исключения, записаны в IS_PRODUCTION_EXCEPTIONS

    is_production не рекомендуется к использованию потому,
    что он создает поведение, не проверяемое на ТС
    Если надо другое поведение на бетах -- рекомендуется IS_beta

    NO_PRODUCTION проверяется в соседнем тесте no_no_production.t
=cut

use strict;
use warnings;

use File::Slurp;
use Data::Dumper;
use Test::More;

use Test::ListFiles;
use Settings;

use Yandex::Test::UTF8Builder;

use utf8;


# список файлов-исключений для проверки на SettingsALL
# SettingsALL предназначен для разовых посчетов всяческй статистики.
# В репозитории скриптов, которые используют SettingsALL, быть не должно,
# потому что SettingsALL создает неестественные связи между разными средами.
# Если кажется, что есть новое исключение, не принимай решение в одиночку!
# Обсуди с кем-нибудь, как можно решить задачу _без_ SettingsALL.
my @SettingsALL_exceptions = (
    'protected/maintenance/prepare_bs_resync_file.pl',
    'protected/maintenance/prepare_mod_resync_file.pl',
    'protected/maintenance/sync_avatars.pl',
    'protected/one-shot/get_dynamic_camps_with_stat.pl',
    'protected/one-shot/DIRECT-73145_calc_change_href.pl',
);
my $SettingsALL_exceptions_regexp = "^\Q$Settings::ROOT\E/(?:".join('|', map {"\Q$_\E"} @SettingsALL_exceptions).")\$";

=head2 @Parallel_ForkManager_exceptions

    Parallel::ForkManager плохо сочетается с управлением скриптами через switchman;
    правильное состояние -- когда все экземпляры (процессы) запускаются независимо и могут работать на разных машинах.

    Можно использовать его в служебных скриптах/миграциях: protected/maintenance/, protected/one-shot/
    В обычных скриптах -- не надо.
    Если кажется, что без Parallel::ForkManager никак не обойтись -- не принимай решение в одиночку, обсуди еще с кем-нибудь, как сделать лучше.

=cut
my @Parallel_ForkManager_exceptions = (
    '^api/t/v5/Reports/collect_test_cases\.pl$',
    '^protected/maintenance/',
    '^protected/one-shot/',
    '^protected/ppcAutobudgetForecast.pl',
    '^protected/ppcFetchClientMulticurrencyTeaserData.pl',
    '^protected/ppcPrepareCurrencyTeaserClients.pl',
    '^deploy/20160525_increase_BannerID_size.pl',
);
my $Parallel_ForkManager_exceptions_regexp = "(".join('|', map {"$_"} @Parallel_ForkManager_exceptions).")";


# список имеющихся использований is_production в коде
# расширять список очень не рекомендуется,
# если очень-очень надо -- только после тщательного ревью
# Уменьшать список рекомендуется очень
my %IS_PRODUCTION_EXCEPTIONS = (
    'data/block/i-header-bem/i-header-bem.tt2' =>
    [
        '    IF is_production;',
        '        IF is_production; b_statcounter(); END;'
    ],
    'data/block/i-header/i-header.tt2' =>
    [
        '    IF is_production;',
        '    IF is_production; b_statcounter(); END;'
    ],

    'data3/desktop.blocks/b-page/b-page.bemtree.js' => [
        "            if (!this.data.is_production && this.data.COOKIES['HERMIONE'] == 1) {",
        '            !!this.data.is_production && {'
    ],

    'protected/API.pm' =>
    [
        '    if (! is_production() && $headers->{fake_login}) {',
        '                $die_details = "DEVTEST/$method: ".Dumper($soap_fault) if !is_production();',
        '    if (defined $METHODS_BY_VERSION->{$self->{method}} && (!$TEST_METHODS{$self->{method}} || !is_production())) {'
    ],

    'protected/API/Methods/AdImage.pm' =>
    [
        '    if (!is_production()) {'
    ],

    'protected/APICommon.pm' =>
    [
        '    if (! is_production()) {',
        '    if ( is_production() && $Settings::AVAILABLE_API_VERSIONS{$version_label}{hidden} ) {'
    ],

    'protected/APIMethods.pm' =>
    [
        'unless (is_sandbox() || is_production()) {'
    ],

    'protected/Apache/AuthAdwords.pm' =>
    [
        '    if (! is_production()) {'
    ],

    'protected/Direct/ResponseHelper.pm' =>
    [
        '        next if is_production() && $reqid % 100 > $head->{enable_on};',
    ],

    'protected/Direct/Template.pm' =>
    [
        '        is_production  => is_production(),',
        '    unless (is_production()) {',
        '    if (!is_production() && $ctx_data->{is_internal_ip} && $ctx_data->{FORM}->{get_vars}) {',
        '    if (!is_production()) {',
        '    if (!is_production()) {'
    ],
    'protected/Direct/Feature.pm' =>
    [
        '    if ( !is_production() && $r ){',
    ],

    'protected/DoCmd.pm' =>
    [
        '    my $is_production = is_production();',
        '    my $disable_client_validation = !is_production() || is_beta()',
        '        BALANCE_CART_UI_URL => (is_production() || !$is_crowdtest) ? ${Settings::BALANCE_CART_UI_URL} : ${Settings::BALANCE_CART_UI_URL_CROWDTEST},',
        '    if ( !is_production() && $cookies->{do_not_show_captcha} ) {',
        '    $vars->{is_production} = $is_production;',
        '    #   В $vars к этому моменту будут: has_media_camps, has_text_camps, is_beta, is_direct, is_production',
        '                            is_production => $is_production,',
        '        is_production => 1,',
        '    if ($FORM{human} !~ /^(super)?teamleader$/ && is_production() ) {',
    ],

    'protected/DoCmd/Public.pm' =>
    [
        '                is_production  => is_production(),',
        '    error("Operation not permitted or not available yet...") if is_production();'
    ],

    'protected/DoCmdAdmin.pm' =>
    [
        '    error("incorrect configuration") if is_production();',
        "        error(\"Временно, перенос доступен только \".join(', ', \@allowed)) if is_production() && none {\$operator_domain_login eq \$_} \@allowed;"
    ],

    'protected/DoCmdDev.pm' =>
    [
        '    error("Unsupported stage/configuration") if is_production() || !is_beta();'
    ],

    'protected/DoCmdFakeAdm.pm' =>
    [
        '    error("incorrect configuration") if is_production();',
        '    error("incorrect configuration") if is_production();',
        '    error("incorrect configuration") if is_production();',
        '    error("incorrect configuration") if is_production();',
    ],

    'protected/DoCmdStaff.pm' =>
    [
        '    if ($FORM{role} !~ /^(super)?teamleader$/ && is_production() ) {                                                                                                           ',
    ],

    'protected/EnvTools.pm' =>
    [
        '    is_production',
        "\# также см. is_beta() и is_production()",
        '=head2 is_production()',
        'sub is_production {',
        '=head2 is_production_but_not_roprod()',
        '    Аналог is_production за исключением roprod конфигурации',
        'sub is_production_but_not_roprod {',
        q!    return is_production() && $Settings::CONFIGURATION ne 'roprod';!,
    ],

    'protected/Intapi.pm' =>
    [
        '    my $acl_param_name = is_production() ? \'allow_to\' : \'allow_to_in_testing\';'
    ],

    'protected/Intapi/DostupUserManagement.pm' =>
    [
        '    if (is_production()) {'
    ],

    'protected/InternalReports.pm' =>
    [
        '        rbac_check => { Role => [ qw( super superreader ) ], Code => [ sub { is_production() } ] },',
        '        if (is_production() && !$O{c}->login_rights->{is_devops}) {',
        '    return $err->(\'Этот отчёт не работает в production\') if is_production();',
        '    die if is_production();',
    ],

    'protected/PPCLoginBox.pm' =>
    [
        '        is_production  => is_production(),',
    ],

    'protected/PSGIApp/JSONRPC.pm' =>
    [
        '    if ( is_production() && $Intapi::GROUP_DISABLED_IN_PRODUCTION{$group} ) {',
        '        my $acl_name = is_production() ? \'allow_to\' : \'allow_to_in_testing\'; ',
        '        my $prod = is_production();',
    ],
    'protected/PSGIApp/Intapi.pm' =>
    [
        '    if ( is_production() && $Intapi::GROUP_DISABLED_IN_PRODUCTION{$group} ) {',
    ],
    'protected/PSGIApp/Public.pm' =>
    [
        'use EnvTools qw/is_production/;',
        '    if ( !is_production() && get_cookie($r, \'do_not_show_captcha\') ) {',
    ],

    'protected/RBAC2/DirectChecks.pm' =>
    [
        '    return 1 if is_production();'
    ],

    'protected/bsClientData.pl' =>
    [
        'if (is_production() && (defined $NO_SEND || defined $NO_LOGBROKER)) {',
        '    BS::ExportWorker::log_data("par_id=$PAR_ID,uuid=$request_uuid,data_type=request", $query) if !EnvTools::is_production_but_not_roprod();',
    ],

    'protected/maintenance/create_test_campaigns.pl' =>
    [
        '    die "$0 in production!" if is_production();'
    ],

    'protected/maintenance/unblock_ynd_fixed_test_clients.pl' =>
    [
        '    die "$0 in production!" if is_production();'
    ],

    'protected/maintenance/fix_cpm_productids.pl' =>
    [
        '    $log->die("$0 in production!") if is_production();'
    ],

    'protected/maintenance/create_test_users.pl' =>
    [
        '                die "block mode is only for production configuration" if !is_production();',
        '                die "create mode is only for non-production configurations" if is_production();',
        '    if (! $block_production_users && ! is_production()) {'
    ],

    'protected/maintenance/fill_currency_rates.pl' =>
    [
        "die \"Скрипт должен использоваться только в тестовой конфигурации\" if is_production();"
    ],

    'protected/maintenance/fix_addresses_auto_points.pl' =>
    [
        'local $Lang::Guess::EXTERNAL_QUERYREC = sub { return {eng => 1}; } if !is_production();'
    ],

    'protected/maintenance/fix_vcard_broken_addresses.pl' =>
    [
        'local $Lang::Guess::EXTERNAL_QUERYREC = sub { return {eng => 1}; } if !is_production();'
    ],

    'protected/maintenance/mk_moderate_reasons.pl' =>
    [
        "Если ты знаешь, что делаешь, и тебе нужна разработческая конфигурация: запусти скрипт с ключом --force\" if !is_production() && !\$force;"
    ],

    'protected/maintenance/mk_targeting_categories.pl' =>
    [
        "Если ты знаешь, что делаешь, и тебе нужна разработческая конфигурация: запусти скрипт с ключом --force\" if !is_production() && !\$force;"
    ],

    'protected/maintenance/mk_content_genres.pl' =>
    [
        "Если ты знаешь, что делаешь, и тебе нужна разработческая конфигурация: запусти скрипт с ключом --force\" if !is_production() && !\$force;"
    ],

    'protected/maintenance/test_balance_catch_up.pl' =>
    [
        'if (is_production() || is_sandbox()) {'
    ],

    'protected/one-shot/reshard-user.pl' =>
    [
        '#die "not for production now" if is_production();'
    ],

    'protected/prebuild/update_networks_file.pl' =>
    [
        '                if (! is_production()) {'
    ],

    'api/services/v5/API/Version.pm' => [
         'use EnvTools qw/is_beta is_production/;',
         'our $is_production = is_production() ? 1 : 0;',
    ],
    'api/lib/API/PSGI/Base.pm' => [
        '    if (!$self->is_production()) {',
        q~    if (($auth->is_role_super || $auth->is_role_superreader) && $request->fake_login && !$self->is_production) {~,
        '        if !$service->is_available_on_production && $self->is_production();',
        '    my $prefer_perl = !$self->is_production && $request->prefer_perl_implementation ? 1 : 0;',
        '=head2 is_production',
        'sub is_production { $API::Version::is_production }'
    ],
    'api/services/v5/API/Service/AdImages.pm' => [
        '    my $hostname = $API::Version::is_production ? \'\' : $self->http_host;'
    ],
    'data3/desktop.blocks/b-modify-user/__general-settings/b-modify-user__general-settings.bemtree.js' =>
    [
        '                                disabled: data.is_production && !(data.user_is_any_client || data.user_role == \'agency\') || hasLoginRights(\'limited_support_control\') ? \'yes\' : \'\''
    ],
    'data3/desktop.blocks/p-campaigns/__tabs/p-campaigns__tabs.bemtree.js' =>
    [
        '            isProduction = data.is_production,'
    ],
    'data3/desktop.blocks/p-welcome/p-welcome.bemtree.js' =>
    [
        '                    isProduction: data.is_production,'
    ],
    'data3/desktop.blocks/p-touch-welcome/p-touch-welcome.bemtree.js' =>
    [
        '                    isProduction: data.is_production,'
    ],
    'protected/ppcProcessImageQueue.pl' =>
    [
            'use EnvTools qw/is_production/;',
            'if ($FAKE_CREATIVES && !is_production){',
    ],
    'protected/DoCmd/CheckBySchema.pm' =>
    [
        '    return $data if EnvTools::is_production();',
    ],
    'protected/one-shot/flush_test_mds_reports.pl' =>
    [
        'die "not for production now" if is_production();',
    ],
    'protected/SandboxCommon.pm' => [
        '    $available_type->{manager} = 1 if !is_production();',
        '        } elsif ($params->{type} eq \'manager\' && !is_production()) {'
    ],
    'protected/ADVQ6.pm' =>
    [
         'our $REQUESTS_LOG = is_production() ? 0 : 1;'
    ],
    'api/services/v5/API/Service/Changes.pm' =>
    [
         '    my $use_camp_aggregated_last_change = !$API::Version::is_production ? $self->get_http_request_header(\'useCampAggregatedLastChange\') : undef;'
    ],
    'protected/one-shot/add_products_for_cpm_deals.pl' =>
    [
        'if (        is_production() && $PROD ){',
        '} elsif (   is_production() && $TEST ) {',
        '} elsif ( ! is_production() && $PROD ) {',
        '} elsif ( ! is_production() && $TEST ) {'
    ],
    'protected/one-shot/add_products_for_internal_autobudget.pl' =>
    [
        'my $is_prod = is_production();',
    ],
    'protected/one-shot/add_products_for_cpm_video.pl' =>
    [
        'my $is_prod = is_production();',
    ],
    'protected/one-shot/add_products_for_cpm_outdoor.pl' =>
    [
        'my $is_prod = is_production();',
    ],
    'protected/one-shot/add_products_for_cpm_audio.pl' =>
    [
        'my $is_prod = is_production();',
    ],
    'protected/one-shot/add_products_for_cpm_indoor.pl' =>
     [
         'my $is_prod = is_production();',
     ],
    'protected/Plack/Middleware/ExtractCmdParamsPlainIntapi.pm' =>
    [
      '    my $prod = is_production();',
    ],
    'data3/desktop.blocks/i-utils/__dna/i-utils__dna.utils.js' =>
    [
        '                isProduction: data.is_production,'
    ],
    'data3/desktop.blocks/p-touch/p-touch.bemtree.js' =>
    [
        '                    isProduction: data.is_production,'
    ],
    'data3/desktop.blocks/i-rum-error-counter/i-rum-error-counter.bemtree.js' =>
    [
        '        data.is_production,'
    ],
    'data3/desktop.blocks/i-rum-timing/i-rum-timing.bemtree.js' =>
    [
        '        data.is_production,'
    ],
    'data3/desktop.blocks/b-campaign-info/__menu/b-campaign-info__menu.bemtree.js' =>
    [
        '            url: u.moderation.getCampaignModerationUrl(data.is_production, data.campaign.cid)',
        '            url: u.moderation.getCampaignDocumentsUrl(data.is_production, data.ClientID)',
    ],
    'data3/desktop.blocks/b-campaigns-list-item/__actions/b-campaigns-list-item__actions.bemtree.js' =>
    [
        '            url: u.moderation.getCampaignModerationUrl(data.is_production, camp.cid),'
    ]
);

# ищем все интересные файлы
my @files = grep {-f && /\.(p[ml]|tt2|js|xjst)$/ && !/SettingsALL/ && !/\Q$Settings::ROOT\/bin\/\E/ &&!/\Q$Settings::ROOT\/deploy\/archive\/\E/} Test::ListFiles->list_repository($Settings::ROOT);

is(scalar @SettingsALL_exceptions, 5, "too many exceptions");

my %FILE_ACTUALLY_CONTAIN_IS_PRODUCTION;
for my $file (@files) {
    my $file_rel = $file =~ s!^\Q$Settings::ROOT/\E!!r;
    my $text = read_file($file, binmode => ':utf8' );
    if ($file !~ /$SettingsALL_exceptions_regexp/) {
        ok($text !~ /SettingsALL/, "SettingsALL in $file");
    } else {
        SKIP: {
            skip("$file в списке исключений для SettingsALL", 1);
        };
    }

    if ($file !~ /back-to-back|RBACDirectOld|deploy\/20180212_fix_rbac_and_unservice_camps.pl|protected\/rbacCopyRolesToPpc.pl/) {
        ok($text !~ /RBACDirectOld/, "RBACDirectOld in $file");
    }

    my @matches = ($text =~ /(.*is_production.*)/ig);
    $FILE_ACTUALLY_CONTAIN_IS_PRODUCTION{$file_rel} = 1;
    if ( exists $IS_PRODUCTION_EXCEPTIONS{$file_rel} ){
        is_deeply(\@matches, $IS_PRODUCTION_EXCEPTIONS{$file_rel}, "unexpected amount of is_production in $file:\n".Dumper(\@matches)."\nexpected:\n".Dumper($IS_PRODUCTION_EXCEPTIONS{$file_rel}));
    } else {
        ok(scalar @matches == 0, "is_production $file, use is_beta instead:\n".Dumper(\@matches));
    }

    ok($text !~ /\$r->header_in\(/, "\$r->header_in() in $file, use \$r->headers_in->get() instead");
    ok(($text !~ /\$r->header_out\(/), "\$r->header_out() in $file, use \$r->headers_out->set() instead");
    # надо как-то поменять, для plack-реквестов такая конструкция годится
    ok($text !~ /\$r->header\b/, "\$r->header in $file, use \$r->content_type() instead");
    ok($text !~ /\$r->send_http_header\b/, "\$r->send_http_header in $file, is not needed in 2.0 + \$r->content_type(...)");

    ok($text !~ /Data::Printer/, "Data::Printer in $file");
    ok($text !~ /use\s+DDP/, "DDP in $file");
    ok($text !~ /use\s+E\b/, "E in $file");

    if ( $file_rel =~ $Parallel_ForkManager_exceptions_regexp ){
        # если падает этот тест -- надо удалить лишнее исключение
        # проверяем содержательность исключений только если файл отловлен по полному имени, не по префиксу-каталогу
        if ( $file_rel =~ /^$Parallel_ForkManager_exceptions_regexp$/ ){
            ok($text =~ /use\s+Parallel::ForkManager\b/, "useless exception: Parallel::ForkManager in $file");
        }
    } else {
        # если падает этот тест -- надо переделать скрипт, чтобы не использовал Parallel::ForkManager; в крайнем случае -- добавить исключение
        ok($text !~ /use\s+Parallel::ForkManager\b/, "Parallel::ForkManager in $file");
    }
}

# проверяем, что не осталось бесполезных исключений про is_production
for my $file ( sort keys %IS_PRODUCTION_EXCEPTIONS ){
    ok($FILE_ACTUALLY_CONTAIN_IS_PRODUCTION{$file}, "(ir)relevant is_production exception for '$file'");
}

done_testing;
