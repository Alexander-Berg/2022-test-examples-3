package ru.yandex.common.mining.bd.miner;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import ru.yandex.common.util.html.HtmlUtils;

/**
 * DomAnalysis Tester.
 *
 * @author kozyrev@yandex-team.ru
 */
public class DomAnalysisTest extends TestCase {

    public DomAnalysisTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsRegularLink() throws Exception {
        assertTrue(DomAnalysis.isRegularLink(linkElement("<a href=\"blabla\">comprehensive</a>")));
        assertTrue(!DomAnalysis.isRegularLink(linkElement("<a href=\"#blabla\">comprehensive</a>")));
        assertTrue(!DomAnalysis.isRegularLink(linkElement("<a href=\"javascript:bla()\">comprehensive</a>")));
        assertTrue(!DomAnalysis.isRegularLink(linkElement("<a bref=\"blabla\">comprehensive</a>")));
        assertTrue(!DomAnalysis.isRegularLink(linkElement("<a onclick=\"kjk\" >comprehensive</a>")));
        assertTrue(!DomAnalysis.isRegularLink(linkElement("<a>comprehensive</a>")));
    }

    private Node linkElement(String s) throws SAXException {
        return HtmlUtils.parse(s).getElementsByTagName("A").item(0);
    }

    public void testGetBaseOverride() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetScriptlessTextContent() throws Exception {
        final String html = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"><title>Google</title><script>var _gjwl=location;function _gjuc(){var a=_gjwl.hash.substring(1);if(/(^|&)q=/.test(a)&&a.indexOf(\"#\")==-1&&!/(^|&)cad=h($|&)/.test(a)){_gjwl.replace(\"search?\"+a.replace(/(^|&)fp=[^&]*/g,\"\")+\"&cad=h\");return 1}return 0};\n" +
                "window._gjuc && location.hash && _gjuc();</script><style>body,td,a,p,.h{font-family:arial,sans-serif}.h{color:#36c}.q{color:#00c}.ts td{padding:0}.ts{border-collapse:collapse}#gbar{height:22px;padding-left:2px}.gbh,.gbd{border-top:1px solid #c9d7f1;font-size:1px}.gbh{height:0;position:absolute;top:24px;width:100%}#gbi,#gbs{background:#fff;left:0;position:absolute;top:24px;visibility:hidden;z-index:1000}#gbi{border:1px solid;border-color:#c9d7f1 #36c #36c #a2bae7;z-index:1001}#guser{padding-bottom:7px !important}#gbar,#guser{font-size:13px;padding-top:1px !important}@media all{.gb1,.gb3{height:22px;margin-right:.73em;vertical-align:top}#gbar{float:left}}.gb2{display:block;padding:.2em .5em}a.gb1,a.gb2,a.gb3{color:#00c !important}.gb2,.gb3{text-decoration:none}a.gb2:hover{background:#36c;color:#fff !important}</style><script>window.google={kEI:\"FpF4Sai_EISX_AaFq9SyAQ\",kEXPI:\"17259,17315,19560,19568\",kHL:\"ru\"};\n" +
                "google.y={};google.x=function(e,g){google.y[e.id]=[e,g];return false};window.rwt=function(b,d,e,g,h,f,i){var a=encodeURIComponent||escape,c=b.href.split(\"#\");b.href=\"/url?sa=t\"+(d?\"&oi=\"+a(d):\"\")+(e?\"&cad=\"+a(e):\"\")+\"&ct=\"+a(g)+\"&cd=\"+a(h)+\"&url=\"+a(c[0]).replace(/\\+/g,\"%2B\")+\"&ei=FpF4Sai_EISX_AaFq9SyAQ\"+(f?\"&usg=\"+f:\"\")+i+(c[1]?\"#\"+c[1]:\"\");b.onmousedown=\"\";return true};\n" +
                "window.gbar={};(function(){var b=window.gbar,f,h;b.qs=function(a){var c=window.encodeURIComponent&&(document.forms[0].q||\"\").value;if(c)a.href=a.href.replace(/([?&])q=[^&]*|$/,function(i,g){return(g||\"&\")+\"q=\"+encodeURIComponent(c)})};function j(a,c){a.visibility=h?\"hidden\":\"visible\";a.left=c+\"px\"}b.tg=function(a){a=a||window.event;var c=0,i,g=window.navExtra,d=document.getElementById(\"gbi\"),e=a.target||a.srcElement;a.cancelBubble=true;if(!f){f=document.createElement(Array.every||window.createPopup?\"iframe\":\"div\");f.frameBorder=\"0\";f.src=\"#\";d.parentNode.appendChild(f).id=\"gbs\";if(g)for(i in g)d.insertBefore(g[i],d.firstChild).className=\"gb2\";document.onclick=b.close}if(e.className!=\"gb3\")e=e.parentNode;do c+=e.offsetLeft;while(e=e.offsetParent);j(d.style,c);f.style.width=d.offsetWidth+\"px\";f.style.height=d.offsetHeight+\"px\";j(f.style,c);h=!h};b.close=function(a){h&&b.tg(a)}})();</script></head><body bgcolor=#ffffff text=#000000 link=#0000cc vlink=#551a8b alink=#ff0000 onload=\"document.f.q.focus();if(document.images)new Image().src='/images/nav_logo4.png'\" topmargin=3 marginheight=3><div id=gbar><nobr><b class=gb1>Веб</b> <a href=\"http://images.google.ru/imghp?hl=ru&tab=wi\" onclick=gbar.qs(this) class=gb1>Картинки</a> <a href=\"http://maps.google.ru/maps?hl=ru&tab=wl\" onclick=gbar.qs(this) class=gb1>Карты</a> <a href=\"http://news.google.ru/nwshp?hl=ru&tab=wn\" onclick=gbar.qs(this) class=gb1>Новости</a> <a href=\"http://groups.google.ru/grphp?hl=ru&tab=wg\" onclick=gbar.qs(this) class=gb1>Группы</a> <a href=\"http://mail.google.com/mail/?hl=ru&tab=wm\" class=gb1>Gmail</a> <a href=\"http://www.google.ru/intl/ru/options/\" onclick=\"this.blur();gbar.tg(event);return !1\" class=gb3><u>ещё</u> <small>&#9660;</small></a><div id=gbi> <a href=\"http://blogsearch.google.ru/?hl=ru&tab=wb\" onclick=gbar.qs(this) class=gb2>Блоги</a> <a href=\"http://www.google.ru/dirhp?hl=ru&tab=wd\" onclick=gbar.qs(this) class=gb2>Каталог</a> <div class=gb2><div class=gbd></div></div> <a href=\"http://ru.youtube.com/?hl=ru&tab=w1\" onclick=gbar.qs(this) class=gb2>YouTube</a> <a href=\"http://www.google.com/calendar/render?hl=ru&tab=wc\" class=gb2>Календарь</a> <a href=\"http://picasaweb.google.ru/home?hl=ru&tab=wq\" onclick=gbar.qs(this) class=gb2>Фотографии</a> <a href=\"http://docs.google.com/?hl=ru&tab=wo\" class=gb2>Документы</a> <a href=\"http://www.google.ru/reader/view/?hl=ru&tab=wy\" class=gb2>Reader</a> <a href=\"http://sites.google.com/?hl=ru&tab=w3\" class=gb2>Сайты</a> <a href=\"http://otvety.google.ru/otvety/?hl=ru&tab=w2\" onclick=gbar.qs(this) class=gb2>Вопросы и ответы</a> <div class=gb2><div class=gbd></div></div> <a href=\"http://www.google.ru/intl/ru/options/\" class=gb2>Все продукты &raquo;</a></div> </nobr></div><div class=gbh style=left:0></div><div class=gbh style=right:0></div><div align=right id=guser style=\"font-size:84%;padding:0 0 4px\" width=100%><nobr><b>Pavel.Kozyrev@gmail.com</b> | <a href=\"/url?sa=p&pref=ig&pval=3&q=http://www.google.ru/ig%3Fhl%3Dru%26source%3Diglk&usg=AFQjCNGA90yIbM1R8iZtlxuqENUj3kH4hw\">Моя страница iGoogle</a> | <a href=\"https://www.google.com/accounts/ManageAccount\">Мой аккаунт</a> | <a href=\"/accounts/ClearSID?continue=http://www.google.com/accounts/Logout%3Fcontinue%3Dhttp://www.google.ru/webhp%253Fhl%253Dru\">Выйти</a></nobr></div><center><br clear=all id=lgpd><div align=left style=\"background:url(/intl/en_com/images/logo_plain.png) no-repeat;height:110px;width:276px\" title=\"Google\"><div nowrap style=\"color:#666;font-size:16px;font-weight:bold;left:208px;position:relative;top:78px\">Россия</div></div><br><form action=\"/search\" name=f><table cellpadding=0 cellspacing=0><tr valign=top><td width=25%>&nbsp;</td><td align=center nowrap><input name=hl type=hidden value=ru><input autocomplete=\"off\" maxlength=2048 name=q size=55 title=\"Поиск в Google\" value=\"\"><br><input name=btnG type=submit value=\"Поиск в Google\"><input name=btnI type=submit value=\"Мне повезёт!\"></td><td nowrap width=25%><font size=-2>&nbsp;&nbsp;<a href=/advanced_search?hl=ru>Расширенный поиск</a><br>&nbsp;&nbsp;<a href=/preferences?hl=ru>Настройки</a><br>&nbsp;&nbsp;<a href=/language_tools?hl=ru>Языковые инструменты</a></font></td></tr><tr><td align=center colspan=3><font size=-1><span style=\"text-align:left\"><input id=all type=radio name=lr value=\"\" checked><label for=all> Поиск в Интернете </label><input id=il type=radio name=lr value=\"lang_ru\"><label for=il> Поиск страниц на русском </label></span></font></td></tr></table></form><br><br><font size=-1><a href=\"/intl/ru/ads/\">Рекламные программы</a> - <a href=\"/services/\">Решения для предприятий</a> - <a href=\"/intl/ru/about.html\">Всё о Google</a> - <a href=http://www.google.com/ncr>Google.com in English</a></font><p><font size=-2>&copy;2009 - <a href=\"/intl/ru/privacy.html\">Конфиденциальность</a></font></p></center></body><script>if(google.y)google.y.first=[];window.setTimeout(function(){var xjs=document.createElement('script');xjs.src='/extern_js/f/CgJydRICcnUrMAo4DUABLCswDjgELCswGDgDLA/KjuquoJdWRg.js';document.getElementsByTagName('head')[0].appendChild(xjs)},0);google.y.first.push(function(){google.ac.i(document.f,document.f.q,'','')})</script><script>function _gjp() {!(location.hash && _gjuc()) && setTimeout(_gjp, 500);}window._gjuc && _gjp();</script></html>";

        String content = null;

        Document doc = HtmlUtils.parse(html);
        long t = System.nanoTime();
        for (int i = 1; i < 10000; i++) { //for benchmark
            content = DomAnalysis.getScriptlessTextContent(doc.getFirstChild());
        }
        System.out.println(System.nanoTime() - t);

        System.out.println(content);

        assertTrue(content.contains("Поиск в Интернете"));
    }

