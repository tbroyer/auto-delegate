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

public class AutoDelegateProcessorTest {
  @Test
  public void simpleCase() {
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
                    """));
    assertThat(compilation)
        .generatedSourceFile("foo.bar.AutoDelegate_C")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceString(
                "foo.bar.AutoDelegate_C",
                """
                package foo.bar;

                import java.lang.Override;
                import javax.annotation.processing.Generated;

                @Generated("net.ltgt.auto.delegate.processor.AutoDelegateProcessor")
                abstract class AutoDelegate_C implements I {
                  private final I __I_unlikelyToConflictWithExistingMember;

                  AutoDelegate_C(I __I_unlikelyToConflictWithExistingMember) {
                    super();
                    this.__I_unlikelyToConflictWithExistingMember = __I_unlikelyToConflictWithExistingMember;
                  }

                  @Override
                  public void i() {
                    this.__I_unlikelyToConflictWithExistingMember.i();
                  }
                }
                """));
  }

  @Test
  public void targetJava8() {
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .withOptions("--release", "8")
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
                    """));
    assertThat(compilation)
        .generatedSourceFile("foo.bar.AutoDelegate_C")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceString(
                "foo.bar.AutoDelegate_C",
                """
                package foo.bar;

                import java.lang.Override;
                import javax.annotation.Generated;

                @Generated("net.ltgt.auto.delegate.processor.AutoDelegateProcessor")
                abstract class AutoDelegate_C implements I {
                  private final I __I_unlikelyToConflictWithExistingMember;

                  AutoDelegate_C(I __I_unlikelyToConflictWithExistingMember) {
                    super();
                    this.__I_unlikelyToConflictWithExistingMember = __I_unlikelyToConflictWithExistingMember;
                  }

                  @Override
                  public void i() {
                    this.__I_unlikelyToConflictWithExistingMember.i();
                  }
                }
                """));
  }

  @Test
  public void multipleInterfacesAndExtend() {
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
                    "baz.qux.I",
                    """
                    package baz.qux;

                    public interface I {
                      boolean i(int p);
                    }
                    """),
                JavaFileObjects.forSourceString(
                    "foo.bar.S",
                    """
                    package foo.bar;

                    class S {
                      S(int p1) {}
                      S(int p1, boolean p2) {}
                    }
                    """),
                JavaFileObjects.forSourceString(
                    "foo.bar.C",
                    """
                    package foo.bar;

                    import net.ltgt.auto.delegate.AutoDelegate;

                    @AutoDelegate(value = { I.class, baz.qux.I.class }, extend = S.class)
                    class C extends AutoDelegate_C {
                      C(I i, baz.qux.I i2, int p1) {
                        super(i, i2, p1);
                      }
                      C(I i, baz.qux.I i2, int p1, boolean p2) {
                        super(i, i2, p1, p2);
                      }
                    }
                    """));
    assertThat(compilation)
        .generatedSourceFile("foo.bar.AutoDelegate_C")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceString(
                "foo.bar.AutoDelegate_C",
                """
                package foo.bar;

                import java.lang.Override;
                import javax.annotation.processing.Generated;

                @Generated("net.ltgt.auto.delegate.processor.AutoDelegateProcessor")
                abstract class AutoDelegate_C extends S implements I, baz.qux.I {
                  private final I __I_unlikelyToConflictWithExistingMember;
                  private final baz.qux.I __I_unlikelyToConflictWithExistingMember_;

                  AutoDelegate_C(I __I_unlikelyToConflictWithExistingMember, baz.qux.I __I_unlikelyToConflictWithExistingMember_, int p1) {
                    super(p1);
                    this.__I_unlikelyToConflictWithExistingMember = __I_unlikelyToConflictWithExistingMember;
                    this.__I_unlikelyToConflictWithExistingMember_ = __I_unlikelyToConflictWithExistingMember_;
                  }

                  AutoDelegate_C(I __I_unlikelyToConflictWithExistingMember, baz.qux.I __I_unlikelyToConflictWithExistingMember_, int p1, boolean p2) {
                    super(p1, p2);
                    this.__I_unlikelyToConflictWithExistingMember = __I_unlikelyToConflictWithExistingMember;
                    this.__I_unlikelyToConflictWithExistingMember_ = __I_unlikelyToConflictWithExistingMember_;
                  }

                  @Override
                  public void i() {
                    this.__I_unlikelyToConflictWithExistingMember.i();
                  }

                  @Override
                  public boolean i(int p) {
                    return this.__I_unlikelyToConflictWithExistingMember_.i(p);
                  }
                }
                """));
  }

  @Test
  public void nestedClasses() {
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.Enclosing",
                    """
                    package foo.bar;

                    import net.ltgt.auto.delegate.AutoDelegate;

                    class Enclosing {
                      interface I {
                        void i();
                      }
                      static class S {
                        S(int p) {}
                      }
                      @AutoDelegate(value = I.class, extend = S.class)
                      static class C extends AutoDelegate_Enclosing_C {
                        C(I i) {
                          super(i, 42);
                        }
                      }
                    }
                    """));
    assertThat(compilation)
        .generatedSourceFile("foo.bar.AutoDelegate_Enclosing_C")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceString(
                "foo.bar.AutoDelegate_Enclosing_C",
                """
                package foo.bar;

                import java.lang.Override;
                import javax.annotation.processing.Generated;

                @Generated("net.ltgt.auto.delegate.processor.AutoDelegateProcessor")
                abstract class AutoDelegate_Enclosing_C extends Enclosing.S implements Enclosing.I {
                  private final Enclosing.I __I_unlikelyToConflictWithExistingMember;
                  AutoDelegate_Enclosing_C(Enclosing.I __I_unlikelyToConflictWithExistingMember, int p) {
                    super(p);
                    this.__I_unlikelyToConflictWithExistingMember = __I_unlikelyToConflictWithExistingMember;
                  }
                  @Override
                  public void i() {
                    this.__I_unlikelyToConflictWithExistingMember.i();
                  }
                }
                """));
  }

  @Test
  public void chaining() {
    var compilation =
        javac()
            .withProcessors(new AutoDelegateProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "foo.bar.I",
                    """
                package foo.bar;

                public interface I {
                  void i();
                }
                """),
                JavaFileObjects.forSourceString(
                    "foo.bar.C",
                    """
                package foo.bar;

                import net.ltgt.auto.delegate.AutoDelegate;

                @AutoDelegate(I.class)
                public class C extends AutoDelegate_C {
                  public C(I i) {
                    super(i);
                  }
                }
                """),
                JavaFileObjects.forSourceString(
                    "baz.qux.I",
                    """
                package baz.qux;

                interface I {
                  void i();
                }
                """),
                JavaFileObjects.forSourceString(
                    "baz.qux.C",
                    """
                package baz.qux;

                import net.ltgt.auto.delegate.AutoDelegate;

                @AutoDelegate(value = I.class, extend = foo.bar.C.class)
                class C extends AutoDelegate_C {
                  C(I i, foo.bar.I i2) {
                    super(i, i2);
                  }
                }
                """));
    assertThat(compilation).succeededWithoutWarnings();
  }
}
