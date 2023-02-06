package ru.yandex.market.reporting.generator.presentation;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFTextBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author nettoyeur
 */
public class AssertUtil {
    public static <RG extends RegionCategoryGroup, T extends MarketReportData<RG>> void presentationShouldNotHaveAnyPlaceholder(T reportData, Path path, MarketReportSlideShowRenderer<RG> slideShowRenderer)
            throws IOException {
        try {
            slideShowRenderer.buildReport(reportData, path);
            try (XMLSlideShow slideShow = new XMLSlideShow(OPCPackage.open(path.toFile(), PackageAccess.READ))) {
                assertThat(textStream(slideShow).anyMatch(placeHolderPredicate()), is(false));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static Predicate<String> placeHolderPredicate() {
        Pattern azPattern = Pattern.compile("a_((?:\\w|\\.|-)+)_z");
        return ((Predicate<String>) text -> text.contains("${"))
                .or(text -> text.contains("}"))
                .or(text -> azPattern.matcher(text).lookingAt());
    }

    public static Stream<String> textStream(final XMLSlideShow slideShow) {
        return slideShow.getSlides().stream().flatMap(slide ->
        Stream.concat(slide.getShapes().stream().filter(XSLFGroupShape.class::isInstance)
                        .map(XSLFGroupShape.class::cast).flatMap(group -> group.getShapes().stream()),
                slide.getShapes().stream())
                .filter(XSLFTextBox.class::isInstance)
                .map(XSLFTextBox.class::cast)
                .map(XSLFTextBox::getText));
    }
}
