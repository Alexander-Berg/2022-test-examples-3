package ru.yandex.market.pricingmgmt.service

import freemarker.template.TemplateModel
import freemarker.template.TemplateScalarModel
import org.springframework.stereotype.Service
import ru.yandex.market.yql_test.YqlTestTemplateVariable

@Service
class TmpFolderYqlTestVariable : YqlTestTemplateVariable {

    override fun name(): String {
        return "tmp_folder_pragma"
    }

    override fun variable(params: Map<String, Any>): TemplateModel {
        return object : TemplateScalarModel {
            override fun  getAsString() : String {
                return ""
            }
        }
    }
}