    public void testGetScriptlessTextContentUsingFold() throws Exception {
        final String html = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"><title>Google</title><script>var _gjwl=location;function _gjuc(){var a=_gjwl.hash.substring(1);if(/(^|&)q=/.test(a)&&a.indexOf(\"#\")==-1&&!/(^|&)cad=h($|&)/.test(a)){_gjwl.replace(\"search?\"+a.replace(/(^|&)fp=[^&]*/g,\"\")+\"&cad=h\");return 1}return 0};\n" +
                "window._gjuc && location.hash && _gjuc();</script><style>body,td,a,p,.h{font-family:arial,sans-serif}.h{color:#36c}.q{color:#00c}.ts td{padding:0}.ts{border-collapse:collapse}#gbar{height:22px;padding-left:2px}.gbh,.gbd{border-top:1px solid #c9d7f1;font-size:1px}.gbh{height:0;position:absolute;top:24px;width:100%}#gbi,#gbs{background:#fff;left:0;position:absolute;top:24px;visibility:hidden;z-index:1000}#gbi{border:1px solid;border-color:#c9d7f1 #36c #36c #a2bae7;z-index:1001}#guser{padding-bottom:7px !important}#gbar,#guser{font-size:13px;padding-top:1px !important}@media all{.gb1,.gb3{height:22px;margin-right:.73em;vertical-align:top}#gbar{float:left}}.gb2{display:block;padding:.2em .5em}a.gb1,a.gb2,a.gb3{color:#00c !important}.gb2,.gb3{text-decoration:none}a.gb2:hover{background:#36c;color:#fff !important}</style><script>window.google={kEI:\"FpF4Sai_EISX_AaFq9SyAQ\",kEXPI:\"17259,17315,19560,19568\",kHL:\"ru\"};\n" +
                "google.y={};google.x=function(e,g){google.y[e.id]=[e,g];return false};window.rwt=function(b,d,e,g,h,f,i){var a=encodeURIComponent||escape,c=b.href.split(\"#\");b.href=\"/url?sa=t\"+(d?\"&oi=\"+a(d):\"\")+(e?\"&cad=\"+a(e):\"\")+\"&ct=\"+a(g)+\"&cd=\"+a(h)+\"&url=\"+a(c[0]).replace(/\\+/g,\"%2B\")+\"&ei=FpF4Sai_EISX_AaFq9SyAQ\"+(f?\"&usg=\"+f:\"\")+i+(c[1]?\"#\"+c[1]:\"\");b.onmousedown=\"\";return true};\n" +
                "window.gbar={};(function(){var b=window.gbar,f,h;b.qs=function(a){var c=window.encodeURIComponent&&(document.forms[0].q||\"\").value;if(c)a.href=a.href.replace(/([?&])q=[^&]*|$/,function(i,g){return(g||\"&\")+\"q=\"+encodeURIComponent(c)})};function j(a,c){a.visibility=h?\"hidden\":\"visible\";a.left=c+\"px\"}b.tg=function(a){a=a||window.event;var c=0,i,g=window.navExtra,d=document.getElementById(\"gbi\"),e=a.target||a.srcElement;a.cancelBubble=true;if(!f){f=document.createElement(Array.every||window.createPopup?\"iframe\":\"div\");f.frameBorder=\"0\";f.src=\"#\";d.parentNode.appendChild(f).id=\"gbs\";if(g)for(i in g)d.insertBefore(g[i],d.firstChild).className=\"gb2\";document.onclick=b.close}if(e.className!=\"gb3\")e=e.parentNode;do c+=e.offsetLeft;while(e=e.offsetParent);j(d.style,c);f.style.width=d.offsetWidth+\"px\";f.style.height=d.offsetHeight+\"px\";j(f.style,c);h=!h};b.close=function(a){h&&b.tg(a)}})();</script></head><body bgcolor=#ffffff text=#000000 link=#0000cc vlink=#551a8b alink=#ff0000 onload=\"document.f.q.focus();if(document.images)new Image().src='/images/nav_logo4.png'\" topmargin=3 marginheight=3><div id=gbar><nobr><b class=gb1>Веб</b> <a href=\"http://images.google.ru/imghp?hl=ru&tab=wi\" onclick=gbar.qs(this) class=gb1>Картинки</a> <a href=\"http://maps.google.ru/maps?hl=ru&tab=wl\" onclick=gbar.qs(this) class=gb1>Карты</a> <a href=\"http://news.google.ru/nwshp?hl=ru&tab=wn\" onclick=gbar.qs(this) class=gb1>Новости</a> <a href=\"http://groups.google.ru/grphp?hl=ru&tab=wg\" onclick=gbar.qs(this) class=gb1>Группы</a> <a href=\"http://mail.google.com/mail/?hl=ru&tab=wm\" class=gb1>Gmail</a> <a href=\"http://www.google.ru/intl/ru/options/\" onclick=\"this.blur();gbar.tg(event);return !1\" class=gb3><u>ещё</u> <small>&#9660;</small></a><div id=gbi> <a href=\"http://blogsearch.google.ru/?hl=ru&tab=wb\" onclick=gbar.qs(this) class=gb2>Блоги</a> <a href=\"http://www.google.ru/dirhp?hl=ru&tab=wd\" onclick=gbar.qs(this) class=gb2>Каталог</a> <div class=gb2><div class=gbd></div></div> <a href=\"http://ru.youtube.com/?hl=ru&tab=w1\" onclick=gbar.qs(this) class=gb2>YouTube</a> <a href=\"http://www.google.com/calendar/render?hl=ru&tab=wc\" class=gb2>Календарь</a> <a href=\"http://picasaweb.google.ru/home?hl=ru&tab=wq\" onclick=gbar.qs(this) class=gb2>Фотографии</a> <a href=\"http://docs.google.com/?hl=ru&tab=wo\" class=gb2>Документы</a> <a href=\"http://www.google.ru/reader/view/?hl=ru&tab=wy\" class=gb2>Reader</a> <a href=\"http://sites.google.com/?hl=ru&tab=w3\" class=gb2>Сайты</a> <a href=\"http://otvety.google.ru/otvety/?hl=ru&tab=w2\" onclick=gbar.qs(this) class=gb2>Вопросы и ответы</a> <div class=gb2><div class=gbd></div></div> <a href=\"http://www.google.ru/intl/ru/options/\" class=gb2>Все продукты &raquo;</a></div> </nobr></div><div class=gbh style=left:0></div><div class=gbh style=right:0></div><div align=right id=guser style=\"font-size:84%;padding:0 0 4px\" width=100%><nobr><b>Pavel.Kozyrev@gmail.com</b> | <a href=\"/url?sa=p&pref=ig&pval=3&q=http://www.google.ru/ig%3Fhl%3Dru%26source%3Diglk&usg=AFQjCNGA90yIbM1R8iZtlxuqENUj3kH4hw\">Моя страница iGoogle</a> | <a href=\"https://www.google.com/accounts/ManageAccount\">Мой аккаунт</a> | <a href=\"/accounts/ClearSID?continue=http://www.google.com/accounts/Logout%3Fcontinue%3Dhttp://www.google.ru/webhp%253Fhl%253Dru\">Выйти</a></nobr></div><center><br clear=all id=lgpd><div align=left style=\"background:url(/intl/en_com/images/logo_plain.png) no-repeat;height:110px;width:276px\" title=\"Google\"><div nowrap style=\"color:#666;font-size:16px;font-weight:bold;left:208px;position:relative;top:78px\">Россия</div></div><br><form action=\"/search\" name=f><table cellpadding=0 cellspacing=0><tr valign=top><td width=25%>&nbsp;</td><td align=center nowrap><input name=hl type=hidden value=ru><input autocomplete=\"off\" maxlength=2048 name=q size=55 title=\"Поиск в Google\" value=\"\"><br><input name=btnG type=submit value=\"Поиск в Google\"><input name=btnI type=submit value=\"Мне повезёт!\"></td><td nowrap width=25%><font size=-2>&nbsp;&nbsp;<a href=/advanced_search?hl=ru>Расширенный поиск</a><br>&nbsp;&nbsp;<a href=/preferences?hl=ru>Настройки</a><br>&nbsp;&nbsp;<a href=/language_tools?hl=ru>Языковые инструменты</a></font></td></tr><tr><td align=center colspan=3><font size=-1><span style=\"text-align:left\"><input id=all type=radio name=lr value=\"\" checked><label for=all> Поиск в Интернете </label><input id=il type=radio name=lr value=\"lang_ru\"><label for=il> Поиск страниц на русском </label></span></font></td></tr></table></form><br><br><font size=-1><a href=\"/intl/ru/ads/\">Рекламные программы</a> - <a href=\"/services/\">Решения для предприятий</a> - <a href=\"/intl/ru/about.html\">Всё о Google</a> - <a href=http://www.google.com/ncr>Google.com in English</a></font><p><font size=-2>&copy;2009 - <a href=\"/intl/ru/privacy.html\">Конфиденциальность</a></font></p></center></body><script>if(google.y)google.y.first=[];window.setTimeout(function(){var xjs=document.createElement('script');xjs.src='/extern_js/f/CgJydRICcnUrMAo4DUABLCswDjgELCswGDgDLA/KjuquoJdWRg.js';document.getElementsByTagName('head')[0].appendChild(xjs)},0);google.y.first.push(function(){google.ac.i(document.f,document.f.q,'','')})</script><script>function _gjp() {!(location.hash && _gjuc()) && setTimeout(_gjp, 500);}window._gjuc && _gjp();</script></html>";

        String content = null;

        Document doc = HtmlUtils.parse(html);
        long t = System.currentTimeMillis();
        for (int i = 1; i < 10000; i++) { //for benchmark

            content = DomAnalysis.getScriptlessTextContent(doc.getFirstChild());
        }
        System.out.println(System.currentTimeMillis() - t);

        System.out.println(content);

        assertTrue(content.contains("Поиск в Интернете"));
    }

    public static Test suite() {
        return new TestSuite(DomAnalysisTest.class);
    }
}
