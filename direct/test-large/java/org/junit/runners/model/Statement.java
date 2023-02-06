package org.junit.runners.model;

/**
 * GenericContainer run from Jupiter tests require JUnit 4.x library on runtime classpath.
 * Here's a workaround if no dependency on JUnit 4 is a must: create fake TestRule and Statement classes under your
 * test root.
 * Since GenericContainer doesn't really use them unless run by JUnit 4, this hack works fine.
 *
 * @see
 * <a href="https://github.com/testcontainers/testcontainers-java/issues/970#issuecomment-625044008">Relevant GitHub issue #970</a>
 **/
@SuppressWarnings("unused")
public class Statement {
}
