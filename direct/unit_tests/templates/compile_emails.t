#!/usr/bin/perl

=head1 DESCRIPTION

     Проверка, что компилируются русские оригиналы писем и их переводы

=cut

use strict;
use warnings;

use Test::More;

use Yandex::MailTemplate;
use Yandex::I18n;
use Yandex::I18nTools;
use Yandex::Test::UTF8Builder;

use Settings;
use MailService;

use utf8;

use File::Temp;
use File::Copy;

# Пожалуйста, не записывайте исключение в одну строку,
# т.к. оно всегда конфликтует при мерже
my %KNOWN_BAD_TR_TEMPLATES = map {$_ => 1} qw(
    ya_agency_order_paid
    active_orders_money_out_touch
    active_orders_money_out_touch_sms
    active_orders_money_warning_touch
    active_orders_money_warning_touch_sms
    moderate_result_touch
    moderate_result_touch_sms
);

my $default_lang = Yandex::I18n::default_lang();
my @langs = ($default_lang, Yandex::I18n::get_other_langs());

# для проверки на компиляцию шаблонов писем на всех языках
# создаем временную директорию в /tmp/compile_emails<XXXXX>
my $temp_dir = File::Temp->newdir("compile_emailsXXXXX", TMPDIR => 1);

# папка с шаблонами языка по умолчанию
my $source_dir = $Yandex::MailTemplate::EMAIL_TEMPLATES_FOLDER.'/'.$default_lang;
# временная папка шаблонов языка по умолчанию
my $target_dir = $temp_dir.'/'.$default_lang;

local $Yandex::MailTemplate::EMAIL_TEMPLATES_FOLDER = $temp_dir;

# создаем поддиректории для всех языков
for my $lang_dir (@langs){
    mkdir ($Yandex::MailTemplate::EMAIL_TEMPLATES_FOLDER.'/'.$lang_dir);
}

# копируем шаблоны для языка по умолчанию во временную директорию.
opendir(my $DIR, $source_dir) || die "can't opendir $source_dir: $!";
my @files = readdir($DIR);

for my $t (@files)
{
   if(-f "$source_dir/$t" ) {
      copy "$source_dir/$t", "$target_dir/$t";
   }
}

closedir($DIR);

# собираем шаблоны на других языках
Yandex::I18nTools::update_emails();

# сколько непустых шаблонов должно быть для каждого языка
my $MIN_TEMPLATE_CNT_FOR_LANG = 15;

my $MIN_LANG_COUNT = 4;

cmp_ok(scalar(@langs), '>=', $MIN_LANG_COUNT, "Language count");

my @email_template_names = grep {! m/^i_/} get_email_template_list_names();
my %templates_by_lang;
for my $lang (@langs) {
    my $template_for_lang_cnt = 0;
    for my $email_template_name (@email_template_names) {
        my $result = check_email_template($email_template_name, $lang);
        if ($result eq 'no template') {
            if ($lang ne $default_lang) {
                # отсутствие перевода для шаблонов на других языках проблемой пока не считаем
                # вместо них будут использоваться английский или русский варианты
                $result = 'ok';
            }
        } else {
            $template_for_lang_cnt++;
            $templates_by_lang{$lang}{$email_template_name} = 1;
        }
        is($result, 'ok', "Checking $email_template_name for language $lang");
    }
    cmp_ok($template_for_lang_cnt, '>=', $MIN_TEMPLATE_CNT_FOR_LANG, "Non-empty template count for language $lang");
}

# для турецкого языка фоллбек есть только в английский. если английского нет, будем падать.
for my $email_template_name (@email_template_names) {
    my $template_exists = $templates_by_lang{tr}{$email_template_name} || $templates_by_lang{en}{$email_template_name};
    if ($KNOWN_BAD_TR_TEMPLATES{$email_template_name}) {
        ok(!$template_exists, "Шаблон $email_template_name из списка плохих должен быть плохим");
    } else {
        ok($template_exists, "Есть шаблон $email_template_name для турецкого (или есть английский для фоллбека). Любая попытка отправить турецкое письмо с таким шаблоном будет падать, роняя _весь_ отправляющий его скрипт. Дойди до ответственного менеджера или релиз-инженера и убедись, что к моменту релиза перевод появится.");
    }
}

done_testing;

# --------------------------------------------------------------------
sub compile_tt
{
    my ($template_text, $lang) = @_;

    my $template = MailService::_get_tt_object($lang);

    eval {
        $template->template(\$template_text) || die $template->error();
    };

    return $@;
}

# --------------------------------------------------------------------
sub check_email_template {
    my ($email_template_name, $lang) = @_;

    my $email = eval { get_email_template($email_template_name, $lang, 1) };
    if ($email) {
        for my $field (qw/name subject content description lang/) {
            return "parse failed" unless defined $email->{$field};

            my $err = compile_tt($email->{$field}, $lang);
            return "$field compiling failed: $err" if $err;
        }
    } elsif ($@) {
        return "get_email_template error: $@";
    } else {
        return 'no template';
    }

    return 'ok';
}
