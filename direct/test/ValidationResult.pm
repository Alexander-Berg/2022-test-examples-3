package Yandex::Test::ValidationResult;
## no critic (TestingAndDebugging::RequireUseStrict, TestingAndDebugging::RequireUseWarnings)

use Exporter;
our @EXPORT = qw/
    vr_errors
    cmp_validation_result
    ok_validation_result
/;

=head1 NAME
    
    Yandex::Test::ValidationResult - модуль для проверки результата валидации массива объектов (Direct::ValidationResult)  
    
=head1 SYNOPSIS

    use Test::More;
    use Yandex::Test::ValidationResult;
    use Direct::Validation::Keywords;
    use Direct::Validation::MinusWords;
    
    
    my $validation_result = validate_add_keywords([
        {phrase => 'бесперебойный источник питания -обзор  -промышленный  -тест  -тестирование  -реферат  -форум'},
        {phrase => 'Sony Ericsson CAR100'},
        {phrase => 'iRiver 395T'},
        {phrase => 'iFP-590T'},
    ]);
    ok_validation_result($validation_result); # all phrases are valid
    
    cmp_validation_result(
        validate_add_keywords([
            {phrase => "-оборудование -бизнес -план -куплю -открыть -продается"},
            {phrase => '-а0 -а1 -а2'}
        ]),
        [
            {phrase => vr_errors(qr/Ключевая фраза не может состоять только из минус\-слов/, qr/Ключевая фраза не может состоять только из стоп\-слов/)},
            {phrase => vr_errors('MinusWords', 'StopWords')},
        ]
    );
    
    my $validation_result_2 = validate_keyword_minus_words(["металлический","металлопрофиль","мерб#;ау!"]);
    cmp_validation_result($validation_result_2, {
        generic_errors => vr_errors(qr/Минус-слово должно состоять из букв .* Ошибки в словах: мерб\#;ау\!/)
    });
    
    done_testing;
    
=head1 DESCRIPTION
    
    Формат описания ожидаемой структуры результата валидации для cmp_validation_result следующий.

    Все ожидаемые ошибки проверяемого объекта нужно задавать через функцию vr_errros(@expected_errors).
    @expected_errors может содержать в себе либо строку, либо регулярное выражение qr//.
    
    Для строки проверяется соответствие имени ошибки заданному значению

        $error->name eq $expected
        
    Для регулярного выражение проверяется соответствие текстового описания ошибки шаблону
    
        $error->text =~ qr/expected pattern/
        
    Порядок следования ошибок в объекте результат валидации не учитывается. Т.е. записи эквивалентны
        
        vr_errors('BadLang', 'ReqField', qr/Не корректное использование/) == vr_errors('BadLang', qr/Не корректное использование/, 'ReqField')
        
    C регулярными выражениями нужно быть внимательным, т.к. одно регулярное выражение может соответствовать нескольким текстам ошибки.
    
    Общий формат ожидаемой структуры ошибок:
    
    {
        # перечисление ошибок на весь результат проверки  
        generic_errors => vr_errros(),
        # список проверок на каждый объект в отдельности
        objects_results => [
            {
                # список ошибок на объект в целом
                generic_errors => vr_errors(), 
                # список ошибок относящихся к конкретному имени поля
                field_name_1 => vr_errors(),
                field_name_2 => vr_errors(),
                ....
            },
            {}, # объект проверки не содержит ошибок и варнингов (т.е. валидный)
            {}
        ]
    }
    
    Если generic_errors в проверяемом результате не ожидается, можно задать список проверок объектов следующим образом:
    
    [
        {
            # список ошибок на объект в целом
            generic_errors => vr_errors(), 
            # список ошибок относящихся к конкретному имени поля
            field_name_1 => vr_errors(),
            field_name_2 => vr_errors(),
            ....
        },
        {}, # объект проверки не содержит ошибок и варнингов (т.е. валидный)
        {}
    ]
    
    Для сложных объектов проверки можно делать произвольный уровень вложенности.
    
    # один объект Direct::ValidationResult
    [
        {}, # валидный объект
        {
            # простой объект проверки
            field_name_1 => vr_errors(...),
            
            # сложный объект проверки, другой объект Direct::ValidationResult (2-ой)
            field_name_2 => {
                common_errors => vr_errors(...),
                objects_errors => [
                    ....
                ]
            }
            
            # сложный объект проверки, другой объект Direct::ValidationResult (3-ий)
            field_name_3 => [
                {},
                {},
                {field_name => vr_errors(....)}
            ]
        }
    ]
             
    
=head1 FUNCTIONS    

=cut

use strict;
use warnings;
use utf8;

use base qw/Test::Builder::Module/;

use Test::Deep::NoTest qw/:all/;
use List::MoreUtils;

=head2 cmp_validation_result($got, $expected, $test_name)

    Проверка результата валидации массива объектов на соответствие заданной структуре

    Параметры:
        $got - экземпляр класса Direct::ValidationResult который необходимо проверить
        $expected - описание ожидаемой структуры результата валидации
        $test_name - имя теста  

=cut

sub cmp_validation_result {
    my ($got_validation_result, $expected_validation_result, $test_name) = @_;

    my $got = $got_validation_result->convert_to_hash;
    my $expected = _expand_sample($expected_validation_result);
    my ($ok, $stack) = cmp_details($got, $expected);
    my $test = Yandex::Test::ValidationResult->builder;
    unless ($test->ok($ok, $test_name)) {
        $test->diag(deep_diag($stack));
    }
}

=head2 ok_validation_result($ok, $test_name)

    Проверка валидности результата валидации массива объектов.
    Т.е. результат проверки не содержит ошибки (но при этом может содержать варнинги)

    Параметры:
        $got - экземпляр класса Direct::ValidationResult который необходимо проверить
        $test_name - имя теста  
    
=cut

sub ok_validation_result {
    
    my ($got_validation_result, $test_name) = @_;
    
    Yandex::Test::ValidationResult->builder->ok($got_validation_result->is_valid, $test_name);
}

=head2 eq_validation_result($got_validation_result, $expected_validation_result)

    Сопоставление двух vr. Описание см в. SYNOPSIS

=cut

sub eq_validation_result {
    
    my ($got_validation_result, $expected_validation_result) = @_;
    
    my $expected = _expand_sample($expected_validation_result);
    return eq_deeply($got_validation_result->convert_to_hash, $expected);
}

=head2 vr_errors(@errors)

    Задаёт массив ошибок, которые должен содержать проверяемый объект

    Параметры:
        @errors - массив ошибок которые должен содержать проверяемый объект (порядок следования ошибок не учитывается)
                    элемент массива либо строка, либо регулярное выражение qr//
                    строка - в этом случае будет проверено название ошибки или варнинга($error->name) на равенство заданной строке
                    регулярное выражение - будет проверено соответствие текстового описания ошибки заданному регулярному выражению
=cut

sub vr_errors {

    my @tests = @_;
    return bless [@tests], 'Yandex::Test::ValidationResult::Errors';
}

sub _expand_sample {
    
    my ($sample, $inside_objects) = @_;

    my $ref_sample = ref $sample;
    if ($ref_sample eq 'ARRAY') {
        if ($inside_objects) {
            return [map {_expand_sample($_)} @$sample];
        }
        return {
            objects_results => [map {_expand_sample($_)} @$sample]
        }
    } elsif ($ref_sample eq 'HASH') {
        my %expanded_sample;
        my $is_objects = List::MoreUtils::any {$_ eq 'objects_results'} keys %$sample;
        while (my ($field, $errors) = each %$sample) {
            $expanded_sample{$field} = _expand_sample($errors, $is_objects);
        }
        return \%expanded_sample;
    } elsif ($ref_sample eq 'Yandex::Test::ValidationResult::Errors') {
        return bag(map {
            _expand_sample($_)
        } @$sample);
    } elsif ($ref_sample eq 'Regexp') {
        return superhashof({description => re($sample)});
    } elsif ($ref_sample eq '') {
        return superhashof({name => $sample})
    } else {
        die "unhadle ref $ref_sample";
    }
}

1;
