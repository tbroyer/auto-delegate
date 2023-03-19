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
package net.ltgt.auto.delegate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that <a href="https://github.com/tbroyer/auto-delegate">AutoDelegate</a> should
 * generate a superclass for the annotated class, extending the {@link #extend() given superclass},
 * and implementing the {@link #value() given interfaces}.
 *
 * <p>A simple example:
 *
 * <pre>
 *    &#064;AutoDelegate(Query.class)
 *    public class ForwardingQuery extends AutoDelegate_ForwardingQuery {
 *      protected ForwardingQuery(Query query) {
 *        super(query);
 *      }
 *    }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoDelegate {
  /**
   * The interfaces to be implemented by the generated class.
   *
   * @return The interfaces to be implemented by the generated class.
   */
  Class<?>[] value();

  /**
   * The class to be extended by the generated class.
   *
   * @return The class to be extended by the generated class.
   */
  Class<?> extend() default Object.class;
}
