package ru.yandex.tours.indexer.agencies

import java.io.ByteArrayInputStream

import ru.yandex.tours.metro.MetroHolder
import ru.yandex.tours.testkit.{BaseSpec, TestData}

import scala.collection.JavaConversions._

class XmlAgencyParserSpec extends BaseSpec with TestData {
  val tree = data.regionTree

  "Xml agency parser" should {
    "parse export without substitute phones" in {
      val parser = new XmlAgencyParser(tree, Map(1110245107l -> 123), Map.empty, new MetroHolder(Iterable.empty))
      val agencies = parser.parseInputStream(new ByteArrayInputStream(raw.getBytes))
      agencies should have size 1
      val agency = agencies.head
      agency.getPhonesCount shouldBe 1
      agency.getPhones(0) shouldBe "+7 (38822) 3-21-86"
      agency.getNameList should have size 3
      agency.getNameList.find(_.getLang == "ru").get.getValue shouldBe "Алтай-Тур"
      agency.getNameList.find(_.getLang == "en").get.getValue shouldBe "Altay-Tur"
      agency.getAddressList should have size 2
      agency.getAddressList.find(_.getLang == "ru").get.getValue shouldBe "Горно-Алтайск, ул. Улагашева, 13"
      agency.getAddressList.find(_.getLang == "en").get.getValue shouldBe "Gorno-Altaysk, ul. Ulagasheva, 13"
      agency.getGeoId shouldBe 11319
      agency.getPoint.getLatitude shouldBe 51.960718
      agency.getPoint.getLongitude shouldBe 85.957445
      agency.getId shouldBe 1110245107
      agency.getShows shouldBe 123
      agency.hasUrl shouldBe false
      agency.hasTimetable shouldBe false
    }

    "parse export with substitute phones" in {
      val parser = new XmlAgencyParser(tree, Map(1110245107l -> 123), Map(1110245107l -> "some phone"), new MetroHolder(Iterable.empty))
      val agencies = parser.parseInputStream(new ByteArrayInputStream(raw.getBytes))
      agencies should have size 1
      val agency = agencies.head
      agency.getPhonesCount shouldBe 1
      agency.getPhones(0) shouldBe "some phone"
      agency.getNameList should have size 3
      agency.getNameList.find(_.getLang == "ru").get.getValue shouldBe "Алтай-Тур"
      agency.getNameList.find(_.getLang == "en").get.getValue shouldBe "Altay-Tur"
      agency.getAddressList should have size 2
      agency.getAddressList.find(_.getLang == "ru").get.getValue shouldBe "Горно-Алтайск, ул. Улагашева, 13"
      agency.getAddressList.find(_.getLang == "en").get.getValue shouldBe "Gorno-Altaysk, ul. Ulagasheva, 13"
      agency.getGeoId shouldBe 11319
      agency.getPoint.getLatitude shouldBe 51.960718
      agency.getPoint.getLongitude shouldBe 85.957445
      agency.getId shouldBe 1110245107
      agency.getShows shouldBe 123
      agency.hasUrl shouldBe false
      agency.hasTimetable shouldBe false
    }

    "preserve phone order" in {
      val parser = new XmlAgencyParser(tree, Map(1110245107l -> 123), Map.empty, new MetroHolder(Iterable.empty))
      val xml = raw.replaceAllLiterally("</Phones>", """<Phone type="phone" hide="0">
                                                       |       <formatted>+7 (929) 269-51-31</formatted>
                                                       |       <country>7</country>
                                                       |       <prefix>929</prefix>
                                                       |       <number>2695131</number>
                                                       |     </Phone></Phones>""".stripMargin)
      val agencies = parser.parseInputStream(new ByteArrayInputStream(xml.getBytes))
      agencies should have size 1
      val agency = agencies.head
      agency.getPhonesCount shouldBe 2
      agency.getPhones(0) shouldBe "+7 (38822) 3-21-86"
      agency.getPhones(1) shouldBe "+7 (929) 269-51-31"
    }

    def ignoreTest(name: String, tag: String) = {
      s"ignore $name agencies" in {
        val parser = new XmlAgencyParser(tree, Map.empty, Map.empty, new MetroHolder(Iterable.empty))
        val xml = raw.replaceAllLiterally("<!--insert_place-->", tag)
        parser.parseInputStream(new ByteArrayInputStream(xml.getBytes)) should have size 0
      }
    }

    ignoreTest("unreliable", "<unreliable/>")
    ignoreTest("permanent closed", "<closed>permanent</closed>")
    ignoreTest("temporary closed", "<closed>temporary</closed>")
  }

