package dev.portfolio.fintrack.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.portfolio.fintrack.core.BaseTest;

/**
 * Guards the tag model CI relies on: automatic runs select "smoke" and
 * exclude "mutating", so every browser test must say whether it changes
 * application data, and nothing that does may be tagged smoke.
 */
@Tag("unit")
class TestTagPolicyTest {

    private static final String READ_ONLY = "read-only";
    private static final String MUTATING = "mutating";
    private static final String SMOKE = "smoke";

    @Test
    void everyBrowserTestDeclaresWhetherItChangesData() throws Exception {
        List<Class<?>> browserTests = browserTestClasses();

        assertFalse(browserTests.isEmpty(), "Expected to find browser test classes");

        assertAll(browserTests.stream().map(testClass -> () -> {
            Set<String> tags = classTags(testClass);

            assertTrue(
                    tags.contains(READ_ONLY) ^ tags.contains(MUTATING),
                    testClass.getSimpleName()
                            + " must carry exactly one of @Tag(\"read-only\")"
                            + " or @Tag(\"mutating\"); has " + tags);
        }));
    }

    @Test
    void smokeNeverIncludesATestThatChangesData() throws Exception {
        assertAll(browserTestClasses().stream().map(testClass -> () -> {
            Set<String> classTags = classTags(testClass);

            boolean smoke = classTags.contains(SMOKE)
                    || Arrays.stream(testClass.getDeclaredMethods())
                            .anyMatch(method -> methodTags(method).contains(SMOKE));

            if (smoke) {
                assertTrue(
                        classTags.contains(READ_ONLY),
                        testClass.getSimpleName()
                                + " is tagged smoke, so it must be read-only");
            }

            for (Method method : testClass.getDeclaredMethods()) {
                assertFalse(
                        methodTags(method).contains(MUTATING)
                                || methodTags(method).contains(READ_ONLY),
                        testClass.getSimpleName() + "." + method.getName()
                                + ": declare read-only or mutating on the class");
            }
        }));
    }

    @Test
    void stagingOnlyTestsAreNeverPartOfSmoke() throws Exception {
        assertAll(browserTestClasses().stream()
                .filter(testClass -> classTags(testClass).contains("staging-only"))
                .map(testClass -> () -> assertFalse(
                        classTags(testClass).contains(SMOKE),
                        testClass.getSimpleName()
                                + " is staging-only and must not be smoke")));
    }

    /** Concrete BaseTest subclasses compiled into this test package. */
    private static List<Class<?>> browserTestClasses()
            throws ClassNotFoundException, URISyntaxException {

        String packageName = TestTagPolicyTest.class.getPackageName();
        URL directory = TestTagPolicyTest.class.getClassLoader()
                .getResource(packageName.replace('.', '/'));

        File[] files = new File(directory.toURI()).listFiles(
                (dir, name) -> name.endsWith(".class") && !name.contains("$"));

        List<Class<?>> classes = new ArrayList<>();

        for (File file : files == null ? new File[0] : files) {
            String className = packageName + "."
                    + file.getName().replace(".class", "");
            Class<?> candidate = Class.forName(className);

            if (BaseTest.class.isAssignableFrom(candidate)
                    && !Modifier.isAbstract(candidate.getModifiers())) {
                classes.add(candidate);
            }
        }

        return classes;
    }

    private static Set<String> classTags(Class<?> testClass) {
        Set<String> tags = new HashSet<>();

        for (Tag tag : testClass.getAnnotationsByType(Tag.class)) {
            tags.add(tag.value());
        }

        return tags;
    }

    private static Set<String> methodTags(Method method) {
        Set<String> tags = new HashSet<>();

        for (Tag tag : method.getAnnotationsByType(Tag.class)) {
            tags.add(tag.value());
        }

        return tags;
    }
}
