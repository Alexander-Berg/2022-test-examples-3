package ru.yandex.direct.model.generator.old.registry;

import javax.annotation.ParametersAreNonnullByDefault;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.direct.model.generator.example.ExampleStatus;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class PropertyHolderMetaFactoryTest {

    private PropertyHolderMetaFactory factory = new PropertyHolderMetaFactory();

    private final TypeName attrType = TypeName.get(ExampleStatus.class);

    @Parameter
    public ClassName className;

    @Parameter(1)
    public String attr;

    @Parameter(2)
    public String expectedConfFullName;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return asList(
                testCase(ClassName.get("entity.user", "SomewhatUser"), "id",
                        "entity.user.prop.SomewhatUserIdPropHolder"),
                testCase(ClassName.get("banner", "SomewhatBanner"), "title",
                        "banner.prop.SomewhatBannerTitlePropHolder"),
                testCase(ClassName.get("", "BlaBlaClass"), "campaignId",
                        "prop.BlaBlaClassCampaignIdPropHolder")
        );
    }

    @Test
    public void createPropertyHolder_FullName_Positive() {
        PropertyHolderMeta propertyHolder = factory.createPropertyHolder(className, attr, attrType, false);

        assertThat(propertyHolder.getFullName()).isEqualTo(expectedConfFullName);
    }

    private static Object[] testCase(ClassName className, String attr, String expected) {
        return new Object[]{className, attr, expected};
    }

}
