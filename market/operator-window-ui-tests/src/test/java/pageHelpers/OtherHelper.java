package ui_tests.src.test.java.pageHelpers;

import Classes.Email;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import tools.Tools;
import unit.Config;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * пример запроса для поиска
 * api.db.of('loyaltyPromo')
 * .withFilters{ eq('title', '508') }
 * .list()
 * <p>
 * пример запроса для редактирование
 * def obj = api.db.get('loyaltyPromo@234234') \\n
 * api.bcp.edit(obj, ['status' : 'archived','':''])
 * <p>
 * Пример запроса на создания
 * api.bcp.create('loyaltyPromo',['code':'100014','promoId':'100014','promoValue':'29900'])
 * <p>
 * Пример получения свойства
 * api.db.get(gid).archived
 * <p>
 * Пример с фильтрацией
 * def ticket = api.db.of('ticket$beru')
 * .withFilters {
 * not(eq('order', null))
 * eq('archived', false)
 * }
 * .withOrders(api.db.orders.desc('creationTime'))
 * .limit(1)
 * .get()
 */
public class OtherHelper {

    private WebDriver webDriver;

    public OtherHelper(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Выполнить скрипт со страницы администрирования и получить его ответ
     *
     * @param script скрипт который необходимо выполнить. скрипт передавать одной строкой без переносов
     * @return
     */
    public String runningScriptFromAdministrationPage(String script) {
        return runningScriptFromAdministrationPage(script, true);
    }


    /**
     * Выполнить скрипт со страницы администрирования
     *
     * @param script          скрипт который необходимо выполнить. скрипт передавать одной строкой без переносов
     * @param waitForResponse жать ответа от запроса
     * @param countSecondWaitingResult количество секунд на ожидание ответа
     * @return
     */
    public String runningScriptFromAdministrationPage(String script, boolean waitForResponse,int countSecondWaitingResult) {

        //Результат запроса
        StringBuilder result = new StringBuilder();
        String errorMessage="";

        for (int x = 0; x < 5; x++) {
            try {
                //Флаг падения запроса
                boolean resultRequest = true;
                //Текст запроса
                String s;
                //Количество повторений ожидания ответа запроса
                int secondsToWaitForResult;
                if (waitForResponse) {
                    secondsToWaitForResult = countSecondWaitingResult;
                } else {
                    secondsToWaitForResult = 2;
                }

                script = script.replaceAll("\\n", "\\\\n");

                webDriver.manage().logs().get(LogType.BROWSER);

                s = "fetch(\"" + Config.getProjectURL() + "/api/jmf/ui/admin/script\", {\n" +
                        "  \"headers\": {\n" +
                        "    \"accept\": \"*/*\",\n" +
                        "    \"accept-language\": \"ru,en;q=0.9\",\n" +
                        "    \"cache-control\": \"no-cache\",\n" +
                        "    \"content-type\": \"text/plain\",\n" +
                        "    \"pragma\": \"no-cache\",\n" +
                        "    \"sec-fetch-dest\": \"empty\",\n" +
                        "    \"sec-fetch-mode\": \"cors\",\n" +
                        "    \"sec-fetch-site\": \"same-origin\",\n" +
                        "    \"x-ajax-request\": \"true\",\n" +
                        "    \"x-requested-with\": \"XMLHttpRequest\",\n" +
                        "     \"x-xsrf-token\": \""+getCSRFToken(webDriver)+"\",\n" +
                        "  },\n" +
                        "  \"referrer\": \"" + Config.getProjectURL() + "/admin\",\n" +
                        "  \"referrerPolicy\": \"no-referrer-when-downgrade\",\n" +
                        "  \"body\": \"" + script + "\",\n" +
                        "  \"method\": \"POST\",\n" +
                        "  \"mode\": \"cors\",\n" +
                        "  \"credentials\": \"include\"\n" +
                        "})\n" +
                        ".then(response => response.text())\n";
                if (waitForResponse) {
                    s += ".then(result => console.error('Successful result  ',result))\n";
                }
                s += ".catch(error => console.error('Successful error', error));";
                Tools.scripts(webDriver).runScript(s);

                do {
                    if (secondsToWaitForResult-- > 0) {
                        Tools.waitElement(webDriver).waitTime(1000);

                        LogEntries logEntries = webDriver.manage().logs().get(LogType.BROWSER);
                        String lastMessage = "";
                        for (LogEntry logEntry : logEntries) {
                            String message = logEntry.getMessage();
                            if (!resultRequest) {
                                errorMessage = "Произошла ошибка при выполнении скрипта " + script + "\n логи ошибок: " + lastMessage + "\n" + logEntry;
                                throw new Error(errorMessage);
                            }
                            if (message.contains("Failed to load resource:")) {
                                resultRequest = false;
                                lastMessage = message;
                            }
                            if (message.contains("Successful result")) {
                                Pattern pattern = Pattern.compile("\\\\\".*\\\\\"");
                                Matcher matcher = pattern.matcher(message);
                                while (matcher.find()) {
                                    result.append(message.substring(matcher.start(), matcher.end()).replace("\\\"", ""));
                                }
                                break;
                            }
                        }
                    } else {
                        break;
                    }

                } while (result.toString().equals(""));
                if (result.toString().
                        equals("")) {
                    return null;
                } else {
                    return result.toString().trim();
                }
            } catch (Throwable throwable) {
                if (throwable.getMessage().contains("Произошла ошибка при выполнении скрипта")){
                    continue;
                }
            }
        }
        throw new Error(errorMessage);
    }

    /**
     * Выполнить скрипт со страницы администрирования
     *
     * @param script          скрипт который необходимо выполнить. скрипт передавать одной строкой без переносов
     * @param waitForResponse жать ответа от запроса
     * @return
     */
    public String runningScriptFromAdministrationPage(String script, boolean waitForResponse) {
       return runningScriptFromAdministrationPage(script,waitForResponse,30);
    }


    /**
     * Архивировать/разархивировать запись через выполнение скрипта.
     *
     * @param gidEntity         метакласс и id. Пример loyaltyPromo@123123
     * @param archiveARecording Архивировать/разархивировать запись
     * @return
     */
    public void archivedARecordThroughScripts(String gidEntity, boolean archiveARecording) {
        if (archiveARecording) {
            runningScriptFromAdministrationPage("def obj = api.db.get('" + gidEntity + "') \napi.bcp.edit(obj, ['status' : 'archived'])", false);
        } else {
            runningScriptFromAdministrationPage("def obj = api.db.get('" + gidEntity + "') \napi.bcp.edit(obj, ['status' : 'active'])", false);
        }
    }

    /**
     * Создать обращение через скрипт на основе письма
     *
     * @param email       письмо из которого нужно создать обращение
     * @param typeMailBox код почтового ящика
     */
    public void createTicketFromMail(Email email, String typeMailBox) {
        StringBuffer script = new StringBuffer();
        script.append("api.security.doAsSuperUser {\n" +
                "\n" +
                "def mailMessage = api.bcp.create('mailMessage$in', [\n" +
                "'receivedByMailServerAt': java.time.OffsetDateTime.now(),");
        if (email.getReplyTo().size() > 0) {
            Object[] emails = email.getReplyTo().keySet().toArray();
            Object[] nameEmails = email.getReplyTo().keySet().toArray();

            StringBuffer emails2 = new StringBuffer();
            for (int i = 0; i < emails.length; i++) {
                emails2.append("'" + emails[i].toString() + "'");
                if (i != emails.length - 1) {
                    emails2.append(",");
                }
            }
            script.append("'replyToList': [" + emails2.toString() + "], \n");//список из заголовка reply_to
            script.append("'senderName': '" + nameEmails[0].toString() + "', \n");// Имя отправителя
        }
        if (email.getTo() != null) {
            script.append("'recipientsTo':'" + email.getTo() + "', \n");//ящик получателя
        }
        if (email.getFromAlias() != null) {
            script.append("'sender': '" + email.getFromAlias() + "', \n");//ящик отправителя
        }
        script.append("'connection': '" + typeMailBox + "', \n");//тип ящика получателя
        script.append("'title': '" + email.getSubject() + "', \n");// заголовок
        script.append("'body': ''' \n" +
                "<div style=\\\"white-space:pre-wrap\\\">\n" +
                email.getText() +
                "</div>\n" +
                "''',\n");
        String headers ="";
        headers+="'to':['"+email.getTo()+"'],";
        headers+="'from':['"+email.getFromAlias()+"'],";
        if (!email.getHeaders().isEmpty()){
            for (Map.Entry head : email.getHeaders().entrySet()){
                headers+="'"+head.getKey()+"':['"+head.getValue()+"'],";
            }
        }
        script.append("'headers': ["+headers.substring(0,headers.length()-1)+"],\n");
        script.append("'messageId': java.util.UUID.randomUUID().toString(),\n" +
                "'deduplicationKey': java.util.UUID.randomUUID().toString()\n" +
                "])\n" +
                "\n" +
                "api.mailProcessing.processInMessage(mailMessage)\n\n" +
                "}\n" +
                "return 'OK'");
        runningScriptFromAdministrationPage(script.toString(), true);
    }

    /**
     * Найти запись по title
     *
     * @param metaTypeEntity в каком метаклассе искать
     * @param titleEntity    заголовок записи
     * @return
     */
    public String findEntityByTitle(String metaTypeEntity, String titleEntity) {
        String gid = null;

        for (int i = 0; i<Config.DEF_TIME_WAIT_LOAD_PAGE;i++){
            gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.of('"+metaTypeEntity+"')." +
                            "withFilters{ eq('title', '" + titleEntity + "') }\n" +
                            ".limit(1)\n" +
                            ".get()");
            if (gid==null){
                Tools.waitElement(webDriver).waitTime(1000);
            } else {
                break;
            }
        }
        if (gid == null) {
            throw new Error("Не нашлось записи метакласса " + metaTypeEntity + " с темой " + titleEntity + " за " + Config.DEF_TIME_WAIT_LOAD_PAGE + " сек");
        } else {
            return gid;
        }
    }

    /**
     * Сделать у заказа плательщика и получателя одним человеком
     * @param orderNumber номер заказа или его гид
     */
    public void buyerAndRecipientAreOnePerson(String orderNumber){
        Pattern pattern = Pattern.compile("\\w*@\\d*");
        Matcher matcher = pattern.matcher(orderNumber);

        runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "or{\n" +

                        (matcher.find()?
                                "   eq('gid','"+orderNumber+"')\n":"   eq('title','"+orderNumber+"')\n") +
                        "}\n" +
                        "}.limit(1).get()\n" +
                        "\n" +
                        "def customer = order.customer\n" +
                        "api.security.doAsSuperUser {\n" +
                        "  api.bcp.edit(customer,['email':order.buyerEmail,'phone':order.buyerPhone])\n" +
                        "}", false
        );
    }

    private String getCSRFToken(WebDriver webDriver){
        Set<Cookie> cookies = webDriver.manage().getCookies();
        for (Cookie cookie:cookies){
            if (cookie.getName().equals("XSRF-TOKEN")){
                return cookie.getValue();
            }
        }
        return null;
    }
}
