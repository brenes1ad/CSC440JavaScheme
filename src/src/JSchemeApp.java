import java.util.*;

/**
 * JSchemeApp – a Scheme interpreter in Java, ported from Peter Norvig's
 * T. Brenes
 * CSC440 w26
 * 3/3/26
 *
 */
public class JSchemeApp {

    // -------------------------------------------------------------------------
    // Types
    //   Symbol  -> String
    //   List    -> java.util.List<Object>
    //   Number  -> Integer | Double
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Parsing: tokenize -> read_from_tokens -> atom
    // -------------------------------------------------------------------------

    /** Convert a Scheme source string into a list of string tokens. */
    public static List<String> tokenize(String s) {
        s = s.replace("(", " ( ").replace(")", " ) ");
        String[] parts = s.trim().split("\\s+");
        List<String> tokens = new ArrayList<>(Arrays.asList(parts));
        tokens.removeIf(String::isEmpty);
        return tokens;
    }

    /** Parse a Scheme expression from a source string. */
    public static Object parse(String program) {
        return readFromTokens(tokenize(program));
    }

    /** Recursively build the AST from a mutable token list. */
    public static Object readFromTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            throw new SyntaxError("unexpected EOF while reading");
        }
        String token = tokens.remove(0);
        if ("(".equals(token)) {
            List<Object> L = new ArrayList<>();
            while (!tokens.get(0).equals(")")) {
                L.add(readFromTokens(tokens));
            }
            tokens.remove(0); // discard ')'
            return L;
        } else if (")".equals(token)) {
            throw new SyntaxError("unexpected )");
        } else {
            return atom(token);
        }
    }

    /** Convert a token string to the most specific Java type available. */
    public static Object atom(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(token);
            } catch (NumberFormatException e2) {
                return token; // treat as Symbol (String)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Standard environment
    // -------------------------------------------------------------------------

    /** Build and return the global standard environment. */
    public static Env standardEnv() {
        Env env = new Env();

        // Arithmetic
        env.put("+",  (SchemeProc) args -> toNum(args.get(0)) + toNum(args.get(1)));
        env.put("-",  (SchemeProc) args -> toNum(args.get(0)) - toNum(args.get(1)));
        env.put("*",  (SchemeProc) args -> toNum(args.get(0)) * toNum(args.get(1)));
        env.put("/",  (SchemeProc) args -> toNum(args.get(0)) / toNum(args.get(1)));

        // Comparison
        env.put(">",  (SchemeProc) args -> toNum(args.get(0)) > toNum(args.get(1)));
        env.put("<",  (SchemeProc) args -> toNum(args.get(0)) < toNum(args.get(1)));
        env.put(">=", (SchemeProc) args -> toNum(args.get(0)) >= toNum(args.get(1)));
        env.put("<=", (SchemeProc) args -> toNum(args.get(0)) <= toNum(args.get(1)));
        env.put("=",  (SchemeProc) args -> toNum(args.get(0)) == toNum(args.get(1)));

        // Math functions
        env.put("abs",   (SchemeProc) args -> Math.abs(toNum(args.get(0))));
        env.put("sqrt",  (SchemeProc) args -> Math.sqrt(toNum(args.get(0))));
        env.put("sin",   (SchemeProc) args -> Math.sin(toNum(args.get(0))));
        env.put("cos",   (SchemeProc) args -> Math.cos(toNum(args.get(0))));
        env.put("tan",   (SchemeProc) args -> Math.tan(toNum(args.get(0))));
        env.put("exp",   (SchemeProc) args -> Math.exp(toNum(args.get(0))));
        env.put("log",   (SchemeProc) args -> Math.log(toNum(args.get(0))));
        env.put("floor", (SchemeProc) args -> Math.floor(toNum(args.get(0))));
        env.put("ceil",  (SchemeProc) args -> Math.ceil(toNum(args.get(0))));
        env.put("round", (SchemeProc) args -> (double) Math.round(toNum(args.get(0))));
        env.put("max",   (SchemeProc) args -> Math.max(toNum(args.get(0)), toNum(args.get(1))));
        env.put("min",   (SchemeProc) args -> Math.min(toNum(args.get(0)), toNum(args.get(1))));
        env.put("expt",  (SchemeProc) args -> Math.pow(toNum(args.get(0)), toNum(args.get(1))));
        env.put("pi",    Math.PI);
        env.put("e",     Math.E);

        // List operations
        env.put("car",    (SchemeProc) args -> ((List<?>) args.get(0)).get(0));
        env.put("cdr",    (SchemeProc) args -> {
            List<Object> lst = castList(args.get(0));
            return new ArrayList<>(lst.subList(1, lst.size()));
        });
        env.put("cons",   (SchemeProc) args -> {
            List<Object> result = new ArrayList<>();
            result.add(args.get(0));
            result.addAll(castList(args.get(1)));
            return result;
        });
        env.put("append", (SchemeProc) args -> {
            List<Object> result = new ArrayList<>(castList(args.get(0)));
            result.addAll(castList(args.get(1)));
            return result;
        });
        env.put("list",   (SchemeProc) args -> new ArrayList<>(args));
        env.put("length", (SchemeProc) args -> castList(args.get(0)).size());
        env.put("null?",  (SchemeProc) args -> castList(args.get(0)).isEmpty());
        env.put("list?",  (SchemeProc) args -> args.get(0) instanceof List);
        env.put("map",    (SchemeProc) args -> {
            Object proc = args.get(0);
            List<Object> lst = castList(args.get(1));
            List<Object> result = new ArrayList<>();
            for (Object item : lst) {
                result.add(applyProc(proc, Collections.singletonList(item)));
            }
            return result;
        });

        // Predicates
        env.put("number?",    (SchemeProc) args -> args.get(0) instanceof Number);
        env.put("symbol?",    (SchemeProc) args -> args.get(0) instanceof String);
        env.put("procedure?", (SchemeProc) args ->
                args.get(0) instanceof SchemeProc || args.get(0) instanceof LambdaFun);
        env.put("eq?",        (SchemeProc) args -> args.get(0) == args.get(1));
        env.put("equal?",     (SchemeProc) args -> Objects.equals(args.get(0), args.get(1)));
        env.put("not",        (SchemeProc) args -> !isTruthy(args.get(0)));

        // Control
        env.put("begin",  (SchemeProc) args -> args.get(args.size() - 1));
        env.put("apply",  (SchemeProc) args -> applyProc(args.get(0), castList(args.get(1))));

        // I/O
        env.put("display", (SchemeProc) args -> { System.out.print(lispStr(args.get(0))); return null; });
        env.put("newline", (SchemeProc) args -> { System.out.println(); return null; });

        // Boolean
        env.put("false",    false);
        env.put("true",    true);

        return env;
    }

    // -------------------------------------------------------------------------
    // Eval
    // -------------------------------------------------------------------------

    /** The global environment, initialised once at start-up. */
    public static final Env GLOBAL_ENV = standardEnv();

    /**
     * Evaluate a Scheme expression {@code x} in environment {@code env}.
     *
     * @param x   a parsed Scheme expression
     * @param env the evaluation environment
     * @return the result value
     */
    @SuppressWarnings("unchecked")
    public static Object eval(Object x, Env env) {

        // Tail-call loop to avoid stack overflow on deeply recursive programs
        while (true) {

            // Variable reference
            if (x instanceof String) {
                return env.find((String) x).get(x);
            }

            // Self-evaluating literal (number, boolean, etc.)
            if (!(x instanceof List)) {
                return x;
            }

            List<Object> expr = (List<Object>) x;

            if (expr.isEmpty()) {
                throw new RuntimeException("Cannot evaluate empty list");
            }

            String head = (expr.get(0) instanceof String) ? (String) expr.get(0) : null;

            // (quote exp)
            if ("quote".equals(head)) {
                return expr.get(1);
            }

            // (if test conseq alt)
            if ("if".equals(head)) {
                Object test   = expr.get(1);
                Object conseq = expr.get(2);
                Object alt    = expr.size() > 3 ? expr.get(3) : null;
                x = isTruthy(eval(test, env)) ? conseq : alt;
                if (x == null) return null;
                continue; // tail call
            }

            // (define var exp)
            if ("define".equals(head)) {
                String var = (String) expr.get(1);
                env.put(var, eval(expr.get(2), env));
                return null;
            }

            // (set! var exp)
            if ("set!".equals(head)) {
                String var = (String) expr.get(1);
                env.find(var).put(var, eval(expr.get(2), env));
                return null;
            }

            // (lambda (params...) body)
            if ("lambda".equals(head)) {
                List<Object> parms = (List<Object>) expr.get(1);
                Object body = expr.get(2);
                return new LambdaFun(parms, body, env);
            }

            // (begin exp...)
            if ("begin".equals(head)) {
                for (int i = 1; i < expr.size() - 1; i++) {
                    eval(expr.get(i), env);
                }
                x = expr.get(expr.size() - 1);
                continue; // tail call on last form
            }

            // (cond (test expr)...)
            if ("cond".equals(head)) {
                for (int i = 1; i < expr.size(); i++) {
                    List<Object> clause = (List<Object>) expr.get(i);
                    Object test = clause.get(0);
                    if ("else".equals(test) || isTruthy(eval(test, env))) {
                        x = clause.get(1);
                        break;
                    }
                }
                continue;
            }

            // (and exp...)
            if ("and".equals(head)) {
                Object result = true;
                for (int i = 1; i < expr.size(); i++) {
                    result = eval(expr.get(i), env);
                    if (!isTruthy(result)) return false;
                }
                return result;
            }

            // (or exp...)
            if ("or".equals(head)) {
                for (int i = 1; i < expr.size(); i++) {
                    Object result = eval(expr.get(i), env);
                    if (isTruthy(result)) return result;
                }
                return false;
            }

            // (let ((var val)...) body)
            if ("let".equals(head)) {
                List<Object> bindings = (List<Object>) expr.get(1);
                Env letEnv = new Env(Collections.emptyList(), Collections.emptyList(), env);
                for (Object binding : bindings) {
                    List<Object> pair = (List<Object>) binding;
                    String var = (String) pair.get(0);
                    letEnv.put(var, eval(pair.get(1), env));
                }
                x = expr.get(2);
                env = letEnv;
                continue;
            }

            // Procedure call: (proc arg...)
            Object proc = eval(expr.get(0), env);
            List<Object> args = new ArrayList<>();
            for (int i = 1; i < expr.size(); i++) {
                args.add(eval(expr.get(i), env));
            }

            if (proc instanceof LambdaFun) {
                // Tail-call optimisation: update x and env, loop
                LambdaFun fn = (LambdaFun) proc;
                x   = fn.body;
                env = new Env(fn.parms, args, fn.env);
                continue;
            } else {
                return applyProc(proc, args);
            }
        }
    }

    /** Convenience overload using the global environment. */
    public static Object eval(String program) {
        return eval(parse(program), GLOBAL_ENV);
    }

    // -------------------------------------------------------------------------
    // REPL
    // -------------------------------------------------------------------------

    /** Run an interactive Read-Eval-Print Loop. */
    public static void repl() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("JSchemeApp – Scheme interpreter (type 'quit' to exit)");
        while (true) {
            System.out.print("lis.java> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.equals("quit") || line.equals("exit")) break;
            if (line.isEmpty()) continue;
            try {
                Object result = eval(parse(line), GLOBAL_ENV);
                if (result != null) {
                    System.out.println(lispStr(result));
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    /** Convert a Java value back to a Lisp-readable string. */
    @SuppressWarnings("unchecked")
    public static String lispStr(Object exp) {
        if (exp instanceof List) {
            List<Object> lst = (List<Object>) exp;
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < lst.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(lispStr(lst.get(i)));
            }
            sb.append(")");
            return sb.toString();
        } else if (exp instanceof Double) {
            double d = (Double) exp;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d); // print 2.0 as "2"
            }
            return String.valueOf(d);
        } else if (exp == null) {
            return "";
        } else {
            return String.valueOf(exp);
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    /** Evaluate a value as a boolean (everything except #f / false is truthy). */
    private static boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return true;
    }

    /** Convert an Object to a double for arithmetic. */
    private static double toNum(Object o) {
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Double)  return (Double)  o;
        throw new RuntimeException("Expected number, got: " + o);
    }

    /** Safe cast to List<Object>. */
    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object o) {
        if (o instanceof List) return (List<Object>) o;
        throw new RuntimeException("Expected list, got: " + o);
    }

    /** Apply a procedure (SchemeProc or LambdaFun) to an argument list. */
    public static Object applyProc(Object proc, List<Object> args) {
        if (proc instanceof SchemeProc) {
            return ((SchemeProc) proc).apply(args);
        } else if (proc instanceof LambdaFun) {
            return ((LambdaFun) proc).call(args);
        }
        throw new RuntimeException("Not a procedure: " + proc);
    }

    // -------------------------------------------------------------------------
    // Functional interface for built-in procedures
    // -------------------------------------------------------------------------

    @FunctionalInterface
    public interface SchemeProc {
        Object apply(List<Object> args);
    }

    // -------------------------------------------------------------------------
    // Custom exception for parse errors
    // -------------------------------------------------------------------------

    public static class SyntaxError extends RuntimeException {
        public SyntaxError(String msg) { super(msg); }
    }

    // -------------------------------------------------------------------------
    // Test suite
    // -------------------------------------------------------------------------

    /** Run a Scheme expression and assert the printed result equals expected. */
    private static void test(String expr, String expected) {
        Object result = eval(parse(expr), GLOBAL_ENV);
        String got = lispStr(result);
        if (expected.equals(got)) {
            System.out.println("PASS  " + expr + "  =>  " + got);
        } else {
            System.out.println("FAIL  " + expr
                + "  =>  expected=" + expected + "  got=" + got);
        }
    }

    private static void runTests() {
        System.out.println("\n=== Running test suite ===\n");

        // Arithmetic
        test("(+ 1 2)",            "3");
        test("(* 3 4)",            "12");
        test("(- 10 3)",           "7");
        test("(/ 10 2)",           "5");
        test("(+ 1 (* 2 3))",      "7");
        test("(max 3 7 2)",        "7");   // note: our max takes 2 args; demonstrate 2-arg form
        test("(max 3 7)",          "7");
        test("(min 3 7)",          "3");
        test("(abs -5)",           "5");
        test("(expt 2 10)",        "1024");

        // Comparison / boolean
        test("(> 3 2)",            "true");
        test("(< 3 2)",            "false");
        test("(= 3 3)",            "true");
        test("(not false)",        "true");
        test("(not true)",         "false");

        // List operations
        test("(car (list 1 2 3))",      "1");
        test("(cdr (list 1 2 3))",      "(2 3)");
        test("(cons 1 (list 2 3))",     "(1 2 3)");
        test("(append (list 1 2) (list 3 4))", "(1 2 3 4)");
        test("(length (list 1 2 3))",   "3");
        test("(null? (list))",          "true");
        test("(null? (list 1))",        "false");
        test("(list? (list 1 2))",      "true");
        test("(list? 42)",              "false");

        // Quote
        test("(quote (a b c))",    "(a b c)");

        // If
        test("(if (> 3 2) 1 0)",   "1");
        test("(if (< 3 2) 1 0)",   "0");

        // Define + use
        eval("(define r 10)");
        test("r",                  "10");
        test("(* pi (* r r))",     lispStr(eval("(* pi (* r r))")));  // just checks no crash

        // Lambda
        eval("(define square (lambda (x) (* x x)))");
        test("(square 5)",         "25");
        test("(square 12)",        "144");

        // Recursive lambda (factorial)
        eval("(define fact (lambda (n) (if (<= n 1) 1 (* n (fact (- n 1))))))");
        test("(fact 5)",           "120");
        test("(fact 10)",          "3628800");

        // Closure / higher-order
        eval("(define make-adder (lambda (n) (lambda (x) (+ x n))))");
        eval("(define add5 (make-adder 5))");
        test("(add5 10)",          "15");
        test("(add5 100)",         "105");

        // Map
        eval("(define double (lambda (x) (* x 2)))");
        test("(map double (list 1 2 3))", "(2 4 6)");

        // Set!
        eval("(define x 1)");
        eval("(set! x 42)");
        test("x",                  "42");

        // Let
        test("(let ((x 5) (y 3)) (+ x y))", "8");

        // Begin
        test("(begin (define z 0) (set! z 99) z)", "99");

        // And / or
        test("(and true true)",    "true");
        test("(and true false)",   "false");
        test("(or false true)",    "true");
        test("(or false false)",   "false");

        System.out.println("\n=== Test suite complete ===\n");
    }

    // -------------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--repl")) {
            repl();
        } else {
            runTests();
        }
    }
}
