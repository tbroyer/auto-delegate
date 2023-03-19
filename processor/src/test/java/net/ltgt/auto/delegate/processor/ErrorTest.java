/*
 * Copyright © 2023 Thomas Broyer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ltgt.auto.delegate.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

public class ErrorTest {
  @Test
  public void undefined() {
    var source =
        JavaFileObjects.forSourceString(
            "foo.bar.C",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(I.class)
            class C extends AutoDelegate_C {
              C(I i) {
                super(i);
              }
            }
            """);
    var compilation = javac().withProcessors(new AutoDelegateProcessor()).compile(source);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateUndefined]")
        .inFile(source)
        .onLine(6)
        .atColumn(1);
  }

  @Test
  public void wrongType() {
    var iface =
        JavaFileObjects.forSourceString(
            "foo.bar.II",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(I.class)
            interface II {}
            """);
    var enumeration =
        JavaFileObjects.forSourceString(
            "foo.bar.E",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(I.class)
            enum E {}
            """);
    var annotation =
        JavaFileObjects.forSourceString(
            "foo.bar.A",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(I.class)
            @interface A {}
            """);
    var record =
        JavaFileObjects.forSourceString(
            "foo.bar.R",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(I.class)
            record R() {}
            """);
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.I",
                    """
                    package foo.bar;

                    interface I {
                      void i();
                    }
                    """),
                iface,
                enumeration,
                annotation,
                record);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateWrongType]")
        .inFile(iface)
        .onLine(6)
        .atColumn(1);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateWrongType]")
        .inFile(enumeration)
        .onLine(6)
        .atColumn(1);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateWrongType]")
        .inFile(annotation)
        .onLine(6)
        .atColumn(2);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateWrongType]")
        .inFile(record)
        .onLine(6)
        .atColumn(1);
  }

  @Test
  public void superClass() {
    var source =
        JavaFileObjects.forSourceLines(
            "foo.bar.C",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(I.class)
            class C {}
            """);
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.I",
                    """
                    package foo.bar;

                    interface I {
                      void i();
                    }
                    """),
                source);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateSuperClass]")
        .inFile(source)
        .onLine(6)
        .atColumn(1);
  }

  @Test
  public void interface_() {
    var source =
        JavaFileObjects.forSourceLines(
            "foo.bar.C",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(S.class)
            class C extends AutoDelegate_C {}
            """);
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.S",
                    """
                    package foo.bar;

                    class S {}
                    """),
                source);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateInterface]")
        .inFile(source)
        .onLine(5)
        .atColumn(16);
  }

  @Test
  public void extend() {
    var source =
        JavaFileObjects.forSourceLines(
            "foo.bar.C",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            @AutoDelegate(value = I.class, extend = S.class)
            class C extends AutoDelegate_C {}
            """);
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.I",
                    """
                    package foo.bar;

                    interface I {
                      void i();
                    }
                    """),
                JavaFileObjects.forSourceString(
                    "foo.bar.S",
                    """
                    package foo.bar;

                    interface S {
                    }
                    """),
                source);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateExtend]")
        .inFile(source)
        .onLine(5)
        .atColumn(42);
  }

  @Test
  public void private_() {
    var source =
        JavaFileObjects.forSourceLines(
            "foo.bar.Enclosing",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            class Enclosing {
              @AutoDelegate(I.class)
              private static class C {}
            }
            """);
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.I",
                    """
                    package foo.bar;

                    interface I {
                      void i();
                    }
                    """),
                source);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegatePrivate]")
        .inFile(source)
        .onLine(7)
        .atColumn(18);
  }

  @Test
  public void inner() {
    var source =
        JavaFileObjects.forSourceLines(
            "foo.bar.Enclosing",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;

            class Enclosing {
              @AutoDelegate(I.class)
              class C {}
            }
            """);
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.I",
                    """
                    package foo.bar;

                    interface I {
                      void i();
                    }
                    """),
                source);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateInner]")
        .inFile(source)
        .onLine(7)
        .atColumn(3);
  }
}
