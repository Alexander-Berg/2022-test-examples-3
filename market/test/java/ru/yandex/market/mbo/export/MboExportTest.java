package ru.yandex.market.mbo.export;

import java.util.List;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.sku_transition.ExportSkuTransition;
import ru.yandex.market.mbo.tree.ExportTovarTree;

/**
 * @author amaslak
 */
public class MboExportTest {

    public static final int ID_NUMBER = 1;
    public static final int TYPE_NUMBER = 4;

    @Test
    public void testMboParametersParamValues() {
        Descriptors.Descriptor descriptor1 = MboParameters.ParameterValue.getDescriptor();
        Descriptors.Descriptor descriptor2 = ModelStorage.ParameterValue.getDescriptor();

        /*
            Ignore package name difference:
            type_name: ".Market.Mbo.Parameters."
            type_name: ".Market.Mbo.Models."
         */
        String descriptorStr1 = descriptor1.toProto().toString();
        String descriptorStr2 = descriptor2.toProto().toString();
        descriptorStr2 = descriptorStr2.replace("\".Market.Mbo.Models.", "\".Market.Mbo.Parameters.");

        Assert.assertEquals(descriptorStr1, descriptorStr2);
    }

    @Test
    public void testMboParametersLocalizedString() {
        Descriptors.Descriptor descriptor1 = MboParameters.LocalizedString.getDescriptor();
        Descriptors.Descriptor descriptor2 = ModelStorage.LocalizedString.getDescriptor();

        Assert.assertEquals(descriptor1.toProto(), descriptor2.toProto());
    }

    @Test
    public void testMboParametersModificationSource() {
        Descriptors.EnumDescriptor descriptor1 = MboParameters.ModificationSource.getDescriptor();
        Descriptors.EnumDescriptor descriptor2 = ModelStorage.ModificationSource.getDescriptor();

        Assert.assertEquals(descriptor1.toProto(), descriptor2.toProto());
    }

    /**
     * ExportTovarTree.TovarCategory must be a subset of MboParameters.Category fields.
     */
    @Test
    public void tovarTreeTest() {
        Descriptors.Descriptor categoryDescriptor = MboParameters.Category.getDescriptor();
        Descriptors.Descriptor tovarTreeCategoryDescriptor = ExportTovarTree.TovarCategory.getDescriptor();

        List<Descriptors.FieldDescriptor> fields = tovarTreeCategoryDescriptor.getFields();
        for (Descriptors.FieldDescriptor field : fields) {

            // some exceptions
            String name = field.getName();
            if (name.equals("active_experiment")) {
                name = "active_experiments";
            } else if (name.equals("show_model_type")) {
                name = "show_model_types";
            }

            Descriptors.FieldDescriptor categoryField = categoryDescriptor.findFieldByName(name);

            Assert.assertNotNull("No matching field in MboParameters.Category for ExportTovarTree.TovarCategory." +
                    name, categoryField);

            DescriptorProtos.FieldDescriptorProto expectedProto = field.toProto().toBuilder().setName(name).build();
            Assert.assertEquals(field.getFullName() + " does not match " + categoryField.getFullName(),
                    expectedProto, categoryField.toProto()
            );
        }

    }

    @Test
    public void skuTransitionTest() {
        Descriptors.Descriptor descriptor = ExportSkuTransition.SkuTransition.getDescriptor();
        Descriptors.FieldDescriptor field = descriptor.findFieldByName("id");
        Assert.assertEquals(ID_NUMBER, field.getNumber());
        Assert.assertEquals("Market.Mbo.Data.SkuTransition.id", field.getFullName());
        field = descriptor.findFieldByName("type");
        Assert.assertEquals(TYPE_NUMBER, field.getNumber());
        Assert.assertEquals("Market.Mbo.Data.SkuTransition.type", field.getFullName());
    }

}
