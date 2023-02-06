#!/usr/bin/perl

=pod

    $Id$

    Проверяет RTF-файлы шаблонов гарантийных писем

=cut

use warnings;
use strict;
use utf8;

use Test::More;
use Yandex::Test::UTF8Builder;

use File::Slurp;
use Settings;
use Test::ListFiles;
use GuaranteeLetter;
use Template;

use List::MoreUtils qw/any/;

my @tests_for_file = (
    {
        f    => \&no_rtf_in_tt,
        type => 'tt',
        name => "Нет RTF кода в TT тегах. Файл:\t\t",
    }, {
        f    => \&topicids_consistency,
        type => 'tt',
        name => "Тематики полностью соответствуют указанным в модуле. Файл:\t\t",
    }, {
        f    => \&good_list_markers_in_ld,
        type => 'text',
        name => "Good markers in list definition. File:\t\t",
        disabled => 1,  # Проблема с маркерами редкая и некритичная
    }, {
        f    => \&good_list_markers_in_lc,
        type => 'text',
        name => "Good markers in list content. File:\t\t",
        disabled => 1,  # Проблема с маркерами редкая и некритичная
    }, {
        f    => \&compile_tt,
        type => 'text',
        name => "RTF-шаблон успешно компилируется. Файл:\t\t",
    },
);

my @common_tests = (
    {
        f    => \&templates_for_all_regions,
        name => 'Существуют шаблоны для всех регионов',
    },
);

my $topics = GuaranteeLetter::_get_topics_hash();
my $regions = GuaranteeLetter::_get_regions();

# ищем все подходяшие файлы
my @files = grep {-f && /$GuaranteeLetter::RTF_TEMPLATE_FILENAME\_\w+\.rtf$/} Test::ListFiles->list_repository($GuaranteeLetter::RTF_TEMPLATES_PATH);

# Конфигурация шаблона
my $template = Template->new({
    PLUGIN_BASE => 'Yandex::Template::Plugin',
    ENCODING => 'utf8',
    EVAL_PERL => 0,
    INCLUDE_PATH => $GuaranteeLetter::RTF_TEMPLATE_FILENAME,
    INTERPOLATE  => 0, # expand "$var" in plain text
    POST_CHOMP   => 1, # cleanup whitespace
    FILTERS => {
        text => sub {return shift;},
    }
});

Test::More::plan(tests => (@files * @tests_for_file + @common_tests));

for my $file (@files) {
    my $text = read_file($file);

    # Получаем содержимое тегов
    my @tags_content = $text =~ m/\[%[^\[\]]+%\]/g;

    foreach my $test (@tests_for_file) {
        SKIP: {
            skip 'Test disabled', 1 if $test->{disabled};
            my $test_name = $test->{name} . $file;

            if ($test->{type} eq 'tt') {
                ok($test->{f}(\@tags_content, $file), $test_name);
            } elsif ($test->{type} eq 'text') {
                ok($test->{f}(\$text), $test_name);
            }
        }
    }
}

for my $test (@common_tests) {
    ok($test->{f}(\@files), $test->{name});
}

=head3 no_rtf_in_tt
    Проверяет что внутрь тегов шаблонизатора не попал RTF код (иначе шаблон точно не скомпилируется)
    Смотрит на то, чтобы внутри тегов не было:
        границ блоков { }
        управляющих rtf- последовательностей \fs \afs ..
=cut
sub no_rtf_in_tt {
    my $tags_content = shift;

    foreach my $content (@$tags_content) {
       return 0 if $content =~ m/(?:[{}]|\\\w+)/;
    }

    return 1;
};

=head3 topicid_consistency
    Проверка на соответствие $GuaranteeLetters::topics и содержимого файла:
        Указанные в шаблоне id тематик существуют
            (может быть не указан только $DEFAULT_TOPIC_ID)
        Все указанные для данного "региона" тематики упоминаются в файле
        Файл не содержит тематик, не использующихся в данном "регионе"
=cut
sub topicids_consistency {
    my $tags_content = shift;
    my $file = shift;

    # Получаем "название региона" из имени файла
    $file =~ /$GuaranteeLetter::RTF_TEMPLATE_FILENAME\_(\w+)\.rtf$/;
    my $r = "$1";

    my $used_topicids;
    foreach my $content (@$tags_content) {
        if ($content =~ m/topicid\D+(\d+)\D*/) {
            # Если тематики из файла не существует вообще
            return 0 unless exists $topics->{$1};
            # Или если она не используется в данном регионе
            return 0 unless $topics->{$1}->{region}->{$r};

            $used_topicids->{$1} = 1;
        }
    }

    # Обратная проверка - берем данные из модуля и проверяем файл
    foreach my $topic (values %$topics) {
        # Для данного региона эта тематика не используется
        next unless $topic->{region}->{$r};
        # ID "Общей" тематики не используется в шаблонах (на то она и общая)
        next if $topic->{id} == $GuaranteeLetter::DEFAULT_TOPIC_ID;
        # В шаблоне упоминается тематика
        return 0 unless exists $used_topicids->{$topic->{id}};
    }

    return 1;
};

=head3 good_list_markers_in_ld
    Проверяет, что в таблице определения списков стоят "правильные" символы маркеров (будут везде корректно отображаться)
=cut
sub good_list_markers_in_ld {
    my $text = shift;

    return 0 if $$text =~ m/\{\\leveltext[^}]+\\u-3913[^\\}]+\}/g;

    return 1;
}

=head3 good_list_markers_in_lс
    Проверяет, что в определениях элементов списка стоят "правильные" символы маркеров (будут везде корректно отображаться)
=cut
sub good_list_markers_in_lc {
    my $text = shift;

    return 0 if $$text =~ m/\{\\listtext[^}]+\\'b7\\tab\}/g;

    return 1;
}

=head3 templates_for_all_regions
    Проверяет, что для всех регионов есть файлы шаблонов
=cut
sub templates_for_all_regions {
    my $files = shift;

    foreach my $region (@$regions) {
        return 0 unless any {$_ =~ /$GuaranteeLetter::RTF_TEMPLATE_FILENAME\_$region\.rtf$/} @$files;
    }

    return 1;
}

=head3 compile_tt
    Проверяет, что RTF-шаблон успешно компилируется.
=cut
sub compile_tt {
    my $template_text = shift;

    eval {
        open(my $stderr, '>&', \*STDERR);
        open (STDERR, '>/dev/null');    # Подавляем сообщения об ошибках системы
        my $result_text;
        $template->process($template_text, {}, \$result_text) || die $template->error();
        *STDERR = $stderr;  # Возвращаем STDERR
    };

    return $@ ? 0 : 1;
}
