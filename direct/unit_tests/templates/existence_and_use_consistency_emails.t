#!/usr/bin/perl

=pod

     $Id: $
     Проверка консистентности существования и использования email-шаблонов

=cut

use warnings;
use strict;

use File::Slurp;
use File::Basename qw/basename/;
use Test::More;
use Test::ListFiles;
use Yandex::HashUtils;
use Yandex::MailTemplate;

use Settings;
use Yandex::I18n;

use utf8;
use open ':std' => ':utf8';

my $TMPL_ROOT = $Yandex::MailTemplate::EMAIL_TEMPLATES_FOLDER;
my $PERL_ROOT = $Settings::ROOT;
my $main_lang = 'ru';

# Какие файлы считаем perl-кодом (регвыр на окончание имени файла)
my @PERL_FILE_REGEXP = qw/
    \.pm 
    \.pl 
/;

# Дополнительные испольуемые шаблоны, которые проблемно извлечь из кода.
my @ADDITIONAL_USED_TEMPLATES = (
    'first_aid_request_declined_to_client',
    'first_aid_request_to_client',
    'second_aid_request_declined_to_client',
    'second_aid_request_to_client',
    'first_aid_result_to_client_vip_repeat',
    'first_aid_result_to_client_repeat',
    'first_aid_result_to_client_vip',
    'first_aid_result_to_client',
    'media_client_request',
    'media_request', 
);

my %SKIP;
# шаблоны которые задаются не в вызове send_prepared_*
$SKIP{$_} = 1 for qw/
    active_orders_money_out
    active_orders_money_out_with_auto_overdraft
    active_orders_money_out_reminder
    active_orders_money_out_reminder_sms
    active_orders_money_out_sms
    active_orders_money_out_touch_sms
    active_orders_money_out_campaign_stopped_sms
    active_orders_money_out_with_auto_overdraft_sms
    active_orders_money_warning
    easy_active_orders_money_out
    easy_active_orders_money_out_sms
    easy_active_orders_money_warning
    autopay_error_not_enough_funds
    autopay_error_not_enough_funds_sms
    autopay_error_expired_card
    autopay_error_expired_card_sms
    autopay_error_other
    autopay_error_other_sms
    i_footer
    paused_by_day_budget_wallet
    paused_by_day_budget_wallet_sms
/;
# последние два шаблона закоммичены в транк заранее, для перевода. начнут использоваться с задачей DIRECT-64423

my $PERL_FILE_PATTERN = "(".(join '|', @PERL_FILE_REGEXP).")";

# смотрим, какие файлы шаблонов существуют
my %FILE_EXISTS;
for my $file (grep {-f} Test::ListFiles->list_repository("$TMPL_ROOT/$main_lang")) {
    $FILE_EXISTS{basename($file)} = 1;
}

# составляем список perl-файлов
my @perl_files = grep {-f && /$PERL_FILE_PATTERN$/ && $_ !~ m!/deploy/archive/! } Test::ListFiles->list_repository($PERL_ROOT);

# USED_FROM_PERL: Список шаблонов, которые используются в perl-коде
# (имя шаблона => 1 )

my %USED_FROM_PERL;
for my $filename_full (@perl_files){
    my $tt_text = read_file($filename_full);

    # выбираем все, похожее на имена шаблонов
    hash_merge \%USED_FROM_PERL
        , {map {$_ => 1} $tt_text =~ /send_prepared_(?:mail|sms)\s*\(\s*[\'\"](\w+?)[\'\"]\s*,/gs}
        , {map {$_ => 1} $tt_text =~ /send_(?:msg|sms)[^;)]*?template\s*=>\s*[\'\"](\w+?)[\'\"]/gs}
        , {map {$_ => 1} $tt_text =~ /notification_api_finance\((?:[\$\w,\s>{}\-]+')?([\w_]+)'(?:[\$\w,\s>{}\-]*)\)/gs}
        , {map {$_ => 1} $tt_text =~ /_compile_mail_from_template\s*\(\s*[\'\"](\w+?)[\'\"]\s*,/gs}
    ;
}

for (@ADDITIONAL_USED_TEMPLATES) {
    $USED_FROM_PERL{$_} = 1;
}

my %LANGUAGES_FILES;
my $other_lang_files_count = 0;
for my $lang (Yandex::I18n::get_other_langs()) {
    my @files = (grep {-f} Test::ListFiles->list_repository("$TMPL_ROOT/$lang"));
    if (scalar(@files)) {
        $LANGUAGES_FILES{$lang} = \@files;
        # Считаем количество файлов шаблонов в других языках, чтобы сначала добавить это количество в число тестов, 
        # а затем провести тест на то, что нет лишних шаблонов (шаблонов, которые бы были на другом языке, но отсутствовали на русском)
        $other_lang_files_count += scalar(@files);
    }

}
Test::More::plan(tests => scalar(keys %FILE_EXISTS) + scalar(keys %USED_FROM_PERL)+$other_lang_files_count);

# Проверка, что все шаблоны используются
for my $template (sort keys(%FILE_EXISTS)){
  SKIP: {
      skip("Temporary skipped", 1) if $SKIP{$template};
      ok((exists $USED_FROM_PERL{$template}), "\"$template\" template exists in the repository but is not used");
    }
}

# Проверка, что все используемые шаблоны существуют
for my $used_file (sort keys(%USED_FROM_PERL)){
    ok(exists $FILE_EXISTS{$used_file}, "\"$used_file\" is used but does not exists in the repository");
}

# Проверяем, что в других языках нет шаблона, который бы отсутствовал в русском языке.

for my $lang(sort keys (%LANGUAGES_FILES)) {
    for my $file (sort @{$LANGUAGES_FILES{$lang}}) {
        my $basename = basename($file);
        ok($FILE_EXISTS{$basename}, "'$basename' template exists in '$lang' language folder, but does not exists in 'ru' language");
    }
}
