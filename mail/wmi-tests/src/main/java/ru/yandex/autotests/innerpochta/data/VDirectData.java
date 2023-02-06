package ru.yandex.autotests.innerpochta.data;

import ru.yandex.autotests.innerpochta.wmicommon.Util;

import java.net.IDN;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.07.15
 * Time: 18:22
 */
public class VDirectData {
    public static final Object THE_SAME = null;
    public static List<Object[]> getLinks() {
       return asList(
                new Object[]{
                        "https://account.live.com/Email/Verify?otc=*Cr*GQSNv6bh0QwPp64Zi69wuDwNxdqURb2ogWqVip!2rPYwvLTVGT" +
                                "JjpOdc!pGgiw6UgBF5esaoKrFGm1d53*gU$&mn=tz%40kkrz.tk&ru=https://login.live.com/login.srf%" +
                                "3fwa%3dwsignin1.0%26rpsnv%3d11%26rver%3d6.1.6206.0%26wp%3dMBI%26wreply%3dhttps://office" +
                                ".microsoft.com/wlid/authredir.aspx%3furl%3dhttps%253A%252F%252Foffice%252Emicrosoft%252E" +
                                "com%252Fen%252Dus%252Fpurchase%252Easpx%253Fpi%253Df7d9daff%25252D98ab%25252D460c%25252D" +
                                "87f6%25252Dd1d228a0fb7f%2526pt%253DOfficePreview%26hurl%3d933D31E4FF6B710E8131CBE8EECE6" +
                                "131%26ipt%3d1%26%20id%3d34134%26cbcxt%3dType_OfficePreview__OfferGuid_f7d9daff-98ab-460c" +
                                "-87f6-d1d228a0fb7f%26bk%3d1359257017%26uiflavor%3dWeb%26mkt%3dEN-US%26lc%3d1033%26lic%3d1" +
                                "%26sutk%3d1359257153559&cxt=EV",
                        THE_SAME
                },
                new Object[]{
                        "http://clck.yandex.ru/redir/lyBG7xl2OZnf8mVbHwkAu77LH5e0Oeu/vZN99iESOE8=data=pQq6vujNEtczX1xungVlgHp" +
                                "0Nbt44ZkqcNljzZVKPgMUXiYrRwnVzzVT%2FSRE%2BMIE3vtazW%2FvzRGsgaWEy15eeg%3D%3D&b64e=1&sign=497a" +
                                "3cf2227de9c8fc1d774869a0dbac&keyno=9",
                        THE_SAME
                },
                new Object[]{
                        "<a href='http://goble.ru/reset_pass.php?submit=reset&email:=i32l^goble.ru&pass=c4ca4238a0b923820dcc" +
                                "509a6f75849b'></a>",
                        "http://goble.ru/reset_pass.php?submit=reset&email:=i32l^goble.ru&pass=c4ca4238a0b923820dcc509a6f75849b",
                },

                //[WMI-749]
                new Object[]{
                        "http://goble.ru/reset_pass.php?submit=reset&email=i32l@goble.ru&pass=c4ca4238a0b923820dcc509a6f75849b",
                        THE_SAME
                },


                //[WMI-749]
                new Object[]{
                        "<a href='http://goble.ru/reset_pass.php?submit=reset&email=i32l@goble.ru&pass=c4ca4238a0b923820dcc" +
                                "509a6f75849b'></a>",
                        "http://goble.ru/reset_pass.php?submit=reset&email=i32l@goble.ru&pass=c4ca4238a0b923820dcc509a6f75849b"
                },

                //[WMI-749]
                new Object[]{
                        "http://goble.ru/reset_pass.php?submit=reset&email=i32l%40goble.ru&pass=c4ca4238a0b923820dcc509a6f75849b",
                        THE_SAME
                },

//                //[DARIA-42806] (обращаться к ctor)
//                new Object[]{
//                        "http://irmingha.dom-file-group.ru",
//                        "/infected?url=http://irmingha.dom-file-group.ru/"
//                },

                //[DARIA-26088]
                new Object[]{
                        "http://ruxpert.ru/Миф_о_пользе_сезонного_перевода_стрелок_часов",
                        THE_SAME
                },


                //[DARIA-20197]
//        new Object[]{
//                "<a href='http://xserver.a-real.ru/cost/ '>http://xserver.a-real.ru/cost/ </a>",
//                "http://xserver.a-real.ru/cost/",
//                "http://xserver.a-real.ru/cost/"
//        },

                new Object[]{
                        "https://jira.yandex-team.ru/issues/" +
                                "?jql=component%20%3D%20'Email%20analysis'%20AND%20status%20in%20(Open)",
                        THE_SAME
                },


                //[DARIA-20197]
                new Object[]{
                        "<a href='http://www.facebook.com/?sda=f|sdf'>http://www.facebook.com/?sda=f|sdf</a>",
                        "http://www.facebook.com/?sda=f|sdf"
                },

//[WMI-729]
//        new Object[]{
//                "ftp://qwerty@ya.ru:80",
//                THE_SAME,
//                THE_SAME
//        },


                new Object[]{
                        "http://market-click2.yandex.ru/redir/GAkkM7lQwz6kEtXSvIPZ1g3kIBRmjonSN088jE6B9t4JeqZxTRHYMHSqOPWpa8e" +
                                "KPIwxG_i9XsNJrmtwPVyzdR5h2FjOdUN4rrEIFcCvuxnMRvY5G1UFQ0waC7Azb4OuO83BcfVKjcCjGwJW6loAdEsJ9Zk" +
                                "EdJV041WzYgmsfxDA_UDPcH3I43LNusRxbH0-cCqxJn6KbfewNguAibOomDhJYtKCjoE5KwKfyVOYn25AYmxMcSwJBDp" +
                                "fR5h-sPGgmgYUNhFuzdu_LcZJp5SjqcgzrfjgaDEB?data=QVyKqSPyGQwwaFPWqjjgNs7p3UcO_XPHWYi-k7i91rmhx" +
                                "8WeKBPH7UjOxIqDo39RIx_q00GzLjAyF9oGQUgDvkTj7IHKVGoOGF6xAq7IJl4V6zCjP8z89KWpF3ecf2ljO9i_u_WJW" +
                                "0AFR0HUYRPqUWetOFAaFqFIoi5ZSNSnUYJEJFj3II_z1CJEHOyUNs-o5RwvhqpVf2eTb25YeMwi1PnLpXlVCR9bd7FYx" +
                                "ttqG9KzCppuHH8K2oMenJPXhIz5H83XUH4KvgZRusVCsxaOQvjM10wi3rMy-U7kHtOJhNmh5wwfJE_W0A&b64e=2&sig" +
                                "n=7701adbdecbf2b4ebf7003b77de5fbcf&keyno=1",
                        THE_SAME
                },


                new Object[]{
                        "http://президент.рф",
                        "http://" + IDN.toASCII("президент.рф") + "/"
                },

                new Object[]{
                        "http://ya.ru",
                        "http://ya.ru/"
                },

                new Object[]{
                        "http://ya.ru/404.html",
                        THE_SAME
                },


                new Object[]{
                        "http://news.yandex.ru/yandsearch?cl4url=www.vesti.ru%2Fdoc.html%3Fid%3D824712",
                        THE_SAME
                },
                new Object[]{
                        "http://vk.com/share.php?url=http://www.fontanka.ru/2012/05/29/108/",
                        THE_SAME
                },
                new Object[]{
                        "http://www.avp.travel.ru/AVP_99.htm#%D0%9F%D0%9E%D0%A5%D0%9E%D0%94%D0%AB",
                        THE_SAME

                },
                new Object[]{
                        "http://www.google.ru/search?aq=f&sugexp=chrome,mod=1&sourceid=chrome&ie=UTF-8&q=dsfsd",
                        THE_SAME
                },
                //new Object[]{
                //        "http://www.wakaleo.com/thucydides/#_using_pages_in_a_step_library",
                //        THE_SAME
                //},
                new Object[]{
                        "ftp://ftp.mozilla.org/pub/mozilla.org/",
                        THE_SAME
                },

                new Object[]{
                        "http://www.ya.ru",
                        "http://www.ya.ru/"
                },

                new Object[]{
                        "https://www.google.ru/search?q=fgrep&ie=utf-8&oe=utf-8&aq=t&rls=org.mozilla:ru:official&client=firef" +
                                "ox#hl=ru&newwindow=1&client=firefox&hs=5w5&rls=org.mozilla:ru%3Aofficial&sclient=psy-ab&q=fg" +
                                "rep+lalal++dfsldkfjsf+sldkfjlllllllllllllllllllllllllllllllllllllls&oq=fgrep+lalal++dfsldkfj" +
                                "sf+sldkfjlllllllllllllllllllllllllllllllllllllls&aq=f&aqi=&aql=&gs_l=serp.3...8393l14295l0l1" +
                                "4454l64l34l0l0l0l0l620l6056l2j25j4j5-2l33l0.&pbx=1&fp=1&biw=1600&bih=1056&bav=on.2,or.r_gc.r" +
                                "_pw.r_cp.r_qf.,cf.osb&cad=b",
                        THE_SAME
                },

                new Object[]{
                        "http://mail.ru",
                        "http://mail.ru/"
                },
                new Object[]{
                        "http://qwert@twalrus.yandex.ru:20000/",
                        THE_SAME
                },
                new Object[]{
                        "http://quentao.yandex-team.ru:9999?db=base-21#problems/3841", // MAILSUPPORT-257
                        THE_SAME
                },
                new Object[]{
                        "https://mail.yandex.ru/neo2/#setup/collectors",
                        THE_SAME
                },

                new Object[]{
                        "https://r.mail.yandex.net/url/sGc0yujI1yYJgKedK3vvwA,1338392086" +
                                "/video.yandex.ru%2F%23%2Fmail%2FCsh" +
                                "TgW8RZ%2DjQRHKHu%2DlIPzgOFuTIpJfM",
                        THE_SAME
                },
                new Object[]{
                        "http://video.yandex.ru/#/mail/CshTgW8RZ-jQRHKHu-lIPzgOFuTIpJfM",
                        THE_SAME
                },
                new Object[]{
                        "http://video.yandex.ru/urls/mail/CshTgW8RZ-jQRHKHu-lIPzgOFuTIpJfM",   //DARIA-18649
                        THE_SAME
                },

//[WMI-729]
//        String link = "http://www.avp.travel.ru%2FAVP%5F99%2Ehtm%23%25D0%259" +
//                "F%25D0%259E%25D0%25A5%25D0%259E%25D0%2594%25D0%25AB";
//        new Object[]{
//                link,
//                link,
//                link
//        },


                new Object[]{
                        "http://clck.yandex.ru/redir/dtype=stred/pid=116/cid=2237/path=reply.reply/*data=" +
                                "url%3Dhttp%253A%25" +
                                "2F%252Fclubs.at.yandex-team.ru%252Ftanks%252Freplies.xml%253Fparent_id%253D181%2526item_no" +
                                "%253D177%2526with_parent%253D1%2523reply-tanks-181",
                        THE_SAME
                },
                new Object[]{
                        "http://clubs.at.yandex-team.ru/tanks/replies.xml?parent_id=181&item_no=177&with_parent=1" +
                                "#reply-tanks-181",
                        THE_SAME
                },
                new Object[]{
                        "http://r.mail.yandex.net/url/fls_b59bC-YbHcZEdLaX3Q,1334736138/market-click2.yandex.ru/redir/" +
                                "GAkkM7lQwz6kEtXSvIPZ1g3kIBRmjonSN088jE6B9t4JeqZxTRHYMHSqOPWpa8eKPIwxG_i9XsNJrmtwPVyzd" +
                                "R5h2FjOdUN4rrEIFcCvuxnMRvY5G1UFQ0waC7Azb4OuO83BcfVKjcCjGwJW6loAdEsJ9ZkEdJV041WzYgmsfx" +
                                "DA_UDPcH3I43LNusRxbH0-cCqxJn6KbfewNguAibOomDhJYtKCjoE5KwKfyVOYn25AYmxMcSwJBDpfR5h-sPG" +
                                "gmgYUNhFuzdu_LcZJp5SjqcgzrfjgaDEB?data=QVyKqSPyGQwwaFPWqjjgNs7p3UcO_XPHWYi-k7i91rmhx8" +
                                "WeKBPH7UjOxIqDo39RIx_q00GzLjAyF9oGQUgDvkTj7IHKVGoOGF6xAq7IJl4V6zCjP8z89KWpF3ecf2ljO9i" +
                                "_u_WJW0AFR0HUYRPqUWetOFAaFqFIoi5ZSNSnUYJEJFj3II_z1CJEHOyUNs-o5RwvhqpVf2eTb25YeMwi1PnL" +
                                "pXlVCR9bd7FYxttqG9KzCppuHH8K2oMenJPXhIz5H83XUH4KvgZRusVCsxaOQvjM10wi3rMy-U7kHtOJhNmh5" +
                                "wwfJE_W0A&b64e=2&sign=7701adbdecbf2b4ebf7003b77de5fbcf&keyno=1",
                        THE_SAME
                },
                new Object[]{
                        "http://img7-fotki.yandex.net/get/6207/140147405.10/0_STATIC8e8e7_bd93dbfc_L?"
                                + Util.getLongString()
                                + Util.getLongString()
                                + Util.getLongString(),
                        THE_SAME
                },
                new Object[]{
                        "https://r.mail.yandex.net/url/seZgbZRpIQLepaUVf7APAg,1339356377/" +
                                "kyprizel.net%0dSet-Cookie:%20test=1",
                        THE_SAME
                },

                new Object[]{
                        "https://kyprizel.net%0dSet-Cookie:%20test=1",
                        "https://kyprizel.net%0dSet-Cookie%20test=1"
                });

               //MPROTO-1797
//                new Object[]{
//                        "https://t.co/redirect?url=http%3A%2F%2Ft.co%2FWupiHUJAUq%3Fcn%3DYmFja2ZpbGxfZGlnZXN0X2FjdGl2ZQ%253D%253D" +
//                                "%26refsrc%3Demail&t=1&cn=YmFja2ZpbGxfZGlnZXN0X2FjdGl2ZQ%3D%3D&sig=18a0a004e04845526d9e0a87b27" +
//                                "dea045515295d&iid=7a3ab92898ac4668be20dbd5d6c1bc84&uid=2167345981&nid=244+1459+20141104",
//
//                        THE_SAME
//                }););


    }
}
