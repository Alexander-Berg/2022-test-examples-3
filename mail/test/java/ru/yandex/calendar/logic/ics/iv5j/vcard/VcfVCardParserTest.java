package ru.yandex.calendar.logic.ics.iv5j.vcard;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.ics.iv5j.vcard.parameter.VcfEncodingParameter;
import ru.yandex.calendar.logic.ics.iv5j.vcard.parameter.VcfParameter;
import ru.yandex.calendar.logic.ics.iv5j.vcard.parameter.VcfTypeParameter;
import ru.yandex.calendar.logic.ics.iv5j.vcard.parameter.VcfValueParameter;
import ru.yandex.calendar.logic.ics.iv5j.vcard.property.VcfProperty;
import ru.yandex.misc.test.Assert;

import org.junit.Test;

/**
 * @author Stepan Koltsov
 * @author shinderuk
 */
public class VcfVCardParserTest {

    @Test
    public void parse() {
        String string =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "N:Gabriel;Peter;;;\n" +
            "FN:Peter Gabriel\n" +
            "ORG:Some;\n" +
            "X-ABUID:59B66EFE-E6A2-49B3-92CF-3B435BC5AEAF\\:ABPerson\n" +
            "UID:EB117CD6-97AD-4B1E-9A4A-A18585AD3798-ABSPlugin\n" +
            "REV:2010-12-12T23:50:14Z\n" +
            "END:VCARD\n"
            ;
        VcfVCard vcard = VcfVCardParser.P.parse(string);
    }

