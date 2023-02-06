#include <mail/notsolitesrv/src/config/firstline.h>
#include <mail/notsolitesrv/src/firstline/firstline_impl.h>
#include <mail/notsolitesrv/src/message/parser.h>
#include <mail/notsolitesrv/src/meta_save_op/types/request.h>
#include <mail/notsolitesrv/src/meta_save_op/util/firstline.h>
#include <mail/notsolitesrv/src/util/file.h>
#include <mail/notsolitesrv/tests/unit/fakes/context.h>

#include <butil/StrUtils/utfutils.h>

#include <library/cpp/testing/unittest/env.h>

#include <gtest/gtest.h>

#include <boost/range/algorithm.hpp>

#include <memory>
#include <string>

namespace {

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NMetaSaveOp;
using namespace NNotSoLiteSrv::NFirstline;
using namespace NNotSoLiteSrv::NFirstline::NImpl;

struct TTestFirstlineLib : TestWithParam<std::tuple<std::size_t, std::string, bool, std::wstring, std::string>> {
    NConfig::TFirstlineUptr MakeConfig() {
        return std::make_unique<NConfig::TFirstline>(
            384,
            ArcadiaSourceRoot() + "/mail/notsolitesrv/package/deploy/app/config/subscription.rules.xml",
            ArcadiaSourceRoot() + "/mail/notsolitesrv/package/deploy/app/config/firstline.rules.txt",
            "reactor"
        );
    }

    void ReadEml(const std::string& filename) {
        auto data = NUtil::ReadFile(GetWorkPath() + "/firstline/" + filename);
        MessageData.assign(data.data(), data.size());
        OrigMessage = ParseMessage(MessageData, Ctx);
        auto headers = OrigMessage->GetHeaders();
        auto subject = boost::range::find_if(headers, [&](const auto& header) {
            return boost::algorithm::to_lower_copy(header.first) == "subject";
        });
        if (subject != headers.end()) {
             OrigMessage->SetSubject(subject->second);
        }
    }

    NFirstline::TFirstlineRequest MakeFirstlineRequest(bool isPeople) {
        auto message = CreateMessage(Ctx, OrigMessage);
        NFirstline::TFirstlineRequest firstlineRequest;
        std::tie(firstlineRequest.IsHtml, firstlineRequest.Part) = FindPartForFirstline(message.parts);
        firstlineRequest.Subject = message.subject;
        if (!message.from.empty()) {
            firstlineRequest.From = message.from.front();
        }
        firstlineRequest.IsPeopleType = isPeople;
        return firstlineRequest;
    }

