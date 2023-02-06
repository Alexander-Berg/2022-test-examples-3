#!/usr/bin/perl

# $Id$

=pod

    Нечеловекочитаемые файлы должны иметь подходящее svn-свойство svn:mime-type (например, 'application/octet-stream')
    Файл считается нечеловекочитаемым, если имеет расширение rt/pdf/doc/..., или если в нем встречаются строки длиннее $TOO_LONG_LINE

    Установить свойство:
    svn propset svn:mime-type 'application/octet-stream' <my_file>

=cut

use warnings;
use strict;

use Test::More;

use XML::LibXML;
use File::Slurp;

use Test::ListFiles;

use Settings;

#..........................................................................................................
my $ROOT = $Settings::ROOT;

my $TOO_LONG_LINE = 500;
my @binary_formats = qw/ pdf rtf doc /;

# Список исключений. В идеале пополняться не должен.
my @EXCEPTIONS = <DATA>;
s/\s*//g for @EXCEPTIONS;
@EXCEPTIONS = grep {$_} @EXCEPTIONS;

my $CORRECT_NON_HUMAN_READABLE_TYPE_REGEXP = join '|', (
    'application/octet-stream',
    'application/pdf',
    'application/rtf',
);

my $binary_extensions_regexp = "(" . join('|', @binary_formats).")";

#..........................................................................................................

my $exceptions_regexp = "^$ROOT/(?:".join("|", @EXCEPTIONS).")\$";

#..........................................................................................................

my @files_to_check;
for my $file (grep {-f } Test::ListFiles->list_repository($ROOT)) {
    next if $file =~ m!$exceptions_regexp!;
    push @files_to_check, $file;
}

#..........................................................................................................

Test::More::plan(tests => scalar(@files_to_check));

my $xml = `svn propget -R --xml svn:mime-type $ROOT 2>&1`;

SKIP: {
if ($xml =~ /not a working copy/) {
    skip "only svn working copy is supported", scalar(@files_to_check);
}

my $doc = XML::LibXML->new()->parse_string($xml)->documentElement();
my @targets = $doc->findnodes("/properties/target");

my %TYPE;
for my $t (@targets) {
    my $path = $t->getAttribute('path');
    my $value = $t->findvalue("./property/text()");

    $TYPE{$path} = $value;
}

#..........................................................................................................

for my $file (@files_to_check){

    my $has_binary_mime_type = ($TYPE{$file} || '') =~ $CORRECT_NON_HUMAN_READABLE_TYPE_REGEXP ? 1 : 0;;

    my @problems;

    # расширение файла указывает на нечеловекочитаемый формат
    push @problems, "'binary' extension" if !$has_binary_mime_type && $file =~ /\.$binary_extensions_regexp$/;

    if (!$has_binary_mime_type){
        my $text = read_file($file, binmode => ':utf8');
        # в файле слишком длинные строки -- нечеловекочитаемо
        push @problems , "too_long_lines" if $text =~ /[^\n]{$TOO_LONG_LINE,}/;
    }

    my $problems_text = join ", ", @problems;
    ok($problems_text eq '', "file $file seems to be non-human readable ($problems_text), but has no proper svn:mime-type property (expected $CORRECT_NON_HUMAN_READABLE_TYPE_REGEXP)");
}

}

__DATA__
cmd-schema/output/global-vars.schema.json
data3/.*
db_schema/ppc/feeds.schema.sql
db_schema/ppc/adgroup_additional_targetings.schema.sql
db_schema/ppc/clients_options.schema.sql
db_schema/ppcdict/ess_logic_objects_blacklist.schema.sql
db_schema/ppc/hierarchical_multipliers.schema.sql
protected/geo_regions.pm
locale/direct.pot
locale/emails.json
data/css/jquery-ui/jquery-ui-1.7.3.custom.css
data/pdf_reports/yareport05.tex
data/pdf_reports/english_pdf_appendix_for_reports.tex
data/welcome.html
data/tests/functional/css/_global_reset.css
data/tests/functional/Campaigns/Search.html
data/tests/functional/languages/www.js
data/tests/Direct/SpellChecker.html
data/js/jq/direct/theme.js
data/block/b-advanced-forecast/calculated-expense/b-advanced-forecast__calculated-expense.tt2
data/block/b-comment-form-template/_mode/b-comment-form-template_mode_optimization.tt2
data/block/b-menu/_theme/b-menu_theme_shadow.css
data/block/b-banner-pic/b-banner-pic.tt2
data/lego/blocks/.*
data/t/get_ya_category_prices.html
data/t/order_campaign_optimizing.html
data/t/media_show_stat.html
data/t/campaigns_stat_clients.html
data/t/campaign_stat_pages_print.html
data/t/list_optimize.html
data/t/welcome.html
data/t/copy_campaign.html
data/errors/500.html
data/.+?\.i18n\.html
deploy/.*
protected/data/translation/moderate_reasons.trans
protected/data/translation/moderate_reasons.new.trans
data/t/m_contact_info.html
data/js/clite.js
data/css/_yandex-global.css
data/block/b-banner-form/text/b-banner-form__text.js
data/block/i-translation/i-translation_lang_tr.js
data/block/i-translation/i-translation_lang_ua.js
data/block/i-translation/i-translation_lang_en.js
data/block/i-translation/i-translation.tt2
data/t/emails/ua/second_aid_result_to_client_repeat
etc/frontend/nginx/direct-accel.conf
protected/maintenance/decode_direct_db_timetarget.pl
protected/QualityScore.pm
unit_tests/SentryTools/_calc_fingerprint.t
unit_tests/translations/validate_emails_exceptions.json
unit_tests/translations/validate_pos_exceptions.json
protected/data/translation/adv-network-renaming.trans
locale/en_US.po
locale/tr_TR.po
locale/uk_UA.po
etc/deploy/clusrc.d/yandex-direct
clh_schema/.*
protected/one-shot/20190719_insert_new_intersts_into_crypta_goals.pl
protected/one-shot/migr_content_video_to_new_schema.sh
etc/frontend/nginx/direct-vhost.conf
etc/intapi/nginx/intapi-direct-vhost.conf
db_schema/ppc/campaigns.schema.sql
db_schema/ppc/strategies.schema.sql
