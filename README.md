# Auto Delegate

Generate (as a super-class) implementations of the [delegation pattern](https://en.wikipedia.org/wiki/Delegation_pattern),
loosely modeled after [Kotlin's built-in delegation](https://kotlinlang.org/docs/delegation.html).

A class annotated with `@AutoDelegate` will have an `AutoDelegate` class generated
that will implement the given interfaces by delegating them to an instance passed to the constructor.
The generated class is meant to be the superclass of the annotated class.
A superclass of the annotated class can be given to the annotation as well.
The annotated class can freely override any of the generated methods

In the below example, the AutoDelegate annotation processor generates the `AutoDelegate_Derived` class that implements the `Base` interface by delegating to a `Base` instance passed to its constructor.

```java
interface Base {
    void print();
}

class BaseImpl implements Base {
  private final int x;

  BaseImpl(int x) {
    this.x = x;
  }

  @Override void print() {
    System.out.println(x);
  }
}

@AutoDelegate(@Delegate(value = Base.class, name = "base"))
class Derived extends AutoDelegate_Derived {
  Derived(Base b) {
    super(b);
  }
}

class Main {
  public static void main(String[] args) {
    var b = new BaseImpl(10);
    new Derived(b).print();
  }
}
```

The `AutoDelegate_Derived` generated class looks more or less like the following

```java
abstract class AutoDelegate_Derived implements Base {
  protected final Base base;

  AutoDelegate_Derived(Base base) {
    this.base = base;
  }

  @Override
  public void print() {
    this.base.print();
  }
}
```

## TODO

* Ignore non-visible constructors of the superclass
* Handle generics (generic interfaces, generic superclass, generic methods)
* Handle methods contributed by several interfaces and/or inherited from the superclass
* More user-friendly errors for actually-private nested classes (non-private classes nested in a private class)