    @Test
    public void bugReportedByDsfox() {
        String string =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "PRODID:-//Inverse inc.//SOGo Connector 1.0//EN\n" +
            "UID:C49E6776-6260-0001-AD26-195E8299AB10\n" +
            "N:Голубничий;Дмитрий\n" +
            "FN:Дмитрий Голубничий\n" +
            "ORG:ООО Яндекс;Разработка ПО\n" +
            "NICKNAME:dsfox\n" +
            "ADR;TYPE=work:;дом 16;улица Льва Толстого;Москва;Московская область;100001;\n" +
            " Россия\n" +
            "ADR;TYPE=home:;дом 61к2;улица Кастанаевская;Москва;Московская область;10840\n" +
            " 6;Россия\n" +
            "TEL;TYPE=work:+74956666666\n" +
            "TEL;TYPE=home:+74991111111\n" +
            "TEL;TYPE=cell:+79055767127\n" +
            "TEL;TYPE=fax:+74992222222\n" +
            "TEL;TYPE=pager:+7903152026\n" +
            "X-MOZILLA-HTML:FALSE\n" +
            "EMAIL;TYPE=work:dsfox@mail.ru\n" +
            "EMAIL;TYPE=home:ds.golubnichiy@gmail.com\n" +
            "URL;TYPE=work:http://company.yandex.ru\n" +
            "URL;TYPE=home:http://dsfox.ru\n" +
            "TITLE:Разработчик\n" +
            "BDAY:1981-05-20\n" +
            "CUSTOM1:прочее 1\n" +
            "CUSTOM2:прочее 2\n" +
            "CUSTOM3:прочее 3\n" +
            "CUSTOM4:прочее 4\n" +
            "NOTE:Это поле для заметок\n" +
            "PHOTO;ENCODING=b;TYPE=GIF:R0lGODlhZABkALMAAAkJCYeHh0ZGRsfHxycnJ6enp2dnZ/Ly8\n" +
            " hcXF5eXl1dXV9fX1zY2Nra2tnd3dwAAACH5BAEAAA8ALAAAAABkAGQAQAT/8MhJq70401XK0mA\n" +
            " ojmQ5NQagIs5nvnA8LglDAAhw50jgysDYIkBgKAqDYIgjYACchJZyKhkKAIKGZTBYLFCBxiBhC\n" +
            " CgMZUfY4Uh0x0kKl3oZBJwIAgLByOlzOCqBBnEXXhsDBQ0/VQ2LFowUDX9PgDY2Kjt7DAI+kSJ\n" +
            " DNgR6OgpdFWN9eTh7ekcbLlZFezo4RTYJhwcFAjyZDA6FdBkLp4YDDYrGJF7IYowFaAZNOALBw\n" +
            " xUcBjx/ejemVcsNAgK5B5+QH9OcBXejOnyzDEYE5OQKDp1sBo/YJymjFCQAgW5DAgcKoGjKs2q\n" +
            " PgAL+THAgBYiSnzPUdHCysceXrY4I/3rpGMWj1SUPEUE04FhKgZYXdwJFYVbAgZMVD1PqhLSzJ\n" +
            " 5Uho6T4HPpiHC1bBAYSXZrBzqhb3lohAAdiDDlhTLMpWIWDgZsJxQqK4PKBhgNvQpV8cSCNjQA\n" +
            " 0AdgaUMOWDT6EZwS+1GBHUYIEHdQoCmAgQYO/KJsmiMtmX2PCctGwgcxWoMqErFjluWGjHIll/\n" +
            " 6aRK9MPRAE8XOGBZFCaToOEASCdXZgJhwGxQwB99BbFxZgAhA0kJhqtYxSsJbyIlVOATCtytSn\n" +
            " dEHAEtAk7oiyR3DRvXmyD9ZRKRMEQaQ5gy5kOwFMrD4MABRQQUIBPzOJP2lQ1FFSSlh4GEGUFl\n" +
            " v9NxgWChQN7IbLYNNxsZg4kDSAk3QoA3MZUfJ2kN0MATyT4whAGEGChgEEMYIBLJKaIQTTeeKi\n" +
            " igECxUoQPL5K4gHw3rJAHITUy9ZqMJP3yHSit2Qifhhs014VyYSHTAXJgtbNJgDUukAIf81yTD\n" +
            " RDKEdFQPVT2eMB6O/BDEJPn/FCWdWAlQI6LPS7AlniwSDCANcUoEBd9ZdyF4EFv6LKTNgwVukk\n" +
            " rgXTESobJFYQOOnfuAM92mnXzFBZ6FjQAgYr60mlIQxIEZTbFsCnbU5ut4E1tb4VJk3x+6IgDV\n" +
            " dkcdiJCAeTCyJ3kGOZFB/TlU4YijvzAi460dOQSkiY0ENP/VK6eg0Y3WAjgjVePzvaEtfGwwqO\n" +
            " dzipQSgCm6vRFkRxsVctICC5hBkc7vEdlNAFQ54mRozzYE5MJyEcdbypkWaRrbu6nQ1rJJcNmM\n" +
            " XvxEm92RWRS0kcAjjrWicheK4iIvi1Zr3fKEfSMlfDWY20fXi1pbjvwKYIEWWF5kcCU2SABC25\n" +
            " ibMrQU9v9R+eLclorSCcQhUUqMe1gItVmqC3KrE4m5njUCsIlVMRbSRzGRa7WjtKLR4BgufQo3\n" +
            " 5JY8GZBkhRF0YSJ5J8fR11tDgrcGhc2tDB6qZvAJBglFYoafDHNDt3o69MAVkdhuEQBbDZics4\n" +
            " 5ZPEwX0wOAwqLw/DF/3Bidu45KPd9niIvXokuIOJkP236FNv4gfDqOvFSEmuw94Q43DbQWjs2H\n" +
            " NxUEgE07o5NfGqzGq3wQERdSc/HIT+FnDlatArwSG5qWJxFUXeGGvhsPw3gGUQ4z8824gonl+G\n" +
            " b8ccrPd4eEvj+CP6HQ6HWWAAp1fCjZrlLIOOFl/Aom5jcRBH9pSQRnTke0EK0h8zFQmHOQlBzk\n" +
            " KAMxMQiG21QHVHWUwb8zMFECvDCidqmp3IA5yA+SMT5eEeWJQWKCy1kUjOK0Q7LkaFltzrDntC\n" +
            " giDMoECxLKlWTmhHDUrnwDfWSHtqiohql7UGAxJAhmkImqAywaB6/Y8glLMUJkv945gSYmV+OM\n" +
            " mMgCn3REJHgALoUwYiQXcABvwtSgSpyHmsM7BzEy4TUVDMSP5CrUVUEy9M2hT9KgURi8+EcMfq\n" +
            " lmyfUJlVJGUEioMQwlgGHf1WY2SHfQZE8lCOQF4OCH/DnkBWeA4bZ6oOIbHZKFWYgHzKamB46Y\n" +
            " bkR1EQV73CIA93Cm3oUphB2CMMHNsWRQuUOHx4Y5lakxg0H/FAGxbBJH57ggAsQ5iajmMcK3vM\n" +
            " oHL3Pdx1pVxWsdIVtPUSDHzqNDeonLXEtbVLvCZ82i5eoMFSBLfViwet84qyvJOks/EEK/C6gz\n" +
            " mzm8i0NU8NcarkvDEQIR9aw5wgiyBZ7qQn/n5c4AjqfB5xnpsRKIbmCf0SUuZREQ5WPM1cy/hI\n" +
            " iBpwoelHhhETjR0BO0k5zWtPQFxKQy4Qk6wbbeaJwNkpQA0AsOwKwWLEw4KiatGsB3BIEJ5ZHI\n" +
            " ZCQgjol3VAxK6KjToDrETd6YqAC94zTzNJfpDBZGeaSjJX6ioigJFJUyQgSLN30BA54QlaZKgH\n" +
            " 1dSM7LCAqM645D+0hhDpzAU4+elMBMqR0sE2Y41OS6pO4nioP+0QF/+zgTFXmUlY5eKyKXlMEs\n" +
            " t3xM/lAlCU8xQd2kgh1dZ2L4HLFyqXKxrPIqmstyMbQQfHUPRsrgnOuVg5dqQkFGMEff9DWrc1\n" +
            " kdilXo+TML7x2Byy+x7FxUYgvgFqeSbUHJ6a0HQMlpRmH6CmJfRzlxna7o3WkjSt+oCyMYBk9q\n" +
            " /5SDm6CV2qyaccN+DV6IwmPYGVQME5u4iGWi2ZDQvvML2yFtXnwaPy+JhM8JecsTHOtIdhCOD5\n" +
            " AsSf10uOHJXmHPehuLAQcSfD4eTJxAiE+epVBhKZDvoiMwUyucWlvNRCfvQqPAwPekvOGTOSIR\n" +
            " AAAOw==\n" +
            "PHOTO;VALUE=uri:http://www.abc.com/pub/photos/jqpublic.gif\n" +
            "END:VCARD\n" +
            "";
        VcfVCard vcard = VcfVCardParser.P.parse(string);
        Assert.A.isTrue(vcard.getProperties("ADR").first().getValue().endsWith(";Россия"));
        ListF<VcfProperty> photos = vcard.getProperties("PHOTO");
        Assert.sizeIs(2, photos);

        {
            VcfProperty photoEmbed = photos.first();
            ListF<VcfParameter> params = photoEmbed.getParameters();
            Assert.sizeIs(2, params);
            for (VcfParameter param : params) {
                String name = param.getName();
                if (VcfEncodingParameter.ENCODING.equals(name)) {
                    Assert.equals("b", param.getValue());
                } else if (VcfTypeParameter.TYPE.equals(name)) {
                    Assert.equals("GIF", param.getValue());
                } else {
                    Assert.fail("Unexpected parameter name=" + name);
                }
            }
            Assert.isTrue(photoEmbed.getValue().startsWith("R0lGOD"));
            Assert.isTrue(photoEmbed.getValue().endsWith("AAAOw=="));
        }

        {
            VcfProperty photoUrl = photos.last();
            ListF<VcfParameter> params = photoUrl.getParameters();
            Assert.sizeIs(1, params);
            VcfParameter param = params.first();
            Assert.equals(VcfValueParameter.VALUE, param.getName());
            Assert.equals("uri", param.getValue());
            Assert.equals("http://www.abc.com/pub/photos/jqpublic.gif", photoUrl.getValue());
        }
    }

