#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

BEGIN { use_ok('MinusWords'); }

use open ':std' => ':utf8';

use Settings;

subtest 'check_minus_words' => sub {
    my $f = \&MinusWords::check_minus_words;

    ok((sub {
        my $n = ($Settings::CAMPAIGN_MINUS_WORDS_LIMIT / 5) + 1;
        my $x = $f->([map {"слово$_"} (0..$n)], type => 'campaign');
        return @$x == 1 && $x->[0] eq "Длина минус-фраз превышает $Settings::CAMPAIGN_MINUS_WORDS_LIMIT символов.";
    })->());

    ok((sub {
        my $n = ($Settings::GROUP_MINUS_WORDS_LIMIT / 5) + 1;
        my $x = $f->([map {"слово$_"} (1..$n)], type => 'group');
        return @$x == 1 && $x->[0] eq "Длина минус-фраз превышает $Settings::GROUP_MINUS_WORDS_LIMIT символов.";
    })->());

    ok((sub {
        my $x = $f->([qw/минус &* слово/], type => 'campaign');
        return @$x == 1 && $x->[0] =~ /\QВ минус-фразах разрешается использовать только буквы английского, турецкого, казахского, русского, украинского или белорусского алфавита, кавычки, квадратные скобки, знаки "-", "+", "!", пробел. Ошибки во фразах: &*\E/;
    })->());

    ok((sub {
        my $x = $f->(['минус[слово]'], type => 'campaign');
        return @$x == 0;
    })->());

    
    my $errors = $f->([qw/беларусь белоруссия белорусский века велюкс венге веранда выбрать германия германский готовый дача дачный деревопластиковый/]);
    ok(@$errors == 0);
    
    $errors = $f->(['2011', '24700', 'hobbit', 'kbe', 'rehau', 'ukko', 'veka velux', 'алюминиевый алюмо', 'английский балкон', ';тиковый']);
    ok(@$errors == 1);
    ok($errors->[0] =~ /\QВ минус-фразах разрешается использовать только буквы\E/);
};

subtest 'polish_minus_words' => sub {
    my $f = \&MinusWords::polish_minus_words;

    is($f->(undef, return_if_undef => 1), undef, 'Return if undef');
    is($f->(''), '', 'Empty string');

    is($f->("кредит авто -авто, москва - питер"), "авто кредит москва питер", 'Simple text');
    is($f->("кредит авто -авто, москва - питер", add_minus => 1), "-авто -кредит -москва -питер", 'Simple text with add_minus');
    is($f->("кредит авто -авто, москва - питер", sort => 0), "кредит авто москва питер", 'Simple text without sort');
    is($f->("кредит авто -авто, москва - питер", sort => 0, uniq_norm => 0), "кредит авто авто москва питер", 'Simple text without sort and uniq_norm');

    is($f->("ипотека ипотекой кредит кредит", sort => 0), "ипотека кредит", 'Simple text with default uniq_norm');
    is($f->("ипотека ипотекой кредит кредит", sort => 0, uniq => 1), "ипотека ипотекой кредит", 'Simple text with uniq only');

    is($f->("из москвы в питер", sort => 0), "!из москвы !в питер", 'Текст с предполгами и союзами');

    is($f->("ананас !ананас"), "!ананас ананас", "Repeat words with special symbol");
    is($f->("!ананас +апельсин -банан", add_minus => 1), "-!ананас -+апельсин -банан", "Words with special symbol");

    is($f->("Рюкзак;палатка.спальник,коврик!еда - всё что надо для похода! да  ! ", sort => 0), "Рюкзак палатка спальник коврик !еда !всё !что надо !для похода !да", "Non allowed symbols in the string");
    is($f->('%^(&*<%&,$.@>"^^%/$|#\}@{@^$+*\'\%&  =  (-%&)*^ ~@`&^ ^&*(^ $&%^^#$^&*?'), "", "Non allowed symbols only");

    is($f->("температура 36.6", add_minus => 1, sort => 0), "-температура -36.6", "Words with numbers and dots like num.num");
    is($f->("-+город.герой -!прямо1.2брюхо. -.355.6. -!55.88.77.", add_minus => 1, sort => 0), "-+город -герой -!прямо1 -2брюхо -355.6 -!55.88.77", "Words with numbers and letters and dots like let.num");

    is($f->("!!+ авто +дорога.!питер .545..234...6654. 1.. ..24 .."), "!питер +дорога 1 24 545.234.6654 авто");
};

