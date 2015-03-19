package io.tals.flux4j.apt;

import com.google.common.collect.Lists;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.List;

import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.truth0.Truth.ASSERT;

public class DispatcherProcessorTest {

    @Test
    public void testProcess() throws Exception {

        List<JavaFileObject> sources = Lists.newArrayList(
                JavaFileObjects.forResource("io/tals/flux4j/apt/sample1/AnAction.java"),
                JavaFileObjects.forResource("io/tals/flux4j/apt/sample1/OtherStore.java"),
                JavaFileObjects.forResource("io/tals/flux4j/apt/sample1/SomeStore.java"),
                JavaFileObjects.forResource("io/tals/flux4j/apt/sample1/MyFluxDispatcher.java")
        );

        ASSERT.about(javaSources())
                .that(sources)
                .processedWith(new DispatcherProcessor())
                .compilesWithoutError()
        ;
    }
}