    @Test
    public void anotherVcf() {
        String string =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "PRODID:-//Inverse inc.//SOGo Connector 1.0//EN\n" +
            "UID:C49FAD59-9660-0001-BAB0-1848EFA01CAD\n" +
            "X-MOZILLA-HTML:FALSE\n" +
            "EMAIL;TYPE=work:user13865@rambler.ru\n" +
            "END:VCARD\n"
            ;
        VcfVCard vcard = VcfVCardParser.P.parse(string);
        Assert.A.equals("user13865@rambler.ru", vcard.getProperty("EMAIL").get().getValue());
    }

    @Test
    public void linesUnfolding() {
        String string =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "FN:Forrest\n" +
            "  Gump\n" +
            "ORG:Bubba Gump\n" +
            "\t Shrimp \n" +
            " Co.\n" +
            "END:VCARD\n"
            ;

        VcfVCard vcard = VcfVCardParser.P.parse(string);

        Assert.sizeIs(3, vcard.getProperties());
        Assert.equals("Forrest Gump", vcard.getProperty("FN").get().getValue());
        Assert.equals("Bubba Gump Shrimp Co.", vcard.getProperty("ORG").get().getValue());
    }

    @Test
    public void stripEmptyLines() {
        String string =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n\n" +
            "FN:Forrest Gump\n" +
            "END:VCARD\n\n"
            ;

        VcfVCard vcard = VcfVCardParser.P.parse(string);

        Assert.sizeIs(2, vcard.getProperties());
        Assert.equals("3.0", vcard.getProperty("VERSION").get().getValue());
        Assert.equals("Forrest Gump", vcard.getProperty("FN").get().getValue());
    }

} //~