subtest 'polish_minus_words_array' => sub {
    my $f = \&MinusWords::polish_minus_words_array;
    is_deeply($f->([]), [], 'Empty array');
    is_deeply($f->(["кредит", "авто", "-авто,", "москва", "-", "питер"]), ["авто", "кредит","москва", "питер"], 'Simple text 1');
    is_deeply($f->(["кредит", "авто", "-авто, москва", "- питер"]), ["авто", "авто москва", "кредит", "питер"],'Simple text 2');
    is_deeply($f->(["кредит", "авто", "-авто,", "москва", "-", "питер"]), ["авто", "кредит", "москва", "питер"], 'Simple text 3');
    
    is_deeply($f->(["ананас", "!ананас"]), ["!ананас", "ананас"], "Repeat words with special symbol");
    is_deeply($f->(["!ананас", "+апельсин", "-банан"]), ["!ананас", "+апельсин", "банан"], "Words with special symbol");

    is_deeply($f->([ q['%^(&*<%&,$.@>^^%/$|#}@{@^$+*'%&], '=', q[(%&)*^ ~@`&^ ^&*(^ $&%^^#$^&*?] ]), [], "Non allowed symbols only");

    is_deeply($f->(["температура", "36.6"]), ["36.6", "температура"], "Words with numbers and dots like num.num");
    is_deeply($f->(["-+город.герой", "-!прямо1.2брюхо.", "-.355.6.", "-!55.88.77."]), ["!55.88.77", "!прямо1 2брюхо", "+город герой", "355.6"], "Words with numbers and letters and dots like let.num");

    is_deeply($f->(["говорить по-английски", "говорить по английски"]), ["говорить !по английски", "говорить по-английски"], "Do not glue");

    is_deeply($f->(["[   !авто купить]","[   летим к солнцу  ]","[!прогноз погоды]","[поезда   ]"]),
              ["[!авто купить]","[!прогноз погоды]","[летим к солнцу]","[поезда]"]);

    is_deeply($f->(['" !ящик смешные "']), ['"!ящик смешные"']);

    is_deeply($f->(["\"bosch gop 10.8 v li professional цена\""]), ["\"bosch gop 10.8 v li professional цена\""]);
    is_deeply($f->(["\"прямо1.2брюхо.\""]), ["\"прямо1 2брюхо\""]);
};

subtest 'key_words_with_minus_words_intersection' => sub {
    my $f = \&MinusWords::key_words_with_minus_words_intersection;

    my $found_bugs = [
                      {
                        input   => { key_words => ['йог'],
                                     minus_words => ['сами'],
                                   },
                        result   => {},
                        comment => "bug 0.1",
                      },
                  ];
    my $test_cases1 = [
                      { input   => {key_words => ['купить коня'],
                                    campaign_minus_words => ['купить', '[купить коня]', '!купить !коня', 'купить коня', '[коня купить]',
                                                             '!купить коня', '!купить коня', 'купить коня серого', '!купить коня серого', 
                                                             '"купить коня"', '"купить"', '"купить коня серого"'],
                                   },
                        result  => {campaign_key_words => ['купить коня'],
                                    campaign_minus_words => ['купить', 'купить коня'],
                                   },
                        comment => "test 1.1",
                      },
                      {
                        input   => { key_words => ['!купить коня'],
                                     campaign_minus_words => ['!купить коня', '[купить коня]', '!купить !коня', 'купить !коня', 'купить коня',
                                                              '[коня купить]', '!купить коня серого', "купить коня", '"коня купить"', '"!купить коня"'], 
                                   },
                        result  => {campaign_key_words => ['!купить коня'],
                                    campaign_minus_words => ['!купить коня', 'купить коня'],
                                    },
                        comment => "test 1.2",
                      },
                      {
                        input   => { key_words => ['!купить коня'],
                                     minus_words => ['купить коня', '[коня купить]', '!купить коня серого', "купить коня"], 
                                     campaign_minus_words => ['!купить коня', '[купить коня]', '!купить !коня', 'купить !коня', '"коня купить"', '"!купить коня"'],
                                   },
                        result  => {key_words => ['!купить коня'] ,
                                    campaign_key_words => ['!купить коня'],
                                    minus_words => ['купить коня'],
                                    campaign_minus_words => ['!купить коня'],
                                   },
                        comment => "test 1.3",
                      },
                      {
                        input   => { key_words => ['купить !коня'],
                                     minus_words => ['купить коня', '!купить коня','купить !коня', '!купить !коня', '!купить коня серого', 'купить коня серого',
                                                     '[коня купить]', '[купить коня]', '[купить !коня]', '"купить коня"', '"купить !коня"'],
                                   },
                        result   => {key_words => ['купить !коня'],
                                     minus_words => ['купить !коня', 'купить коня'], 
                                   },
                        comment => "test 1.4",
                      },
                      {
                        input   => { key_words => ['!купить !коня'],
                                     minus_words => ['купить коня', '!купить коня','купить !коня', '!купить !коня', '!купить коня серого', 'купить коня серого',
                                                     '[коня купить]', '[купить коня]', '[купить !коня]', '"купить коня"', '"купить !коня"'],
                                   },
                        result   => {key_words => ['!купить !коня'],
                                     minus_words => ['!купить !коня', '!купить коня', 'купить !коня', 'купить коня'], 
                                   },
                        comment => "test 1.5",
                      },
                      {
                        input   => {key_words => ['купить коня серого'],
                                    campaign_minus_words => ['купить', 'купить коня', '!купить коня', 'купить !коня', '!купить !коня', '[купить коня]',
                                                             '[коня купить]', '[купить коня серого]', '"купить коня серого"'],
                                   },
                        result  => {campaign_key_words => ['купить коня серого'],
                                    campaign_minus_words => ['купить', 'купить коня'],
                                   },
                        comment => "test 1.6",
                      },
                      {
                        input   => { key_words => ['!купить коня серого'],
                                     minus_words => ['!купить коня','купить !коня', '!купить коня', '[коня купить]'], 
                                   },
                        result   => {key_words => ['!купить коня серого'],
                                     minus_words => ['!купить коня'], 
                                   },
                        comment => "test 1.7",
                      },
                      {
                        input   => { key_words => ['"купить коня серого"'],
                                     minus_words => ['"купить коня серого"', 'купить коня серого', '[купить коня серого]', '"купить коня"', 'купить коня', '[коня]'],
                                   },
                        result   => {key_words => ['"купить коня серого"'],
                                     minus_words => ['купить коня', '"купить коня серого"', 'купить коня серого', '[коня]'], 
                                   },
                        comment => "test 1.8",
                      },
                      {
                        input   => { key_words => ['купить для коня'],
                                     minus_words => ['самовар', 'купить +для коня', 'купить коня'],
                                     campaign_minus_words => ['купить !для коня'],
                                   },
                        result   => {key_words => ['купить для коня'],
                                     minus_words => ['купить коня'],
                                   },
                        comment => "test 1.9",
                      },
                      {
                        input   => { key_words => ['купить для коня', 'билет !на самолет'],
                                     minus_words => ['купить коня', 'купить для коня', 'билет !на самолет'],
                                   },
                        result   => {key_words => ['купить для коня', 'билет !на самолет'],
                                     minus_words => ['купить коня','билет !на самолет'],
                                   },
                        comment => "test 1.10",
                      },
                      {
                        input   => { key_words => ['купить +для коня', 'билет +на самолет'],
                                     minus_words => ['самовар', 'купить !для коня', 'купить коня', 'билет !на самолет'],
                                   },
                        result   => {key_words => ['купить +для коня', 'билет +на самолет'],
                                     minus_words => ['купить !для коня', 'купить коня', 'билет !на самолет'],
                                   },
                        comment => "test 1.11",
                      },
                      {
                        input   => { key_words => ['купить коня'],
                                     minus_words => ['для самовара'],
                                   },
                        result   => {},
                        comment => "test 1.12",
                      },
    ];
    my $test_cases2 = [
                      { input   => {key_words => ['купить цветы'],
                                    campaign_minus_words => ['цветок'],
                                   },
                        result  => {campaign_key_words => ['купить цветы'],
                                    campaign_minus_words => ['цветок'],
                                   },
                        comment => "test 2.1",
                      },
                      { input   => {key_words => ['купить цветы +в Москве'],
                                    campaign_minus_words => ['+в'],
                                   },
                        result  => {campaign_key_words => ['купить цветы +в Москве'],
                                    campaign_minus_words => ['+в'],
                                   },
                        comment => "test 2.2",
                      },
                      { input   => {key_words => ['билеты [из !Москвы в Париж]'],
                                    campaign_minus_words => ['!Москвы', '!Москвой'],
                                    minus_words => ['Москва'],
                                   }, 
                        result  => {key_words => ['билеты [из !Москвы в Париж]'],
                                    campaign_key_words => ['билеты [из !Москвы в Париж]'],
                                    minus_words => ['Москва'],
                                    campaign_minus_words => ['!Москвы'],
                                   },
                        comment => "test 2.3",
                      },
                      { input   => {key_words => ['"решетка +для камина"', '"клумба у погреба"'],
                                    campaign_minus_words => ['+для', '"+для"'],
                                    minus_words => ['клумба !у погреба'],
                                   }, 
                        result  => {campaign_key_words => ['"решетка +для камина"'],
                                    campaign_minus_words => ['+для'],
                                    key_words => ['"клумба у погреба"'],
                                    minus_words => ['клумба !у погреба'],
                                   },
                        comment => "test 2.4",
                      },
                      { input   => {key_words => ['Купить цветы в Москве'],
                                    minus_words => ['купить'],
                                    campaign_minus_words => ['цветы'],
                                   }, 
                        result  => {key_words => ['Купить цветы в Москве'],
                                    campaign_key_words => ['Купить цветы в Москве'],
                                    minus_words => ['купить'],
                                    campaign_minus_words => ['цветы'],
                                   },
                        comment => "test 2.5",
                      },
                      {
                        input   => { key_words => ['одежда +и обувь +для мужчины'],
                                     minus_words => ['одежда и обувь для мужчины', 'одежда и обувь +для мужчины', 'одежда +и обувь для мужчины',
                                                     'одежда +и обувь +для мужчины', 'одежда и обувь для женщины'],
                                     campaign_minus_words => ['одежда +и обувь +для мужчин', 'одежда и обувь +для детей'],
                                   },
                        result   => {key_words => ['одежда +и обувь +для мужчины'],
                                     campaign_key_words => ['одежда +и обувь +для мужчины'],
                                     minus_words => ['одежда !и обувь !для мужчины', 'одежда !и обувь +для мужчины', 'одежда +и обувь !для мужчины',
                                                     'одежда +и обувь +для мужчины'],
                                     campaign_minus_words => ['одежда +и обувь +для мужчин'],
                                   },
                        comment => "test 2.6",
                      },
                      {
                        input   => { key_words => ['"!купить цветы +в горшках недорого"'],
                                     minus_words => ['купить цветы в горшках недорого', 'купить цветы +в горшках недорого', '!купить цветы в горшках недорого',
                                                     '!купить цветы +в горшках недорого', '"купить цветы в горшках недорого"', '"купить цветы +в горшках недорого"',
                                                     '"!купить цветы в горшках недорого"', '"!купить цветы +в горшках недорого"', '!купить цветы без горшка близко к дому'],
                                     campaign_minus_words => ['"!купить цветы +в горшок недорого"', '!купить цветы +без горшка недорого'],
                                   },
                        result   => {key_words => ['"!купить цветы +в горшках недорого"'],
                                     campaign_key_words => ['"!купить цветы +в горшках недорого"'],
                                     minus_words => ['купить цветы !в горшках недорого', 'купить цветы +в горшках недорого', '!купить цветы !в горшках недорого',
                                                     '!купить цветы +в горшках недорого', '"купить цветы в горшках недорого"', '"купить цветы +в горшках недорого"',
                                                     '"!купить цветы в горшках недорого"', '"!купить цветы +в горшках недорого"',],
                                     campaign_minus_words => ['"!купить цветы +в горшок недорого"'],
                                   },
                        comment => "test 2.7",
                      },                                           
                      { input   => {key_words => ['билеты [Москва Париж]'],
                                    minus_words => ['[Москва Париж] билеты'],
                                   }, 
                        result  => {key_words => ['билеты [Москва Париж]'],
                                    minus_words => ['[Москва Париж] билеты'],
                                   },
                        comment => "test 2.8",
                      },
                      { input   => {key_words => ['билеты [!Москва Париж]'],
                                    minus_words => ['[Москва Париж] билеты', '[Москва !Париж] билеты'],
                                   }, 
                        result  => {key_words => ['билеты [!Москва Париж]'],
                                    minus_words => ['[Москва Париж] билеты'],
                                   },
                        comment => "test 2.9",
                      },
                      { input   => {key_words => ['[из !Москвы в Париж]'],
                                    minus_words => ['+из !Москвы +в Париж'],
                                    campaign_minus_words => ['из !Москвы +в Париж'],
                                   }, 
                        result  => {key_words => ['[из !Москвы в Париж]'],
                                    campaign_key_words => ['[из !Москвы в Париж]'],
                                    minus_words => ['+из !Москвы +в Париж'],
                                    campaign_minus_words => ['!из !Москвы +в Париж'],
                                   },
                        comment => "test 2.10",
                      },
                      { input   => {key_words => ['[из !Москвы в Париж]'],
                                    minus_words => ['[+из Москвы +в Париж]'],
                                   }, 
                        result  => {errors => ['Внутри скобок [] недопустимы символы +-"", минус-фразы: [+из Москвы +в Париж]'],
                                   },
                        comment => "test 2.11",
                      },

    ];
    my $test_cases3 = [
                      { input   => {key_words => ['интернет-магазин мебели'],
                                    minus_words => ['интернет-магазин мебели'],
                                   }, 
                        result  => {key_words => ['интернет-магазин мебели'],
                                    minus_words => ['интернет-магазин мебели'],
                                   },
                        comment => "test 3.1",
                      },
                      { input   => {key_words => ['по-английски говорить'],
                                    minus_words => ['говорить по-английски', '[по-английски говорить]'],
                                    campaign_minus_words => ['говорить по английски'],
                                   }, 
                        result  => {key_words => ['по-английски говорить'],
                                    campaign_key_words => [],
                                    minus_words => ['говорить по-английски'],
                                    campaign_minus_words => [],
                                   },
                        comment => "test 3.2",
                      },
                      { input   => {key_words => ['!по-английски !говорить'],
                                    minus_words => ['по-английски !говорить', 'по-английски'],
                                    campaign_minus_words => ['!по-английски !говорить', '!по-английски'],
                                   }, 
                        result  => {key_words => ['!по-английски !говорить'],
                                    campaign_key_words => ['!по-английски !говорить'],
                                    minus_words => ['по-английски !говорить', 'по-английски'],
                                    campaign_minus_words => ['!по-английски !говорить', '!по-английски'],
                                   },
                        comment => "test 3.3",
                      },
                      { input   => {key_words => ['"говорить по-английски"', '"мебель в интернет-магазине"'],
                                    minus_words => ['говорить по-английски', 'мебель +в интернет-магазине', '"мебель !в интернет-магазине"'],
                                    campaign_minus_words => ['"по-английски говорить"'],
                                   }, 
                        result  => {key_words => ['"говорить по-английски"', '"мебель в интернет-магазине"'],
                                    campaign_key_words => ['"говорить по-английски"'],
                                    minus_words => ['говорить по-английски', 'мебель +в интернет-магазине', '"мебель !в интернет-магазине"'],
                                    campaign_minus_words => ['"по-английски говорить"'],
                                   },
                        comment => "test 3.4",
                      },
                      { input   => {key_words => ['"говорить +по-английски и +по-немецки"'],
                                    minus_words => ['говорить по-английски и по-немецки', '"по-немецки и по-английски говорить"'],
                                    campaign_minus_words => ['говорить по-английски и +по-немецки'],
                                   }, 
                        result  => {key_words => ['"говорить +по-английски и +по-немецки"'],
                                    campaign_key_words => ['"говорить +по-английски и +по-немецки"'],
                                    minus_words => ['говорить по-английски !и по-немецки', '"по-немецки и по-английски говорить"'],
                                    campaign_minus_words => ['говорить по-английски !и +по-немецки'],
                                   },
                        comment => "test 3.5",
                      },
    ];

    foreach my $test (@$found_bugs, @$test_cases1, @$test_cases2, @$test_cases3) {
        my $r = $f->(%{$test->{input}});
        foreach (qw/campaign_key_words campaign_minus_words errors key_words minus_words/) {
            $test->{result}->{$_} = [sort @{$test->{result}->{$_} || []}];
            $r->{$_} = [sort @{$r->{$_} || []}];
        }

        is_deeply ($r, $test->{result}, $test->{comment});

    }

};

subtest 'merge_private_and_library_minus_words' => sub {
    my $f = \&MinusWords::merge_private_and_library_minus_words;

    my $test_cases = [
        {
            input   => {
                private_minus_words => ['слон', 'конь'],
                lib_minus_words => undef,
            },
            result  => ['слон', 'конь'],
            comment => "merge_private_and_library_minus_words return private_minus_words for undef lib",
        },
        {
            input   => {
                private_minus_words => ['слон', 'конь'],
                lib_minus_words => [],
            },
            result  => ['слон', 'конь'],
            comment => "merge_private_and_library_minus_words return private_minus_words for empty lib",
        },
        {
            input   => {
                private_minus_words => [],
                lib_minus_words => [],
            },
            result  => [],
            comment => "merge_private_and_library_minus_words empty",
        },
        {
            input   => {
                private_minus_words => ['купить коня'],
                lib_minus_words => [],
            },
            result  => ['купить коня'],
            comment => "merge_private_and_library_minus_words empty libs",
        },
        {
            input   => {
                private_minus_words => ['купить коня'],
                lib_minus_words => [
                    ['купить коня'],
                ],
            },
            result  => ['купить коня'],
            comment => "merge_private_and_library_minus_words remove duplicates",
        },
        {
            input   => {
                private_minus_words => ['купить коня'],
                lib_minus_words => [
                    ['купить', 'коня'],
                ],
            },
            result  => ['коня', 'купить', 'купить коня'],
            comment => "merge_private_and_library_minus_words minus words",
        },
        ,
        {
            input   => {
                private_minus_words => ['купить коня'],
                lib_minus_words => [
                    ['купить', 'коня'],
                    ['купить слона', 'или семь'],
                ],
            },
            result  => ['!или семь', 'коня', 'купить', 'купить коня', 'купить слона'],
            comment => "merge_private_and_library_minus_words complex",
        },
    ];

    foreach my $test (@$test_cases) {
        my $r = $f->($test->{input}->{private_minus_words}, $test->{input}->{lib_minus_words});
        is_deeply ($r, $test->{result}, $test->{comment});
    }

};

done_testing();

1;
