package just.the;

import edu.umd.cs.findbugs.annotations.NonNull;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;

/**
 * The ultimate immutable and thread-safe Cons List that implements {@link java.util.Collection}
 * and does not consume the stack.
 *
 * <p>Usage:
 *
 * <pre>
 * import static just.the.ConsList.*;
 * ...
 * ConsList&lt;String&gt; list = list(&quot;Apples&quot;, &quot;Oranges&quot;, &quot;Bananas&quot;);
 * list.forEach(fruit -> System.out.println(&quot;I like &quot; + fruit);</pre>
 *
 * @param <E> element type
 */
@Immutable
@ThreadSafe
public class ConsList<E> extends AbstractCollection<E> implements Serializable {
    private static final long serialVersionUID = -2746754218342304128L;
    private static final ConsList NIL = new ConsList<Void>(null, null);

    /**
     * Returns the empty cons list.
     *
     * The result is a singleton instance shared by all empty cons lists.
     *
     * @param <T> element type
     * @return singleton empty list
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> ConsList<T> nil() {
        return (ConsList<T>) NIL;
    }

    /**
     * Constructs a new cons list with elements <tt>head</tt> and <tt>tail</tt>.
     *
     * <p>The head, the tail elements and the resulting list have the same compile-time type.
     *
     * <p>The original list is not modified.
     *
     * @param head  first element of the new list
     * @param tail  sublist of second and consecutive elements of the new list
     * @param <V>   element type
     * @return      a cons list with the given head and tail elements
     */
    @NonNull
    public static <V> ConsList<V> cons(V head, @NonNull ConsList<V> tail) {
        return new ConsList<>(head, Objects.requireNonNull(tail, "tail is null"));
    }

    /**
     * Constructs a new cons list of element type <tt>klass</tt> with elements <tt>head</tt>
     * and <tt>tail</tt>.
     *
     * <p>The head, the tail elements and the resulting list have the same compile-time type
     * defined by the parameter <tt>klass</tt>. This type must be the common supertype
     * of both the new head element and the elements of the tail. The parameter is required
     * to overcome the deficiencies of the Java generics and type inference.
     *
     * <p>This works without the explicit type parameter:
     * <pre>ConsList&apos;Number&apos; l = cons(3.14d, cons(10, nil()));</pre>
     *
     * <p>But this requires it:
     * <pre>ConsList&apos;Integer&apos; l = cons(10, nil());
     *     ConsList&apos;Number&apos; l = cons(3.14d, i, Number.class);</pre>
     *
     * <p>The original list is not modified.
     *
     * @param head  first element of the new list
     * @param tail  sublist of second and consecutive elements of the new list
     * @param klass type-evidence parameter, unused at runtime, and only required
     *              to provide static type binding at compile-time
     * @param <V>   element type of the resulting list: base class of both <tt>head</tt>
     *              and elements of <tt>tail</tt>
     * @param <U>   element type of the new list&apos;s head
     * @return      a cons list with the given head and tail elements and of given element type
     */
    @NonNull
    @SuppressWarnings({"unchecked", "unused"})
    public static <V, U extends V> ConsList<V> cons(U head, @NonNull ConsList<? extends V> tail,
                                                    @NonNull Class<V> klass) {
        return (ConsList<V>) new ConsList(head,
            Objects.requireNonNull(tail, "tail is null"));
    }

    /**
     * Constructs a new cons list containing elements in the given order.
     *
     * <p>An invocation with an empty parameter list will produce the empty
     * list instance <tt>nil()</tt>.
     *
     * <p>With a non-empty parameter array, its first element is the head of the new cons list,
     * the rest are the parameters for constructing its tail.
     *
     * @param elements  any number of elements of the list to be constructed
     * @param <V>       element type
     * @return          the cons list with
     */
    @NonNull
    @SafeVarargs
    public static <V> ConsList<V> list(V... elements) {
        ConsList<V> cons = nil();
        for (int i = elements.length - 1; i >= 0; i--) {
            cons = cons(elements[i], cons);
        }
        return cons;
    }

    private final E head;
    private final ConsList<E> tail;

    private ConsList(E head, ConsList<E> tail) {
        this.tail = tail;
        this.head = head;
    }

    /**
     * Returns the first element of the cons list.
     *
     * <p>Throws {@link NoSuchElementException} if the list is empty.
     *
     * @return first element of the cons list
     */
    @NonNull
    public E head() {
        if (tail == null) {
            throw new NoSuchElementException();
        }
        return head;
    }

    /**
     * Returns another cons list containing the current list&apos;s elements after the first one.
     *
     * <p>Throws {@link NoSuchElementException} if the list is empty.
     *
     * @return elements of the cons list after the first one
     */
    @NonNull
    public ConsList<E> tail() {
        if (tail == null) {
            throw new NoSuchElementException();
        }
        return tail;
    }

    /**
     * Constructs a new cons list with the elements of the current one in the reverse order.
     *
     * @return a new cons list with elements in reversed order
     */
    @NonNull
    public ConsList<E> reverse() {
        ConsList<E> result = nil();
        ConsList<E> cons = this;
        while (cons.tail != null) {
            result = cons(cons.head, result);
            cons = cons.tail;
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return tail == null;
    }

    @Override
    public int size() {
        int size = nilSize();
        ConsList<E> cons = this;
        while (cons.tail != null && size != Integer.MAX_VALUE) {
            size++;
            cons = cons.tail;
        }
        return size;
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new ConsIterator<>(this);
    }

    @NonNull
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.IMMUTABLE);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsList)) {
            return false;
        }
        ConsList<?> cons = this;
        ConsList<?> otherCons = (ConsList<?>) o;
        while (cons.tail != null && otherCons.tail != null) {
            if (!Objects.equals(cons.head, otherCons.head)) {
                return false;
            }
            cons = cons.tail;
            otherCons = otherCons.tail;
        }
        return cons.tail == null && otherCons.tail == null;
    }

    @Override
    public final int hashCode() {
        int result = 1;
        ConsList<E> cons = this;
        while (cons.tail != null) {
            result = 31 * result + (cons.head == null ? 0 : cons.head.hashCode());
            cons = cons.tail;
        }
        return result;
    }

    private static final class ConsIterator<E> implements Iterator<E> {
        private ConsList<E> cons;

        private ConsIterator(ConsList<E> cons) {
            this.cons = cons;
        }

        @Override
        public boolean hasNext() {
            return cons.tail != null;
        }

        @Override
        public E next() {
            if (cons.tail == null) {
                throw new NoSuchElementException();
            }
            E next = cons.head;
            cons = cons.tail;
            return next;
        }
    }

    // Method for tests.
    int nilSize() {
        return 0;
    }
}