    TFirstlinePtr FirstlineLib = std::make_shared<TFirstline>(MakeConfig());
    TContextPtr Ctx = GetContext();
    std::string MessageData;
    TMessagePtr OrigMessage;
};

TEST_P(TTestFirstlineLib, test_firstline_lib) {
    const auto& [testCase, filename, isPeople, firstline, comment] = GetParam();
    ReadEml(filename);
    ASSERT_TRUE(OrigMessage);
    auto result = FirstlineLib->GenerateFirstline(MakeFirstlineRequest(isPeople));
    ASSERT_EQ(firstline, utf::to_wstring(result));
}

INSTANTIATE_TEST_SUITE_P(
    test_firstline_lib,
    TTestFirstlineLib,
    Values(
        std::make_tuple(
            0,
            R"(mp2299.eml)",
            false,
            LR"(Исправили сегодня.)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-2299] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            1,
            R"(mailproto1922.eml)",
            false,
            LR"()",
            R"([EXMAILPROTO-1922] Недопущение прокидывания в ферстлайн текстов из вложения)"
        ),
        std::make_tuple(
            2,
            R"(startrack17.eml)",
            false,
            LR"(123123)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            3,
            R"(fotostrana1995.eml)",
            false,
            LR"(Ваши данные для входа на сайт)",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            4,
            R"(mailproto2174-3.eml)",
            false,
            LR"(Скоро завершатся Розы и тюльпаны от салона цветов «Терра» Скидка 50% за 10000 р. Оригинальный подарок для любого случая: шарж по фотографии Скидка 50% за 10000 р. Абонементы на 8 или 12 занятий фитнесом в Уручье Скидка 50%)",
            R"([EXMAILPROTO-2174] Искоренение бессмысленных ферстлайнов  "Письмо отображается некорректно? Посмотрите исходную версию на сайте!)"
        ),
        std::make_tuple(
            5,
            R"(turk3.eml)",
            false,
            LR"(How has your stumbling been? We hope you've been enjoying using StumbleUpon since you joined us a few weeks ago. We may be biased, but we think it's the best place to discover stuff on the web, recommended just for you! Here are a few tips to help you do even more with us:)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            6,
            R"(fotostrana1995-7.eml)",
            false,
            LR"(Ваши данные для входа на сайт)",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            7,
            R"(18.eml)",
            false,
            LR"(Doğum gününüz kutlu olsun!)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            8,
            R"(1660.142062698.93621908780372458716005768242.eml)",
            false,
            LR"(в еженедельно, вторник, начиная с 12.01.2010 с 17:00 до 18:00 (GMT+03:00) Moscow, St. Petersburg, Volgograd.)",
            R"(Общий кейс: письмо от соответствующего домена (я.календарь))"
        ),
        std::make_tuple(
            9,
            R"(mp2147.eml)",
            false,
            LR"(Выкатили.)",
            R"([MPROTO-162][EXMAILPROTO-2147][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            10,
            R"(startrek-mp-3082-2.eml)",
            false,
            LR"(Дима сказал, что эластик долго катится, и возможно не стоит катить их одновременно - если катить так как сделано сейчас - за одну выкладку, блогмонитор не будет работать пока эластик полностью не выкатится, я)",
            R"([MPROTO-3082] Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            11,
            R"(mp1852smile6.eml)",
            false,
            LR"(J & L Self Defense is a family owned company providing high quality self defense products to the general public as well as the law enforcement community. http://www.selfdefenseproducts.com/ J)",
            R"([EXMAILPROTO-1852] Обработка смайликов, которую так и не исправили)"
        ),
        std::make_tuple(
            12,
            R"(support_lovesupport1.eml)",
            false,
            LR"(Влад (М), 30 - 1 сообщение Александр (М), 23 - 1 сообщение)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            13,
            R"(mp162_1.eml)",
            false,
            LR"(С уважением, Матвеев Григорий matveieff@yandex-team.ru Можно ли ожидать этого в ближайшее время или делать это в клиентском коде?)",
            R"([MPROTO-162][EXMAILPROTO-1514][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            14,
            R"(mp88-forsq9.eml)",
            false,
            LR"(Ура! Мальчиков от девочек отделил?)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            15,
            R"(1258048700.eml)",
            false,
            LR"(Здравствуйте, Айман! Людмила . приглашает Вас присоединиться к списку ее друзей на odnoklassniki.ru Чтобы откликнуться на приглашение, пожалуйста, перейдите по ссылке:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            16,
            R"(startrack2.eml)",
            false,
            LR"(Паша, мы с тобой на прошлой неделе договорились, что ты создашь тестовый стенд мулек, на который будут выкатывать релизы до передачи в тестирование. С тебя стенды, с нас ручка на запуск тестов)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            17,
            R"(mp-2196.eml)",
            false,
            LR"(В рамках проекта "Качество данных в статистике" мы подключили новый тип проверок, помогающий отслеживать свежесть и работоспособность словарей. *Под словарями мы понимаем как словари из //statbox/statbox-dict, так и)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-2196] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            18,
            R"(mp2207_2.eml)",
            false,
            LR"(Недавно Вы просматривали на Яндекс.Маркете предложения магазинов. Возможно, Вы что-то купили, и Вам понравилось. Или, напротив, магазин Ваших ожиданий не оправдал. Поделитесь своими впечатлениями — оставьте)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            19,
            R"(16.eml)",
            false,
            LR"(istanbul.com'a hoşgeldiniz...)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            20,
            R"(mp88-forsq7.eml)",
            false,
            LR"(Ты уже вернулся?!)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            21,
            R"(1258048699.eml)",
            false,
            LR"(В просматриваемой Вами теме, появился новый ответ от пользователя Море. Прочитать ответ: http://www.sp.rostovmama.ru/index.php?topic=13320.new;topicseen#new Чтобы отказаться от уведомления из темы, нажмите сюда:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            22,
            R"(mailproto1994-1.eml)",
            false,
            LR"(Скидка до 30% Hocus Hocus представляет интереснейшую коллекцию для Ваших детей. Модели выглядят по-настоящему яркими и нарядными; Вы можете приобрести куртки, плащи, ветровки, а также брюки на флисе или хлопковой)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            23,
            R"(test.eml)",
            false,
            LR"(Message Another message)",
            R"(Общий кейс: тело письма состоящее только из текста кодированного)"
        ),
        std::make_tuple(
            24,
            R"(1258048627.eml)",
            false,
            LR"(This is the mail system at host yandex.ru I'm sorry to have to inform you that your message could not be delivered to one or more recipients. It's attached below. Please, do not reply to this message. ********** Это письмо отправлено почтовым сервером yandex.ru К сожалению, мы вынуждены сообщить Вам о том,)",
            R"(Общий кейс: вычисление ферстлайна для писем о недоставке)"
        ),
        std::make_tuple(
            25,
            R"(mailproto1643.eml)",
            true,
            LR"(Здесь дело не в копировании, а в возвращаемом bool-значении — часто бывает полезно знать, была ли на самом деле изменена строка. В твоём случае, кажется, вполне можно сделать так: Stroka targetUTF = WideToUTF8(to_lower(target)); P.S. Не)",
            R"([EXMAILPROTO-1643][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            26,
            R"(1258048554.eml)",
            false,
            LR"(Более половины российских компаний игнорируют угрозу гриппа)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            27,
            R"(ufotki11.eml)",
            false,
            LR"(Поздравляем! Ваша фотография «610x.jpg» 12 марта 2009 года стала Фото дня на Яндекс.Фотках.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            28,
            R"(sve.eml)",
            false,
            LR"(Раз Два Три Четыре С уважением, Хазова Светлана Инженер по тестированию havr@yandex-team.ru)",
            R"(Общий кейс: тело письма состоящее только из текста кодированного)"
        ),
        std::make_tuple(
            29,
            R"(smiles.eml)",
            false,
            LR"()",
            R"(Общий кейс: на обработку картиночных смайликов в письме)"
        ),
        std::make_tuple(
            30,
            R"(mp2123.eml)",
            false,
            LR"()",
            R"([MPROTO-31][EXMAILPROTO-2123][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи и начала цитирования)"
        ),
        std::make_tuple(
            31,
            R"(twitter.eml)",
            false,
            LR"(Венера Абрарова @VenerkaAbrarova теперь читает вас (@sistemmma). VenerkaAbrarova Венера Абрарова ICE-BABY )) Набережные Челны 16 Твиты 22 Читает 10 Читатели Посмотреть профиль @VenerkaAbrarova Хотите получать уведомления на мобильный телефон?)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            32,
            R"(startrack22.eml)",
            false,
            LR"(— added file Screenshot 20.02.14, 2226.png — removed files Chrysanthemum.jpg Desert.jpg Hydrangeas.jpg Jellyfish.jpg)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            33,
            R"(mp1662.eml)",
            false,
            LR"(Поинтересовались у нас новогодней Испанией и Израилем.)",
            R"([MPROTO-162][EXMAILPROTO-1662] Срезка подписи)"
        ),
        std::make_tuple(
            34,
            R"(money_mail4.eml)",
            false,
            LR"()",
            R"([DARIA-30317] Письма о переводе Яндекс.Денег во вложении должны быть с пустым ферстлайном)"
        ),
        std::make_tuple(
            35,
            R"(mp282-vk-7.eml)",
            false,
            LR"(Alexander, Евгений Георгиус, Костя Колясников отмечают свой день рождения 9 сентября. Празднуют день рождения 9 сентября Евгений Георгиус 24 года Костя Колясников Поздравить друзей с днем рождения Alexander, Вы можете)",
            R"([MPROTO-282] улучшение ферстлайна для писем от ВК)"
        ),
        std::make_tuple(
            36,
            R"(startrack23.eml)",
            false,
            LR"(жизнь комментарий больше 5 минут)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            37,
            R"(mp2286_2.eml)",
            false,
            LR"(Ваш пароль доступа к Одноклассникам был изменён. Если вы не меняли пароль доступа к Одноклассникам, восстановить его можно по ссылке http://www.odnoklassniki.ru/cdk/st.cmd/anonymPasswordRecovery Всегда ваши, Одноклассники -- С уважением,)",
            R"([EXMAILPROTO-2286][DARIA-3096] Срезка HTML-тегов)"
        ),
        std::make_tuple(
            38,
            R"(mp1933_6_aboutme.eml)",
            false,
            LR"(We are very happy to announce our integration with 500px! When you add the 500px app to your page, everyone will be able to see your work and be able to follow you on 500px from within the app. It's the easiest way to display your photography, as well as discover talented photographers on about.me. We’ve also added awesome new backgrounds curated from the best images on 500px.)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает бессмысленные фразы вида "Email not displaying ...".)"
        ),
        std::make_tuple(
            39,
            R"(1258050979.eml)",
            false,
            LR"(This is GameShadow Newsletter 127. The featured titles this week include Assassin's Creed 2, Just Cause 2 and Two Worlds II. We hope you enjoy it, however, if it isn't displaying quite right, you can always view it online here: http://www.gameshadow.com/newsletter/127/ Read Online | GameShadow.com | Privacy Policy | Unsubscribe Top Content: Just Cause 2 The Grapple Video Mass)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            40,
            R"(youtube.ru)",
            false,
            LR"(справочный центр | настройки электронной почты | сообщить о спаме)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            41,
            R"(1258051016.eml)",
            false,
            LR"(Владимир Смык)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            42,
            R"(mp2037_1.eml)",
            false,
            LR"(If you are unable to view this message correctly, click here Special Offer for AEC Users: 30% off Your Purchase of DraftSight Professional Register now for a no-cost, 30-day trial of DraftSight Professional for Windows, and get your code for 30% off to apply at purchase. Hurry, this offer ends this Friday June 27th, 2014! The Professional Pack Offering for DraftSight provides)",
            R"([EXMAILPROTO-2037]  Срезка бессмысленного "If you are unable to view this message correctly, click here." для всех писемв том числе и от "europcar-eci@europcar-news.com")"
        ),
        std::make_tuple(
            43,
            R"(mailproto1528.eml)",
            true,
            LR"(В 12:20 сломалась доставка логов на агрегаторы (я отфорвордил письмо на logs-tech@, там написано чуть подробнее), в течение дня они докачивались в MR, из-за этого замедлились быстрые процессы.)",
            R"([EXMAILPROTO-1528][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            44,
            R"(mailproto1994-7.eml)",
            false,
            LR"(Здравствуйте, Валентина! Вы получили это письмо потому что указали данный email при регистрации в клубе распродаж Mamsy. Добро пожаловать в клуб исполнения детских желаний! Мы рады Вам предложить самую лучшую)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            45,
            R"(mp1933_7_aboutme.eml)",
            false,
            LR"(Pro Tip: Add your city to find photographers near you)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает бессмысленные фразы вида "Email not displaying ...".)"
        ),
        std::make_tuple(
            46,
            R"(mp-1851.eml)",
            false,
            LR"(Я в отпуске была) Ещё актуально? Блаб бла бла)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-1851] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            47,
            R"(money_mail2.eml)",
            false,
            LR"()",
            R"([DARIA-30317] Письма о переводе Яндекс.Денег во вложении должны быть с пустым ферстлайном)"
        ),
        std::make_tuple(
            48,
            R"(mp-2167.eml)",
            false,
            LR"(Очень похоже, что потеряли таймстемпы и прочее)",
            R"([EXMAILPROTO-2167] Срезка слова "прокомментировал")"
        ),
        std::make_tuple(
            49,
            R"(wrong_utf5.eml)",
            false,
            LR"(2X2TV.ru Еженедельное подписное издание обновлений Ваш логин на сайте: sekas У нас произошли следующие обновления: Блог : Новости: Сетка с 7 по 13 сентября. [ Adult Swim ] is back ( 04.09.2009 / 18:24 ) - Комментариев: 89 Форум (10 последних)",
            R"(Письма в неправильных кодировках)"
        ),
        std::make_tuple(
            50,
            R"(mailproto1994-13.eml)",
            false,
            LR"(Получи билеты на фестиваль Burn DJ и выступление Paul van Dyk. 21 декабря в московском клубе Stadium Live Радио Рекорд проведет фестиваль Burn DJ, на котором выступит легенда электронной музыки — всемирно признанный DJ и)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            51,
            R"(uzoo20.eml)",
            false,
            LR"(m Kvaka, Яндекс.Календарь напоминает, что сегодня в 18:00 у вас запланировано событие "Вечерние новости" (Телеканал 'Первый'). Телепрограмма «Вечерние новости» в 18:00 Телеканал 'Первый' Перейти в Календарь Совет Даже)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            52,
            R"(test_fl_DARIA_3096.eml)",
            false,
            LR"(��. <i>�� ��������, ����?!</i>)",
            R"([EXMAILPROTO-2286][DARIA-3096] Срезка HTML-тегов)"
        ),
        std::make_tuple(
            53,
            R"(oriflame.eml)",
            false,
            LR"(Это сообщение вы также можете просмотреть в виде веб-страницы. Спасибо за ваш заказ! Мы надеемся, вам понравятся продукты Орифлэйм, которые вы выбрали. Продукты будут доставлены вам 10.12.2016. Если у вас есть)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            54,
            R"(wrong_utf2.eml)",
            false,
            LR"(Кавказский филиал: Интернет-форум для корпоративных абонентов! Уважаемые абоненты корпоративных тарифных планов! Приглашаем вас на Форум Кавказского филиала ОАО «МегаФон». Кавказский филиал: Мобильные)",
            R"(Письма в неправильных кодировках)"
        ),
        std::make_tuple(
            55,
            R"(ufotki15.eml)",
            false,
            LR"(Пользователь Свин)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            56,
            R"(mp2269_2.eml)",
            false,
            LR"()",
            R"([MPROTO-162][EXMAILPROTO-2269][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            57,
            R"(fotostrana1995-4.eml)",
            false,
            LR"(Ирина 24 года, Москва Она мне не интересна Ирина указала адрес своей страницы Вконтакте Обсудить Перейти в её профиль Посмотреть все интерсные новости (+23))",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            58,
            R"(mp88-forsq14.eml)",
            false,
            LR"(Счастливого пути,Николя!Ждем снова в гости!)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            59,
            R"(startrack16.eml)",
            false,
            LR"(— updated fields: Status Resolved, Closed — linked with: TEST-750 : Задача для проверки почтовых уведомления)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            60,
            R"(test_fl_DARIA_6741_2.eml)",
            false,
            LR"(.)",
            R"([EXMAILPROTO-1512][DARIA-6741] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            61,
            R"(fotostrana1995-5.eml)",
            false,
            LR"(Артур, 22 года – Хотите встретиться? Другие люди из твоего города, которые хотят втретиться: ...)",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            62,
            R"(daria33292.eml)",
            false,
            LR"([Почта наша хороша, багов нету никаких])",
            R"([DARIA-33292] Исправление ферстлайна для писем c текстом с квадратными скобками)"
        ),
        std::make_tuple(
            63,
            R"(zhmurov2.eml)",
            false,
            LR"(Егор всё смешал в одну кучу. Всё что знал :) WMI в частности, как и вебы в целом, тут не при чём. Вчера запустили спамооборону в мытищах. Запустили, похоже, неудачно...)",
            R"(Общий кейс: на цитаты в письме)"
        ),
        std::make_tuple(
            64,
            R"(mailproto1531.eml)",
            true,
            LR"(up)",
            R"([EXMAILPROTO-1531][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            65,
            R"(mailproto2172.eml)",
            false,
            LR"(Платье стойка-вилле Alina Assi 3950 руб. Блуза Alina Assi 2550 руб. Брюки Alina Assi 3950 руб. Футболка Love Moschino 7 450 руб. 4 590 руб. Кардиган Love Moschino 15 650 руб. 9 650 руб. Толстовка Love Moschino 11 150 руб. 6 850 руб.)",
            R"([EXMAILPROTO-2172] Искоренение бессмысленных ферстлайнов  "Шопинг-клуб KupiVIP.ru подобрал для Вас самые выгодные акции SALE")"
        ),
        std::make_tuple(
            66,
            R"(11.eml)",
            false,
            LR"(Bu email'i doğru şekilde görüntüleyemiyorsanız lütfen tıklayın.)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            67,
            R"(1258048679.eml)",
            false,
            LR"(Ваш email был указан при регистрации на сайте Anime-Online.su Если это вы регистрировались, пройдите по ссылке или введите в адресной строке браузера адрес http://anime-online.su/auth.php?confirm=44894503d369db4a4ae7e195c7854cbd Иначе просто забудте)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            68,
            R"(1258048610.eml)",
            false,
            LR"(Грац :))",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            69,
            R"(letter1993-2.eml)",
            false,
            LR"(SBA San Diego District Office Healthcare Law Webinars for Small Businesses What the Healthcare Law Means for your California Small Business IMPORTANT REMINDER: SHOP marketplace for small businesses is OPEN. Although Covered California's open enrollment period for individuals and families is now closed, its SHOP for small business is open year-round. Register now to learn about)",
            R"([EXMAILPROTO-1993] Удаление из ферстлайна бессмысленного "Если выпуск не отображается, вы можете прочесть его на сайте")"
        ),
        std::make_tuple(
            70,
            R"(SpecSymbol.eml)",
            false,
            LR"(буква ё в теме письма раз два три)",
            R"(Письмо с буквой Ё)"
        ),
        std::make_tuple(
            71,
            R"(1258048666.eml)",
            false,
            LR"(Ваша ссылка принята в "world-realty.org/rulinks/". Ваша ссылка сейчас доступна по адресу http://world-realty.org/rulinks/cat/9/59/ ВНИМАНИЕ! Модератор каталога может изменить категорию вашей ссылки, если выбранная Вами категория не совсем)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            72,
            R"(1258048549.eml)",
            false,
            LR"(Здравствуйте, Ирина! Вашей фотографии поставлена новая оценка! Чтобы ее увидеть, перейдите по ссылке: http://www.odnoklassniki.ru/ Если указанная выше ссылка не открывается, скопируйте ее в буфер обмена, вставьте в)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            73,
            R"(mailproto2135.eml)",
            false,
            LR"(Было у меня почему-то такое подозрение. Эх.)",
            R"([EXMAILPROTO-2135][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            74,
            R"(mailproto1662.eml)",
            false,
            LR"(Поинтересовались у нас новогодней Испанией и Израилем.)",
            R"([EXMAILPROTO-1662] Ферстлайн пересланного письма)"
        ),
        std::make_tuple(
            75,
            R"(mailproto2174.eml)",
            false,
            LR"(Услуги Рестораны Отели Туры Женщинам Мужчинам Детям Для дома Электроника ⁄ техника Красота Маникюр, педикюр, наращивание ногтей от салона красоты «Дамский каприз» 2100 руб. - 75% = 525 руб. Банная вечеринка с)",
            R"([EXMAILPROTO-2174] Искоренение бессмысленных ферстлайнов  "Письмо отображается некорректно? Посмотрите исходную версию на сайте!)"
        ),
        std::make_tuple(
            76,
            R"(mp162_2.eml)",
            false,
            LR"(вавав)",
            R"([MPROTO-162][EXMAILPROTO-1514][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            77,
            R"(subscribe.eml)",
            false,
            LR"(Если выпуск не отображается корректно, то вы можете прочесть его на сайте)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            78,
            R"(mailproto1515market.eml)",
            false,
            LR"(Ага, мне тоже понравился)",
            R"([EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            79,
            R"(mp282-vk-1.eml)",
            false,
            LR"(Николай, спасибо, будем ждать!)",
            R"([MPROTO-282] улучшение ферстлайна для писем от ВК)"
        ),
        std::make_tuple(
            80,
            R"(test_fl_MAILPROTO_84.eml)",
            true,
            LR"(Ќе должны, сейчас разберусь.)",
            R"([EXMAILPROTO-84][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            81,
            R"(test_fl_DARIA_3141_2.eml)",
            false,
            LR"(Друзья мои, весть про вечер третьего декабря была дезинформацией, сорри Важно:)",
            R"([DARIA-3141] Ошибочная обрезка первой буквы)"
        ),
        std::make_tuple(
            82,
            R"(startrack5.eml)",
            false,
            LR"()",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            83,
            R"(mp2207_1.eml)",
            false,
            LR"(Недавно Вы просматривали на Яндекс.Маркете предложения магазинов. Возможно, Вы что-то купили, и Вам понравилось. Или, напротив, магазин Ваших ожиданий не оправдал. Поделитесь своими впечатлениями — оставьте)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            84,
            R"(mailproto2174-6.eml)",
            false,
            LR"(1 кг ароматного и сочного шашлыка от кафе «Шашлык 58» Скидка 50% за 70 руб. Всё wok-меню в кафе Wokman Penza за полцены Скидка 50% за 30 руб. LED-телевизоры Shivaki и др. Мягкие игрушки Мульти-пульти и др. Постельное белье Cleo Весь)",
            R"([EXMAILPROTO-2174] Искоренение бессмысленных ферстлайнов  "Письмо отображается некорректно? Посмотрите исходную версию на сайте!)"
        ),
        std::make_tuple(
            85,
            R"(mailproto2026.eml)",
            false,
            LR"(Я приаттачил письмо, в котором были зафиксированы последние наши договоренности насчет места. По идее там должно хватить на заказанное сейчас, и даже с запасом. О да. http://www.nikhef.nl/pub/experiments/atlas/daq/ROB-12-1-98/OG.gif)",
            R"([EXMAILPROTO-2026] Срезка дат-цитат в нерусской локали)"
        ),
        std::make_tuple(
            86,
            R"(startrack9.eml)",
            false,
            LR"(Задача не решена, переоткрываю. Почему вы не будете заниматься поддержкой doxygen?)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            87,
            R"(turk.eml)",
            false,
            LR"(Alisher Hasanov)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            88,
            R"(mp2034.eml)",
            false,
            LR"()",
            R"([EXMAILPROTO-1518][EXMAILPROTO-2034] Срезка подписи)"
        ),
        std::make_tuple(
            89,
            R"(1258048548.eml)",
            false,
            LR"(Новости "Кардиовикторины 2009" Приглашаем Вас принять участие в кардиовикторине от кардиологической группы Солвей Фарма! С пожеланием победы, Кардиология, Солвей Фарма Новости медицины Все медицинские новости)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            90,
            R"(mp1933_1.eml)",
            false,
            LR"(We'd like to ask you a favor. It's been a while since you logged in and we'd like you to give our service another try. There are literally millions of people on about.me from every country in the world. People use their pages to: • Present themselves • Discover new people • Connect with people Join over 5 million people on the site. Our whole team is plugged in almost 24/7)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает бессмысленные фразы вида "Email not displaying ...".)"
        ),
        std::make_tuple(
            91,
            R"(1258048647.eml)",
            false,
            LR"(Обновления на сайте еРабота®-Новосибирск: 23:49, 12.11.2009 Вакансии 12.11.09 Торговый представитель - 25000 руб/мес. Фабула, торговая компания, Новосибирск. Условия работы: зарплата 25000 руб. в месяц, полный рабочий день.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            92,
            R"(mp2037_5_europcar.eml)",
            false,
            LR"(Ready for this summer? Get 15% off your rental)",
            R"([EXMAILPROTO-2037]  Срезка бессмысленного "If you are unable to view this message correctly, click here." для всех писемв том числе и от "europcar-eci@europcar-news.com")"
        ),
        std::make_tuple(
            93,
            R"(1258048708.eml)",
            false,
            LR"(Марина Сагакова)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            94,
            R"(mp88-forsq15.eml)",
            false,
            LR"(with Bonifaci, Anastasiya)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            95,
            R"(mailproto2118.eml)",
            false,
            LR"(Снова доступна.)",
            R"([EXMAILPROTO-2118][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            96,
            R"(mailproto2058.eml)",
            false,
            LR"(Мы прямо сейчас с этим разбираемся.)",
            R"([EXMAILPROTO-2058] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            97,
            R"(15.eml)",
            false,
            LR"(Bu email'i doğru şekilde görüntüleyemiyorsanız lütfen tıklayın.)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            98,
            R"(daria33292_2.eml)",
            false,
            LR"(Текст из скобок должен извлечься [Сработало!]. Вотъ)",
            R"([DARIA-33292] Исправление ферстлайна для писем c текстом с квадратными скобками)"
        ),
        std::make_tuple(
            99,
            R"(mp88-forsq6.eml)",
            false,
            LR"(а Диска нет?)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            100,
            R"(1258048530.eml)",
            false,
            LR"(Один из ваших избранных авторов только что опубликовал новую фотографию «Красный дом» http://www.photographer.ru/nonstop/picture.htm?id=630492 С наилучшими пожеланиями, Photographer.Ru)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            101,
            R"(lamoda.eml)",
            false,
            LR"(Подробная информация о доставке Вашего заказа в письме Не отображается письмо? Смотрите онлайн версию Женщинам Мужчинам Детям Спорт Премиум Новинки Красота Распродажа % Ваш заказ передан в службу доставки)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            102,
            R"(uzoo19.eml)",
            false,
            LR"(Сообщаем, что срок хранения Вашего файла "Baginski. Katedra.(2002).avi" на сервисе Яндекс.Народ заканчивается 14 апреля 2009г. Для того чтобы продлить срок хранения файла на 45 дней, перейдите по ссылке:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            103,
            R"(letter1999.eml)",
            false,
            LR"(Вы получили это письмо, так как подписаны на рассылку "Актуальные новости". Если у Вас возникли проблемы с чтением новостей, Вы можете прочитать оригинальную версию на специальной странице. Подписаться на)",
            R"([EXMAILPROTO-1999] Удаление из ферстлайна бессмысленного  "вы получили это письмо")"
        ),
        std::make_tuple(
            104,
            R"(17.eml)",
            false,
            LR"(Bu email'i doğru şekilde görüntüleyemiyorsanız lütfen tıklayın.)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            105,
            R"(mp88-forsq3.eml)",
            false,
            LR"(with Bonifaci, Борис)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            106,
            R"(uzoo6.eml)",
            false,
            LR"(Напоминаем, что Вам прислали открытку. Отправитель открытки: Калашникова Алиса <luciddeep@yandex.ru> Чтобы посмотреть открытку, нажмите на ссылку или скопируйте ее в адресную строку браузера.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            107,
            R"(1258048535.eml)",
            false,
            LR"()",
            R"(Общий кейс: письмо состоящее только из вложения-изображения)"
        ),
        std::make_tuple(
            108,
            R"(1258048569.eml)",
            false,
            LR"(Заказ номер 2658507 выполнен! Посмотреть свой Фамильный диплом можно по ссылке: http://name.wapson.ru/your_name/dyplom.aspx?id=2658507&alk=d28b5896ac834aec8647d723cbcb4d02 Посмотреть свой Фамильный диплом можно также в разделе "Мои заказы" по ссылке:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            109,
            R"(x247-1.eml)",
            false,
            LR"(да, любимая, пойдем (((((((((((((((((((((()))))))))))))))))))))))) спокойной ночи, сладкая, ложусь рядышком, целую тебя и очень скоро приду к тебе во сне ((((((((((((((((((((((((())))))))))))))))))))))) до завтра, счастье моё)",
            R"([MPROTO-1673] В firstline письма не должны попадать непечатные символы)"
        ),
        std::make_tuple(
            110,
            R"(uzoo18.eml)",
            false,
            LR"(m Kvaka, Яндекс.Календарь напоминает, что сегодня в 14:00 у вас запланировано событие "Другие новости" (Телеканал 'Первый'). Телепрограмма «Другие новости» в 14:00 Телеканал 'Первый' Перейти в Календарь Совет Хотите)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            111,
            R"(mp-1512-2.eml)",
            false,
            LR"(Шлю вам привет из далекой галактики.)",
            R"([EXMAILPROTO-1512] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            112,
            R"(mp2312.eml)",
            false,
            LR"(Я в пятницу написал руководителям Справочника. Пока ответа от них не было. Я занимаюсь проверкой карт уже лет 5. Отсюда могу предположить к чему может привести потеря или случайная замена адресов, потеря)",
            R"([EXMAILPROTO-2312] Вырезание фраз)"
        ),
        std::make_tuple(
            113,
            R"(groupon1.eml)",
            false,
            LR"(Москва Скидка 33% за 0 Р. Швейная фабрика Lacy - скидка 33% на восхитительные нарядные платья от российского производителя Акция длится до: 18.08.2013 Скидка 45% за 0 Р. Академия Комплементарной медицины. Скидка 45% на)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            114,
            R"(uzoo12.eml)",
            false,
            LR"(С 24 февраля базу вакансий Моего Круга ежедневно пополняют вакансии с HeadHunter и Работа@Mail.ru. Откликнуться на них Вы можете прямо из Моего Круга: работодатель получит Ваше письмо и резюме, содержащее данные из)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            115,
            R"(mailproto1552.eml)",
            true,
            LR"(Проект типа kiwi пришел к тебе и спросил можно ли так делать. Ты сказал - ок. Теперь ты утверждаешь что полнейшая деградация запуска транзакций на кластере следствие этого. Если текущая схема заливки киви)",
            R"([EXMAILPROTO-1552][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            116,
            R"(utkonos.eml)",
            false,
            LR"(Информация по вашему заказу Посмотреть письмо на сайте | Задать вопрос | Помощь | Напомнить пароль Благодарим вас за оформленный заказ и ваше доверие к нам. Мы будем информировать вас о ходе доставки по СМС и)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            117,
            R"(mp1933_8_aboutme.eml)",
            false,
            LR"(Pro Tip: Add your city to find photographers near you)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает бессмысленные фразы вида "Email not displaying ...".)"
        ),
        std::make_tuple(
            118,
            R"(vshtate.eml)",
            false,
            LR"(Ваши поисковые агенты нашли 2 новые вакансии. Просмотреть вакансии можно на странице:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            119,
            R"(support_lovesupport3.eml)",
            false,
            LR"(Эмми (Ж), 23 - 3 сообщения)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            120,
            R"(ya_money1.eml)",
            false,
            LR"(Пользователь upyrj@yandex.ru пересылает Вам бонус, полученный за платеж Яндекс.Деньгами: Скидка 10% при бронировании номера на сайте Hotels.com. Чтобы воспользоваться подарком, нужно активировать пин-код (подробная)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            121,
            R"(mailproto2171.eml)",
            false,
            LR"()",
            R"([EXMAILPROTO-2171] Искоренение бессмысленных ферстлайнов  "Шопинг-клуб KupiVIP.ru подобрал для Вас лучшие товары дня")"
        ),
        std::make_tuple(
            122,
            R"(mp1852smile5.eml)",
            false,
            LR"(SmileJ)",
            R"([EXMAILPROTO-1852] Обработка смайликов, которую так и не исправили)"
        ),
        std::make_tuple(
            123,
            R"(uzoo3.eml)",
            false,
            LR"(По Вашей просьбе 15.04.2009 15:07:42 прекращено обслуживание по тарифу "Беззаботный" Вашей рекламной кампании на Яндекс.Директе "Почта России" (1739486) (http://direct.yandex.ru/registered/main.pl): Служба поддержки Яндекс.Директ)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            124,
            R"(support_lovesupport2.eml)",
            false,
            LR"(Slava (М), 32 - 1 сообщение Рома (М), 28 - 1 сообщение)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            125,
            R"(19april-03.eml)",
            false,
            LR"(Но тут кажется все это качается через браузер.)",
            R"(Общий кейс: письмо с цитатами (данное письмо было добавлено по жалобе))"
        ),
        std::make_tuple(
            126,
            R"(fotostrana1995-8.eml)",
            false,
            LR"(Ирина 24 года, Москва Она мне не интересна Новое фото в конкурсе «Лицо с обложки» Ирины Нравится 911 Комментировать Перейти в её профиль Посмотреть все интерсные новости (+1))",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            127,
            R"(mp2207_7.eml)",
            false,
            LR"(Имею Марк II, 450D и 70D и 6-ть объективов L- серии. Этого достаточно?)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            128,
            R"(uzoo17.eml)",
            false,
            LR"(neo ie, Яндекс.Календарь напоминает, что сегодня в 10:30 у вас запланировано событие "Английский язык". Мои события «Английский язык» в 10:30 в офисе уроки английского языка Перейти в Календарь Совет Хотите перенести)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            129,
            R"(uzoo21.eml)",
            false,
            LR"(Сообщаем, что срок хранения Вашего файла "Мої сервіси пошук листів.doc" на сервисе Яндекс.Народ заканчивается 16 апреля 2009г. Для того чтобы продлить срок хранения файла на 45 дней, перейдите по ссылке:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            130,
            R"(mp2037_6_europcar.eml)",
            false,
            LR"(Early bird winter special : Book now and save 15%)",
            R"([EXMAILPROTO-2037]  Срезка бессмысленного "If you are unable to view this message correctly, click here." для всех писемв том числе и от "europcar-eci@europcar-news.com")"
        ),
        std::make_tuple(
            131,
            R"(mp2294_1.eml)",
            false,
            LR"(Как ты?)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-2294] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            132,
            R"(yaru.eml)",
            false,
            LR"(Жарко.... Ещё горячее: http://www.01tv.prav.tv/ тест драйв сегодня в 21 по москве.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            133,
            R"(mailproto1994-6.eml)",
            false,
            LR"(Bell Bimbo зима СКИДКА ДО 30% Bell Bimbo выбирают родители, которые видят, как радуются дети, надевая яркие вещи. Конструкции всех моделей обладают высокими эргономическими и потребительскими свойствами, что позволяет)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            134,
            R"(2.eml)",
            false,
            LR"(Resimleri göremiyor musunuz? "Resimleri göster"i seçin ya da bu iletiyi tarayıcınızda görüntüleyin. Fırsat epostalarımızı düzenli olarak)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            135,
            R"(turk5.eml)",
            false,
            LR"(Bu email'i doğru şekilde görüntüleyemiyorsanız lütfen tıklayın.)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            136,
            R"(money_mail3.eml)",
            false,
            LR"()",
            R"([DARIA-30317] Письма о переводе Яндекс.Денег во вложении должны быть с пустым ферстлайном)"
        ),
        std::make_tuple(
            137,
            R"(fotostrana1995-2.eml)",
            false,
            LR"(Ваши данные для входа на сайт)",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            138,
            R"(startrack15.eml)",
            false,
            LR"(— linked with: TEST-559 : Работает?)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            139,
            R"(letter1992.eml)",
            false,
            LR"(Как очистить кафель и обновить затирку без затрат? Никто не сомневается, что вы постоянно моете кафельный пол и оттираете кафельную плитку на кухне и в ванной, если она загрязнилась... Читать дальше Найти в)",
            R"([EXMAILPROTO-1992] Искоренение бессмысленного ферстлайна "Группы")"
        ),
        std::make_tuple(
            140,
            R"(uzoo14.eml)",
            false,
            LR"(Александр Яковлев приглашает Вас в свой 1-й круг. Принять или отклонить приглашение. Внимание! По ссылкам в этом письме можно зайти в Ваш профиль без ввода пароля, поэтому никому их не передавайте. Мой Круг —)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            141,
            R"(mp1852smile2.eml)",
            false,
            LR"(Весёлый смайлик J)",
            R"([EXMAILPROTO-1852] Обработка смайликов, которую так и не исправили)"
        ),
        std::make_tuple(
            142,
            R"(mp282-vk-6.eml)",
            false,
            LR"(Ваня Шевчук А чем он экзотичен?)",
            R"([MPROTO-282] улучшение ферстлайна для писем от ВК)"
        ),
        std::make_tuple(
            143,
            R"(video.eml)",
            false,
            LR"(видеописьмо Открыть видеописьмо http://video.yandex.ru/users/oliktester2/letter/1/Wz_7yV-qQtY Если приведенная выше ссылка не работает, скопируйте и вставьте ее в адресную строку браузера.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            144,
            R"(uyaru.eml)",
            false,
            LR"()",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            145,
            R"(mailproto1994-10.eml)",
            false,
            LR"(Способ экономить на бензине – 4 вида альтернативного транспорта на irr.ru Как сэкономить на бензине? Решение есть! Из рук в руки/IRR.RU предлагает весной использовать 4 вида альтернативного транспорта. Обновленный)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            146,
            R"(1258048649.eml)",
            false,
            LR"(Вы успешно зарегистрировались на сайте Фотоконкурс.ру. Для входа на сайт используйте следующие данные: Email: KrKOVD@ya.ru Пароль: 52563ca1554fd09be5f182beab536009 Начните свою работу с сайтом из личного кабинета:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            147,
            R"(mailproto2111.eml)",
            true,
            LR"(Сломалось. При этом сертификат в порядке, но не работает по https. Отпишусь в таске.)",
            R"([EXMAILPROTO-2111][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            148,
            R"(1258051661.eml)",
            false,
            LR"(This is GameShadow Newsletter 127. The featured titles this week include Assassin's Creed 2, Just Cause 2 and Two Worlds II. We hope you enjoy it, however, if it isn't displaying quite right, you can always view it online here: http://www.gameshadow.com/newsletter/127/ Read Online | GameShadow.com | Privacy Policy | Unsubscribe Top Content: Just Cause 2 The Grapple Video Mass)",
            R"(Письма на латинских языках)"
        ),
        std::make_tuple(
            149,
            R"(mailproto1994-9.eml)",
            false,
            LR"(Здравствуйте, Алина, Вы оформили заказ в клубе Mamsy. Проверьте, пожалуйста, правильность информации о заказе: Наименование Кол-во Стоимость 1 Жакет (Рост: 110 см, Размер на этикетке: Рост: 110, Размер: 56, 4-5 лет, Возраст:)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            150,
            R"(yasupport)",
            false,
            LR"(Если Вы подозреваете, что Ваш аккаунт взломали, пожалуйста, заполните анкету на странице http://passport.yandex.ru/passport?mode=supportrestore . Мы решим эту проблему в течение нескольких дней, если при регистрации Вы добросовестно)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            151,
            R"(mailproto2172-2.eml)",
            false,
            LR"(Платье Patrizia Pepe 4190 руб. Блуза Patrizia Pepe 2590 руб. Юбка Patrizia Pepe 3190 руб.)",
            R"([EXMAILPROTO-2172] Искоренение бессмысленных ферстлайнов  "Шопинг-клуб KupiVIP.ru подобрал для Вас самые выгодные акции SALE")"
        ),
        std::make_tuple(
            152,
            R"(ulmart.eml)",
            false,
            LR"((495) 287-42-41 Уважаемый (ая) Демина Анна Ивановна, благодарим Вас за то, что Вы воспользовались услугами системы электронного резервирования www.ulmart.ru Номер резерва: 0042884914 от 13.12.2016г Артикул Наименование товара)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            153,
            R"(mailproto1994-3.eml)",
            false,
            LR"(Скидка до 55% Kidly Натуральная ткань и европейский дизайн от компании Kidly. Вот что любят маленькие принцессы! Kidly Скидка до 50% Одежда бренда Kidly — настоящая находка для гардероба маленького модника! MM-dadak Скидка до)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            154,
            R"(mp-1734.eml)",
            false,
            LR"(Я бы оставила 1 вариант названий. Ибо вопросы в основном, только по 1 и 2 классам. Я считаю что для 1 класса самое подходящие название "Автострады". Название 2 класса "Важнейшие автомагистрали", по по сути своей)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-1734] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            155,
            R"(mp1933_2.eml)",
            false,
            LR"(Семинар при поддержке Сбербанка России. 15 практических способов удвоения прибыли. Четверг, 3 июля 2014, 16:00 Бесплатный семинар для руководителей и владельцев бизнеса: 15 практических способов удвоения прибыли.)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает фразы бессмысленные фразы "Email not displaying ...".)"
        ),
        std::make_tuple(
            156,
            R"(mailproto1994-2.eml)",
            false,
            LR"(Скидка до 40% STEEN AGE Яркие и практичные модели верхней одежды для детей от бренда Steen Age. Скидка до 55% JAN STEEN Качественная детская одежда Jan Steen: толстовки, поло, костюмы. Imoga Скидка до 20% Любопытные принты и смелые)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            157,
            R"(spf.eml)",
            false,
            LR"(wsfqafw)",
            R"(Общий кейс: тело письма состоящее только из текста)"
        ),
        std::make_tuple(
            158,
            R"(startrek-mp-3082.eml)",
            false,
            LR"(— изменила поля Статус Протестировано Закрыт Резолюция Решен)",
            R"([MPROTO-3082] Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            159,
            R"(mailproto1990.eml)",
            false,
            LR"(Сахарная паста Virginia для депиляции в домашних условиях со скидкой 50%! Хороший эффект и недорого!)",
            R"([EXMAILPROTO-1990] Искоренение бессмысленных ферстлайнов "iPhone Android")"
        ),
        std::make_tuple(
            160,
            R"(eapteka.eml)",
            false,
            LR"(Ваш заказ принят! Алеся Щ , по указанному вами номеру 9671103024 с вами свяжется оператор для подтверждения заказа. Отслеживать статус заказа можно в личном кабинете. Заказ № N2260479 Уро-Ваксом, капсулы 6 мг, 30 шт. 2 x1397.00)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            161,
            R"(mp-2074.eml)",
            false,
            LR"()",
            R"([EXMAILPROTO-1518][EXMAILPROTO-2069] Срезка подписи)"
        ),
        std::make_tuple(
            162,
            R"(mp1933_4.eml)",
            false,
            LR"(Website Getting here Flight info Airport guide Destinations Shopping & eating Dear Traveller Welcome to the July newsletter. This issue find out about some great destinations on offer for Summer 2015. Also learn about an expansive route network available from your doorstep with Lufthansa or explore the fantastic fjords of Bergen with Wideroe! If you're headed to London Heathrow,)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает бессмысленные фразы вида "Email not displaying ...".)"
        ),
        std::make_tuple(
            163,
            R"(mp2207_3.eml)",
            false,
            LR"(На Яндекс.Маркете появились новые предложения магазинов, соответствующие параметрам Вашей подписки. Samsung Galaxy Note 10.1 2014 Edition Wifi+3G P6010 16Gb до 20 000 руб., регион Керчь arenasale.ru — 19 490 руб. Все магазины и цены До окончания)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            164,
            R"(startrack11.eml)",
            false,
            LR"(И от зомба комментарий)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            165,
            R"(letter1996.eml)",
            false,
            LR"(Лучшие рекрутинговые видео года Ночные клубы и войска спецназначения, пекарня и футбольный стадион — эти видеоролики запомнятся надолго! Исследования > Самые большие зарплаты 2013 года Вспоминаем самые)",
            R"([EXMAILPROTO-1996] Искоренение бессмысленного ферстлайна "Поиск вакансий")"
        ),
        std::make_tuple(
            166,
            R"(12.eml)",
            false,
            LR"(Resimleri göremiyorsaniz "Güvenli olarak isaretle" seçenegini seçin ya da bu mesaji web tarayicinizda görüntüleyin.)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            167,
            R"(mp-1519.eml)",
            false,
            LR"(Я в чт никак. Мб смогу, но станет ясно в чт вечером :(()",
            R"([EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            168,
            R"(test_autoreply.eml)",
            false,
            LR"(by mxback4.mail.yandex.net with LMTP id WA2GhZdr for <asergeev@kmsg.ru>; Thu, 4 Aug 2011 15:32:10 +0400 Received: from forward4.mail.yandex.net (forward4.mail.yandex.net [77.88.46.9]) by mxback4.mail.yandex.net (nwsmtp/Yandex) with ESMTP id W9AiVHi1; Thu, 4 Aug 2011 15:32:09 +0400 X-Yandex-Front: mxback4.mail.yandex.net X-Yandex-TimeMark: 1312457529 X-Yandex-Spam: 1 Received: from)",
            R"(Странный кейс скорее на контроль разницы в работе парсера заголовков. У нвсмтп и фастрв разные парсеры. Поэтому в ферстлайн и тело письма пролезают заголовки)"
        ),
        std::make_tuple(
            169,
            R"(uzoo8.eml)",
            false,
            LR"(Сообщаем, что срок хранения Вашего файла "birdofparadise.bmp" на сервисе Яндекс.Народ заканчивается 16 апреля 2009г. Для того чтобы продлить срок хранения файла на 45 дней, перейдите по ссылке:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            170,
            R"(wrong_utf1.eml)",
            false,
            LR"(Кавказский филиал: Интернет-форум для корпоративных абонентов! Уважаемые абоненты корпоративных тарифных планов! Приглашаем вас на Форум Кавказского филиала ОАО «МегаФон». Кавказский филиал: Мобильные)",
            R"(Письма в неправильных кодировках)"
        ),
        std::make_tuple(
            171,
            R"(mp88-forsq13.eml)",
            false,
            LR"(Когда жил неподалеку)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            172,
            R"(avon.eml)",
            false,
            LR"(Оксана! Спасибо за заказ в кампании 2017-04! Информация о заказе и его статусе: Номер операции: 1377309 Адрес доставки: Оксана Евсеева KRS300 ГОРШЕЧНОЕ РП 306800 Дата доставки заказа: 11/03/2017 Заказ: Код Количество Название 3323-2)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            173,
            R"(mp282-vk-2.eml)",
            false,
            LR"(У тебя есть лекции по стеганографии на русском ? Можешь отправить?)",
            R"([MPROTO-282] улучшение ферстлайна для писем от ВК)"
        ),
        std::make_tuple(
            174,
            R"(1258050978.eml)",
            false,
            LR"(Вы запросили инструкцию, чтобы восстановить пароль. Код для восстановления пароля: 4CgUt4G8k)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            175,
            R"(mp-31.eml)",
            false,
            LR"(я в пт нет меня пригласии на свадьбу)",
            R"([MPROTO-2948][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            176,
            R"(mp88-forsq8.eml)",
            false,
            LR"(Забухай, епт!)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            177,
            R"(letter1991-5.eml)",
            false,
            LR"(Вы получили это письмо, потому что подписаны на рассылку "Скидки и акции".)",
            R"([EXMAILPROTO-1991] Удаление из ферстлайна бессмысленного "Если не отображаются картинки, смотрите здесь")"
        ),
        std::make_tuple(
            178,
            R"(uzoo15.eml)",
            false,
            LR"(Новый пользователь, который работал или работает вместе с Вами: Алена Иванова: Рога и Копыта, секретарь (пригласить) Если Вы не хотите в дальнейшем получать новости, укажите это в настройках. Внимание! По)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            179,
            R"(d-37150.eml)",
            true,
            LR"(Приветствую)",
            R"([DARIA-37150] Исправление обработки приветствий в письме)"
        ),
        std::make_tuple(
            180,
            R"(fotostrana1995-6.eml)",
            false,
            LR"(Ваши данные для входа на сайт)",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            181,
            R"(vedomostiru.ru)",
            false,
            LR"("Ведомости". Ежедневная деловая газета)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            182,
            R"(taggered_you.eml)",
            false,
            LR"(Hi Nastassia, Olga tagged you in a post. To view or comment on the post, follow the link below: http://www.facebook.com/n/?permalink.php&id=1618877304&story_fbid=141206985951168&mid=420317fG45ea21dcG284fe1aG52&bcode=nvvHaIjX&n_m=terpsihora4%40yandex.ru You can now tag your friends in your status or post. Type @ and then type the friend's name. For example: "Had lunch with @John)",
            R"(Общий кейс: письмо от соответствующего домена (фейсбук))"
        ),
        std::make_tuple(
            183,
            R"(zhmurov1.eml)",
            false,
            LR"(SKIP для нас это такой же валидный ответ как и HAM или SPAM. SKIP - это значит письмо от одноклассников, вконтакте или других соц. сетей. SKIP для нас означает то, что нужно немедленно прекратить проверку на спам и считать)",
            R"(Общий кейс: на цитаты в письме)"
        ),
        std::make_tuple(
            184,
            R"(mp-2081.eml)",
            false,
            LR"(Зависимость от geobase-binary-getter4 была убрана ещё в 4.1-30 версии. Вместе с удалением пакета. Схема распространения бинарных файлов по http морально устарела. Мы стремимся уйти от неё. Замечу, что сама по себе библиотека -)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-2081] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            185,
            R"(ufotki1.eml)",
            false,
            LR"(Пользователь ВиниПух добавил вас в Любимые авторы.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            186,
            R"(mailproto1994-11.eml)",
            false,
            LR"(Вещи c Титаника на irr.ru? Знаете ли вы, что на IRR размещают вещи - ровесники Титаника? Обратите внимание на сходство. Возможно, именно эти вещи были на лайнере 102 года назад. Авторизация на Автомании через)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            187,
            R"(amazon.eml)",
            false,
            LR"(Amazon Video | Your Account | Amazon.com Order Confirmation Order #D01-4103881-2557065 Thank you for shopping with us. You can find all of your new and previous Amazon Video purchases - movies, TV episodes and unexpired movie rentals - in Your Video Library. Instantly watch your videos anywhere with your Amazon Fire TV, Kindle Fire, iPad, Roku, PlayStation, XBox, Wii, PC, Mac, or)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            188,
            R"(apteka.eml)",
            false,
            LR"(Ваш заказ № MI-8346589 принят. Стоимость заказа 1895 рублей. Стоимость доставки - бесплатно! Состав заказа: СКИН-КАП 0,2% 50,0 КРЕМ - 1шт. (1895.00 руб.) Заказ будет доставлен в аптеку ориентировочно 31.03.2017. Об отправке заказа в)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            189,
            R"(mp2286_1.eml)",
            false,
            LR"(<Alt> <Down> [::-- bas59sql (2014/06/22 08:25:38 ق.ظ) --::] <Up><Escape>;hk,v[kj k,h<Down><Down><Up><Up><Down><Down><Up><Down><Down><Up><Down><Up><Down><Down><Down><Up><Up><Up><Up><Down><Down><Down><Down><Up><Escape><Escape><Escape><Down><Down><Down><Down><Down> [::-- IP Address --::] 46.224.115.75)",
            R"([EXMAILPROTO-2286][DARIA-3096] Срезка HTML-тегов)"
        ),
        std::make_tuple(
            190,
            R"(1635.142062698.11751212019453066158067674802.eml)",
            false,
            LR"(в еженедельно, вторник, с 12.01.2010 по 28.12.2010 с 14:00 до 15:00 (GMT+03:00) Москва, Санкт-Петербург, Волгоград.)",
            R"(Общий кейс: письмо от соответствующего домена (я.календарь))"
        ),
        std::make_tuple(
            191,
            R"(1682.142062698.970311937202646641015949741286.eml)",
            false,
            LR"(в еженедельно, среда, с 03.02.2010 по 07.07.2010 с 14:00 до 15:00 (GMT+03:00) Москва, Санкт-Петербург, Волгоград.)",
            R"(Общий кейс: письмо от соответствующего домена (я.календарь))"
        ),
        std::make_tuple(
            192,
            R"(mp88-forsq16.eml)",
            false,
            LR"(Аэропорт Ростова,это норма)Я час раз стоял у окна регистрации,они решили спортсменов и пассажиров обычных регистрировать вместе-коллапс был))",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            193,
            R"(mp2335.eml)",
            true,
            LR"(Указывай какой интерфейс, в старом вполне нормально видно To do In progress Done Total Partitioning chunks 0 25 2,951 2,976 Sorting partitions 582 0 0 582 Слева есть легенда, которая говорит, что в колонке In Progress указанно количество чанков,)",
            R"([EXMAILPROTO-2335])"
        ),
        std::make_tuple(
            194,
            R"(mailproto2126.eml)",
            true,
            LR"(Надо понимать, что копирование делается по принципе пуша из YT в Yamr, на наших нодах с не очень большой параллельностью запускается команда write (не более 100 одновременно запущенных джобов). Поэтому количество)",
            R"([EXMAILPROTO-2126][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            195,
            R"(startrack19.eml)",
            false,
            LR"(— unlinked from: TEST-750 : Задача для проверки почтовых уведомления)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            196,
            R"(mailproto1513.eml)",
            false,
            LR"(Для минимального времени простоя нужно >1 мастера, репликация не нужна.)",
            R"([EXMAILPROTO-1513] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            197,
            R"(14.eml)",
            false,
            LR"(Merhaba Alisher Hasanov,)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            198,
            R"(yacards.eml)",
            false,
            LR"(Вам открытка. Отправитель открытки: Assembler Chuck <assemblerrr@yandex.ru> Чтобы стать первооткрывателем этой открытки, нажмите на ссылку или скопируйте ее в адресную строку браузера. http://cards.yandex.ru/show.xml?id=c51c686f4a23a6fe6f40d81ad04c2159)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            199,
            R"(mp282-vk-4.eml)",
            false,
            LR"(Вы получили: 3 новые записи на стене Alexander, Вы можете ограничить или отменить уведомления на E-Mail в Настройках оповещений)",
            R"([MPROTO-282] улучшение ферстлайна для писем от ВК)"
        ),
        std::make_tuple(
            200,
            R"(turk2.eml)",
            false,
            LR"(Meet Your Matches at Yonja)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            201,
            R"(mailproto2037.eml)",
            false,
            LR"(Wherever you go in this world, you'll always find our great offers. Spring is here! Drive towards the sun! And still: save up to 25% when you pay online! Discover Europe's romantic getaways Spain from $17 a day > France from $23 a day > Italy from $55 a day > and many other destinations Discover Europe's top spots UK from $19 a day > Portugal from $28 a day > Sweden from $33 a day)",
            R"([EXMAILPROTO-2037] Искоренение бессмысленных ферстлайнов  "If you are unable to view this message correctly, click here.")"
        ),
        std::make_tuple(
            202,
            R"(mp2207_8.eml)",
            false,
            LR"(Ну вот и всё трольчёнок :) Слился на элементарном вопросе касаемом фотосъёмки в конкретных условиях , что было вполне предсказуемо . Шанс отличиться и показать окружающим , что ты из себя что-то представляешь ,)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            203,
            R"(mp-2057.eml)",
            false,
            LR"(С уважением, Матвеев Григорий matveieff@yandex-team.ru Можно ли ожидать этого в ближайшее время или делать это в клиентском коде?)",
            R"([EXMAILPROTO-1518][EXMAILPROTO-2057] Срезка подписи)"
        ),
        std::make_tuple(
            204,
            R"(support_lovesupport5.eml)",
            false,
            LR"(Костя (М), 30 - 1 сообщение blah-blah-blah)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            205,
            R"(uzoo13.eml)",
            false,
            LR"(В период кризиса многие сталкиваются с проблемой трудоустройства. Даже если сейчас перед Вами такая проблема не стоит, лучше заранее обзавестить полезными контактами на случай поиска работы. Для того чтобы)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            206,
            R"(1258048636.eml)",
            false,
            LR"(Подскажите, можно ли в Kmplayer убрать паузы между треками при возспроизведении аудиофайлов. Всем хорош проигрыватель, а вот эта проблема раздражает. С уважением, Николай)",
            R"(Общий кейс: на цитаты в письме и вырезание приветствий)"
        ),
        std::make_tuple(
            207,
            R"(startrek-mp-3082-3.eml)",
            false,
            LR"(в блогмониторе неделю не использовались все афекченные ручки, я за то чтобы катить вместе, на всякий случай еще у пользователей узнаю сейчас проверю в тестинге)",
            R"([MPROTO-3082] Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            208,
            R"(aliexpress.eml)",
            false,
            LR"(Мои заказы | Помощь | Защита Покупателя Ваш заказ № 500216418258678 был отправлен Julia Sergeeva, Продавец отправил Ваш заказ № 500216418258678 2016 ILIFE Мокрой Робот Пылесос для Дома Мокрый Сухой Чистый Резервуар Для Воды Двойной)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            209,
            R"(mailproto2174-4.eml)",
            false,
            LR"(Услуги› Аллерготест (компьютерное тестирование) для взрослых и детей за полцены Скидка 50% за 10000 р. Мануальная диагностика, терапия и другие процедуры Скидка 60% за 10000 р. Горячее ламинирование волос с)",
            R"([EXMAILPROTO-2174] Искоренение бессмысленных ферстлайнов  "Письмо отображается некорректно? Посмотрите исходную версию на сайте!)"
        ),
        std::make_tuple(
            210,
            R"(19.eml)",
            false,
            LR"(Merhaba Alisher Hasanov,)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            211,
            R"(10.eml)",
            false,
            LR"(Hemen paylaşın, fırsatları çoğaltın: 13 Temmuz için Günün Serinleten Fırsatları: Galata Kulesi'ne giriş ve İstanbul manzarası eşliğinde serpme kahvaltı ziyafeti ile sınırsız çay 66 TL yerine 33 TL! 33 TL Değeri İndirim Kazancınız 66 TL %50 33 TL Paylaş, 6 TL Kazan! Neşeli Tur’dan Ocean Majesty Cruise gemisi ile, 4 gece 5 günlük, tam pansiyon,)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            212,
            R"(mp-2221.eml)",
            false,
            LR"(Pull request STARDUST/browser/17811 Wp 14.7.1916/BROWSER-15192/4 to master-14.7.1916/rc MERGED by ilezhankin)",
            R"([EXMAILPROTO-2221] Коррекция письма от Джиры)"
        ),
        std::make_tuple(
            213,
            R"(startrack21.eml)",
            false,
            LR"(— deleted comment: жизнь комментарий больше 5 минут)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            214,
            R"(uyaru1.eml)",
            false,
            LR"(Уж послала так послала!.. Any time means no time)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            215,
            R"(mailproto2174-5.eml)",
            false,
            LR"(Услуги› Стрижка и уход для волос Satinique Скидка 60% за 10000 р. Фотоэпиляция различных зон лица и тела от салона красоты «Лилита» Скидка 50% за 12000 р. Полная профилактика полости рта с УЗ-чисткой зубов и лечение кариеса)",
            R"([EXMAILPROTO-2174] Искоренение бессмысленных ферстлайнов  "Письмо отображается некорректно? Посмотрите исходную версию на сайте!)"
        ),
        std::make_tuple(
            216,
            R"(1258048609.eml)",
            false,
            LR"(Произошли изменения в показах объявлений Вашей рекламной кампании N 1912599 (Автоцентр на Волгоградке), размещенной через Яндекс.Директ: Объявление M-6992587 "KIA RIO 3 от 4 660 руб в месяц!" По ключевому)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            217,
            R"(mp367-2.eml)",
            false,
            LR"(1 октября 2014 Startup Career Night 14-16 октября 2014 года в Технополисе «Москва» пройдет ежегодный Форум «Открытые инновации» В этом году мы проводим Startup Career Night (мероприятие аналогичное нашему ежегодному фестивалю)",
            R"([MPROTO-367] искоренение бессмысленного ферстлайна "Если письмо отображается неправильно")"
        ),
        std::make_tuple(
            218,
            R"(1258051088.eml)",
            false,
            LR"(Si vous ne parvenez pas à lire cet email correctement, consultez cette page Mariy, profitez de 50€ remboursés sur votre dossier de crédit validé en vous inscrivant gratuitement à iGraal. Cetelem est une marque de BNP Paribas Personal Finance. BNP Paribas Personal Finance - Etablissement de crédit - S.A. au capital de 453 225 976 €. Siege social : 1, Boulevard Haussmann)",
            R"(Письма на латинских языках)"
        ),
        std::make_tuple(
            219,
            R"(letter1991.eml)",
            false,
            LR"()",
            R"([EXMAILPROTO-1991] Удаление из ферстлайна бессмысленного "Если не отображаются картинки, смотрите здесь")"
        ),
        std::make_tuple(
            220,
            R"(ebay-real.eml)",
            false,
            LR"(Подтверждено. Расчет. дата доставки: Thu. May. 18 - Wed. Jun. 14. eBay обновит расчетную дату доставки после того, когда он будет отправлен по адресу Молодежная, 8-52.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            221,
            R"(mp1852smile4.eml)",
            false,
            LR"(Безразличный смайлик K)",
            R"([EXMAILPROTO-1852] Обработка смайликов, которую так и не исправили)"
        ),
        std::make_tuple(
            222,
            R"(mailproto1990-2.eml)",
            false,
            LR"(Зеркальный лабиринт - 100 руб. вместо 200 Самое необычное развлечение! Билет в семейный Зеркальный лабиринт Pikabollo за 100 руб. вместо 200! Испытайте себя и найдите выход!)",
            R"([EXMAILPROTO-1990] Искоренение бессмысленных ферстлайнов "iPhone Android")"
        ),
        std::make_tuple(
            223,
            R"(1258048536.eml)",
            false,
            LR"(Здравствуйте, Наталья! Вашей фотографии поставлена новая оценка! Чтобы ее увидеть, перейдите по ссылке: http://www.odnoklassniki.ua/ Если указанная выше ссылка не открывается, скопируйте ее в буфер обмена, вставьте в)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            224,
            R"(ufotki8.eml)",
            false,
            LR"(Ваша фотография «f_49800f6f23156.jpg» больше не участвует в конкурсе «Очень приятно, Пух» на Яндекс.Фотках.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            225,
            R"(letter1991-3.eml)",
            false,
            LR"(Теплоноситель "Эко-Норма": http://avtohim-rost.ru/g3391294-teplonositel-eko-norma Теплоноситель "Эксперт": http://avtohim-rost.ru/g3391286-teplonositel-ekspert-osnove Отличия в применении теплоносителей на основе пропиленгликоля и этиленгликоле I.)",
            R"([EXMAILPROTO-1991] Удаление из ферстлайна бессмысленного "Если не отображаются картинки, смотрите здесь")"
        ),
        std::make_tuple(
            226,
            R"(mailproto1994-4.eml)",
            false,
            LR"(Mini Shatsu СКИДКА ДО 35% Mini Shatsu – бренд детской одежды из США, который существует уже более 20 лет. Эти товары уникальны, они выпускаются в ограниченном количестве, чтобы больше ни у кого не было таких же вещей. Kimuratan)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            227,
            R"(trivial.eml)",
            false,
            LR"(Как дела?)",
            R"(Общий кейс: на вырезание  тривиальных приветствий)"
        ),
        std::make_tuple(
            228,
            R"(1258048631.eml)",
            false,
            LR"()",
            R"(Общий кейс: письмо состоящее только из ссылки)"
        ),
        std::make_tuple(
            229,
            R"(mp-1512.eml)",
            false,
            LR"(+ lego-team@ Про чиселки помню, что лет 5 назад они были, но потом от них отказались. Ребята, не помните подробностей? Лола Кристаллинская руководитель отдела дизайна и исследований интерфейсов)",
            R"([EXMAILPROTO-1512] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            230,
            R"(turk6.eml)",
            false,
            LR"(Formspring just asked you (aliahturk) a question.)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            231,
            R"(mp2207_4.eml)",
            false,
            LR"(Похоже Вы кроме как собственных мыслей, иных даже не признаете. Что же, флаг Вам в руки, keep it up!)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            232,
            R"(startrack18.eml)",
            false,
            LR"(— deleted comment: а этот удалим)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            233,
            R"(1258050994.eml)",
            false,
            LR"(Я в Моем Мире - http://my.mail.ru/mail/mariya-0788/)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            234,
            R"(mp1852smile1.eml)",
            false,
            LR"(Смайликов ватага J J L L K K на помощь к нам спешит.)",
            R"([EXMAILPROTO-1852] Обработка смайликов, которую так и не исправили)"
        ),
        std::make_tuple(
            235,
            R"(turk4.eml)",
            false,
            LR"(We noticed that you signed up for StumbleUpon a few days ago and we just wanted to check in with you and help you make the most of your time exploring the web with us!)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            236,
            R"(mp88-forsq4.eml)",
            false,
            LR"(with Bonifaci, Олег)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            237,
            R"(startrack12.eml)",
            false,
            LR"(— добавил связь с MAILWEB-207 : Кривое сообщение об ошибке капчи у юзера)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            238,
            R"(ufotki10.eml)",
            false,
            LR"(Ваша фотография «добавиь, снять и запретить на конкурс» больше не участвует в конкурсе «Очень приятно, Пух» на Яндекс.Фотках.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            239,
            R"(mp2269_1.eml)",
            false,
            LR"()",
            R"([MPROTO-162][EXMAILPROTO-2269][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            240,
            R"(mailproto1554.eml)",
            true,
            LR"(Мы и не закладываемся, но хочется чтобы тестирование на небольших объемах не тормозило на пустом месте. Подозреваю что и все остальные операции задействующие мастер тормозят просто это то что первое что сразу)",
            R"([EXMAILPROTO-1554][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            241,
            R"(mailproto2071.eml)",
            true,
            LR"(Это то, что отображает интерфейс YT (но там еще без архива пробок, так что еще +50-60Tb).)",
            R"([EXMAILPROTO-2071][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            242,
            R"(mp282-vk-5.eml)",
            false,
            LR"(Alexander, под шдяпой. Очевидно же)",
            R"([MPROTO-282] улучшение ферстлайна для писем от ВК)"
        ),
        std::make_tuple(
            243,
            R"(mailproto1515.eml)",
            false,
            LR"(дежурных нет, но мало-ли кому в выходные хочется почитать рабочую почту)",
            R"([EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            244,
            R"(mailproto1994-8.eml)",
            false,
            LR"(Света, спасибо, что зарегистрировались в Клубе Mamsy! Ваш логин: sveta.AlekseeVaSV@mail.ru Ваш пароль: ac503ce8 Всё, что Вам необходимо знать о Mamsy: Проверяйте свою почту Каждое утро в 8:45 Вас ждут более 15 новых акций распродаж на)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            245,
            R"(mp-2069.eml)",
            true,
            LR"(Я не понял где тут аппенд. Если писать все строго из одного места, то можно записать уже сортированную таблицу при помощи -writesorted из командной строки или UM_SORTED флага TUpdate в С++ интерфейсе.)",
            R"([EXMAILPROTO-1518][EXMAILPROTO-2069] Срезка подписи)"
        ),
        std::make_tuple(
            246,
            R"(mp88-forsq1.eml)",
            false,
            LR"(Расти жопа)",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            247,
            R"(startrack7.eml)",
            false,
            LR"(— изменила поля: Статус Открыт, Утвержден Исполнитель Екатерина Горенчук, Мария Вульбрун Доступ Екатерина Горенчук)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            248,
            R"(vkrugudruzei.eml)",
            false,
            LR"(Вам пришло новое сообщение на сайте vkrugudruzei.ru.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            249,
            R"(amazon-real.eml)",
            false,
            LR"(Order Confirmation Thank you for shopping with us. You ordered "Derek Heart Women's...". We’ll send a confirmation when your item ships. Details Order #112-1040215-5657068 Arriving: Tuesday, May 2 - Thursday, May 11 Ship to: Julia Sergeeva Molodegnaya, 8-52... Total Before Tax: Estimated Tax: Order Total: $39.03 $0.00 $39.03 *RUB 2 340,40 We hope to see you again soon.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            250,
            R"(1258048678.eml)",
            false,
            LR"(Здравствуйте, Nano! Вашей фотографии поставлена новая оценка! Чтобы ее увидеть, перейдите по ссылке: http://www.odnoklassniki.ru/ Если указанная выше ссылка не открывается, скопируйте ее в буфер обмена, вставьте в адресную)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            251,
            R"(support_lovesupport4.eml)",
            false,
            LR"(Костя (М), 30 - 1 сообщение)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            252,
            R"(mp2037_2.eml)",
            false,
            LR"(If you are unable to view this, please click here This email was sent to tauman@ya.ru by Innvo Labs Limited. You may UNSUBSCRIBE from this email, but you’ll miss out on PLEOworld latest news and offers.)",
            R"([EXMAILPROTO-2037]  Срезка бессмысленного "If you are unable to view this message correctly, click here." для всех писемв том числе и от "europcar-eci@europcar-news.com")"
        ),
        std::make_tuple(
            253,
            R"(1881.137271438.3940802955101674572742887676979.eml)",
            false,
            LR"(да. нас могут читать дети. срочно все стираем)",
            R"(Общий кейс: письмо от соответствующего домена (фейсбук))"
        ),
        std::make_tuple(
            254,
            R"(mailproto1562.eml)",
            true,
            LR"(Дайте какие-нибудь времена, когда у вас в логе этот запрос появился - посмотрим по логам.)",
            R"([EXMAILPROTO-1562][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            255,
            R"(mailproto1994-5.eml)",
            false,
            LR"(Ami Ami СКИДКА ДО 35% Ami-Ami появился в Японии чуть больше 20 лет назад. Специалисты этого бренда своими основными приоритетами считают безупречное качество, необычный яркий дизайн и только натуральные ткани и)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            256,
            R"(yacal)",
            false,
            LR"(zoria stella, Яндекс.Календарь напоминает, что сегодня в 20:55 у вас запланировано событие "Интерны" (Телеканал '1+1').)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            257,
            R"(mp1933_5_aboutme.eml)",
            false,
            LR"(Pro Tip: Add your city to find photographers near you)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает бессмысленные фразы вида "Email not displaying ...".)"
        ),
        std::make_tuple(
            258,
            R"(mp1847.eml)",
            false,
            LR"(Я с вами ))",
            R"([EXMAILPROTO-1847] Срезка цитирования вложенного письма)"
        ),
        std::make_tuple(
            259,
            R"(wrong_utf3.eml)",
            false,
            LR"(2X2TV.ru Еженедельное подписное издание обновлений Ваш логин на сайте: art-evdokimov У нас произошли следующие обновления: Блог : Новости: Сетка с 7 по 13 сентября. [ Adult Swim ] is back ( 04.09.2009 / 18:24 ) - Комментариев: 89 Форум (10)",
            R"(Письма в неправильных кодировках)"
        ),
        std::make_tuple(
            260,
            R"(welcome.eml)",
            false,
            LR"(Welcome to Twitter, Nastassia Klishevich (@lostlittle)! We're excited you're here! Twitter is all about what's happening right now. Follow what you're interested in and get Tweets in real time. You'll never be out of the loop again. Get started on Twitter Discover who's on twitter Browse popular accounts by interest or look for your friends and follow the ones you like. Check back)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            261,
            R"(startrack3.eml)",
            false,
            LR"(так ты правь не в общем тикете, а только своем. Так или иначе тут только "методом научного тыка" у меня нет доступа к этой форме, так что пробуй)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            262,
            R"(fotostrana1995-3.eml)",
            false,
            LR"(Агриппина, вы недавно зарегистрировались на Фотостране.. Это сайт, где легко найти друзей и развлечения! Уже 44,132,251 человек пользуются Фотостраной. Найди своих друзей на Фотостране прямо сейчас! Найти своих)",
            R"([EXMAILPROTO-1995] Исправление ферстлайна для писем различной тематики от сайта "Фотострана")"
        ),
        std::make_tuple(
            263,
            R"(letter1991-2.eml)",
            false,
            LR"(Уважаемые коллеги!!! Спешите забронировать Вьетнам на майские праздники Гарантированные номера в лучших отелях Нячанга, Фантиета и Дананга! 11 ночей/12 дней Asia Paradise 3*BB 1440 у.е Michelia 4*BB 1600 у.е Novotel 4*BB 1725 у.е Havana 5*BB 1700)",
            R"([EXMAILPROTO-1991] Удаление из ферстлайна бессмысленного "Если не отображаются картинки, смотрите здесь")"
        ),
        std::make_tuple(
            264,
            R"(mp2122.eml)",
            false,
            LR"()",
            R"([MPROTO-31][EXMAILPROTO-2122][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи и начала цитирования)"
        ),
        std::make_tuple(
            265,
            R"(startrack4.eml)",
            false,
            LR"(Машину поднял, но выдать доступы через cauth по-человечески не могу, потому что при создании виртуалки в openstack'е знание доезжает до голема не сразу. Саша, дал тебе пока root'а.)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            266,
            R"(mp1933_9_aboutme.eml)",
            false,
            LR"(Pro Tip: Add your city to find photographers near you)",
            R"([EXMAILPROTO-1933] На правило для отправителя team.about.me и общее правило, которое вырезает бессмысленные фразы вида "Email not displaying ...".)"
        ),
        std::make_tuple(
            267,
            R"(mp-2948.eml)",
            false,
            LR"(Скидка 15% для всех искателей сокровищ! ПРАЗДНИК ДЛЯ ВАС ПРАЗДНИК ДЛЯ ДЕТЕЙ ВСЕ СКИДКИ САЙТА Следуйте списку и станьте пиратом! Отправиться в захватывающее приключение вам поможет скидка 15% на всё в разделе)",
            R"([MPROTO-2948] Срезка бессмысленного "Не видно картинки? Кликните сюда")"
        ),
        std::make_tuple(
            268,
            R"(1258048531.eml)",
            false,
            LR"(Вложила пару статей,почитай,может поможет что-то понять,какие-то рекомендации для себя принять.)",
            R"(Общий кейс: вычисление ферстлайна для писем с вложениями)"
        ),
        std::make_tuple(
            269,
            R"(money_mail1.eml)",
            false,
            LR"()",
            R"([DARIA-30317] Письма о переводе Яндекс.Денег во вложении должны быть с пустым ферстлайном)"
        ),
        std::make_tuple(
            270,
            R"(uzoo2.eml)",
            false,
            LR"(Ага, мне тоже понравился)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            271,
            R"(mp282-vk-3.eml)",
            false,
            LR"(1 человек принял Вашу заявку в друзья Имя: Максим Булыгин Выпуск: СПбГУТ им. Бонч-Бруевича '13 У Вас с Максимом 7 общих друзей Написать Максиму Рекомендовать друзей Alexander, Вы можете ограничить или отменить)",
            R"([MPROTO-282] улучшение ферстлайна для писем от ВК)"
        ),
        std::make_tuple(
            272,
            R"(smth.eml)",
            false,
            LR"(- "А я уважаю пиратов)",
            R"(Общий кейс: письмо от соответствующего домена (фейсбук))"
        ),
        std::make_tuple(
            273,
            R"(mp2207_6.eml)",
            false,
            LR"(да я по цене глянул.... и понял что лучше на зуме купить пентакс.. канон дороговат...)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            274,
            R"(startrack20.eml)",
            false,
            LR"(фффффайлы)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            275,
            R"(1258048706.eml)",
            false,
            LR"(Здравствуйте, Виктор! Вашей фотографии поставлена новая оценка! Чтобы ее увидеть, перейдите по ссылке: http://www.odnoklassniki.ru/ Если указанная выше ссылка не открывается, скопируйте ее в буфер обмена, вставьте в)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            276,
            R"(uzoo11.eml)",
            false,
            LR"(7 апреля Ольга Куликова указала, что работала счетоводом в компании «ООО "Рога и Копыта"» в одно время с вами)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            277,
            R"(1667.142062698.282448285971560141612146061859.eml)",
            false,
            LR"(в еженедельно, вторник, начиная с 12.01.2010 с 17:00 до 18:00 (GMT+03:00) Moscow, St. Petersburg, Volgograd.)",
            R"(Общий кейс: письмо от соответствующего домена (я.календарь))"
        ),
        std::make_tuple(
            278,
            R"(mp2207_5.eml)",
            false,
            LR"(:) при чем тут ронять-то? Ведь речь то не о том, будите Вы его ронять/бросать/или еще что-тов этом духе, ведь не для того камера покупаеться (я так надеюсь). Я ведь имел ввиду (и писал об этом) стоимость)",
            R"([EXMAILPROTO-2207] Улучшение ферстлайна писем от Маркета)"
        ),
        std::make_tuple(
            279,
            R"(Order_Exist_RU.eml)",
            false,
            LR"(Подтверждение заказа № ZV-0295019 Выбранный вами способ доставки: Самовывоз Каталог Код детали Описание Кол-во Сумма Mahle Original TM 13 97 Термостат c прокладкой 1 2198 Стоимость доставки 0,00 Р Общая сумма 2198,00 Р Ваш код)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            280,
            R"(1258048691.eml)",
            false,
            LR"(- Внимание! В конце выпуска важное объявление! Читать Новости Курса (12 ноября 2009). C удовольствием награждаем Вас, Татьяна! Здравствуйте Татьяна, вам встречалось выражение "First lady - Первая Леди"? Конечно,)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            281,
            R"(mp88-forsq5.eml)",
            false,
            LR"(Вернулся и сразу к подданным))",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            282,
            R"(1258048656.eml)",
            false,
            LR"(Trimpampusik ???Tanusik???)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            283,
            R"(mp162_3.eml)",
            false,
            LR"(cgsdf)",
            R"([MPROTO-162][EXMAILPROTO-1514][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            284,
            R"(mailproto1994-14.eml)",
            false,
            LR"(Новый поиск на www.irr.ru Мы значительно улучшили алгоритм выдачи результатов по поисковым запросам. Например, отныне при поиске автомобиля вы не увидите объявлений о продаже загородного дома с гаражом – ничего)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            285,
            R"(mp-1797.eml)",
            false,
            LR"(Здравствуйте, это команда Яндекс.Почты. Жаль, что Вы редко к нам заходите. Возможно, Почта могла бы помочь Вам в повседневных делах – для этого мы её и создаем. Посмотрите, какие задачи можно решить прямо сейчас,)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-1797] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            286,
            R"(letter1991-4.eml)",
            false,
            LR"(приглашаем Вас принять участие в курсе обучения ШКОЛЫ ПРОДАКТ МЕНЕДЖЕРОВ с 16 мая - по 15 июня 2014 года! Зарегистрировать ЗАЯВКУ Школа Продакт Менеджеров - специальные знания и навыки за короткий период!)",
            R"([EXMAILPROTO-1991] Удаление из ферстлайна бессмысленного "Если не отображаются картинки, смотрите здесь")"
        ),
        std::make_tuple(
            287,
            R"(livejournal.eml)",
            false,
            LR"(lokomotiv написал на тему "Мои твиты". Метки записи: "twitter")",
            R"([MAILDLV-1986] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            288,
            R"(startrack13.eml)",
            false,
            LR"(— updated fields: Status Open, In Progress)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            289,
            R"(vkrugudruzej.eml)",
            false,
            LR"(Вам пришло новое сообщение на сайте vkrugudruzei.ru.)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            290,
            R"(mp1852smile3.eml)",
            false,
            LR"(Печальный смайлик L)",
            R"([EXMAILPROTO-1852] Обработка смайликов, которую так и не исправили)"
        ),
        std::make_tuple(
            291,
            R"(19april-02.eml)",
            false,
            LR"(Их контакты для меня уже ищут, но пока BirdsEye выглядит интереснее.)",
            R"(Общий кейс: письмо с цитатами (данное письмо было добавлено по жалобе))"
        ),
        std::make_tuple(
            292,
            R"(mailproto2171-2.eml)",
            false,
            LR"()",
            R"([EXMAILPROTO-2171] Искоренение бессмысленных ферстлайнов  "Шопинг-клуб KupiVIP.ru подобрал для Вас лучшие товары дня")"
        ),
        std::make_tuple(
            293,
            R"(mailproto1994-12.eml)",
            false,
            LR"(1000 рублей в подарок от Biglion Ретро-автомобили на IRR.RU Сперва поздравляем всех владельцев авто с их днем – 27 октября в России мастера кардана и жести справляли день автомобилиста. Этот праздник в равной степени)",
            R"([EXMAILPROTO-1994] Искоренение бессмысленных ферстлайнов "Услуги Рестораны Отели Туры Женщинам")"
        ),
        std::make_tuple(
            294,
            R"(1258048639.eml)",
            false,
            LR"(Всего доброго, Юрий Данько Chat icq: 461276200 Skype: yuridanko Google Talk: yuradan Contact Me Данное сообщение отправлено Вам, так как Вы являетесь подписчиком группы "Киевская группа развития IAAP" в Группах Google. Для того, чтобы отправить)",
            R"(Общий кейс: вычисление ферстлайна для писем с вложениями)"
        ),
        std::make_tuple(
            295,
            R"(1258048612.eml)",
            false,
            LR"(very thanks for you for your care of me ... i'm good only i want see you ...how are you ? how you live ? i know you have many work & you can not came in near time but i want you try and try came to me ... i don not know what i write to you..... dodo i need money because i have some problems about money ...if you can send me ...i will very thanks for you and if you can not send)",
            R"(Общий кейс: на обработку картиночных смайликов в письме)"
        ),
        std::make_tuple(
            296,
            R"(x247-2.eml)",
            false,
            LR"(Ну ниче так) он бухает? Хуй большой? Кто в сексе?)",
            R"([MPROTO-1673] В firstline письма не должны попадать непечатные символы)"
        ),
        std::make_tuple(
            297,
            R"(uzoo10.eml)",
            false,
            LR"(Ваше видео прокомментировали)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            298,
            R"(mp2017_1.eml)",
            false,
            LR"()",
            R"([EXMAILPROTO-1518][EXMAILPROTO-2017] Срезка подписи)"
        ),
        std::make_tuple(
            299,
            R"(mp2364.eml)",
            false,
            LR"(Я зачислила средства с вашего баланса на оплату выставленного счета. В дальнейшем вы сможете оплачивать счета, выставленные до пополнения баланса по данной инструкции:)",
            R"([EXMAILPROTO-2364] Искоренение бессмысленной фразы "Напишите свой ответ над этой чертой")"
        ),
        std::make_tuple(
            300,
            R"(startrack6.eml)",
            false,
            LR"(тогда с этими изменениями можем ехать на базки с неактивными пользователями?)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            301,
            R"(mp88-forsq2.eml)",
            false,
            LR"(Финансируешь американских буржуев?)))",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            302,
            R"(mailproto2174-2.eml)",
            false,
            LR"(Услуги› Перманентный макияж в салоне красоты «Жетэм» Скидка 50% за 44000 р. Уход за кожей лица в салоне красоты «Инфинити» за полцены Скидка 50% за 12000 р. Оформление однократной литовской визы от туристической)",
            R"([EXMAILPROTO-2174] Искоренение бессмысленных ферстлайнов  "Письмо отображается некорректно? Посмотрите исходную версию на сайте!)"
        ),
        std::make_tuple(
            303,
            R"(mp367.eml)",
            false,
            LR"(У нас важные новости! В связи с нерентабельностью отправки через СПСР мы временно прекращаем работать с Pochtoy.com express и Pochtoy.com priority . Последние отправки этими службами будут 6-го и 13-го октября. Все остальные)",
            R"([MPROTO-367] искоренение бессмысленного ферстлайна "Если письмо отображается неправильно")"
        ),
        std::make_tuple(
            304,
            R"(mp2294_2.eml)",
            false,
            LR"(Как ты?)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-2294] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            305,
            R"(1258048595.eml)",
            false,
            LR"(Произошли изменения в показах объявлений Вашей рекламной кампании N 1809585 (Швейцария), размещенной через Яндекс.Директ: Объявление M-5509504 "Все #отели Швейцарии#!!!" По ключевому слову/словосочетанию "отели Гштаада")",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            306,
            R"(mp2037_3.eml)",
            false,
            LR"(Wherever you go in this world, you'll always find our great offers. Spring is here! Drive towards the sun! And still: save up to 25% when you pay online! Discover Europe's romantic getaways Spain from $17 a day > France from $23 a day > Italy from $55 a day > and many other destinations Discover Europe's top spots UK from $19 a day > Portugal from $28 a day > Sweden from $33 a day)",
            R"([EXMAILPROTO-2037]  Срезка бессмысленного "If you are unable to view this message correctly, click here." для всех писемв том числе и от "europcar-eci@europcar-news.com")"
        ),
        std::make_tuple(
            307,
            R"(mp88-forsq11.eml)",
            false,
            LR"(Да, недурно )))",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            308,
            R"(turk1.eml)",
            false,
            LR"(Bu email'i doğru şekilde görüntüleyemiyorsanız lütfen tıklayın.)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            309,
            R"(letter1993.eml)",
            false,
            LR"(Автор Rustem Компьютеры и интернет → Обзоры → Обзоры ПО → Xportable - портативный софт и игры Photodex ProShow Producer 6.0.3397 portable XPORTABLE.ru - Portable soft Photodex ProShow Producer 6.0.3397 portable 2014-02-10 22:03 botanik Photodex ProShow Producer — программа для)",
            R"([EXMAILPROTO-1993] Удаление из ферстлайна бессмысленного "Если выпуск не отображается, вы можете прочесть его на сайте")"
        ),
        std::make_tuple(
            310,
            R"(startrack14.eml)",
            false,
            LR"(А теперь на сцене Андре и англ. в настройках)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            311,
            R"(mp88-forsq10.eml)",
            false,
            LR"(Да ты ему просто завидуешь!)))",
            R"([MPROTO-88] улучшение ферстлайна для писем от форсквера)"
        ),
        std::make_tuple(
            312,
            R"(test_fl_DARIA_6741.eml)",
            false,
            LR"(Прошу прощения, что так долго отвечала, у меня письмо в спам упало зачем-то.)",
            R"([EXMAILPROTO-1512][DARIA-6741] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            313,
            R"(test_fl_DARIA_3141.eml)",
            false,
            LR"(Аутглюк не запускался на моей машине практически никогда. лцмо ьл туомжцущшо тожумоцоумрф мдуьмуомуомцйу мьлдуджошуц мцьудлп)",
            R"([DARIA-3141] Ошибочная обрезка первой буквы)"
        ),
        std::make_tuple(
            314,
            R"(mailproto1553.eml)",
            true,
            LR"(То, что некоторые команды выполняются по 7 секунд не является деградацией работы кластера, он и не рассчитан на интерактивное выполнение. То, что вы закладываетесь на это - это не очень хорошо.)",
            R"([EXMAILPROTO-1553][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            315,
            R"(mp1514.eml)",
            false,
            LR"(Я думаю, это -- дата центры -- производство? не всё же они делают по контрактам в Китае. --ab)",
            R"([MPROTO-162][EXMAILPROTO-1514][EXMAILPROTO-1519][EXMAILPROTO-1518] Срезка подписи)"
        ),
        std::make_tuple(
            316,
            R"(startrack1.eml)",
            false,
            LR"(Голосом договорились, что фейковые ленульки lenulca-qa.mail.yandex.net, lenulca2-qa.mail.yandex.net будут работать на самом балансере. Поэтому необходимость в этих хостах отпала, я их разобрал.)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            317,
            R"(mp2037_4_europcar.eml)",
            false,
            LR"(3 days left ! Up to 20% off your next rental)",
            R"([EXMAILPROTO-2037]  Срезка бессмысленного "If you are unable to view this message correctly, click here." для всех писемв том числе и от "europcar-eci@europcar-news.com")"
        ),
        std::make_tuple(
            318,
            R"(1258048625.eml)",
            false,
            LR"(Дайджест MOSKVA.FM с 25 по 2 октября Мы рады предложить вам дайджест MOSKVA.FM с 25 по 2 октября. Новости FM-радиостанций На частоте Мегаполис FM начала работу Интернет-радиостанция для студентов и молодежи «Зачетное» радио)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            319,
            R"(videoletter)",
            false,
            LR"(Открыть видеописьмо Пароль: gtgG4 http://video.yandex.ru/mail/jLW5_N9dFEgR_sfXyqd03XzrxXwWhIpG)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            320,
            R"(uzoo9.eml)",
            false,
            LR"(У вашего видео появилась новая метка)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            321,
            R"(yandexvideo.eml)",
            false,
            LR"(Ваше видео сохранено на Яндекс.Видео)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            322,
            R"(letter1997.eml)",
            false,
            LR"(Отписаться можно здесь Твой гид на каждый день! Самое интересное в одном письме Как себя побаловать? Развлечения (5 шт.) Все развлечения > Скидка до 96% на вход, приватные танцы, фирменный коктейль и карту клуба)",
            R"([EXMAILPROTO-1997] Искоренение бессмысленных ферстлайнов про отображение изображений)"
        ),
        std::make_tuple(
            323,
            R"(startrack10.eml)",
            false,
            LR"(Сейчас lenulca отдаёт 400.)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            324,
            R"(1258048542.eml)",
            false,
            LR"(Национальный сервис цифровой фотопечати NetPrint.ru Подарочные фотокомплекты - новогодняя сказка Вы уже задумываетесь, что подарить вашим близким на Новый год? Подсчитывали, сколько времени и денег нужно)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            325,
            R"(1258048626.eml)",
            false,
            LR"(Quality HERBAL in licensed clinic now VISITIN US NOW >>)",
            R"(Общий кейс: письмо состоящее из текста и внешней ссылки)"
        ),
        std::make_tuple(
            326,
            R"(letter1999-2.eml)",
            false,
            LR"(Вы получили это письмо, так как подписаны на обновления Сергея Абрамова Для корректного отображения письма, включите показ изображений в почтовой программе Вас заинтересовал обучающий тренинг “Системный)",
            R"([EXMAILPROTO-1999] Удаление из ферстлайна бессмысленного  "вы получили это письмо")"
        ),
        std::make_tuple(
            327,
            R"(yandexfotki.eml)",
            false,
            LR"("И от улыбки беспрестанной - крепчает кожа на щеках.........")",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            328,
            R"(citilink.eml)",
            false,
            LR"(Клуб Ситилинк Акции Мой кабинет Андрей, Спасибо за оформление заказа Q8912630 на www.citilink.ru! Состав Вашего заказа: Товар Кол-во Цена 669058 Батарея для ИБП IPPON IP12-9 12В, 9Ач 1 1240 руб. 294583 Внешний жесткий диск SEAGATE Expansion Portable)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            329,
            R"(wrong_utf4.eml)",
            false,
            LR"(2X2TV.ru Еженедельное подписное издание обновлений Ваш логин на сайте: maxim007 У нас произошли следующие обновления: Блог : Новости: Сетка с 7 по 13 сентября. [ Adult Swim ] is back ( 04.09.2009 / 18:24 ) - Комментариев: 89 Форум (10 последних)",
            R"(Письма в неправильных кодировках)"
        ),
        std::make_tuple(
            330,
            R"(uzoo4.eml)",
            false,
            LR"(В Вашей рекламной кампании 1739486 ("Почта России"), произошли следующие изменения: 15.04.2009 14:59: Добавлены новые объявления: в M-4928939:)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            331,
            R"(uzoo1.eml)",
            true,
            LR"(К сожалению, Ваша кампания N1739486 "Почта России" была отклонена. Причины отклонения: Объявление M-4928939, "Доставка почты голубями": По тексту объявления: Содержание страницы сайта, на которую ссылается объявление,)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            332,
            R"(mp2294_3.eml)",
            false,
            LR"(Привет, как дела?)",
            R"([EXMAILPROTO-1512][EXMAILPROTO-2294] Срезка обращений и приветствий)"
        ),
        std::make_tuple(
            333,
            R"(mailproto1617.eml)",
            false,
            LR"(И опять порвали)",
            R"([EXMAILPROTO-1617][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            334,
            R"(startrack8.eml)",
            false,
            LR"(Выехало в Угрешку. Я бы предпочёл, чтобы в остальные ДЦ доехало в понедельник. Кстати, мы по-тихоньку начинаем обрастать приборами - http://gr.yandex-team.ru/dashboard/#mail_furita.)",
            R"(Улучшение фестлайна для писем от startrack)"
        ),
        std::make_tuple(
            335,
            R"(uzoo16.eml)",
            false,
            LR"(Ваше видео сохранено на Яндекс.Видео)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            336,
            R"(suggest_friend.eml)",
            false,
            LR"(Hi Nastassia, Daniil Fukalov suggests you add Victor Sergeev as a friend on Facebook. Victor Sergeev Thanks,)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            337,
            R"(mailproto2072.eml)",
            true,
            LR"(А это сжатых, с тройной репликацией, или исходных?)",
            R"([EXMAILPROTO-2072][EXMAILPROTO-1515] Недопущение взятия ферстлайна из цитат)"
        ),
        std::make_tuple(
            338,
            R"(1.eml)",
            false,
            LR"(Hemen paylaşın, fırsatları çoğaltın: 13 Temmuz için Günün İstanbul Tatil Fırsatları: Neşeli Tur’dan Ocean Majesty Cruise gemisi ile, 4 gece 5 günlük, tam pansiyon plus, Mykonos, Rodos, Santorini, Pire, Atina turu size özel fiyatlarla! Limitli sayıdadır! 669 TL Değeri İndirim Kazancınız 1199 TL %44 530 TL Paylaş, 6 TL Kazan! Gununtatili.com'dan Belek)",
            R"(Письма на турецком)"
        ),
        std::make_tuple(
            339,
            R"(mp1847_2.eml)",
            false,
            LR"(А когда в планах обновление Plato?)",
            R"([EXMAILPROTO-1847] Срезка цитирования вложенного письма)"
        ),
        std::make_tuple(
            340,
            R"(1258048618.eml)",
            false,
            LR"(We are glad to inform you that the member OLIVE75 (1000042639) sends you a virtual smile! A virtual smile means that the member is interested in contacting you. Please, be polite to answer with a message or a smile in return. You can send it by following the link: http://www.natashaclub.com/vkiss.php?sendto=1000042639&from=1000200252&ConfCode=ZG10NVRXVXVabXgwZG5oNVVRPT0%3D Thank)",
            R"(Общий кейс: письмо от соответствующего домена)"
        ),
        std::make_tuple(
            341,
            R"(1258048551.eml)",
            false,
            LR"(Ирина Щекалева)",
            R"(Общий кейс: письмо от соответствующего домена)"
        )
    )
);

}  // namespace anonymous