  val raw =
    """
      |<Companies  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maps.yandex.ru/backa/1.x"
      |            xmlns:atom="http://www.w3.org/2005/Atom" xmlns:biz="http://maps.yandex.ru/business/1.x"
      |            xmlns:xal="urn:oasis:names:tc:ciq:xsdschema:xAL:2.0" xmlns:gml="http://www.opengis.net/gml"
      |            xsi:schemaLocation="http://maps.yandex.ru/backa/1.x ../backa.xsd
      |                http://www.opengis.net/gml ../../../ymaps/ymaps/1.x/gml.xsd
      |                urn:oasis:names:tc:ciq:xsdschema:xAL:2.0
      |                http://docs.oasis-open.org/election/external/xAL.xsd">
      |<Company id="1110245107" source="backa">
      |   <attribution>yandex</attribution>
      |   <name xml:lang="ru">Алтай-Тур</name>
      |   <name xml:lang="en">Altay-Tur</name>
      |   <name xml:lang="en">ALTAJJ-TUR OAO</name>
      |     <Phones>
      |       <Phone type="phone" hide="0">
      |         <formatted>+7 (38822) 3-21-86</formatted>
      |         <country>7</country>
      |         <prefix>38822</prefix>
      |         <number>32186</number>
      |       </Phone>
      |     </Phones>
      |   <Rubrics>
      |     <rubric ref="184106432" snippet="auto"/>
      |   </Rubrics>
      |   <Geo>
      |     <Location>
      |       <gml:pos>85.957445 51.960718</gml:pos>
      |       <gml:Envelope>
      |         <gml:lowerCorner>85.949216 51.955634</gml:lowerCorner>
      |         <gml:upperCorner>85.965674 51.965801</gml:upperCorner>
      |       </gml:Envelope>
      |       <kind>house</kind>
      |       <precision>exact</precision>
      |       <geoid>11319</geoid>
      |     </Location>
      |     <Address xmlns="http://maps.yandex.ru/geocoder/internal/1.x">
      |       <country_code>RU</country_code>
      |       <formatted xml:lang="en">Gorno-Altaysk, ul. Ulagasheva, 13</formatted>
      |       <formatted xml:lang="ru">Горно-Алтайск, ул. Улагашева, 13</formatted>
      |       <Component>
      |         <kind>country</kind>
      |         <name locale="ru">Россия</name>
      |         <name locale="en">Russia</name>
      |       </Component>
      |       <Component>
      |                <kind>province</kind>
      |         <name locale="ru">Республика Алтай</name>
      |         <name locale="en">Respublika Altay</name>
      |       </Component>
      |       <Component>
      |         <kind>locality</kind>
      |         <name locale="ru">город Горно-Алтайск</name>
      |         <name locale="en">gorod Gorno-Altaysk</name>
      |       </Component>
      |       <Component>
      |         <kind>street</kind>
      |         <name locale="ru">улица Улагашева</name>
      |         <name locale="en">ulitsa Ulagasheva</name>
      |       </Component>
      |       <Component>
      |         <kind>house</kind>
      |         <name locale="ru">13</name>
      |       </Component>
      |     </Address>
      |   </Geo>
      |   <!--insert_place-->
      | </Company>
      |</Companies>
    """.stripMargin
}
