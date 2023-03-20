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
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(@Delegate(value = I.class, name = "i"))
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
        .onLine(7)
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
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(@Delegate(value = I.class, name = "i"))
            interface II {}
            """);
    var enumeration =
        JavaFileObjects.forSourceString(
            "foo.bar.E",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(@Delegate(value = I.class, name = "i"))
            enum E {}
            """);
    var annotation =
        JavaFileObjects.forSourceString(
            "foo.bar.A",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(@Delegate(value = I.class, name = "i"))
            @interface A {}
            """);
    var record =
        JavaFileObjects.forSourceString(
            "foo.bar.R",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(@Delegate(value = I.class, name = "i"))
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
        .onLine(7)
        .atColumn(1);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateWrongType]")
        .inFile(enumeration)
        .onLine(7)
        .atColumn(1);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateWrongType]")
        .inFile(annotation)
        .onLine(7)
        .atColumn(2);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateWrongType]")
        .inFile(record)
        .onLine(7)
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
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(@Delegate(value = I.class, name = "i"))
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
        .onLine(7)
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
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(@Delegate(value = S.class, name = "s"))
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
        .onLine(6)
        .atColumn(34);
  }

  @Test
  public void name() {
    var source =
        JavaFileObjects.forSourceString(
            "foo.bar.C",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate({
                @Delegate(value = I.class, name = "i.i"),
                @Delegate(value = II.class, name = "42"),
                @Delegate(value = III.class, name = "oops?")
            })
            class C extends AutoDelegate_C {
              C(I i) {
                super(i);
              }
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
                JavaFileObjects.forSourceString(
                    "foo.bar.II",
                    """
                    package foo.bar;

                    interface II {
                      void ii();
                    }
                    """),
                JavaFileObjects.forSourceString(
                    "foo.bar.III",
                    """
                    package foo.bar;

                    interface III {
                      void iii();
                    }
                    """),
                source);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateName]")
        .inFile(source)
        .onLine(7)
        .atColumn(39);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateName]")
        .inFile(source)
        .onLine(8)
        .atColumn(40);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateName]")
        .inFile(source)
        .onLine(9)
        .atColumn(41);
  }

  @Test
  public void duplicate() {
    var source =
        JavaFileObjects.forSourceString(
            "foo.bar.C",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate({
                @Delegate(value = I.class, name = "i"),
                @Delegate(value = I.class, name = "i2"),
                @Delegate(value = II.class, name = "i")
            })
            class C extends AutoDelegate_C {
              C(I i) {
                super(i);
              }
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
                JavaFileObjects.forSourceString(
                    "foo.bar.II",
                    """
                    package foo.bar;

                    interface II {
                      void ii();
                    }
                    """),
                source);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateDuplicate]")
        .inFile(source)
        .onLine(8)
        .atColumn(24);
    assertThat(compilation)
        .hadErrorContaining("[AutoDelegateDuplicate]")
        .inFile(source)
        .onLine(9)
        .atColumn(40);
  }

  @Test
  public void extend() {
    var source =
        JavaFileObjects.forSourceLines(
            "foo.bar.C",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            @AutoDelegate(value = @Delegate(value = I.class, name = "i"), extend = S.class)
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
        .onLine(6)
        .atColumn(73);
  }

  @Test
  public void private_() {
    var source =
        JavaFileObjects.forSourceLines(
            "foo.bar.Enclosing",
            """
            package foo.bar;

            import net.ltgt.auto.delegate.AutoDelegate;
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            class Enclosing {
              @AutoDelegate(@Delegate(value = I.class, name = "i"))
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
        .onLine(8)
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
            import net.ltgt.auto.delegate.AutoDelegate.Delegate;

            class Enclosing {
              @AutoDelegate(@Delegate(value = I.class, name = "i"))
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
        .onLine(8)
        .atColumn(3);
  }
}
