package ru.yandex.market.abo.core.antifraud.v2

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudClchResult

/**
 * @author zilzilok
 */
class AntiFraudClchResultDeserializationTest {

    @Test
    fun `deserialize clch results with same_contacts feature (+ check contacts filter)`() {
        val result = mapper.readValue(GLUE_CONTACTS_CONTENT, AntiFraudClchResult::class.java)

        assertNotNull(result.glueContacts)
        assertNull(result.currentGlueContacts)
        assertNull(result.passportGlueContacts)
        assertTrue(arrayOf(GLUE_CONTACTS_PHONE, GLUE_CONTACTS_EMAIL) contentEquals result.glueContacts?.sameContacts)
    }

    @Test
    fun `deserialize clch results with current_same_ features`() {
        val result = mapper.readValue(CURRENT_GLUE_CONTACTS_CONTENT, AntiFraudClchResult::class.java)

        assertNotNull(result.currentGlueContacts)
        assertNull(result.passportGlueContacts)
        assertTrue(arrayOf(CURRENT_GLUE_CONTACTS_INN) contentEquals result.currentGlueContacts?.sameJurInfo)
        assertTrue(arrayOf(CURRENT_GLUE_CONTACTS_PHONE) contentEquals result.currentGlueContacts?.sameContacts)
    }

    @Test
    fun `deserialize clch results with passport_same_contacts and current_same_ features`() {
        val result = mapper.readValue(PASSPORT_GLUE_CONTACTS_CONTENT, AntiFraudClchResult::class.java)

        assertNotNull(result.passportGlueContacts)
        assertTrue(arrayOf(PASSPORT_GLUE_CONTACTS_PHONE) contentEquals result.passportGlueContacts?.sameContacts)
    }

    companion object {
        private val mapper = ObjectMapper()
        private const val GLUE_CONTACTS_PHONE = "+7777777777"
        private const val GLUE_CONTACTS_PUID = "123123123"
        private const val GLUE_CONTACTS_EMAIL = "123@123.ru"
        private const val GLUE_CONTACTS_CONTENT = """
        {
          "shop_id": 1,
          "clone_id": 2,
          "distance": 1,
          "features": [
            "same_contacts"
          ],
          "glue_contacts": {
            "same_contacts": ["$GLUE_CONTACTS_PHONE", "$GLUE_CONTACTS_PUID", "$GLUE_CONTACTS_EMAIL"]
          },
          "current_glue_contacts": null,
          "passport_glue_contacts": null
        }
        """
        private const val CURRENT_GLUE_CONTACTS_PHONE = "+8888888888"
        private const val CURRENT_GLUE_CONTACTS_INN = "i12321312312312"
        private const val CURRENT_GLUE_CONTACTS_CONTENT = """
        {
          "shop_id": 1,
          "clone_id": 2,
          "distance": 1,
          "features": [
            "current_glue",
            "current_same_contacts",
            "current_same_jur_info"
          ],
          "glue_contacts": {
            "same_contacts": ["$GLUE_CONTACTS_PHONE"]
          },
          "current_glue_contacts": {
            "same_contacts": ["$CURRENT_GLUE_CONTACTS_PHONE"],
            "same_jur_info": ["$CURRENT_GLUE_CONTACTS_INN"]
          },
          "passport_glue_contacts": null
        }
        """
        private const val PASSPORT_GLUE_CONTACTS_PHONE = "+9999999999"
        private const val PASSPORT_GLUE_CONTACTS_CONTENT = """
        {
          "shop_id": 1,
          "clone_id": 2,
          "distance": 1,
          "features": [
            "passport_glue",
            "current_same_jur_info",
            "passport_same_contacts"
          ],
          "glue_contacts": {
            "same_contacts": ["$GLUE_CONTACTS_PHONE"]
          },
          "current_glue_contacts": {
            "same_contacts": ["$CURRENT_GLUE_CONTACTS_PHONE"],
            "same_jur_info": ["$CURRENT_GLUE_CONTACTS_INN"]
          },
          "passport_glue_contacts": {
            "same_contacts": ["$PASSPORT_GLUE_CONTACTS_PHONE"]
          }
        }
        """
    }
}
