package io.geon.routemate.core.routing;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds the current routing context (Read vs Write, or specific DataSource key)
 * for the current thread using ThreadLocal.
 */
public class RoutingContext {

    private static final ThreadLocal<Deque<String>> CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    public static final String READ = "READ";
    public static final String WRITE = "WRITE";

    public static void set(String dataSourceKey) {
        if (dataSourceKey == null || dataSourceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("dataSourceKey cannot be null or empty");
        }
        CONTEXT.get().push(dataSourceKey);
    }

    public static String get() {
        Deque<String> stack = CONTEXT.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static void clear() {
        Deque<String> stack = CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        // If stack is empty after pop, remove threadlocal
        if (stack.isEmpty()) {
            CONTEXT.remove();
        }
    }

    /**
     * Returns a snapshot of the current context stack for debugging or logging.
     * 
     * @return A copy of the current stack.
     */
    public static Deque<String> getStackSnapshot() {
        return new ArrayDeque<>(CONTEXT.get());
    }

    /**
     * Helper for try-with-resources block.
     * Usage:
     * try (RoutingContext.use("READ")) {
     * // ...
     * }
     */
    public static ContextToken use(String key) {
        return new ContextToken(key);
    }

    public static class ContextToken implements AutoCloseable {
        public ContextToken(String key) {
            set(key);
        }

        @Override
        public void close() {
            clear();
        }
    }
}
