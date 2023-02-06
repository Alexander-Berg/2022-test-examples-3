package ru.yandex.vendor.brand;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.vendor.modeleditor.brandlines.BrandLines;
import ru.yandex.vendor.modeleditor.brandlines.BrandLinesParser;
import ru.yandex.vendor.modeleditor.model.ModelParameterValueOption;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertThat;

public class BrandLinesParserTest {

    private File globalVendorsFile;

    @Before
    public void setUp() {
        ClassLoader classLoader = getClass().getClassLoader();
        globalVendorsFile = ofNullable(classLoader.getResource("ru/yandex/vendor/brand/mbo/global.vendors.xml"))
                .map(URL::getFile)
                .map(File::new)
                .orElseThrow(() -> new AssertionError("Can't get global.vendors.xml file."));
    }

    @Test
    public void testVendorWithLinesContainsLines() {
        Long vendorWithLines = 969570L;
        parseGlobalVendorsFile(b -> {
            if (vendorWithLines.equals(b.getBrandId())) {
                List<ModelParameterValueOption> lines = b.getLines();
                assertThat("Vendor-lines are NOT present", lines, is(not(empty())));

                lines.forEach(line -> {
                    assertThat("Vendor-line ID is empty", line.getId(), is(not(nullValue())));
                    assertThat("Vendor-line NAME is empty", line.getName(), is(not(isEmptyOrNullString())));
                });
            }
        });
    }

    @Test
    public void testVendorWithoutLinesDoesNotContainLines() {
        Long vendorWithLines = 152808L;
        parseGlobalVendorsFile(b -> {
            if (vendorWithLines.equals(b.getBrandId())) {
                List<ModelParameterValueOption> lines = b.getLines();
                assertThat("Vendor-lines are present", lines, is(empty()));
            }
        });
    }

    private void parseGlobalVendorsFile(Consumer<BrandLines> consumer) {
        try (InputStream inputStream = new FileInputStream(globalVendorsFile)) {
            BrandLinesParser parser = new BrandLinesParser(consumer);
            parser.parse(inputStream);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}